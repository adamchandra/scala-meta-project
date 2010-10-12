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

class Exclusion(val host:String) extends SpidieLogging {
  val url = new URL("http://"+host+"/robots.txt")
  val disallow = new scala.collection.mutable.ArrayBuffer[String]
  var crawlDelay = 0
  init
  def allow(url:URL): Boolean = disallow.forall(d => !url.getPath.startsWith(d))
  protected def init: Unit = {
    try {
      val connection = url.openConnection
      // TODO Check for 404 error
      val src: Source = Source.fromInputStream(connection.getInputStream)
      for (line <- src.getLines) line match {
        case Exclusion.commentCmd(comment) => {}
        case Exclusion.userAgentCmd(pattern) => {}
        case "" => {}
        case Exclusion.disallowCmd(path) => disallow += path
        case Exclusion.crawlDelayCmd(seconds) => crawlDelay = seconds.toInt
        case s:String => error("Robots "+url+" unexpected line "+s)
      }
    } catch {
      case e:Exception => info("Error opening or parsing "+url+"\n"+e)
    }
    info("Robots "+host+" "+crawlDelay+" disallow: "+disallow.mkString(", "))
  }
}

object Exclusion {
  private val exclusion = new HashMap[String,Exclusion]
  def allow(u:URL): Boolean = exclusion.getOrElseUpdate(u.getHost, new Exclusion(u.getHost)).allow(u)
  def crawlDelay(host:String) = exclusion.getOrElseUpdate(host, new Exclusion(host)).crawlDelay
  val disallowCmd = "Disallow:\\s+([\\S]+)\\s*".r
  val crawlDelayCmd = "Crawl-delay:\\s+(\\d+)\\s*".r
  val userAgentCmd = "User-agent:\\s+(.*)".r
  val commentCmd = "#(.*)".r
}
