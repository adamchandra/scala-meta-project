package cc.refectorie.heritrix

import Utils._
import cc.acs.commons.util.{ Hash => Digest }
import cc.acs.commons.util.FileOps._
import java.io.{InputStream, File}
import java.util.logging.Logger
import org.apache.commons.exec.{PumpStreamHandler, DefaultExecutor, ExecuteWatchdog, DefaultExecuteResultHandler, CommandLine}
import cc.acs.commons.util.StringOps.wsv
import java.io.File
import java.io._
import java.nio.channels._
import org.apache.commons.io.FileUtils


object ShellCommands {
  val log = Logger.getLogger(ShellCommands.getClass.getName)

  def os_file(filename: String): String = {
    runcommand("file", 10*1000, "-bzi", filename)(0)
  }

  def uncompressor(t:String): Option[Function1[String, Unit]] = compressionType(t) match {
    case Some(n:Int) => Some(List(os_gunzip _, os_bunzip2 _, os_decompress _)(n))
    case None  => None
  }
  
  def compressionType(t:String):Option[Int] = {
    val compressTypes = wsv("x-gzip x-bzip2 x-compress")
    val ext = compressTypes.find(t.contains(_))
    val i = compressTypes.indexOf(ext)
    if (i > -1) Some(i)
    else        None
  }

  def isCompressed(filetype: String) = compressionType(filetype).isDefined

  def os_gzip(filename: String) = {
    runcommand("gzip", 10*1000, "-fq", filename)
    FileUtils.moveFile(file(filename + ".gz"), file(filename))
  }

  def os_gunzip(filename: String):Unit = {
    FileUtils.moveFile(file(filename), file(filename+".gz"))
    runcommand("gunzip", 10*1000, "-fq", filename+".gz")
  }

  def os_bunzip2(filename: String):Unit = {
    FileUtils.moveFile(file(filename), file(filename+".bz2"))
    runcommand("bunzip2", 10*1000, "-fq", filename+".bz2")
  }

  def os_decompress(filename: String):Unit = {
    FileUtils.moveFile(file(filename), file(filename+".gz"))
    runcommand("gunzip", 10*1000, "-fqd", filename+".gz")
  }

  def decompressFile(filename: String): Unit = {
    var filetype = os_file(filename)
    while (isCompressed(filetype)) {
      log.info("inflating file " + filename)
      filetype = os_file(filename)
    }
  }

  def normalizePdf(filename: String): Seq[String] = {
    val s = sha1(file(filename))
    var shas = List(s)
    var filetype = os_file(filename)
    while (isCompressed(filetype)) {
      os_gunzip(filename)
      shas = shas :+ sha1(file(filename))
      filetype = os_file(filename)
    }
    os_gzip(filename)
    shas = shas :+ sha1(file(filename))
    uniqList(shas)
  }


  def sha1sum(is: java.io.InputStream): String = Digest.toHex(Digest("sha1", is))
  def sha1(f: java.io.File): String = sha1sum(fistream(f))
  

  // hacky as hell = clean this up soon
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

}
