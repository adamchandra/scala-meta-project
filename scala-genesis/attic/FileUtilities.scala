/* sbt -- Simple Build Tool
 * Copyright 2008, 2009, 2010 Mark Harrah, Nathan Hamblen, Justin Caballero
 */
package util

import Function.tupled
import OpenResource._
import java.io.{ByteArrayOutputStream, BufferedReader, File, FileInputStream, InputStream}
import java.io.{OutputStream, FileOutputStream, BufferedWriter, FileReader, FileWriter, Reader, Writer, InputStreamReader, OutputStreamWriter}
import java.io.{Closeable}
import java.net.{URI, URISyntaxException, URL}
import java.nio.channels.FileChannel
import java.nio.charset.{Charset, CharsetDecoder, CharsetEncoder}
import java.util.jar.{Attributes, JarEntry, JarFile, JarInputStream, JarOutputStream, Manifest}
import java.util.zip.{GZIPInputStream, GZIPOutputStream, ZipEntry, ZipFile, ZipInputStream, ZipOutputStream}
import scala.collection.mutable.HashSet
import scala.reflect.{Manifest => SManifest}
import util.ErrorHandling.translate
import util.log._

final class Preserved private[util](toRestore: scala.collection.Map[File, Path], temp: File) extends NotNull {
	def restore(log: Logger) = { try {
		Control.lazyFold(toRestore.toList) { 
      case (src, dest) =>
			  FileUtilities.copyFile(src, dest.asFile, log)
    }
   // finally { FileUtilities.clean(Path.fromFile(temp) :: Nil, true, log) }
	}
                            }
}

object FileHandlers {
	/** The maximum number of times a unique temporary filename is attempted to be created.*/
	private val MaximumTries = 10
	/** The producer of randomness for unique name generation.*/
	private lazy val random = new java.util.Random
	val temporaryDirectory = new File(System.getProperty("java.io.tmpdir"))
	/** The size of the byte or char buffer used in various methods.*/
	private val BufferSize = 8192
	private val Newline = System.getProperty("line.separator")

	def utf8 = Charset.forName("UTF-8")

	def classLocation(cl: Class[_]): URL =
	{
		val codeSource = cl.getProtectionDomain.getCodeSource
		if(codeSource == null) error("No class location for " + cl)
		else codeSource.getLocation
	}
	def classLocationFile(cl: Class[_]): File = toFile(classLocation(cl))
	def classLocation[T](implicit mf: SManifest[T]): URL = classLocation(mf.erasure)
	def classLocationFile[T](implicit mf: SManifest[T]): File = classLocationFile(mf.erasure)

	def toFile(url: URL) =
		try { new File(url.toURI) }
		catch { case _: URISyntaxException => new File(url.getPath) }

	/** Converts the given URL to a File.  If the URL is for an entry in a jar, the File for the jar is returned. */
	def asFile(url: URL): File =
	{
		url.getProtocol match
		{
			case "file" => toFile(url)
			case "jar" =>
				val path = url.getPath
				val end = path.indexOf('!')
				new File(new URI(if(end == -1) path else path.substring(0, end)))
			case _ => error("Invalid protocol " + url.getProtocol)
		}
	}
	def assertDirectory(file: File) { assert(file.isDirectory, (if(file.exists) "Not a directory: " else "Directory not found: ") + file) }
	def assertDirectories(file: File*) { file.foreach(assertDirectory) }

	// "base.extension" -> (base, extension)
	def split(name: String): (String, String) =
	{
		val lastDot = name.lastIndexOf('.')
		if(lastDot >= 0)
			(name.substring(0, lastDot), name.substring(lastDot+1))
		else
			(name, "")
	}

	def touch(files: Iterable[File]): Unit = files.foreach(touch)
	/** Creates a file at the given location.*/
	def touch(file: File)
	{
		createDirectory(file.getParentFile)
		val created = translate("Could not create file " + file) { file.createNewFile() }
		if(created || file.isDirectory)
			()
		else if(!file.setLastModified(System.currentTimeMillis))
			error("Could not update last modified time for file " + file)
	}
	def createDirectories(dirs: Iterable[File]): Unit =
		dirs.foreach(createDirectory)
	def createDirectory(dir: File): Unit =
	{
		def failBase = "Could not create directory " + dir
		if(dir.isDirectory || dir.mkdirs())
			()
		else if(dir.exists)
			error(failBase + ": file exists and is not a directory.")
		else
			error(failBase)
	}
	
  // def unzip(from: File, toDirectory: File): Set[File] = unzip(from, toDirectory, AllPassFilter)
	// def unzip(from: File, toDirectory: File, filter: NameFilter): Set[File] = fileInputStream(from)(in => unzip(in, toDirectory, filter))
	// def unzip(from: InputStream, toDirectory: File, filter: NameFilter): Set[File] =
	// {
	// 	createDirectory(toDirectory)
	// 	zipInputStream(from) { zipInput => extract(zipInput, toDirectory, filter) }
	// }
  import util.NameFilter

	private def extract(from: ZipInputStream, toDirectory: File, filter: NameFilter) =
	{
		val set = new HashSet[File]
		def next()
		{
			val entry = from.getNextEntry
			if(entry == null)
				()
			else
			{
				val name = entry.getName
				if(filter.accept(name))
				{
					val target = new File(toDirectory, name)
					//log.debug("Extracting zip entry '" + name + "' to '" + target + "'")
					if(entry.isDirectory)
						createDirectory(target)
					else
					{
						set += target
						translate("Error extracting zip entry '" + name + "' to '" + target + "': ") {
						  fileOutputStream(false).io(target, "", new ConsoleLogger) { out => transfer(from, out); Right("") }
						}
					}
					//target.setLastModified(entry.getTime)
				}
				else
				{
					//log.debug("Ignoring zip entry '" + name + "'")
				}
				from.closeEntry()
				next()
			}
		}
		next()
		Set() ++ set
	}

	/** Copies all bytes from the given input stream to the given output stream.
	* Neither stream is closed.*/
	def transfer(in: InputStream, out: OutputStream): Unit = transferImpl(in, out, false)
	/** Copies all bytes from the given input stream to the given output stream.  The
	* input stream is closed after the method completes.*/
	def transferAndClose(in: InputStream, out: OutputStream): Unit = transferImpl(in, out, true)
	private def transferImpl(in: InputStream, out: OutputStream, close: Boolean)
	{
		try
		{
			val buffer = new Array[Byte](BufferSize)
			def read()
			{
				val byteCount = in.read(buffer)
				if(byteCount >= 0)
				{
					out.write(buffer, 0, byteCount)
					read()
				}
			}
			read()
		}
		finally { if(close) in.close }
	}

