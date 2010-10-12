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

class Page(val url:URL, val code:Int, val date:Date, val contentType:String, val content:String) extends MongoObject with SpidieLogging {
  def this(date:Date, x:(URL,Int,String,String)) = this(x._1, x._2, date, x._4, x._3)
  def this(url:URL) = this(new Date(), LoadURL.asString(url))
  def host = url.getHost
  def text: String = contentType match {
    case "text/html" => dom match { case Some(root) => (root \\ "html").text; case _ => "" }
    case "text/plain" => if (content eq null) "" else content
    case "application/pdf" => if (content eq null) "" else content
    case _ => if (content eq null) "" else content
  }
  lazy val words:Seq[String] = Page.wordLexer.findAllIn(text).map(_ toLowerCase).toSeq 
  lazy val dom: Option[Elem] = 
    if (contentType != "text/html") None 
    else try { Some(XML.loadString(content)) } catch { case e:Exception => { error("Failed to parse "+url); None } } 
  def links: Seq[URL] = dom match {
    case Some(dom) => (dom \\ "a").flatMap(node => (node \ "@href").firstOption match {
    //case Some(dom) => (dom \\ "a").flatMap(node => node.attribute("href") match 
      case Some(nodeseq) => {
        try { 
          //finest("Page.links1: url="+url+" extension="+nodeseq.first.toString);
          val u = Address.normalizedURL(url, nodeseq.first.toString); 
          //finest("Page.links2: "+u); 
          Some(u) 
        } 
        catch { case e:Exception => { warn("Skipping unnormalizable href "+nodeseq.first); Nil } }
      }
      case _ => { info("No href in "+node); Nil }
    }).filter(!_.toString.startsWith("mailto"))
    case _ => Nil
  }
}
object Page extends MongoObjectShape[Page] {
  val wordLexer = new Regex("[a-zA-Z]+")
  def hostFromURL(url:String) = new URL(url).getHost
  lazy val host = Field.scalar("host", _.host)
  lazy val url = Field.scalar("url", _.url.toString)
  lazy val code = Field.scalar("code", _.code)
  lazy val date = Field.scalar("date", _.date)
  lazy val contentType = Field.scalar("type", _.contentType)
  lazy val content = Field.scalar("content", _.content)
  //lazy val orig = Field.scalar("orig", _.orig)
  lazy val words = Field.array("words", _.words)
  override lazy val * = List(url, code, date, contentType, content, words)
  override def factory(dbo: DBObject): Option[Page] =
    for {url(u) <- Some(dbo); code(cd) <- Some(dbo); date(d) <- Some(dbo); contentType(t) <- Some(dbo); content(c) <- Some(dbo)} yield new Page(new URL(u), cd, d, t, c)
}

