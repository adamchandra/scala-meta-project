package cc.refectorie.heritrix

import cc.acs.commons.util.{Hash => Digest}
import cc.acs.commons.util.FileOps._
import cc.acs.mongofs.gridfs.GridFS
import com.mongodb.Mongo
import org.archive.modules.writer.MirrorWriterProcessor.A_MIRROR_PATH

import java.util.logging.Logger
import org.archive.modules.{ Processor, CrawlURI }
import scala.collection.JavaConversions._

class PdfWriter extends Processor {
  val log = Logger.getLogger(classOf[PdfWriter].getName)

  override def shouldProcess(curi: CrawlURI): Boolean = true

  def mimeIsPsPdf(curi: CrawlURI):Boolean = curi.getContentType=="application/pdf" || curi.getContentType=="application/ps"
  def looksLikePsPdf(curi: CrawlURI):Boolean = curi.getURI.endsWith(".ps") || curi.getURI.endsWith(".pdf")

  def innerProcess(curi: CrawlURI): Unit = {
    val mimeType = curi.getContentType() 
    // "application/x-gzip"
    if (mimeIsPsPdf(curi) || looksLikePsPdf(curi)) {
      writePdf(curi)
    }
    else {
      val path = curi.getData().get(A_MIRROR_PATH).asInstanceOf[String]
      log.info("deleting file @path=" + path)
      file("mirror/" + path).delete
    }
  }


  def maybe[T](t: T): Option[T] = if (t != null) Some(t) else None

  val dbname = "rexa.corpus"
  def collection = "pdfs"
  def mongodb = new Mongo().getDB(dbname)

  lazy val gridfs: GridFS = new GridFS(mongodb, collection)


  def writePdf(curi: CrawlURI): Unit = {
    log.info("writing " + curi)
    val path = curi.getData().get(A_MIRROR_PATH).asInstanceOf[String]
    log.info("file @path=" + path)
    put(gridfs, "mirror/" + path)
  }


  def sha1sum(is: java.io.InputStream): String = {
    Digest.toHex(Digest("sha1", is))
  }

  def sha1(f: java.io.File): String = sha1sum(fistream(f))

  def put(coll: GridFS, fname: String) {
    val ffile = file(fname)
    try {
      val fsha = sha1(ffile)
      maybe(gridfs.findOne(fsha)) match {
        case Some(f) => {
          println("duplicate file sha: " + fsha)
        }
        case None => {
          println("putting " + fname + "; sha: " + fsha)
          val gf = gridfs.createFile(fistream(fname), fsha)
          gf.save()
          // todo validate saved file
        }
      }
    } catch {
      case e: Exception => log.info(e.getClass.toString + ":" + e.getMessage)
    }
    finally{ 
      ffile.delete
    }
  }
}