	/** Creates a temporary directory and provides its location to the given function.  The directory
	* is deleted after the function returns.*/
	// def withTemporaryDirectory[T](action: File => T): T =	{
	// 	val dir = createTemporaryDirectory
	// 	try { action(dir) }
	// 	finally { delete(dir) }
	// }

	def createTemporaryDirectory: File =
	{
		def create(tries: Int): File =
		{
			if(tries > MaximumTries)
				error("Could not create temporary directory.")
			else
			{
				val randomName = "sbt_" + java.lang.Integer.toHexString(random.nextInt)
				val f = new File(temporaryDirectory, randomName)

				try { createDirectory(f); f }
				catch { case e: Exception => create(tries + 1) }
			}
		}
		create(0)
	}

	// private[util] def jars(dir: File): Iterable[File] = listFiles(dir, GlobFilter("*.jar"))
  // 
	// def delete(files: Iterable[File]): Unit = files.foreach(delete)
	// def delete(file: File)
	// {
	// 	translate("Error deleting file " + file + ": ")
	// 	{
	// 		if(file.isDirectory)
	// 		{
	// 			delete(listFiles(file))
	// 			file.delete
	// 		}
	// 		else if(file.exists)
	// 			file.delete
	// 	}
	// }

	def listFiles(filter: java.io.FileFilter)(dir: File): Array[File] = wrapNull(dir.listFiles(filter))
	def listFiles(dir: File, filter: java.io.FileFilter): Array[File] = wrapNull(dir.listFiles(filter))
	def listFiles(dir: File): Array[File] = wrapNull(dir.listFiles())
	private def wrapNull(a: Array[File]) =
	{
		if(a == null)
			new Array[File](0)
		else
			a
	}


	// /** Creates a jar file.
	// * @param sources The files to include in the jar file paired with the entry name in the jar.
	// * @param outputJar The file to write the jar to.
	// * @param manifest The manifest for the jar.*/
	// def jar(sources: Iterable[(File,String)], outputJar: File, manifest: Manifest): Unit =
	// 	archive(sources, outputJar, Some(manifest))
	// /** Creates a zip file.
	// * @param sources The files to include in the zip file paired with the entry name in the zip.
	// * @param outputZip The file to write the zip to.*/
	// def zip(sources: Iterable[(File,String)], outputZip: File): Unit =
	// 	archive(sources, outputZip, None)

	// private def archive(sources: Iterable[(File,String)], outputFile: File, manifest: Option[Manifest])
	// {
	// 	if(outputFile.isDirectory)
	// 		error("Specified output file " + outputFile + " is a directory.")
	// 	else
	// 	{
	// 		val outputDir = outputFile.getParentFile
	// 		createDirectory(outputDir)
	// 		withZipOutput(outputFile, manifest)
	// 		{ output =>
	// 			val createEntry: (String => ZipEntry) = if(manifest.isDefined) new JarEntry(_) else new ZipEntry(_)
	// 			writeZip(sources, output)(createEntry)
	// 		}
	// 	}
	// }
	private def writeZip(sources: Iterable[(File,String)], output: ZipOutputStream)(createEntry: String => ZipEntry)
	{
		def add(sourceFile: File, name: String)
		{
			if(sourceFile.isDirectory)
				()
			else if(sourceFile.exists)
			{
				val nextEntry = createEntry(normalizeName(name))
				nextEntry.setTime(sourceFile.lastModified)
				output.putNextEntry(nextEntry)
				transferAndClose(new FileInputStream(sourceFile), output)
			}
			else
				error("Source " + sourceFile + " does not exist.")
		}
		sources.foreach(tupled(add))
		output.closeEntry()
	}
	private def normalizeName(name: String) =
	{
		val sep = File.separatorChar
		if(sep == '/') name else name.replace(sep, '/')
	}

	// private def withZipOutput(file: File, manifest: Option[Manifest])(f: ZipOutputStream => Unit)
	// {
	// 	// fileOutputStream(false)(file) { fileOut =>
	// 	fileOutputStream(false).io(file, "", new ConsoleLogger) { fileOut => 
	// 		val (zipOut, ext) =
	// 			manifest match
	// 			{
	// 				case Some(mf) =>
	// 				{
	// 					import Attributes.Name.MANIFEST_VERSION
	// 					val main = mf.getMainAttributes
	// 					if(!main.containsKey(MANIFEST_VERSION))
	// 						main.put(MANIFEST_VERSION, "1.0")
	// 					(new JarOutputStream(fileOut, mf), "jar")
	// 				}
	// 				case None => (new ZipOutputStream(fileOut), "zip")
	// 			}
  // 
	// 		try { f(zipOut) }
	// 		catch { case e: Exception => "Error writing " + ext + ": " + e.toString }
	// 		finally { zipOut.close }
	// 	}
	// }

	def relativize(base: File, file: File): Option[String] =	{
		val pathString = file.getAbsolutePath
		baseFileString(base) flatMap { baseString => {
				if(pathString.startsWith(baseString))
					Some(pathString.substring(baseString.length))
				else
					None
			}
		}
	}

	private def baseFileString(baseFile: File): Option[String] = {
		if(baseFile.isDirectory) {
			val cp = baseFile.getAbsolutePath
			assert(cp.length > 0)
			if(cp.charAt(cp.length - 1) == File.separatorChar)
				Some(cp)
			else
				Some(cp + File.separatorChar)
		}
		else None
	}
	// def copy(sources: Iterable[(File,File)]): Set[File] = Set( sources.map(tupled(copyImpl)).toSeq.toArray : _*)
	// private def copyImpl(from: File, to: File): File =
	// {
	// 	if(!to.exists || from.lastModified > to.lastModified)
	// 	{
	// 		if(from.isDirectory)
	// 			createDirectory(to)
	// 		else
	// 		{
	// 			createDirectory(to.getParentFile)
	// 			copyFile(from, to)
	// 		}
	// 	}
	// 	to
	// }

	// def copyFile(sourceFile: File, targetFile: File)
	// {
	// 	require(sourceFile.exists, "Source file '" + sourceFile.getAbsolutePath + "' does not exist.")
	// 	require(!sourceFile.isDirectory, "Source file '" + sourceFile.getAbsolutePath + "' is a directory.")
  // 
  // 
	// 	fileOutputChannel.io(targetFile, "", new ConsoleLogger) { out =>
	// 			val copied = out.transferFrom(sourceFile.getChannel, 0, sourceFile.size)
	// 			if(copied != in.size)
	// 				error("Could not copy '" + sourceFile + "' to '" + targetFile + "' (" + copied + "/" + in.size + " bytes copied)")
	// 		}
	// 	}
	// }
	def defaultCharset = utf8
	def write(toFile: File, content: String): Unit = write(toFile, content, defaultCharset)
	def write(toFile: File, content: String, charset: Charset): Unit = write(toFile, content, charset, false)
	// def write(file: File, content: String, charset: Charset, append: Boolean): Unit =
	// 	writeCharset(file, content, charset, append) { _.write(content)  }

