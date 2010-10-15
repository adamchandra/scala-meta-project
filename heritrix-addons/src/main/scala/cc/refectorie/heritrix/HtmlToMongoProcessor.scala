package cc.refectorie.heritrix

import cc.acs.commons.util.{Hash => Digest}
import cc.acs.commons.util.FileOps._
import cc.acs.mongofs.gridfs.GridFS
import com.mongodb.Mongo
import org.archive.modules.writer.MirrorWriterProcessor.A_MIRROR_PATH

import java.util.logging.Logger
import org.archive.modules.{ Processor, CrawlURI }
import scala.collection.JavaConversions._


class HtmlToMongoProcessor extends Processor {
  val log = Logger.getLogger(classOf[HtmlToMongoProcessor].getName)

  override def shouldProcess(curi: CrawlURI): Boolean = true

  override def initialTasks():Unit = {
    val dbname = "rexa.corpus"
    def collection = "pdfs"
    def mongodb = new Mongo().getDB(dbname)
    
    lazy val gridfs: GridFS = new GridFS(mongodb, collection)

    val mongo = new Mongo()
    val db = mongo.getDB(dbName)
    val pages = db.getCollection("pages") of Page
    pages.ensureIndex(new BasicDBObject("url", 1), "urlIndex", true)
    pages.ensureIndex(new BasicDBObject("words", 1), "wordsIndex")
    // todo pages should include 'links' array
  }

  def isHtml(curi: CrawlURI):Boolean = curi.getContentType.toLower.strip.startWith("text/html")

  def innerProcess(curi: CrawlURI): Unit = {
    // put this before mirror processor, then somehow 
    //   stop the processing chain with this class
    if (isHtml(curi)) {
      // getReplay, blah, blah
      // delete file from mirror
    }

    val path = curi.getData().get(A_MIRROR_PATH).asInstanceOf[String]
    if (path != null) try {
      log.info("deleting file @path=" + path)
      file("mirror/" + path).delete
    }
    case IOException =>
    finally {
    }
  }


  def maybe[T](t: T): Option[T] = if (t != null) Some(t) else None

}

