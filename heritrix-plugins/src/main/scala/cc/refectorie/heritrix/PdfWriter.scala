package cc.refectorie.heritrix

import cc.acs.commons.util.{ Hash => Digest }
import cc.acs.commons.util.FileOps._
import cc.acs.mongofs.gridfs.GridFS
import org.archive.modules.writer.MirrorWriterProcessor.A_MIRROR_PATH

import java.util.logging.Logger
import java.io.File
import org.archive.modules.{ Processor, CrawlURI }
import scala.collection.JavaConversions._
import Utils._
import cc.acs.commons.util.StringOps.wsv

class PdfWriter extends Processor {
  val log = Logger.getLogger(classOf[PdfWriter].getName)

  def mimeIsPsPdf(curi: CrawlURI): Boolean = curi.getContentType.contains("application/pdf") || curi.getContentType.contains("application/ps")

  val validSuffixes = wsv("ps pdf ps.gz pdf.gz ps.bz2 pdf.bz2 ps.z pdf.z")
  def extensionIsPsPdf(curi: CrawlURI): Boolean = validSuffixes.exists {e => curi.getURI.endsWith("." + e )}

  def looksLikePsPdf(curi: CrawlURI): Boolean = mimeIsPsPdf(curi) || extensionIsPsPdf(curi)

  override def shouldProcess(curi: CrawlURI): Boolean = looksLikePsPdf(curi)

  def innerProcess(curi: CrawlURI): Unit = {
    if (curi.getFetchStatus() == 200)
      writePdf(curi)
    else
      log.info("fetch status was " + curi.getFetchStatus() + " for " + curi)

    getMirrorFile(curi).foreach { f =>
      log.info("deleting file " + f.getPath)
      f.delete
    }
  }

  def getMirrorPath(curi: CrawlURI): Option[String] = 
    maybe(curi.getData().get(A_MIRROR_PATH).asInstanceOf[String])

  def getMirrorFile(curi: CrawlURI): Option[File] =
    getMirrorPath(curi) map { p => file("mirror/" + p) }

  def writePdf(curi: CrawlURI): Unit = {
    // normalize file to gzip'd, collecting 
    //   sha1 aliases along the way
    getMirrorFile(curi).foreach { f =>
      log.info("normalizing " + f.getPath)
      val shas = ShellCommands.normalizePdf(f.getPath)
      // shas contains all sha hashes for zip'd versions of file
      MongoDB.upsertPdfAliases(shas)
      log.info("writing " + f.getPath)
      MongoDB.put("mirror/" + f)
    }
  }
}

