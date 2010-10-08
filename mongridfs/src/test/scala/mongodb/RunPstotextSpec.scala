package test.cc.acs.mongofs.gridfs

import org.scalatest._

import org.scalatest.junit.AssertionsForJUnit
import scala.collection.mutable.ListBuffer
import org.junit.Assert._
import org.junit.Test
import org.junit.Before
import org.junit.BeforeClass

// todo rename this or break it up into other specs
class RunPstotextSpec extends FunSuite with AssertionsForJUnit with BeforeAndAfterAll {
  import cc.acs.commons.util.StringOps._
  import cc.acs.commons.util.FileOps._

  test("run pstotext on file using fs") {
    import xsbt.Process._
    import java.io.File
  
    val totextBinpath = "../text-from-pdf-projects/text-extractor/pstotext/bin/"
    val totextPath = totextBinpath + "totext"
    def totext_script(fname: File) = new ProcessBuilder(totextPath, "--file", fname.getPath, "--log", "totext.log", "--debug", "--nogzip").run

    val pstotextCmd = totextBinpath + "pstotext"

    def pstotext(infile: File, outfile:File) = (new ProcessBuilder(pstotextCmd, "-ligatures", totextBinpath+"ligatures.txt", infile.getPath) #> outfile).run

    val testfile = file("src/test/resources/92.ps")
    pstotext(testfile, file("92.pstotext.xml"))
  }

  test("fetch a file, run the gamut, put results") {
    // create a master/worker pair
    // worker should 
  }


  test("search for a file by pstotext terms") {
    
  }

}