	// def writeCharset[T](file: File, content: String, charset: Charset, append: Boolean)(f: BufferedWriter => T): T =
	// {
	// 	if(charset.newEncoder.canEncode(content))
	// 		fileWriter(charset, append)(file) { f }
	// 	else
	// 		error("String cannot be encoded by charset " + charset.name)
	// }

	// def read(file: File): String = read(file, defaultCharset)
	// def read(file: File, charset: Charset): String =
	// {
	// 	val out = new ByteArrayOutputStream(file.length.toInt)
	// 	fileInputStream(file){ in => transfer(in, out) }
	// 	out.toString(charset.name)
	// }
	/** doesn't close the InputStream */
	def read(in: InputStream): String = read(in, defaultCharset)
	/** doesn't close the InputStream */
	def read(in: InputStream, charset: Charset): String =
	{
		val out = new ByteArrayOutputStream
		transfer(in, out)
		out.toString(charset.name)
	}
	def readBytes(file: File): Array[Byte] = fileInputStream.io(file, "", new ConsoleLogger) {in => readBytes(in); Right("")}
	// fileOutputStream(false).io(target, "", new ConsoleLogger) { out => transfer(from, out); Right("") }
	/** doesn't close the InputStream */
	def readBytes(in: InputStream): Array[Byte] =
	{
		val out = new ByteArrayOutputStream
		transfer(in, out)
		out.toByteArray
	}

	// Not optimized for large files
	def readLines(file: File): List[String] = readLines(file, defaultCharset)
	def readLines(file: File, charset: Charset): List[String] =
	{
		fileReader(charset)(file){ in =>
			def readLine(accum: List[String]): List[String] =
			{
				val line = in.readLine()
				if(line eq null) accum.reverse else readLine(line :: accum)
			}
			readLine(Nil)
		}
	}
	def writeLines(file: File, lines: Seq[String]): Unit = writeLines(file, lines, defaultCharset)
	def writeLines(file: File, lines: Seq[String], charset: Charset): Unit = writeLines(file, lines, charset, false)
	def writeLines(file: File, lines: Seq[String], charset: Charset, append: Boolean): Unit =
		writeCharset(file, lines.headOption.getOrElse(""), charset, append) { w =>
			lines.foreach { line => w.write(line); w.newLine() }
		}

	/** A pattern used to split a String by path separator characters.*/
	private val PathSeparatorPattern = java.util.regex.Pattern.compile(File.pathSeparator)

	/** Splits a String around path separator characters. */
	def pathSplit(s: String) = PathSeparatorPattern.split(s)

	/** Move the provided files to a temporary location.
	*   If 'f' returns normally, delete the files.
	*   If 'f' throws an Exception, return the files to their original location.*/
	// def stash[T](files: Set[File])(f: => T): T =
	// 	withTemporaryDirectory { dir =>
	// 		val stashed = stashLocations(dir, files.toArray)
	// 		move(stashed)
  // 
	// 		try { f } catch { case e: Exception =>
	// 			try { move(stashed.map(_.swap)); throw e }
	// 			catch { case _: Exception => throw e }
	// 		}
	// 	}
  // 
	// private def stashLocations(dir: File, files: Array[File]) =
	// 	for( (file, index) <- files.zipWithIndex) yield
	// 		(file, new File(dir, index.toHexString))
  // 
	// def move(files: Iterable[(File, File)]): Unit =
	// 	files.foreach(Function.tupled(move))
	// 	
	// def move(a: File, b: File): Unit =
	// {
	// 	if(b.exists)
	// 		delete(b)
	// 	if(!a.renameTo(b))
	// 	{
	// 		copyFile(a, b)
	// 		delete(a)
	// 	}
	// }
}

/** A collection of file related methods. */
object FileUtilities
{
	import Wrappers.readOnly
	/** The size of the byte or char buffer used in various methods.*/
	private val BufferSize = 8192
	val Newline = System.getProperty("line.separator")
	/** A pattern used to split a String by path separator characters.*/
	private val PathSeparatorPattern = java.util.regex.Pattern.compile(File.pathSeparator)

	/** Splits a String around path separator characters. */
	private[util] def pathSplit(s: String) = PathSeparatorPattern.split(s)

	def preserve(paths: Iterable[Path], log: Logger): Either[String, Preserved] =
	{
		for(tmp <- createTemporaryDirectory(log).right) yield
		{
			val pathMap = new scala.collection.mutable.HashMap[File, Path]
			val destinationDirectory = Path.fromFile(tmp)
			for(source <- paths)
			{
				val toPath = Path.fromString(destinationDirectory, source.relativePath)
				copyFile(source, toPath, log)
				pathMap(toPath.asFile) = source
			}
			new Preserved(readOnly(pathMap), tmp)
		}
	}

	// /** Gzips the file 'in' and writes it to 'out'.  'in' cannot be the same file as 'out'. */
	// def gzip(in: Path, out: Path, log: Logger): Option[String] =
	// {
	// 	require(in != out, "Input file cannot be the same as the output file.")
	// 	readStream(in.asFile, log) { inputStream =>
	// 		writeStream(out.asFile, log) { outputStream =>
	// 			gzip(inputStream, outputStream, log)
	// 		}
	// 	}
	// }

	/** Gzips the InputStream 'in' and writes it to 'output'.  Neither stream is closed.*/
	// def gzip(input: InputStream, output: OutputStream, log: Logger): Option[String] =
	// 	gzipOutputStream.ioOption(output, "gzipping", log) { gzStream => transfer(input, gzStream, log) }
  // 
	// def gunzip(input: InputStream, output: OutputStream, log: Logger): Option[String] =
	// 	gzipInputStream.ioOption(input, "gunzipping", log) { gzStream => transfer(gzStream, output, log) }
	// /** Gunzips the file 'in' and writes it to 'out'.  'in' cannot be the same file as 'out'. */
	// def gunzip(in: Path, out: Path, log: Logger): Option[String] =
	// {
	// 	require(in != out, "Input file cannot be the same as the output file.")
	// 	readStream(in.asFile, log) { inputStream =>
	// 		writeStream(out.asFile, log) { outputStream =>
	// 			gunzip(inputStream, outputStream, log)
	// 		}
	// 	}
	// }

