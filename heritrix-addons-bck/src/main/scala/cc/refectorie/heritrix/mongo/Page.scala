package cc.refectorie.heritrix

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
import java.util.logging.Logger

class HtmlPage(val url:String, val host:String, val date:Date, val contentType:String, val content:String, val outlinks:Seq[String], val via:String) extends MongoObject {
  val log = Logger.getLogger(classOf[HtmlPage].getName)
  
  def text: String = contentType match {
    case "text/html" => dom match { case Some(root) => (root \\ "html").text; case _ => "" }
    case _ => if (content eq null) "" else content
  }

  lazy val words:Seq[String] = Set(HtmlPage.wordLexer.findAllIn(text).map(_ toLowerCase).toList:_*).toSeq

  lazy val dom: Option[Elem] = 
    if (contentType != "text/html") None 
    else try { Some(XML.loadString(content)) } catch { case e:Exception => { error("Failed to parse "+url); None } } 

}

object HtmlPage extends MongoObjectShape[HtmlPage] {
  val wordLexer = new Regex("[a-zA-Z]+")
  lazy val host = Field.scalar("host", _.host)
  lazy val url = Field.scalar("url", _.url)
  lazy val date = Field.scalar("date", _.date)
  lazy val contentType = Field.scalar("type", _.contentType)
  lazy val content = Field.scalar("content", _.content)
  lazy val via = Field.scalar("via", _.via)
  lazy val outlinks = Field.array("outlinks", _.outlinks)
  lazy val words = Field.array("words", _.words)
  override lazy val * = List(url, host, date, contentType, content, words, outlinks, via)
  override def factory(dbo: DBObject): Option[HtmlPage] =
    for {url(u) <- Some(dbo); host(h) <- Some(dbo); date(d) <- Some(dbo); 
         contentType(t) <- Some(dbo); content(c) <- Some(dbo); 
         outlinks(out) <- Some(dbo); via(via) <- Some(dbo)
       } yield new HtmlPage(u, h, d, t, c, out, via)
}

