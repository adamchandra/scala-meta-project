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


object ResponseCode {
  val todo = 0
}
class Address(val host:String, val url:URL, val rurl:Option[URL], val date:Date, val code:Int) extends MongoObject {
  def this(url:URL) = this(url.getHost, url, Address.someRURL(url), new Date(), ResponseCode.todo)
}
object Address extends MongoObjectShape[Address] {
  def someRURL(u:URL): Option[URL] = {
    val rurl = u.openConnection.asInstanceOf[HttpURLConnection].getURL
    if (rurl != u) Some(rurl) else None
  }
  def removeTrailingSlash(s:String) = if (s.last == '/') s.substring(0, s.length-1) else s
  def normalizedURL(context:URL, str:String): URL = new URL(removeTrailingSlash(dropAfterHash(new URL(context, str).toURI.normalize.toString)))
  def dropAfterHash(s:String): String = { val i = s.indexOf('#'); if (i == -1) s else s.substring(0,i) }
  lazy val host = Field.scalar("host", _.host)
  lazy val url = Field.scalar("url", _.url.toString)
  lazy val rurl = Field.optional("rurl", (a:Address) => if (a.rurl == None) None else if (a.rurl != a.url) Some(a.rurl.toString) else None)
  lazy val date = Field.scalar("date", _.date)
  //var pages: MongoCollection[Page] = null
  //lazy val page = Field.scalar("page", pages, (u:Address)=> u.page)
  lazy val code = Field.scalar("code", _.code)
  override lazy val * = List(host, url, date, code)
  override def factory(dbo: DBObject): Option[Address] =
    for {val host(h) <- Some(dbo); val url(u) <- Some(dbo); val date(d) <- Some(dbo); val code(st) <- Some(dbo)} yield 
      new Address(h, new URL(u), None, d, st)
}


