package cc.refectorie.heritrix

import org.scalatest._

import org.scalatest.junit.AssertionsForJUnit
import scala.collection.mutable.ListBuffer
import org.junit.Assert._
import org.junit.Test
import org.junit.Before
import org.junit.BeforeClass

class MongoDBTest extends FunSuite with AssertionsForJUnit with BeforeAndAfterAll {
  import cc.acs.commons.util.StringOps._
  import cc.acs.commons.util.FileOps._
  
  import com.osinka.mongodb.shape.ShapedCollection
  def aliasCount(sizes: Seq[Int], coll: ShapedCollection[PdfAliases]) {
    (sizes zip coll) map {
      case (s, a) => 
        assertTrue(a.aliases.length == s)
    }
  }

  test("update/insert shas/urls") {
    MongoDB.dropDatabase

    // create 1 rec w/3 aliases
    MongoDB.upsertPdfAliases(wsv("a b c"))
    aliasCount(List(3), MongoDB.aliasColl)

    // create 1 rec w/4 aliases
    MongoDB.upsertPdfAliases(wsv("c d"))
    aliasCount(List(4), MongoDB.aliasColl)

    // create 2 recs w/4, 2 aliases
    MongoDB.upsertPdfAliases(wsv("f g"))
    aliasCount(List(4, 2), MongoDB.aliasColl)

    // create 1 rec w/7 aliases
    MongoDB.upsertPdfAliases(wsv("a g"))
    aliasCount(List(6), MongoDB.aliasColl)
  }

  test("put pdfs") {
    // MongoDB.put("filename")
  }
}
