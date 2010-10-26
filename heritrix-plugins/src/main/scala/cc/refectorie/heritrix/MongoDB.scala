package cc.refectorie.heritrix

import cc.acs.commons.util.FileOps._
import cc.acs.mongofs.gridfs.GridFS
import org.archive.modules.writer.MirrorWriterProcessor.A_MIRROR_PATH
import com.mongodb._
import com.osinka.mongodb._
import com.osinka.mongodb.shape._

import java.util.logging.Logger
import org.archive.modules.{ Processor, CrawlURI }
import scala.collection.JavaConversions._
import Utils._

case class PdfAliases(aliases: Seq[String]) extends MongoObject
object PdfAliases extends MongoObjectShape[PdfAliases] {
  lazy val aliases = Field.array("aliases", _.aliases)
  override lazy val * = List(aliases)
  override def factory(dbo: DBObject): Option[PdfAliases] =
    for { aliases(n) <- Some(dbo) } yield new PdfAliases(n)
}

object MongoDB {
  val log = Logger.getLogger(MongoDB.getClass.getName)

  val dbname = "rexa"
  def collection = "pdfs"
  def mongodb = new Mongo().getDB(dbname)
  val pages = mongodb.getCollection("htmlPages") of HtmlPage
  pages.ensureIndex(new BasicDBObject("url", 1), "urlIndex", true)
  pages.ensureIndex(new BasicDBObject("words", 1), "wordsIndex")

  val aliasColl = mongodb.getCollection("pdfAliases") of PdfAliases
  val aliasMC = mongodb.getCollection("pdfAliases") asScala
  lazy val gridfs: GridFS = new GridFS(mongodb, collection)

  def containsSha(sha1: String): Boolean = {
    maybe(gridfs.findOne(sha1)) match {
      case Some(f) => true
      case None => false
    }
  }

  import com.osinka.mongodb._
  import org.bson.types.ObjectId
  import com.mongodb._
  import wrapper.DBO
  import scala.collection.mutable.ListBuffer

  def upsertPdfAliases(aliases: Seq[String]): Unit = {
    var ids = Set[ObjectId]()
    var s = Set[String](aliases:_*)
    for (alias <- aliases) {
      (PdfAliases where {
        PdfAliases.aliases has alias
      } in aliasColl) map { pdfAliases =>
        s = s ++ pdfAliases.aliases
        ids = ids ++ pdfAliases.mongoOID
      }
    }

    ids map { id =>
      val q = Query(Map("_id" -> id))
      aliasColl.findAndRemove(q)
    }
    aliasColl << PdfAliases(s.toSeq)
  }

  def put(fname: String) {
    val ffile = file(fname)
    try {
      val fsha = ShellCommands.sha1(ffile)
      if (containsSha(fsha))
        println("duplicate file sha: " + fsha)
      else {
        println("putting " + fname + "; sha: " + fsha)
        val gf = gridfs.createFile(fistream(fname), fsha)
        gf.save()
      }

    } catch {
      case e: Exception => log.info(e.getClass.toString + ":" + e.getMessage)
    }
  }

  def dropDatabase: Unit = dropDatabase(dbname)

  def dropDatabase(dbn: String): Unit = {
    import com.mongodb.DB
    import com.mongodb.Mongo
    import com.mongodb.BasicDBObject
    val db = new Mongo().getDB(dbn)
    db.command(new BasicDBObject("dropDatabase", 1))
  }

}
