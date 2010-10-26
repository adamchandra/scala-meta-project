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
    assertEquals("application/postscript", ShellCommands.os_file(psfile.getPath()))
  }


  test("gzip/unzip") {
    val tmpfile = file("tstfile") 
    FileUtils.copyFile(psfile, tmpfile)
    assertEquals("application/postscript", ShellCommands.os_file(psfile.getPath))
    ShellCommands.os_gzip(tmpfile.getPath())
    assertEquals("application/postscript (application/x-gzip)", ShellCommands.os_file(tmpfile.getPath))
    tmpfile.delete
  }
  
  // test("normalize file") {
  //   val tmpfile = file("tstfile") 
  //   FileUtils.copyFile(psfile, tmpfile)
  //   println("tmpfile.exists() = " + tmpfile.exists())
  //   ShellCommands.os_gzip(tmpfile.getPath)
  //   println("tmpfile.exists() = " + tmpfile.exists())
  //   val shas = ShellCommands.normalizePdf(tmpfile.getPath)
  //   println("shas:  " + shas.mkString("[", ", ", "]"))
  //   FileUtils.forceDelete(tmpfile)
  // }
}
