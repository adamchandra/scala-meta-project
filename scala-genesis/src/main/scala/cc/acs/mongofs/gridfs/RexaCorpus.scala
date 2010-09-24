package cc.acs.mongofs.gridfs

class Corpus(dbname: String) {
  import com.osinka.mongodb.shape.MongoObjectShape
  import com.osinka.mongodb.MongoObject
  import com.mongodb.DBObject
  import com.mongodb._
  import org.bson.types.ObjectId
  import com.osinka.mongodb._
  import com.osinka.mongodb.shape._
  import com.osinka.mongodb.Implicits
  import java.net.URL

  def mongodb = new Mongo().getDB(dbname)
  val pdfFiles: GridFS = new GridFS(mongodb, "corpus.pdf")
  val pstotextFiles: GridFS = new GridFS(mongodb, "corpus.pstotext")
  val metataggerFiles: GridFS = new GridFS(mongodb, "corpus.metatagger")

  val pdfAliases = mongodb.getCollection("corpus.pdf.alias") of PdfAliases
  val pdfAliasColl = mongodb.getCollection("corpus.pdf.alias").asScala
  val pdfFileCollection = mongodb.getCollection("corpus.pdf.files").asScala


  case class PdfAliases(pdfId:ObjectId, urls:Seq[URL], sha1s:Seq[String]) extends MongoObject

  object PdfAliases extends MongoObjectShape[PdfAliases] {
    lazy val pdfId = Field.scalar ("pdfId", _.pdfId)
    lazy val urls  = Field.array  ("urls", _.urls)
    lazy val sha1s = Field.array  ("sha1s", _.sha1s)

    override lazy val * = List(pdfId, urls, sha1s)
    override def factory(dbo: DBObject): Option[PdfAliases] =
      for {pdfId(id)  <- Some(dbo)
           urls(s)  <- Some(dbo)
           sha1s(d) <- Some(dbo)} yield PdfAliases(id, s, d)
  }

  case class PstotextWords(pdfId:ObjectId, words:Seq[String]) extends MongoObject

  object PstotextWords extends MongoObjectShape[PstotextWords] {
    lazy val pdfId  = Field.scalar ("pdfId", _.pdfId)
    lazy val words  = Field.array  ("words", _.words)

    override lazy val * = List(pdfId, words)
    override def factory(dbo: DBObject): Option[PstotextWords] =
      for {pdfId(id)  <- Some(dbo)
           words(w) <- Some(dbo)} yield PstotextWords(id, w)
  }

  case class MetataggerWords(pdfId:ObjectId, words:Seq[String]) extends MongoObject

  object MetataggerWords extends MongoObjectShape[MetataggerWords] {
    lazy val pdfId  = Field.scalar ("pdfId", _.pdfId)
    lazy val words  = Field.array  ("words", _.words)

    override lazy val * = List(pdfId, words)
    override def factory(dbo: DBObject): Option[MetataggerWords] =
      for {pdfId(id)  <- Some(dbo)
           words(w) <- Some(dbo)} yield MetataggerWords(id, w)
  }

  def main(args: Array[String]):Unit = {
    // pdfAliases << PdfAlias(0, "nofile", "invalid-content", new java.util.Date(), List(""))
    var o = pdfAliases.find()
    while (o.hasNext()) {
      println(o.next())
    }
    
    pdfAliasColl << Map("a" -> "b")
    o = pdfAliases.find()
    while (o.hasNext()) {
      println(o.next())
    }
    pdfAliases.drop
  }


  // rexa.corpus.{pdfs(gfs),           -> id, md5, sha1
  //              pdfs.aliases,        -> pdfs.id, list(url), list(sha1)
  //              pstotext(gfs),       -> pdfs.id, ...
  //              pstotext.terms.{words}
  //              metatags(gfs),       -> pdfs.id, ...
  //              metatags.terms.{words}
  //              pngs.meta            -> pdfs.id, ghostscript command, output
  //              pngs(gfs)            -> pdfs.id, page-number(1-n)
  //              thumbs(gfs)          -> pdfs.id, page-number(1-n)
  //              ocropi(gfs)          -> pdfs.id, page-number(1-n)
  //              ocropi.terms         -> pdfs.id, page-number(1-n), bbox
  //              analysis.{pages, identified-fields(intro, abstract, author, ...)},
  //              logs.{pstotext, pdfminer},
}
