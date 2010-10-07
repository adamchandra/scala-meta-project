package test.cc.acs.mongofs.gridfs

import org.scalatest.matchers.ShouldMatchers
import org.scalatest._

import org.scalatest.junit.AssertionsForJUnit
import scala.collection.mutable.ListBuffer
import org.junit.Assert._
import org.junit.Test
import org.junit.Before
import org.junit.BeforeClass
import cc.acs.util.StringOps._

class MongoTaskIterationTest extends FunSuite
with AssertionsForJUnit 
with BeforeAndAfterAll {

  import cc.acs.mongofs.gridfs._
  import org.bson.types.ObjectId
  import com.osinka.mongodb.DBObjectCollection
  import com.mongodb.DBObject
  import cc.acs.util.Hash
  import scala.collection.JavaConversions._
  import com.mongodb.Mongo

  override def beforeAll(configMap: Map[String, Any]) {
    resetTestPdfs
  }
    override def afterAll(configMap: Map[String, Any]) {
    rmTestPdfs
  }

  val dbname = "test"
  def mongodb = new Mongo().getDB(dbname)
  val corpus = new Corpus(dbname)

  val argDefaults = Map(
    "db" -> List("test"),
    "collection" -> List("pdf")
    )
  
  def rmTestPdfs = new GridFSUI(
    argDefaults + ("drop" -> List()))()

  def addTestPdfs = for (x <- "92.ps 93.ps 94.ps".wsv) {
    new GridFSUI(
      argDefaults + ("put" -> List("src/test/resources/" + x)))()
  }

  def resetTestPdfs { rmTestPdfs; addTestPdfs }

  test("pdfs count is correct") {
    val cursor = asIterable(corpus.pdfFileCollection.find())
    assertTrue("wrong # of test pdfs in corpus", cursor.size == 3)
  }

  def write(f:GridFSDBFile) = f.writeTo(f.getFilename)

  import cc.acs.util.FileOps._

  def write(d:java.io.File, f:GridFSDBFile) {
    println("writing " + file(d, f.getFilename).getPath)
    if (!d.exists)
      d.mkdir
    f.writeTo(file(d, f.getFilename).getPath)
  }


  test("iterating over gridfs objects and fetching them works") {
    val cursor = asIterable(corpus.pdfFileCollection.find())
    val testdir = file("test-workdir~")
    assertTrue(testdir.exists)
    for (c <- cursor) {
      val fn = c.get("filename").asInstanceOf[String]
      corpus.pdfFiles findOne fn match {
        case null => fail
        case f => write(testdir, f)
      }
      assertTrue(file(testdir, fn).exists)
    }
  }


//     case WorkerReady(worker) => {
//       log("worker ready signal from " + worker)
//       val actor = actorOf(worker)
//       val next = if (cursor.hasNext) Some(cursor.next) else None
//       next match {
//         case Some(task) => actor ! WorkOrder(task.get("filename").asInstanceOf[String])
//         case None => {
//           actor ! Stop
//           workerCount -= 1
//           if (workerCount == 0) {
//             log("exiting")
//             lock.release
//             exit()
//           }
//         }
//       }
//     }
// 
// collection: DBObjectCollection host:String, port: Int, 
//  val cursor = asIterator(collection.find())

  test("a master actor reads mongo files, hands them to workers") {
    val m = new Master {
      val coll = corpus.pdfFileCollection
      val cursor = coll.find()
      override def nextTaskName():Option[String] = {
        if (cursor.hasNext) 
          Some(cursor.next.get("filename").asInstanceOf[String])
        else None
      }
    }

    // m.join
  }

}