	/** Creates a jar file.
	* @param sources The files to include in the jar file.  The path used for the jar is
	* relative to the base directory for the source.  That is, the path in the jar for source
	* <code>(basePath ##) / x / y</code> is <code>x / y</code>.
	* @param outputJar The file to write the jar to.
	* @param manifest The manifest for the jar.
	* @param recursive If true, any directories in <code>sources</code> are recursively processed.  Otherwise,
	* they are not
	* @param log The Logger to use. */
	def jar(sources: Iterable[Path], outputJar: Path, manifest: Manifest, recursive: Boolean, log: Logger) =
		archive(sources, outputJar, Some(manifest), recursive, log)
	@deprecated def pack(sources: Iterable[Path], outputJar: Path, manifest: Manifest, recursive: Boolean, log: Logger) =
		jar(sources, outputJar, manifest, recursive, log)
	/** Creates a zip file.
	* @param sources The files to include in the jar file.  The path used for the jar is
	* relative to the base directory for the source.  That is, the path in the jar for source
	* <code>(basePath ##) / x / y</code> is <code>x / y</code>.
	* @param outputZip The file to write the zip to.
	* @param recursive If true, any directories in <code>sources</code> are recursively processed.  Otherwise,
	* they are not
	* @param log The Logger to use. */
	def zip(sources: Iterable[Path], outputZip: Path, recursive: Boolean, log: Logger) =
		archive(sources, outputZip, None, recursive, log)

	private def archive(sources: Iterable[Path], outputPath: Path, manifest: Option[Manifest], recursive: Boolean, log: Logger) =
	{
		log.info("Packaging " + outputPath + " ...")
		val outputFile = outputPath.asFile
		if(outputFile.isDirectory)
			Some("Specified output file " + outputFile + " is a directory.")
		else
		{
			val outputDir = outputFile.getParentFile
			val result = createDirectory(outputDir, log) orElse
				withZipOutput(outputFile, manifest, log)
				{ output =>
					val createEntry: (String => ZipEntry) = if(manifest.isDefined) new JarEntry(_) else new ZipEntry(_)
					writeZip(sources, output, recursive, log)(createEntry)
				}
			if(result.isEmpty)
				log.info("Packaging complete.")
			result
		}
	}

	private def writeZip(sources: Iterable[Path], output: ZipOutputStream, recursive: Boolean, log: Logger)(createEntry: String => ZipEntry) =
	{
		def add(source: Path)
		{
			val sourceFile = source.asFile
			if(sourceFile.isDirectory)
			{
				if(recursive)
					wrapNull(sourceFile.listFiles).foreach(file => add(source / file.getName))
			}
			else if(sourceFile.exists)
			{
				val relativePath = source.relativePathString("/")
				log.debug("\tAdding " + source + " as " + relativePath + " ...")
				val nextEntry = createEntry(relativePath)
				nextEntry.setTime(sourceFile.lastModified)
				output.putNextEntry(nextEntry)
				transferAndClose(new FileInputStream(sourceFile), output, log)
				output.closeEntry()
			}
			else
				log.warn("\tSource " + source + " does not exist.")
		}
		sources.foreach(add)
		None
	}

	private def withZipOutput(file: File, manifest: Option[Manifest], log: Logger)(f: ZipOutputStream => Option[String]): Option[String] =
	{
		writeStream(file, log)
		{
			fileOut =>
			{
				val (zipOut, ext) =
					manifest match
					{
						case Some(mf) =>
						{
							import Attributes.Name.MANIFEST_VERSION
							val main = mf.getMainAttributes
							if(!main.containsKey(MANIFEST_VERSION))
								main.put(MANIFEST_VERSION, "1.0")
							(new JarOutputStream(fileOut, mf), "jar")
						}
						case None => (new ZipOutputStream(fileOut), "zip")
					}
				Control.trapUnitAndFinally("Error writing " + ext + ": ", log)
					{ f(zipOut) } { zipOut.close }
			}
		}
	}

	import scala.collection.Set
	/** Unzips the contents of the zip file <code>from</code> to the <code>toDirectory</code> directory.*/
	// def unzip(from: Path, toDirectory: Path, log: Logger): Either[String, Set[Path]] =
	// 	unzip(from, toDirectory, AllPassFilter, log)
	// /** Unzips the contents of the zip file <code>from</code> to the <code>toDirectory</code> directory.*/
	// def unzip(from: File, toDirectory: Path, log: Logger): Either[String, Set[Path]] =
	// 	unzip(from, toDirectory, AllPassFilter, log)
	// /** Unzips the contents of the zip file <code>from</code> to the <code>toDirectory</code> directory.*/
	// def unzip(from: InputStream, toDirectory: Path, log: Logger): Either[String, Set[Path]] =
	// 	unzip(from, toDirectory, AllPassFilter, log)
	// /** Unzips the contents of the zip file <code>from</code> to the <code>toDirectory</code> directory.*/
	// def unzip(from: URL, toDirectory: Path, log: Logger): Either[String, Set[Path]] =
	// 	unzip(from, toDirectory, AllPassFilter, log)
  // 
	// /** Unzips the contents of the zip file <code>from</code> to the <code>toDirectory</code> directory.
	// * Only the entries that match the given filter are extracted. */
	// def unzip(from: Path, toDirectory: Path, filter: NameFilter, log: Logger): Either[String, Set[Path]] =
	// 	unzip(from.asFile, toDirectory, filter, log)
	// /** Unzips the contents of the zip file <code>from</code> to the <code>toDirectory</code> directory.
	// * Only the entries that match the given filter are extracted. */
	// def unzip(from: File, toDirectory: Path, filter: NameFilter, log: Logger): Either[String, Set[Path]] =
	// 	readStreamValue(from, log)(in => unzip(in, toDirectory, filter, log))
	// /** Unzips the contents of the zip file <code>from</code> to the <code>toDirectory</code> directory.
	// * Only the entries that match the given filter are extracted. */
	// def unzip(from: URL, toDirectory: Path, filter: NameFilter, log: Logger): Either[String, Set[Path]] =
	// 	readStreamValue(from, log) { stream => unzip(stream, toDirectory, filter, log) }
	// /** Unzips the contents of the zip file <code>from</code> to the <code>toDirectory</code> directory.
	// * Only the entries that match the given filter are extracted. */
	// def unzip(from: InputStream, toDirectory: Path, filter: NameFilter, log: Logger): Either[String, Set[Path]] =
	// {
	// 	createDirectory(toDirectory, log) match
	// 	{
	// 		case Some(err) => Left(err)
	// 		case None => zipInputStream.io(from, "unzipping", log) { zipInput => extract(zipInput, toDirectory, filter, log) }
	// 	}
	// }

