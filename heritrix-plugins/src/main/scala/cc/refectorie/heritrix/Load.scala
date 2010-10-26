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
import java.util.logging.Level

object LoadURL  {
  val log = Logger.getLogger(getClass.getName)
  def warn = log.log(Level.WARNING, _:String)

  /** Returns the (redirected URL, content, MIME ContentType, origContent) */
  def asString(url:URL): (URL, Int, String, String) = {
    import java.io.StringWriter 
    import java.net.URL
    try {
    val connection = url.openConnection().asInstanceOf[java.net.HttpURLConnection]
    val contentType = try { 
      connection.getContentType.toLowerCase.split("[; ]+")(0) } 
      catch { case e:Exception => { warn(e.getMessage); "ContentTypeError" } }
    val redirectedURL = connection.getURL
    var origContent: String = null
    var content: String = null
    contentType match {
      case "text/html" => {
        import org.ccil.cowan.tagsoup.Parser
        import org.ccil.cowan.tagsoup.XMLWriter 
        import org.xml.sax.InputSource 
        val parser = new Parser() 
        val writer = new StringWriter() 
        parser.setContentHandler(new XMLWriter(writer))
        try {
          parser.parse(new InputSource(connection.getInputStream))
          content = writer.toString
        } catch {
          case e:Exception => { 
            warn("Failed to load "+url+": "+e.toString)
            content = "<?xml version=\"1.0\" standalone=\"yes\"?><html></html>" 
          }
        }
      }
      case "text/plain" => {
        content = inputStreamToString(connection.getInputStream)
      }
      case "application/pdf" => // took out pdfbox for now
      case _ => {
        warn("Unable to obtain text from MIME type "+contentType)
        content = ""
      }
    }
    (redirectedURL, connection.getResponseCode, content, contentType)
    } catch {
      case e:Exception => {
        warn("LoadURL error: "+e)
        (url, -1, null, null)
      }
    }
  }
  import java.io.InputStream
  private def inputStreamToString(is:InputStream, encoding:String = "UTF-8"): String = {
    import java.io.InputStreamReader
    val buffer = new Array[Char](0x10000)
    val out = new StringBuilder()
    val in = new InputStreamReader(is, encoding)
    var read = 0
    do {
      read = in.read(buffer, 0, buffer.length)
      if (read > 0) {
        out.append(buffer, 0, read)
      }
    } while (read>=0)
    out.toString
  }
  private def inputStreamToByteArray(is:InputStream): Array[Byte] = {
    val buffer = new Array[Byte](0x10000)
    val out = new java.io.ByteArrayOutputStream() 
    var read = 0
    do {
      read = is.read(buffer, 0, buffer.length)
      if (read > 0) {
        out.write(buffer, 0, read)
      }
    } while (read >= 0)
    out.toByteArray
  }
}
