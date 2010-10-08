package mongofs

import com.mongodb._
import com.osinka.mongodb._
import com.osinka.mongodb.shape._
import org.bson.types.ObjectId


import java.io.{BufferedReader, Closeable, FilterInputStream, FilterOutputStream, InputStream, InputStreamReader, IOException, OutputStream}

/** Each method will be called in a separate thread.*/
final class ProcessIO(val writeInput: OutputStream => Unit, val processOutput: InputStream => Unit, val processError: InputStream => Unit) extends NotNull
{
	def withOutput(process: InputStream => Unit): ProcessIO = new ProcessIO(writeInput, process, processError)
	def withError(process: InputStream => Unit): ProcessIO = new ProcessIO(writeInput, processOutput, process)
	def withInput(write: OutputStream => Unit): ProcessIO = new ProcessIO(write, processOutput, processError)
}

object BasicIO {
  def ignoreOut = (i: OutputStream) => ()
  val BufferSize = 8192
  def close(c: java.io.Closeable) = try { c.close() } catch { case _: java.io.IOException => () }
  def processFully(processLine: String => Unit)(i: InputStream)
  {
    val reader = new BufferedReader(new InputStreamReader(i))
    processLinesFully(processLine)(reader.readLine)
  }
  def processLinesFully(processLine: String => Unit)(readLine: () => String)
  {
    def readFully()
    {
      val line = readLine()
      if(line != null)
      {
        processLine(line)
        readFully()
      }
    }
    readFully()
  }
  def connectToIn(o: OutputStream) { transferFully(System.in, o) }
  def input(connect: Boolean): OutputStream => Unit = if(connect) connectToIn else ignoreOut
  def standard(connectInput: Boolean): ProcessIO = standard(input(connectInput))
  def standard(in: OutputStream => Unit): ProcessIO = new ProcessIO(in, transferFully(_, System.out), transferFully(_, System.err))

  def transferFully(in: InputStream, out: OutputStream): Unit =
    try { transferFullyImpl(in, out) }
    catch { case  _: InterruptedException => () }

  private[this] def transferFullyImpl(in: InputStream, out: OutputStream)
  {
    val continueCount = 1//if(in.isInstanceOf[PipedInputStream]) 1 else 0
    val buffer = new Array[Byte](BufferSize)
    def read
    {
      val byteCount = in.read(buffer)
      if(byteCount >= continueCount)
      {
        out.write(buffer, 0, byteCount)
        out.flush()
        read
      }
    }
    read
  }
}

object Uncloseable
{
  def apply(in: InputStream): InputStream = new FilterInputStream(in) { override def close() {} }
  def apply(out: OutputStream): OutputStream = new FilterOutputStream(out) { override def close() {} }
  def protect(in: InputStream): InputStream = if(in eq System.in) Uncloseable(in) else in
  def protect(out: OutputStream): OutputStream = if( (out eq System.out) || (out eq System.err)) Uncloseable(out) else out
}

protected object runInterruptible
{
  /** Evaluates 'action' and returns it wrapped in Some.  If this thread is interrupted before 'action' completes,
  * 'destroyImpl' is called and None is returned.*/
  def apply[T](action: => T)(destroyImpl: => Unit): Option[T] =
  {
    try { Some(action) }
    catch { case _: InterruptedException => destroyImpl; None }
  }
}

object MongoScratch extends Application {


  import java.io.File
  import scala.io.Source._


  import scala.util.matching.Regex


  case class Interpolator(s:String) {
    def %(vars: Map[String, String]):String = Interpolator.interpolate(s, vars)
  }
  
  object Interpolator {
    implicit def strToInterp(s:String): Interpolator = Interpolator(s)

    def interpolate(text: String, vars: Map[String, String]) = {
      import scala.util.matching.Regex.Match
      val rep = """\$\{([^}]+)\}""".r
      rep.replaceAllIn(text, (_: Match) match { 
        case Regex.Groups(v) => vars.getOrElse(v, "") 
      })
    }
  }

  import xsbt.Process._
  import Interpolator._

  // def file_sys(fname: String) = ("file -bkzi "  + fname) !!
  // def file_sys(fname: String) = new ProcessBuilder("file","-bkzi", fname) !!
	// new BasicIO()

	// def getString(log: Option[Logger], withIn: Boolean): String = {
	// 	val buffer = new StringBuffer
	// 	val code = this ! BasicIO(buffer, log, withIn)
	// 	if(code == 0) buffer.toString else error("Nonzero exit value: " + code)
	// }
  // 
	// def !! = getString(None, false)
	// def !!(log: Logger) = getString(Some(log), false)
	// def !!< = getString(None, true)
	// def !!<(log: Logger) = getString(Some(log), true)

  import cc.acs.commons.util.Hash
  
  def sha1sum(f: File) = Hash.toHex(Hash("SHA1", f))

  def applyToFiles(dir: File):Unit = {  
    for (f <- dir.listFiles) {
      if (f.isFile) {
        println(sha1sum(f))
        // file_sys(f.getPath)
      }
      else if (f.isDirectory) {
        // applyToFiles(f)
      }
    }
  }

  def file(s:String) = new java.io.File(s)

  applyToFiles(file("."))

  def outputFileInfo(dir: String) {
    "find ${dir} -name *.jar" % Map(
      "dir" -> dir
    ) !
    // val lls = fromFile("")
  }

  // a [#&& #|| #|] b where a,b are type Process

  // a:file/command #< b:url or url #> a Use url as the input to a. a may be a File or a command.
  // a #< file or file #> a Use file as the input to a. a may be a File or a command.
  // a #> file or file #< a Write the output of a to file. a may be a File, URL, or a command.
  // a #>> file or file #<< a Append the output of a to file. a may be a File, URL, or a command. 


  // ( (new java.lang.ProcessBuilder("ls", "-l")) directory new File(System.getProperty("user.home")) ) ! 

  //rexa.corpus.{pdf.{files, meta.{redirect, urls}}, 
  //             pstotext.{files, meta}
  //             thumbs.{files, meta}
  case class RexaCorpus(i: Int) extends MongoObject
  object RexaCorpus extends MongoObjectShape[RexaCorpus] {
    override lazy val * = List()
    override def factory(dbo: DBObject): Option[RexaCorpus] = Some(RexaCorpus(0))
  }

  def mongoStuff() {
    val mongo = new Mongo()
    val db = mongo.getDB("mydb")
    val col = db.getCollection("rexa.corpus") of RexaCorpus
  }
  
  // Connect to default - localhost, 27017
  // collections 
  //  others = pngs, ocropi
  // val gridfs = GridFS(mongoConn) // creates a GridFS handle on ``fs``
  // val logo = new FileInputStream("src/test/resources/novus-logo.png")
  // gridfs(logo) { fh =>
  //   fh.filename = "novus-logo.png";
  //   fh.contentType = "image/png";
  // }

  // connect to mongo 
  // RexaCorpus
  //   create the gridfs and associated schema, with indices
  //   iterate over a set of files, 
  //     run 'file' command and 'sha1sum' on them
  //     if file is compressed, uncompress
  //     if file is ps/pdf and sha1-name doesn't exist in corpus, load it 
  //   
  // 
}
