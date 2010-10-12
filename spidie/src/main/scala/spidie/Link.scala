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


case class Link(src:String, dst:String) extends MongoObject
object Link extends MongoObjectShape[Link] {
  lazy val src = Field.scalar("src", _.src.toString)
  lazy val dst = Field.scalar("dst", _.dst.toString)
  override lazy val * = List(src, dst)
  override def factory(dbo: DBObject): Option[Link] =
    for {val src(s) <- Some(dbo); val dst(d) <- Some(dbo)} yield new Link(s, d)
}