	private def extract(from: ZipInputStream, toDirectory: Path, filter: NameFilter, log: Logger) =
	{
		val set = new scala.collection.mutable.HashSet[Path]
		// don't touch dirs as we unzip because we don't know order of zip entires (any child will
		// update the dir's time)
		val dirTimes = new scala.collection.mutable.HashMap[Path, Long]
		def next(): Option[String] =
		{
			val entry = from.getNextEntry
			if(entry == null)
				None
			else
			{
				val name = entry.getName
				val entryErr =
					if(filter.accept(name))
					{
						val target = Path.fromString(toDirectory, name)
						log.debug("Extracting zip entry '" + name + "' to '" + target + "'")
						if(entry.isDirectory)
						{
							dirTimes += target -> entry.getTime
							createDirectory(target, log)
						}
						else
							writeStream(target.asFile, log) { out => FileUtilities.transfer(from, out, log) } orElse
							{
								set += target
								touchExisting(target.asFile, entry.getTime, log)
								None
							}
					}
					else
					{
						log.debug("Ignoring zip entry '" + name + "'")
						None
					}
				from.closeEntry()
				entryErr match { case None => next(); case x => x }
			}
		}
		val result = next()
		for ((dir, time) <- dirTimes) touchExisting(dir.asFile, time, log)
		result.toLeft(readOnly(set))
	}

	/** Copies all bytes from the given input stream to the given output stream.
	* Neither stream is closed.*/
	def transfer(in: InputStream, out: OutputStream, log: Logger): Option[String] =
		transferImpl(in, out, false, log)
	/** Copies all bytes from the given input stream to the given output stream.  The
	* input stream is closed after the method completes.*/
	def transferAndClose(in: InputStream, out: OutputStream, log: Logger): Option[String] =
		transferImpl(in, out, true, log)
	private def transferImpl(in: InputStream, out: OutputStream, close: Boolean, log: Logger): Option[String] =
	{
		Control.trapUnitAndFinally("Error during transfer: ", log)
		{
			val buffer = new Array[Byte](BufferSize)
			def read: None.type =
			{
				val byteCount = in.read(buffer)
				if(byteCount >= 0)
				{
					out.write(buffer, 0, byteCount)
					read
				}
				else
					None
			}
			read
		}
		{ if(close) in.close }
	}

	/** Creates a file at the given location.*/
	def touch(path: Path, log: Logger): Option[String] = touch(path.asFile, log)
	/** Creates a file at the given location.*/
	def touch(file: File, log: Logger): Option[String] =
	{
		Control.trapUnit("Could not create file " + file + ": ", log)
		{
			if(file.exists)
				touchExisting(file, System.currentTimeMillis, log)
			else
				createDirectory(file.getParentFile, log) orElse { file.createNewFile(); None }
		}
	}
	/** Sets the last mod time on the given {@code file}, which must already exist */
	private def touchExisting(file: File, time: Long, log: Logger): Option[String] =
	{
		def updateFailBase = "Could not update last modified for file " + file
		Control.trapUnit(updateFailBase + ": ", log)
			{ if(file.setLastModified(time)) None else Some(updateFailBase) }
	}
	/** Creates a directory at the given location.*/
	def createDirectory(dir: Path, log: Logger): Option[String] = createDirectory(dir.asFile, log)
	/** Creates a directory at the given location.*/
	def createDirectory(dir: File, log: Logger): Option[String] =
	{
		Control.trapUnit("Could not create directory " + dir + ": ", log)
		{
			if(dir.exists)
			{
				if(dir.isDirectory)
					None
				else
					Some(dir + " exists and is not a directory.")
			}
			else
			{
				dir.mkdirs()
				log.debug("Created directory " + dir)
				None
			}
		}
	}
	/** Creates directories at the given locations.*/
	// def createDirectories(d: Iterable[Path], log: Logger): Option[String] = createDirectories(Path.getFiles(d).toList, log)
	/** Creates directories at the given locations.*/
	// def createDirectories(d: List[File], log: Logger): Option[String] =
	// 	d match
	// 	{
	// 		case Nil => None
	// 		case head :: tail => createDirectory(head, log) orElse createDirectories(tail, log)
	// 	}
	/** The maximum number of times a unique temporary filename is attempted to be created.*/
	private val MaximumTries = 10
	/** Creates a temporary directory and returns it.*/
	def createTemporaryDirectory(log: Logger): Either[String, File] =
	{
		def create(tries: Int): Either[String, File] =
		{
			if(tries > MaximumTries)
				Left("Could not create temporary directory.")
			else
			{
				val randomName = "util_" + java.lang.Integer.toHexString(random.nextInt)
				val f = new File(temporaryDirectory, randomName)

				if(createDirectory(f, log).isEmpty)
					Right(f)
				else
					create(tries + 1)
			}
		}
		create(0)
	}

	def withTemporaryDirectory(log: Logger)(action: File => Option[String]): Option[String] =
		 doInTemporaryDirectory(log: Logger)(file => action(file).toLeft(())).left.toOption
	/** Creates a temporary directory and provides its location to the given function.  The directory
	* is deleted after the function returns.*/
	def doInTemporaryDirectory[T](log: Logger)(action: File => Either[String, T]): Either[String, T] =
	{
		def doInDirectory(dir: File): Either[String, T] =
		{
			Control.trapAndFinally("", log)
				{ action(dir) }
				{ delete(dir, true, log) }
		}
		createTemporaryDirectory(log).right.flatMap(doInDirectory)
	}
	def withTemporaryFile[T](log: Logger, prefix: String, postfix: String)(action: File => Either[String, T]): Either[String, T] =
	{
		Control.trap("Error creating temporary file: ", log)
		{
			val file = File.createTempFile(prefix, postfix)
			Control.trapAndFinally("", log)
				{ action(file) }
				{ file.delete() }
		}
	}

