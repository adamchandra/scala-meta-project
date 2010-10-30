package cc.refectorie.heritrix

import org.scalatest._

import org.scalatest.junit.AssertionsForJUnit
import scala.collection.mutable.ListBuffer
import org.junit.Assert._
import org.junit.Test
import org.junit.Before
import org.junit.BeforeClass

class ShellCommandsTest extends FunSuite with AssertionsForJUnit  {
  import cc.acs.commons.util.StringOps._
  import cc.acs.commons.util.FileOps._
  import org.apache.commons.io.FileUtils
  
  val psfile = file("heritrix-plugins/src/test/resources/92.ps")
  
  test("os file") {
    assertTrue(ShellCommands.os_file(psfile.getPath).contains("application/postscript"))
  }

  test("gzip/unzip") {
    val tmpfile = file("tstfile") 
    FileUtils.copyFile(psfile, tmpfile)
    assertTrue(ShellCommands.os_file(psfile.getPath).contains("application/postscript"))
    ShellCommands.os_gzip(tmpfile.getPath())
    assertTrue(ShellCommands.os_file(tmpfile.getPath).contains("application/x-gzip"))
    tmpfile.delete
  }
  
  test("normalize file") {
    val tmpfile = file("tstfile") 
    FileUtils.copyFile(psfile, tmpfile)
    ShellCommands.os_gzip(tmpfile.getPath)
    val shas = ShellCommands.normalizePdf(tmpfile.getPath)
    assertTrue("zipped/unzipped version", shas.length==2)
    FileUtils.forceDelete(tmpfile)
  }
}
