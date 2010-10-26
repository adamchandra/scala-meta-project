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

case class Host(name:String) extends MongoObject
object Host extends MongoObjectShape[Host] {
  lazy val name = Field.scalar("name", _.name)
  override lazy val * = List(name)
  override def factory(dbo: DBObject): Option[Host] =
    for {name(n) <- Some(dbo)} yield new Host(n)
}

