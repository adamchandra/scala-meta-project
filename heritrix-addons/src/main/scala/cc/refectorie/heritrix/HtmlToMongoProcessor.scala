package cc.refectorie.heritrix

import com.mongodb._
import org.bson.types.ObjectId
import com.osinka.mongodb._
import com.osinka.mongodb.shape._
import cc.acs.mongofs.gridfs.GridFS

import cc.acs.commons.util.{Hash => Digest}
import cc.acs.commons.util.FileOps._

import org.archive.modules.writer.MirrorWriterProcessor.A_MIRROR_PATH

import java.util.logging.Logger
import org.archive.modules.{ Processor, CrawlURI }
import scala.collection.JavaConversions._


class HtmlToMongoProcessor extends Processor {
  val log = Logger.getLogger(classOf[HtmlToMongoProcessor].getName)

  override def shouldProcess(curi: CrawlURI): Boolean = isHtml(curi)
  
  val dbname = "rexa.corpus"
  def collection = "pdfs"
  def db = new Mongo().getDB(dbname)
  
  lazy val gridfs: GridFS = new GridFS(db, collection)
  
  val pages = db.getCollection("htmlPages") of HtmlPage
  pages.ensureIndex(new BasicDBObject("url", 1), "urlIndex", true)
  pages.ensureIndex(new BasicDBObject("words", 1), "wordsIndex")

  def isHtml(curi: CrawlURI):Boolean = curi.getContentType.toLowerCase.trim.startsWith("text/html")
  def looksLikeRobotsTxt(curi: CrawlURI):Boolean = curi.getURI().toLowerCase.contains("robots.txt")

  def innerProcess(curi: CrawlURI): Unit = {
    if (curi.getFetchStatus() == 200 && isHtml(curi) && !looksLikeRobotsTxt(curi)) {
      extractHtmlPage(curi)
    }
  }
  import java.util.Date
  import java.io.StringWriter
  def extractHtmlPage(curi: CrawlURI):Unit = {
    import org.ccil.cowan.tagsoup.Parser
    import org.ccil.cowan.tagsoup.XMLWriter 
    import org.xml.sax.InputSource 
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
      pages << page
    } catch {
      case e:Exception => { 
        log.info("Failed to load "+curi+": "+e.toString)
      }
    }
  }

  def maybe[T](t: T): Option[T] = if (t != null) Some(t) else None

}

