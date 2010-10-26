package cc.refectorie.heritrix

import java.util.logging.Logger
import org.archive.io.ReplayInputStream
import org.archive.modules.{ Processor, CrawlURI }
import scala.collection.JavaConversions._

class LinkContextIndexingProcessor extends Processor {
  val log = Logger.getLogger(classOf[LinkContextIndexingProcessor].getName)

  // override def shouldProcess(curi: CrawlURI):Boolean = isSuccess(curi)
  override def shouldProcess(curi: CrawlURI) = !curi.getOutCandidates().isEmpty

  override def innerProcess(curi: CrawlURI): Unit = {
    indexLinkContext(curi)
    val s = new java.io.StringWriter()
  }

  def indexLinkContext(curi: CrawlURI): Unit = {
    curi.getContentType() match {
      case "application/x-gzip" => {}
      case "application/pdf" | "application/ps" => {
        log.info("for pdf, found: " + curi.getData()("link-context"))
        // index(out => Map("type" -> "application/pdf", "active" -> "true") // update existing record
      }
      case mimeType => {
        val recorder = curi.getRecorder
        val is = recorder.getReplayInputStream
        var os = new java.io.ByteArrayOutputStream()
        is.readFullyTo(os)
        val html = os.toString
        val outlinks = curi.getOutCandidates()
        for (uri <- outlinks) {
          uri.getData() += "link-context" -> "Some text to index"
          // updateSearchDoc(uri)
        }
      }
    }
  }

  def updateSearchDoc(o: Any): Unit = {
    
  }
}