	/** Copies the files declared in <code>sources</code> to the <code>destinationDirectory</code>
	* directory.  The source directory hierarchy is flattened so that all copies are immediate
	* children of <code>destinationDirectory</code>.  Directories are not recursively entered.*/
	def copyFlat(sources: Iterable[Path], destinationDirectory: Path, log: Logger) =
	{
		val targetSet = new scala.collection.mutable.HashSet[Path]
		copyImpl(sources, destinationDirectory, log)
		{
			source =>
			{
				val from = source.asFile
				val toPath = destinationDirectory / from.getName
				targetSet += toPath
				val to = toPath.asFile
				if(!to.exists || from.lastModified > to.lastModified && !from.isDirectory)
				{
					log.debug("Copying " + source + " to " + toPath)
					copyFile(from, to, log)
				}
				else
					None
			}
		}.toLeft(readOnly(targetSet))
	}
	private def copyImpl(sources: Iterable[Path], destinationDirectory: Path, log: Logger)
		(doCopy: Path => Option[String]): Option[String] =
	{
		val target = destinationDirectory.asFile
		val creationError =
			if(target.isDirectory)
				None
			else
				createDirectory(target, log)
		def copy(sources: List[Path]): Option[String] =
		{
			sources match
			{
				case src :: remaining =>
				{
					doCopy(src) match
					{
						case None => copy(remaining)
						case error => error
					}
				}
				case Nil => None
			}
		}
		creationError orElse ( Control.trapUnit("", log) { copy(sources.toList) } )
	}
	/** Retrieves the content of the given URL and writes it to the given File. */
	def download(url: URL, to: File, log: Logger) =
	{
		readStream(url, log) { inputStream =>
			writeStream(to, log) { outputStream =>
				transfer(inputStream, outputStream, log)
			}
		}
	}

	/**
	* Equivalent to {@code copy(sources, destinationDirectory, false, log)}.
	*/
	def copy(sources: Iterable[Path], destinationDirectory: Path, log: Logger): Either[String, Set[Path]] =
	copy(sources, destinationDirectory, false, log)

	/**
	* Equivalent to {@code copy(sources, destinationDirectory, overwrite, false, log)}.
	*/
	def copy(sources: Iterable[Path], destinationDirectory: Path, overwrite: Boolean, log: Logger): Either[String, Set[Path]] =
	copy(sources, destinationDirectory, overwrite, false, log)

	/** Copies the files declared in <code>sources</code> to the <code>destinationDirectory</code>
	* directory.  Directories are not recursively entered.  The destination hierarchy matches the
	* source paths relative to any base directories.  For example:
	*
	* A source <code>(basePath ##) / x / y</code> is copied to <code>destinationDirectory / x / y</code>.
	*
	* @param overwrite if true, existing destination files are always overwritten
	* @param preserveLastModified if true, the last modified time of copied files will be set equal to
	* their corresponding source files.
	*/
	def copy(sources: Iterable[Path], destinationDirectory: Path,
		overwrite: Boolean, preserveLastModified: Boolean, log: Logger): Either[String, Set[Path]] =
	{
		val targetSet = new scala.collection.mutable.HashSet[Path]
		copyImpl(sources, destinationDirectory, log)
		{
			source =>
			{
				val from = source.asFile
				val toPath = Path.fromString(destinationDirectory, source.relativePath)
				targetSet += toPath
				val to = toPath.asFile
				if(!to.exists || overwrite || from.lastModified > to.lastModified)
				{
					val result =
						if(from.isDirectory)
							createDirectory(to, log)
						else
						{
							log.debug("Copying " + source + " to " + toPath)
							copyFile(from, to, log)
						}
					if (result.isEmpty && preserveLastModified)
						touchExisting(to, from.lastModified, log)
					else
						result
				}
				else
					None
			}
		}.toLeft(readOnly(targetSet))
	}

	/** Copies the files declared in <code>sources</code> to the <code>targetDirectory</code>
	* directory.  The source directory hierarchy is flattened so that all copies are immediate
	* children of <code>targetDirectory</code>.  Directories are not recursively entered.*/
	def copyFilesFlat(sources: Iterable[File], targetDirectory: Path, log: Logger) =
	{
		require(targetDirectory.asFile.isDirectory, "Target '" + targetDirectory + "' is not a directory.")
		val byName = new scala.collection.mutable.HashMap[String, File]
		for(source <- sources) byName.put(source.getName, source)
		val uniquelyNamedSources = byName.values
		val targetSet = new scala.collection.mutable.HashSet[Path]
		def copy(source: File): Option[String] =
		{
			if(source.isDirectory)
				copyAll(source.listFiles.toList)
			else if(source.exists)
			{
				val targetPath = targetDirectory / source.getName
				targetSet += targetPath
				if(!targetPath.exists || source.lastModified > targetPath.lastModified)
				{
					log.debug("Copying " + source + " to " + targetPath)
					copyFile(source, targetPath.asFile, log)
				}
				else
					None
			}
			else
				None
		}
		def copyAll(sources: List[File]): Option[String] =
			sources match
			{
				case head :: tail =>
					copy(head) match
					{
						case None => copyAll(tail)
						case x => x
					}
				case Nil => None
			}

		Control.trap("Error copying files: ", log) { copyAll(uniquelyNamedSources.toList).toLeft(readOnly(targetSet)) }
	}
	/** Copies <code>sourceFile</code> to <code>targetFile</code>.  If <code>targetFile</code>
	* exists, it is overwritten.  Note that unlike higher level copies in FileUtilities, this
	* method always performs the copy, even if sourceFile is older than targetFile.*/
	def copyFile(sourceFile: Path, targetFile: Path, log: Logger): Option[String] =
		copyFile(sourceFile.asFile, targetFile.asFile, log)
	/** Copies <code>sourceFile</code> to <code>targetFile</code>.  If <code>targetFile</code>
	* exists, it is overwritten.  Note that unlike higher level copies in FileUtilities, this
	* method always performs the copy, even if sourceFile is older than targetFile.*/
	def copyFile(sourceFile: File, targetFile: File, log: Logger): Option[String] =
	{
		require(sourceFile.exists, "Source file '" + sourceFile.getAbsolutePath + "' does not exist.")
		require(!sourceFile.isDirectory, "Source file '" + sourceFile.getAbsolutePath + "' is a directory.")
		readChannel(sourceFile, log)(
			in => writeChannel(targetFile, log) {
				out => {
					val copied = out.transferFrom(in, 0, in.size)
					if(copied == in.size)
						None
					else
						Some("Could not copy '" + sourceFile + "' to '" + targetFile + "' (" + copied + "/" + in.size + " bytes copied)")
				}
			}
		)
	}

