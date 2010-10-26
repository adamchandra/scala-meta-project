package cc.refectorie.heritrix
import Utils._
import cc.acs.commons.util.{ Hash => Digest }
import cc.acs.commons.util.FileOps._
import java.io.{InputStream, File}

import java.util.logging.Logger
import org.apache.commons.exec.{PumpStreamHandler, DefaultExecutor, ExecuteWatchdog, DefaultExecuteResultHandler, CommandLine}
import scala.collection.mutable.ListBuffer
import xsbt.Process._

object ShellCommands {
  val log = Logger.getLogger(ShellCommands.getClass.getName)

  import java.io.File
  import java.io._
  import java.nio.channels._

  import org.apache.commons.io.FileUtils

  def os_file(filename: String): String = {
    runcommand("file", 10*1000, "-bzi", filename)(0)
  }

  def runcommand(cmd: String, timeoutMs: Int, args: String*):Array[String] = {
    val commandLine = CommandLine.parse(cmd)
    commandLine.addArguments(args.toArray)
    var resultHandler = new DefaultExecuteResultHandler()
    val watchdog = new ExecuteWatchdog(timeoutMs);
    val executor = new DefaultExecutor();
    var os = new java.io.ByteArrayOutputStream()
    executor.setStreamHandler(new PumpStreamHandler(os))
    executor.setWatchdog(watchdog);
    executor.execute(commandLine, resultHandler);
    val exitValue = resultHandler.waitFor();
    val results = (os.toString().split("\n") filter (_.trim.length > 0))
    results
  }


  def normalizePdf(filename: String): Seq[String] = {
    var shas = ListBuffer()
    shas :+ sha1(file(filename))
    println(shas.mkString("[", ", ", "]"))
    var filetype = os_file(filename)
    while (isCompressed(filetype)) {
      log.info("inflating file " + filename)
      os_gunzip(filename)
      shas :+ sha1(file(filename))
      println(shas.mkString("[", ", ", "]"))
      filetype = os_file(filename)
    }
    log.info("gzipping file " + filename)
    os_gzip(filename)
    shas :+ sha1(file(filename))
    println(shas.mkString("[", ", ", "]"))
    uniqList(shas)
  }

  def readFully(is: InputStream): Seq[String] = {
    val isr = new java.io.InputStreamReader(is)
    val bisr = new java.io.BufferedReader(isr)
    var output = List[String]()
    var l = bisr.readLine
    while (l != null) {
      output = output :+ l
      l = bisr.readLine
    }
    output
  }


  def os_gzip(filename: String) = {
    runcommand("gzip", 10*1000, "-fq", filename)
    FileUtils.copyFile(file(filename + ".gz"), file(filename))
    file(filename + ".gz").delete
  }

  def os_gunzip(filename: String) = {
    val tmpfile = File.createTempFile("gunz", "")
    val p = (new ProcessBuilder("gunzip", "-fqc", filename) #> tmpfile).run
    runcommand("gunzip", 10*1000, "-fq", filename)
    val r = p.exitValue()
    // file(filename).delete
    tmpfile.renameTo(file(filename))
  }

  // gz, bzip2, compress(Z)

  import cc.acs.commons.util.StringOps.wsv
  // application/postscript (application/x-bzip2)
  // application/postscript (application/x-gzip)
  // application/postscript (application/x-compress)

  val compressTypes = wsv("x-gzip x-bzip2 x-compress")

  def isCompressed(filetype: String) = compressTypes.exists(filetype.contains(_))

  def decompressFile(filename: String): Unit = {
    var filetype = os_file(filename)
    while (isCompressed(filetype)) {
      log.info("inflating file " + filename)
      os_gunzip(filename)
    }
  }

  def sha1sum(is: java.io.InputStream): String = {
    Digest.toHex(Digest("sha1", is))
  }

  def sha1(f: java.io.File): String = sha1sum(fistream(f))

  // val totextBinpath = "../text-from-pdf-projects/text-extractor/pstotext/bin/"
  // val totextPath = totextBinpath + "totext"
  // def totext_script(fname: File) = new ProcessBuilder(totextPath, "--file", fname.getPath, "--log", "totext.log", "--debug", "--nogzip").run
  // val pstotextCmd = totextBinpath + "pstotext"
  // def pstotext(infile: File, outfile:File) = (new ProcessBuilder(pstotextCmd, "-ligatures", totextBinpath+"ligatures.txt", infile.getPath) #> outfile).run
}
