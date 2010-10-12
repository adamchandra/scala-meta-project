package cc.refectorie.spidie

import com.mongodb._
import org.bson.types.ObjectId
import com.osinka.mongodb._
import com.osinka.mongodb.shape._
import scala.xml._
import scala.util.matching.Regex
import scala.collection.mutable.{HashMap,ArrayBuffer}
import scala.io.Source
import java.io.File
import java.net.{URLConnection, URL, HttpURLConnection}
import java.util.Date
import scala.actors.Actor
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.encryption.AccessPermission
import org.apache.pdfbox.pdmodel.encryption.StandardDecryptionMaterial
import org.apache.pdfbox.util.PDFTextStripper

object Spidie extends SpidieLogging {

  val mongo = new Mongo()
  val db = mongo.getDB("spidie")
  val addresses = db.getCollection("addresses") of Address
  val pages = db.getCollection("pages") of Page
  addresses.ensureIndex(new BasicDBObject("url", 1), "urlIndex", true)
  addresses.ensureIndex(Map("code" -> 1, "host" -> 1), "codeIndex")
  pages.ensureIndex(new BasicDBObject("url", 1), "urlIndex", true)
  pages.ensureIndex(new BasicDBObject("words", 1), "wordsIndex")

  var exiting = false
  var debug = false
  var hostRegex: Regex = ".cs.umass.edu".r

  val spiderMap = new HashMap[String,Spider] {
    override def default(host:String) = { 
      val s = new Spider(host).start.asInstanceOf[Spider]
      this(host) = s
      //s ! 10 // start recursive spidering of 10 URLs from DB each (Spider.delay * 10)
      s
    }
  }

  def spider(u:URL): Unit = spiderMap(u.getHost) ! u
  def spider(host:String, popCount:Int): Unit = spiderMap(host) ! popCount
  def spider(batchSize:Int): Unit = {
    for (address <- (Address where {(Address.code is ResponseCode.todo)} take batchSize in Spidie.addresses)) {
      info("spider batch "+address.url)
      spider(address.url)
    }
  }
  //def ensureSpider(host:String): Unit = spiderMap(host)

  def getPage(url:URL): Option[Page] = {
    (Page where {Page.url is_== url.toString} take 1 in pages).firstOption
  }
  // Return true if actually got a new page
  def addPage(url:URL): Boolean = {
    // Skip if it is already there
    if ((Page where {Page.url is url.toString} take 1 in pages).size > 0) return false
    info("Adding page "+url)
    val page = new Page(url)
    if (!exiting) {
      setAddressCode(url, page.code)
      pages << page
      for (url <- page.links; if ((hostRegex.findFirstIn(url.getHost) ne None) && Exclusion.allow(url))) {
        addAddress(url)
        spider(url)
      }
    }
    true
  }
  def setAddressCode(url:URL, code:Int): Unit = {
    addresses(Address.url is url.toString) = Address.code set code
  }
  def addAddress(url:URL): Unit = {
    if ((Address where {Address.url is url.toString} take 1 in addresses).size > 0) return
    info("Adding addr "+url)
    addresses << new Address(url)
  }

  def main(args:Array[String]): Unit = {
    if (args.length == 1) {
      val batchSize = args(0).toInt
      println("Enquing "+batchSize+" URLs.")
      spider(batchSize)
      //while (spiderMap.values.exists(_.mailboxSize > 0)) 
      //Thread.sleep(1000) // Wait for some spiders to finish
      //spiderMap.values.foreach(s => finer("Spider "+s.host+" message queue="+s.mailboxSize))
    } else {
      val startURL = new URL("http://www.cs.umass.edu/~mccallum") 
      println("Starting with "+startURL)
      spider(startURL)
      //Thread.sleep(10 * 60 * 1000)
      //spiderMap.values.foreach(s => finer("Spider "+s.host+" message queue="+s.mailboxSize))
    }
    interpreter()
  }

  def interpreter(): Unit = {
    var exit = false
    val spiderCmd = """spider\s+([\S]+)""".r
    val enqueueCmd = """enqueue\s+(\d+)""".r
    val statCmd = """stat\s+(\d+)""".r
    while (!exit) {
      print("Spidie> ")
      readLine match {
        case "exit" => exit = true
        case null => exit = true
        case "help" => {
          println("help\nspider URL\nenqueue N\nstat HOST\nstatus")
        }
        case "debug" => {
          Spidie.debug = !Spidie.debug
          println("debug = "+Spidie.debug)
        }
        case spiderCmd(urlString) => {
          spider(new URL(urlString))
        }
        case enqueueCmd(count) => {
          spider(count.toInt)
        }
        case "status" => {
          val pendingCount = (Address where {(Address.code is ResponseCode.todo)} in Spidie.addresses).size
          val doneCount = (Address where {(Address.code not_== ResponseCode.todo)} in Spidie.addresses).size
          println(" "+pendingCount+" URLs pending.  "+doneCount+" URLs done.")
        }
        case unk:String => println("Unknown command: "+unk)
      }
    }
    exiting = true
    println("Spidie exiting in 2 seconds...")
    Thread.sleep(2000)
    System.exit(0)
  }


}