	/** Synchronizes the contents of the <code>sourceDirectory</code> directory to the
	* <code>targetDirectory</code> directory.*/
	def sync(sourceDirectory: Path, targetDirectory: Path, log: Logger): Option[String] =
		syncPaths((sourceDirectory ##) ** AllPassFilter, targetDirectory, log)
	def syncPaths(sources: PathFinder, targetDirectory: Path, log: Logger): Option[String] =
	{
		copy(sources.get, targetDirectory, log).right.flatMap
			{ copiedTo => prune(targetDirectory, copiedTo, log).toLeft(()) }.left.toOption
	}
	def prune(directory: Path, keepOnly: Iterable[Path], log: Logger): Option[String] =
	{
		val existing = ((directory ##) ** AllPassFilter).get
		val toRemove = scala.collection.mutable.HashSet(existing.toSeq: _*)
		toRemove --= keepOnly
		if(log.atLevel(Level.Debug))
			toRemove.foreach(r => log.debug("Pruning " + r))
		clean(toRemove, true, log)
	}

	/** Copies the contents of the <code>source</code> directory to the <code>target</code> directory .*/
	def copyDirectory(source: Path, target: Path, log: Logger): Option[String] =
		copyDirectory(source.asFile, target.asFile, log)
	/** Copies the contents of the <code>source</code> directory to the <code>target</code> directory .*/
	def copyDirectory(source: File, target: File, log: Logger): Option[String] =
	{
		require(source.isDirectory, "Source '" + source.getAbsolutePath + "' is not a directory.")
		require(!target.exists, "Target '" + target.getAbsolutePath + "' already exists.")
		def copyDirectory(sourceDir: File, targetDir: File): Option[String] =
			createDirectory(targetDir, log) orElse copyContents(sourceDir, targetDir)
		def copyContents(sourceDir: File, targetDir: File): Option[String] =
			sourceDir.listFiles.foldLeft(None: Option[String])
			{
				(result, file) =>
					result orElse
					{
						val targetFile = new File(targetDir, file.getName)
						if(file.isDirectory)
							copyDirectory(file, targetFile)
						else
							copyFile(file, targetFile, log)
					}
			}
		copyDirectory(source, target)
	}


	/** Deletes the given file recursively.*/
	def clean(file: Path, log: Logger): Option[String] = clean(file :: Nil, log)
	/** Deletes the given files recursively.*/
	def clean(files: Iterable[Path], log: Logger): Option[String] = clean(files, false, log)
	/** Deletes the given files recursively.  <code>quiet</code> determines the logging level.
	* If it is true, each file in <code>files</code> is logged at the <code>info</code> level.
	* If it is false, the <code>debug</code> level is used.*/
	def clean(files: Iterable[Path], quiet: Boolean, log: Logger): Option[String] =
		  deleteFiles(Path.getFiles(files), quiet, log)
			
	private def deleteFiles(files: Iterable[File], quiet: Boolean, log: Logger): Option[String] =
		((None: Option[String]) /: files)( (result, file) => result orElse delete(file, quiet, log))
	private def delete(file: File, quiet: Boolean, log: Logger): Option[String] =
	{
		def logMessage(message: => String)
		{
			log.log(if(quiet) Level.Debug else Level.Info, message)
		}
		Control.trapUnit("Error deleting file " + file + ": ", log)
		{
			if(file.isDirectory)
			{
				logMessage("Deleting directory " + file)
				deleteFiles(wrapNull(file.listFiles), true, log)
				file.delete
			}
			else if(file.exists)
			{
				logMessage("Deleting file " + file)
				file.delete
			}
			None
		}
	}

	/** Appends the given <code>String content</code> to the provided <code>file</code> using the default encoding.
	* A new file is created if it does not exist.*/
	def append(file: File, content: String, log: Logger): Option[String] = append(file, content, Charset.defaultCharset, log)
	/** Appends the given <code>String content</code> to the provided <code>file</code> using the given encoding.
	* A new file is created if it does not exist.*/
	def append(file: File, content: String, charset: Charset, log: Logger): Option[String] =
		write(file, content, charset, true, log)

	/** Writes the given <code>String content</code> to the provided <code>file</code> using the default encoding.
	* If the file exists, it is overwritten.*/
	def write(file: File, content: String, log: Logger): Option[String] = write(file, content, Charset.defaultCharset, log)
	/** Writes the given <code>String content</code> to the provided <code>file</code> using the given encoding.
	* If the file already exists, it is overwritten.*/
	def write(file: File, content: String, charset: Charset, log: Logger): Option[String] =
		write(file, content, charset, false, log)
	private def write(file: File, content: String, charset: Charset, append: Boolean, log: Logger): Option[String] =
	{
		if(charset.newEncoder.canEncode(content))
			write(file, charset, append, log) { w =>  w.write(content); None }
		else
			Some("String cannot be encoded by charset " + charset.name)
	}

	/** Opens a <code>Writer</code> on the given file using the default encoding,
	* passes it to the provided function, and closes the <code>Writer</code>.*/
	def write(file: File, log: Logger)(f: Writer => Option[String]): Option[String] =
		write(file, Charset.defaultCharset, log)(f)
	/** Opens a <code>Writer</code> on the given file using the given encoding,
	* passes it to the provided function, and closes the <code>Writer</code>.*/
	def write(file: File, charset: Charset, log: Logger)(f: Writer => Option[String]): Option[String] =
		write(file, charset, false, log)(f)
	private def write(file: File, charset: Charset, append: Boolean, log: Logger)(f: Writer => Option[String]): Option[String] =
		fileWriter(charset, append).ioOption(file, Writing, log)(f)

	/** Opens a <code>Reader</code> on the given file using the default encoding,
	* passes it to the provided function, and closes the <code>Reader</code>.*/
	def read(file: File, log: Logger)(f: Reader => Option[String]): Option[String] =
		read(file, Charset.defaultCharset, log)(f)
	/** Opens a <code>Reader</code> on the given file using the default encoding,
	* passes it to the provided function, and closes the <code>Reader</code>.*/
	def read(file: File, charset: Charset, log: Logger)(f: Reader => Option[String]): Option[String] =
		fileReader(charset).ioOption(file, Reading, log)(f)
	/** Opens a <code>Reader</code> on the given file using the default encoding,
	* passes it to the provided function, and closes the <code>Reader</code>.*/
	def readValue[R](file: File, log: Logger)(f: Reader => Either[String, R]): Either[String, R] =
		readValue(file, Charset.defaultCharset, log)(f)
	/** Opens a <code>Reader</code> on the given file using the given encoding,
	* passes it to the provided function, and closes the <code>Reader</code>.*/
	def readValue[R](file: File, charset: Charset, log: Logger)(f: Reader => Either[String, R]): Either[String, R] =
		fileReader(charset).io(file, Reading, log)(f)

	/** Reads the contents of the given file into a <code>String</code> using the default encoding.
	*  The resulting <code>String</code> is wrapped in <code>Right</code>.*/
	def readString(file: File, log: Logger): Either[String, String] = readString(file, Charset.defaultCharset, log)
	/** Reads the contents of the given file into a <code>String</code> using the given encoding.
	*  The resulting <code>String</code> is wrapped in <code>Right</code>.*/
	def readString(file: File, charset: Charset, log: Logger): Either[String, String] = readValue(file, charset, log)(readString)

	def readString(in: InputStream, log: Logger): Either[String, String] = readString(in, Charset.defaultCharset, log)
	def readString(in: InputStream, charset: Charset, log: Logger): Either[String, String] =
		streamReader.io((in, charset), Reading, log)(readString)
	def readString(in: Reader, log: Logger): Either[String, String] =
		Control.trapAndFinally("Error reading bytes from reader: ", log)
			{ readString(in) }
			{ in.close() }
	private def readString(in: Reader): Either[String, String] =
	{
		val builder = new StringBuilder
		val buffer = new Array[Char](BufferSize)
		def readNext()
		{
			val read = in.read(buffer, 0, buffer.length)
			if(read >= 0)
			{
				builder.append(buffer, 0, read)
				readNext()
			}
			else
				None
		}
		readNext()
		Right(builder.toString)
	}
	/** Appends the given bytes to the given file. */
	def append(file: File, bytes: Array[Byte], log: Logger): Option[String] =
		writeBytes(file, bytes, true, log)
	/** Writes the given bytes to the given file. If the file already exists, it is overwritten.*/
	def write(file: File, bytes: Array[Byte], log: Logger): Option[String] =
		writeBytes(file, bytes, false, log)
	private def writeBytes(file: File, bytes: Array[Byte], append: Boolean, log: Logger): Option[String] =
		writeStream(file, append, log) { out => out.write(bytes); None }

	/** Reads the entire file into a byte array. */
	def readBytes(file: File, log: Logger): Either[String, Array[Byte]] = readStreamValue(file, log)(readBytes)
	def readBytes(in: InputStream, log: Logger): Either[String, Array[Byte]] =
		Control.trapAndFinally("Error reading bytes from input stream: ", log)
			{ readBytes(in) }
			{ in.close() }
	private def readBytes(in: InputStream): Either[String, Array[Byte]] =
	{
		val out = new ByteArrayOutputStream
		val buffer = new Array[Byte](BufferSize)
		def readNext()
		{
			val read = in.read(buffer)
			if(read >= 0)
			{
				out.write(buffer, 0, read)
				readNext()
			}
		}
		readNext()
		Right(out.toByteArray)
	}


	/** Opens an <code>OutputStream</code> on the given file with append=true and passes the stream
	* to the provided function.  The stream is closed before this function returns.*/
	def appendStream(file: File, log: Logger)(f: OutputStream => Option[String]): Option[String] =
		fileOutputStream(true).ioOption(file, Appending, log)(f)
	/** Opens an <code>OutputStream</code> on the given file and passes the stream
	* to the provided function.  The stream is closed before this function returns.*/
	def writeStream(file: File, log: Logger)(f: OutputStream => Option[String]): Option[String] =
		fileOutputStream(false).ioOption(file, Writing, log)(f)
	private def writeStream(file: File, append: Boolean, log: Logger)(f: OutputStream => Option[String]): Option[String] =
		if(append) appendStream(file, log)(f) else writeStream(file, log)(f)
	/** Opens an <code>InputStream</code> on the given file and passes the stream
	* to the provided function.  The stream is closed before this function returns.*/
	def readStream(file: File, log: Logger)(f: InputStream => Option[String]): Option[String] =
		fileInputStream.ioOption(file, Reading, log)(f)
	/** Opens an <code>InputStream</code> on the given file and passes the stream
	* to the provided function.  The stream is closed before this function returns.*/
	def readStreamValue[R](file: File, log: Logger)(f: InputStream => Either[String, R]): Either[String, R] =
		fileInputStream.io(file, Reading, log)(f)
	/** Opens an <code>InputStream</code> on the given <code>URL</code> and passes the stream
	* to the provided function.  The stream is closed before this function returns.*/
	def readStream(url: URL, log: Logger)(f: InputStream => Option[String]): Option[String] =
		urlInputStream.ioOption(url, Reading, log)(f)
	/** Opens an <code>InputStream</code> on the given <code>URL</code> and passes the stream
	* to the provided function.  The stream is closed before this function returns.*/
	def readStreamValue[R](url: URL, log: Logger)(f: InputStream => Either[String, R]): Either[String, R] =
		urlInputStream.io(url, Reading, log)(f)
  
	/** Opens a <code>FileChannel</code> on the given file for writing and passes the channel
	* to the given function.  The channel is closed before this function returns.*/
	def writeChannel(file: File, log: Logger)(f: FileChannel => Option[String]): Option[String] =
		fileOutputChannel.ioOption(file, Writing, log)(f)
	/** Opens a <code>FileChannel</code> on the given file for reading and passes the channel
	* to the given function.  The channel is closed before this function returns.*/
	def readChannel(file: File, log: Logger)(f: FileChannel => Option[String]): Option[String] =
		fileInputChannel.ioOption(file, Reading, log)(f)
	/** Opens a <code>FileChannel</code> on the given file for reading and passes the channel
	* to the given function.  The channel is closed before this function returns.*/
	def readChannelValue[R](file: File, log: Logger)(f: FileChannel => Either[String, R]): Either[String, R] =
		fileInputChannel.io(file, Reading, log)(f)

	private[util] def wrapNull(a: Array[File]): Array[File] =
		if(a == null)
			new Array[File](0)
		else
			a

	/** Writes the given string to the writer followed by a newline.*/
	private[util] def writeLine(writer: Writer, line: String)
	{
		writer.write(line)
		writer.write(Newline)
	}

	def toFile(url: URL) =
		try { new File(url.toURI) }
		catch { case _: URISyntaxException => new File(url.getPath) }

	/** The directory in which temporary files are placed.*/
	val temporaryDirectory = new File(System.getProperty("java.io.tmpdir"))
	def classLocation(cl: Class[_]): URL =
	{
		val codeSource = cl.getProtectionDomain.getCodeSource
		if(codeSource == null) error("No class location for " + cl)
		else codeSource.getLocation
	}
	def classLocationFile(cl: Class[_]): File = toFile(classLocation(cl))
	def classLocation[T](implicit mf: scala.reflect.Manifest[T]): URL = classLocation(mf.erasure)
	def classLocationFile[T](implicit mf: scala.reflect.Manifest[T]): File = classLocationFile(mf.erasure)

	lazy val scalaLibraryJar: File = classLocationFile[scala.ScalaObject]
	lazy val scalaCompilerJar: File = classLocationFile[scala.tools.nsc.Settings]
	def scalaJars: Iterable[File] = List(scalaLibraryJar, scalaCompilerJar)

	/** The producer of randomness for unique name generation.*/
	private val random = new java.util.Random

	private val Reading = "reading"
	private val Writing = "writing"
	private val Appending = "appending"
}

