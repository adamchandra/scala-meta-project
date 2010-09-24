package spidie

import com.mongodb._
import org.bson.types.ObjectId
import com.osinka.mongodb._
import com.osinka.mongodb.shape._
import scala.xml._
import scala.util.matching.Regex
import scala.collection.mutable.HashMap
import java.io.File
import java.net.{URLConnection, URL}
import java.util.Date
import scala.actors.Actor

case class Page(url:URL, date:Date, content:String) extends MongoObject {
  def this(url:URL) = this(url, new Date(), TagSoup.loadString(url))
  def this(url:String, content:String) = this(new URL(url), new Date(), content)
  def host = url.getHost
  lazy val words:Seq[String] = dom match {
    case Some(dom) => Page.wordLexer.findAllIn((dom \\ "html").text).map(_ toLowerCase).toSeq
    case _ => Nil
  }
  lazy val dom: Option[Elem] = try { Some(XML.loadString(content)) } catch { case e:Exception => { System.err.println("Failed to parse "+url); None } }
  def links: Seq[URL] = dom match {
    case Some(dom) => (dom \\ "a").flatMap(node => (node \ "@href").firstOption match {
      //case Some(dom) => (dom \\ "a").flatMap(node => node.attribute("href") match
      case Some(nodeseq) => {
        try { val u = Address.normalizedURL(url, nodeseq.first.toString); /* println(u); */ Some(u) }
        catch { case e:Exception => { System.err.println("Skipping href "+nodeseq.first); Nil } }
      }
      case _ => { System.err.println("No href in "+node); Nil }
    })
    case _ => Nil
  }
}

object Page extends MongoObjectShape[Page] {
  val wordLexer = new Regex("[a-zA-Z]+")
  def hostFromURL(url:String) = new URL(url).getHost
  lazy val host = Field.scalar("host", _.host)
  lazy val url = Field.scalar("url", _.url.toString)
  lazy val date = Field.scalar("date", _.date)
  lazy val content = Field.scalar("content", _.content)
  lazy val words = Field.array("words", _.words)
  override lazy val * = List(url, date, content, words)
  override def factory(dbo: DBObject): Option[Page] =
    for {url(u) <- Some(dbo); date(d) <- Some(dbo); content(c) <- Some(dbo)} yield new Page(new URL(u), d, c)


  
}

case class Host(name:String) extends MongoObject
object Host extends MongoObjectShape[Host] {
  lazy val name = Field.scalar("name", _.name)
  override lazy val * = List(name)
  override def factory(dbo: DBObject): Option[Host] =
    for {name(n) <- Some(dbo)} yield new Host(n)
}

object Status extends Enumeration {
  val todo = 0
  val fetched = 1
  val avoid = 2
}
case class Address(host:String, url:URL, date:Date, status:Int) extends MongoObject {
  def this(url:URL) = this(url.getHost, url, new Date(), Status.todo)
}
object Address extends MongoObjectShape[Address] {
  def removeTrailingSlash(s:String) = if (s.last == '/') s.substring(0, s.length-1) else s
  def normalizedURL(context:URL, str:String): URL = new URL(removeTrailingSlash(new URL(context, str).toURI.normalize.toString))
  lazy val host = Field.scalar("host", _.host)
  lazy val url = Field.scalar("url", _.url.toString)
  lazy val date = Field.scalar("date", _.date)
  //var pages: MongoCollection[Page] = null
  //lazy val page = Field.scalar("page", pages, (u:Address)=> u.page)
  lazy val status = Field.scalar("status", _.status)
  override lazy val * = List(host, url, date, status)
  override def factory(dbo: DBObject): Option[Address] =
    for {val host(h) <- Some(dbo); val url(u) <- Some(dbo); val date(d) <- Some(dbo); val status(st) <- Some(dbo)} yield
      new Address(h, new URL(u), d, st)
}


case class Link(src:String, dst:String) extends MongoObject
object Link extends MongoObjectShape[Link] {
  lazy val src = Field.scalar("src", _.src.toString)
  lazy val dst = Field.scalar("dst", _.dst.toString)
  override lazy val * = List(src, dst)
  override def factory(dbo: DBObject): Option[Link] =
    for {val src(s) <- Some(dbo); val dst(d) <- Some(dbo)} yield new Link(s, d)
}

