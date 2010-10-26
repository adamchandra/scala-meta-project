package cc.refectorie.heritrix

import cc.acs.commons.util.FileOps._
import cc.acs.mongofs.gridfs.GridFS

import com.mongodb._
import com.osinka.mongodb._
import com.osinka.mongodb.shape._
import java.io.StringWriter
import java.util.Date
import java.util.logging.Logger
import org.archive.modules.{ Processor, CrawlURI }
import org.ccil.cowan.tagsoup.{XMLWriter, Parser}
import org.xml.sax.InputSource 
import scala.collection.JavaConversions._


class HtmlToMongoProcessor extends Processor {
  val log = Logger.getLogger(classOf[HtmlToMongoProcessor].getName)

  override def shouldProcess(curi: CrawlURI): Boolean = isHtml(curi)

  def isHtml(curi: CrawlURI):Boolean = curi.getContentType.toLowerCase.trim.startsWith("text/html")
  def looksLikeRobotsTxt(curi: CrawlURI):Boolean = curi.getURI().toLowerCase.contains("robots.txt")

  def innerProcess(curi: CrawlURI): Unit = {
    if (curi.getFetchStatus() == 200 && isHtml(curi) && !looksLikeRobotsTxt(curi)) {
      extractHtmlPage(curi)
    }
  }

  def extractHtmlPage(curi: CrawlURI):Unit = {
    val parser = new Parser() 
    val writer = new StringWriter() 
    parser.setContentHandler(new XMLWriter(writer))
    val recorder = curi.getRecorder
    val is = recorder.getReplayInputStream
    try {
      parser.parse(new InputSource(is))
      val content = writer.toString
      val host = curi.getUURI().getHost()
      val outlinks = curi.getOutCandidates map (_.toString)
      val page = new HtmlPage(curi.getURI, host, new Date(), curi.getContentType, 
                              content, outlinks.toList, curi.getVia().toString())
      MongoDB.pages << page
    } catch {
      case e:Exception => { 
        log.info("Failed to load "+curi+": "+e.toString)
      }
    }
  }

  def maybe[T](t: T): Option[T] = if (t != null) Some(t) else None

}