class Spider(val host:String) extends Actor {
  println("Creating Spider for "+host)
  val delay = if (host.contains("cs.umass.edu")) 0 else if (host.contains("umass.edu")) 100 else 10000 // milliseconds
  val batchSize = 10
  var stop = false
  var emptyQueueCount = 0
  def act() = loop {
    react {
      case url:URL => {
        Spider.addPage(url)
        println("Spider "+host+" thread="+currentThread.getId+" message queue = "+mailboxSize)
        Thread.sleep(delay)
      }
      case "start" => if (stop == true) {
        stop = false
        for (address <- (Address where {(Address.status is Status.todo) and (Address.host is host)} take batchSize in Spider.addresses)) {
          Spider.addPage(address.url)
          Thread.sleep(delay)
        }
        if (mailboxSize == 0) { emptyQueueCount += 1; Thread.sleep(delay * 10 * emptyQueueCount) } else emptyQueueCount = 0
        if (!stop) this ! "start"
      }
      //case "kick"
      case "stop" => stop = true
      case x => System.err.println("Spider got unknown message "+x)
    }
  }
}

object Spider {
  val spiderMap = new HashMap[String,Spider] {
    override def default(host:String) = {
      val s = new Spider(host).start.asInstanceOf[Spider]
      this(host) = s
      //s ! 10 /* start recursive spidering of 10 URLs from DB each (Spider.delay * 10) */
      s
    }
  }
  def spider(u:URL): Unit = spiderMap(u.getHost) ! u
  def spider(host:String, popCount:Int): Unit = spiderMap(host) ! popCount
  def ensureSpider(host:String): Unit = spiderMap(host)
  val mongo = new Mongo()
  val db = mongo.getDB("spidie")
  val addresses = db.getCollection("addresses") of Address
  val pages = db.getCollection("pages") of Page
  def getPage(url:URL): Option[Page] = {
    (Page where {Page.url is_== url.toString} take 1 in pages).firstOption
  }
  def addPage(url:URL): Unit = {
    // Skip if it is already there
    if ((Page where {Page.url is url.toString} take 1 in pages).size > 0) return
    println("Adding page "+url)
    val page = new Page(url)
    pages << page
    setAddressStatus(url, Status.fetched)
    for (url <- page.links) {
      addAddress(url)
      spider(url)
    }
  }
  def setAddressStatus(url:URL, status:Int): Unit = {
    addresses(Address.url is url.toString) = Address.status set status
  }
  def addAddress(url:URL): Unit = {
    if ((Address where {Address.url is url.toString} take 1 in addresses).size > 0) return
    println("Adding addr "+url)
    addresses << new Address(url)
  }
  def main(args:Array[String]): Unit = {
    // Ensure unique indices are in place
    addresses.ensureIndex(new BasicDBObject("url", 1), "urlIndex", true)
    addresses.ensureIndex(Map("status" -> 1, "host" -> 1), "statusIndex")
    pages.ensureIndex(new BasicDBObject("url", 1), "urlIndex", true)
    pages.ensureIndex(new BasicDBObject("words", 1), "wordsIndex")
    // Do work
    val startURL = new URL("http://www.cs.umass.edu")
    spider(startURL)
    //println("\nGot Homepage\n")
    /*val numRounds = 10
     for (round <- 1 to numRounds; address <- (Address where {(Address.status is Status.todo) and (Address.host is "www.cs.umass.edu")} take 10 in addresses)) {
     //println("Trying page "+address.url)
     addPage(address.url)
     }*/
    Thread.sleep(60 * 1000)
    spiderMap.values.foreach(s => println("Spider "+s.host+" message queue="+s.mailboxSize))
    System.exit(0)
  }
}

object TagSoup {
  def loadString(url:URL): String = {
    import org.ccil.cowan.tagsoup.Parser
    import org.ccil.cowan.tagsoup.XMLWriter
    import java.io.StringWriter
    import java.net.URL
    import org.xml.sax.InputSource
    val parser = new Parser()
    val writer = new StringWriter()
    parser.setContentHandler(new XMLWriter(writer))
    try {
      parser.parse(new InputSource(url.openConnection().getInputStream()))
      writer.toString
    } catch {
      case e:Exception => {
        System.err.println("Failed to load "+url)
        "<?xml version=\"1.0\" standalone=\"yes\"?><html></html>"
      }
    }
  }
  def loadNode(url:URL): Node = XML.loadString(loadString(url))
}
