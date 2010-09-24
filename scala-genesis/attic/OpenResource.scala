/* sbt -- Simple Build Tool
 * Copyright 2008, 2009 Mark Harrah
 */
package util

import java.io.{Closeable, File, FileInputStream, FileOutputStream, InputStream, OutputStream}
import java.io.{ByteArrayOutputStream, InputStreamReader, OutputStreamWriter}
import java.io.{BufferedReader, BufferedWriter, FileReader, FileWriter, Reader, Writer}
import java.util.zip.{GZIPInputStream, GZIPOutputStream}
import java.net.{URL, URISyntaxException}
import java.nio.charset.{Charset, CharsetDecoder, CharsetEncoder}
import java.nio.channels.FileChannel
import java.util.jar.{Attributes, JarEntry, JarFile, JarInputStream, JarOutputStream, Manifest}
import java.util.zip.{GZIPOutputStream, ZipEntry, ZipFile, ZipInputStream, ZipOutputStream}

import util.ErrorHandling.translate
import util.log.Logger

private abstract class OpenResource[Source, T] extends NotNull
{
	import OpenResource.{unwrapEither, wrapEither}
	protected def open(src: Source, log: Logger): Either[String, T]
	def ioOption(src: Source, op: String, log: Logger)(f: T => Option[String]) =
		unwrapEither( io(src, op, log)(wrapEither(f)) )
	def io[R](src: Source, op: String, log: Logger)(f: T => Either[String,R]): Either[String, R] =
		open(src, log).right flatMap
		{
			resource => Control.trapAndFinally("Error " + op + " "+ src + ": ", log)
				{ f(resource) }
				{ close(resource) }
		}
	protected def close(out: T): Unit
}
private trait CloseableOpenResource[Source, T <: Closeable] extends OpenResource[Source, T]
{
	protected def close(out: T): Unit = out.close()
}
import scala.reflect.{Manifest => SManifest}
private abstract class WrapOpenResource[Source, T <: Closeable](implicit srcMf: SManifest[Source], targetMf: SManifest[T]) extends CloseableOpenResource[Source, T]
{
	private def label[S](m: SManifest[S]) = m.erasure.getSimpleName
	protected def open(source: Source): T
	protected final def open(source: Source, log: Logger): Either[String, T] =
		Control.trap("Error wrapping " + label(srcMf) + " in " + label(targetMf) + ": ", log) { Right(open(source)) }
}
private abstract class OpenFile[T] extends OpenResource[File, T]
{
	protected def open(file: File): T
	protected final def open(file: File, log: Logger): Either[String, T] =
	{
		val parent = file.getParentFile
		if(parent != null) {
      parent.mkdirs()
			// FileUtilities.createDirectory(parent, log)
    }
		Control.trap("Error opening " + file + ": ", log) { Right(open(file)) }
	}
}
private abstract class CloseableOpenFile[T <: Closeable] extends OpenFile[T] with CloseableOpenResource[File, T]
private object OpenResource
{
	private def wrapEither[R](f: R => Option[String]): (R => Either[String, Unit]) = (r: R) => f(r).toLeft(())
	private def unwrapEither(e: Either[String, Unit]): Option[String] = e.left.toOption

	def fileOutputStream(append: Boolean) =
		new CloseableOpenFile[FileOutputStream] { protected def open(file: File) = new FileOutputStream(file, append) }
	def fileInputStream = new CloseableOpenFile[FileInputStream]
		{ protected def open(file: File) = new FileInputStream(file) }
	def urlInputStream = new CloseableOpenResource[URL, InputStream]
		{ protected def open(url: URL, log: Logger) = Control.trap("Error opening " + url + ": ", log) { Right(url.openStream) } }
	def fileOutputChannel = new CloseableOpenFile[FileChannel]
		{ protected def open(f: File) = (new FileOutputStream(f)).getChannel }
	def fileInputChannel = new CloseableOpenFile[FileChannel]
		{ protected def open(f: File) = (new FileInputStream(f)).getChannel }
	def fileWriter(charset: Charset, append: Boolean) = new CloseableOpenFile[Writer]
		{ protected def open(f: File) = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f, append), charset)) }
	def fileReader(charset: Charset) = new CloseableOpenFile[Reader]
		{ protected def open(f: File) = new BufferedReader(new InputStreamReader(new FileInputStream(f), charset)) }
	def jarFile(verify: Boolean) = new OpenFile[JarFile]
		{ protected def open(f: File) = new JarFile(f, verify)
		   override protected def close(j: JarFile) = j.close() }
	def zipFile = new OpenFile[ZipFile]
		{ protected def open(f: File) = new ZipFile(f)
		   override protected def close(z: ZipFile) = z.close() }
	def streamReader = new WrapOpenResource[(InputStream, Charset), Reader]
		{ protected def open(streamCharset: (InputStream, Charset)) = new InputStreamReader(streamCharset._1, streamCharset._2) }
	def gzipInputStream = new WrapOpenResource[InputStream, GZIPInputStream]
		{ protected def open(in: InputStream) = new GZIPInputStream(in) }
	def zipInputStream = new WrapOpenResource[InputStream, ZipInputStream]
		{ protected def open(in: InputStream) = new ZipInputStream(in) }
	def gzipOutputStream = new WrapOpenResource[OutputStream, GZIPOutputStream]
		{ protected def open(out: OutputStream) = new GZIPOutputStream(out)
		   override protected def close(out: GZIPOutputStream) = out.finish() }
	def jarOutputStream = new WrapOpenResource[OutputStream, JarOutputStream]
		{ protected def open(out: OutputStream) = new JarOutputStream(out) }
	def jarInputStream = new WrapOpenResource[InputStream, JarInputStream]
		{ protected def open(in: InputStream) = new JarInputStream(in) }
	def zipEntry(zip: ZipFile) = new CloseableOpenResource[ZipEntry, InputStream] {
		protected def open(entry: ZipEntry, log: Logger) =
			Control.trap("Error opening " + entry.getName + " in " + zip + ": ", log) { Right(zip.getInputStream(entry)) }
	}
}












// abstract class OpenResource[Source, T] extends NotNull
// {
//   import OpenResource.{unwrapEither, wrapEither}
//   protected def open(src: Source, log: Logger): Either[String, T]
//   def ioOption(src: Source, op: String, log: Logger)(f: T => Option[String]) =
//     unwrapEither( io(src, op, log)(wrapEither(f)) )
//   def io[R](src: Source, op: String, log: Logger)(f: T => Either[String,R]): Either[String, R] =
//     open(src, log).right flatMap
//     {
//       resource => Control.trapAndFinally("Error " + op + " "+ src + ": ", log)
//         { f(resource) }
//         { close(resource) }
//     }
//   protected def close(out: T): Unit
// }
// private abstract class OpenFile[T] extends OpenResource[File, T] {
//   protected def open(file: File): T
//   protected final def open(file: File, log: Logger): Either[String, T] =
//   {
//     val parent = file.getParentFile
//     if(parent != null)
//       FileUtilities.createDirectory(parent, log)
//     Control.trap("Error opening " + file + ": ", log) { Right(open(file)) }
//   }
// }
// private abstract class CloseableOpenFile[T <: Closeable] extends OpenFile[T] with CloseableOpenResource[File, T]
// 
// 
// trait OpenFile[T] extends OpenResource[File, T] {
//   // todo: copied from FileUtilities
// 	def createDirectory(dir: File): Unit = {
// 		def failBase = "Could not create directory " + dir
// 		if(dir.isDirectory || dir.mkdirs())
// 			()
// 		else if(dir.exists)
// 			error(failBase + ": file exists and is not a directory.")
// 		else
// 			error(failBase)
// 	}
// 
// 	protected def openImpl(file: File): T
// 	protected final def open(file: File): T =
// 	{
// 		val parent = file.getParentFile
// 		if(parent != null)
// 			createDirectory(parent)
// 		openImpl(file)
// 	}
// }
// 
// trait CloseableOpenResource[Source, T <: Closeable] extends OpenResource[Source, T] {
//   protected def close(out: T): Unit = out.close()
// }
// 
// abstract class CloseableOpenFile[T <: Closeable] extends OpenFile[T] with CloseableOpenResource[File, T]
// 
// import scala.reflect.{Manifest => SManifest}
// abstract class WrapOpenResource[Source, T](implicit srcMf: SManifest[Source], targetMf: SManifest[T]) extends OpenResource[Source, T]
// {
// 	protected def label[S](m: SManifest[S]) = m.erasure.getSimpleName
// 	protected def openImpl(source: Source): T
// 	protected final def open(source: Source): T =
// 		translate("Error wrapping " + label(srcMf) + " in " + label(targetMf) + ": ") { openImpl(source) }
// }
// 
// object OpenResource
// {
//   private def wrapEither[R](f: R => Option[String]): (R => Either[String, Unit]) = (r: R) => f(r).toLeft(())
//   private def unwrapEither(e: Either[String, Unit]): Option[String] = e.left.toOption
// 
//   // def fileOutputStream(append: Boolean) =
//   //   new CloseableOpenFile[FileOutputStream] { protected def open(file: File) = new FileOutputStream(file, append) }
//   // def fileInputStream = new CloseableOpenFile[FileInputStream]
//   //   { protected def open(file: File) = new FileInputStream(file) }
//   // def urlInputStream = new CloseableOpenResource[URL, InputStream]
//   //   { protected def open(url: URL, log: Logger) = Control.trap("Error opening " + url + ": ", log) { Right(url.openStream) } }
//   // def fileOutputChannel = new CloseableOpenFile[FileChannel]
//   //   { protected def open(f: File) = (new FileOutputStream(f)).getChannel }
//   // def fileInputChannel = new CloseableOpenFile[FileChannel]
//   //   { protected def open(f: File) = (new FileInputStream(f)).getChannel }
//   // def fileWriter(charset: Charset, append: Boolean) = new CloseableOpenFile[Writer]
//   //   { protected def open(f: File) = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f, append), charset)) }
//   // def fileReader(charset: Charset) = new CloseableOpenFile[Reader]
//   //   { protected def open(f: File) = new BufferedReader(new InputStreamReader(new FileInputStream(f), charset)) }
//   // def jarFile(verify: Boolean) = new OpenFile[JarFile]
//   //   { protected def open(f: File) = new JarFile(f, verify)
//   //      override protected def close(j: JarFile) = j.close() }
//   // def zipFile = new OpenFile[ZipFile]
//   //   { protected def open(f: File) = new ZipFile(f)
//   //      override protected def close(z: ZipFile) = z.close() }
//   // def streamReader = new WrapOpenResource[(InputStream, Charset), Reader]
//   //   { protected def open(streamCharset: (InputStream, Charset)) = new InputStreamReader(streamCharset._1, streamCharset._2) }
//   // def gzipInputStream = new WrapOpenResource[InputStream, GZIPInputStream]
//   //   { protected def open(in: InputStream) = new GZIPInputStream(in) }
//   // def zipInputStream = new WrapOpenResource[InputStream, ZipInputStream]
//   //   { protected def open(in: InputStream) = new ZipInputStream(in) }
//   // def gzipOutputStream = new WrapOpenResource[OutputStream, GZIPOutputStream]
//   //   { protected def open(out: OutputStream) = new GZIPOutputStream(out)
//   //      override protected def close(out: GZIPOutputStream) = out.finish() }
//   // def jarOutputStream = new WrapOpenResource[OutputStream, JarOutputStream]
//   //   { protected def open(out: OutputStream) = new JarOutputStream(out) }
//   // def jarInputStream = new WrapOpenResource[InputStream, JarInputStream]
//   //   { protected def open(in: InputStream) = new JarInputStream(in) }
//   // def zipEntry(zip: ZipFile) = new CloseableOpenResource[ZipEntry, InputStream] {
//   //   protected def open(entry: ZipEntry, log: Logger) =
//   //     Control.trap("Error opening " + entry.getName + " in " + zip + ": ", log) { Right(zip.getInputStream(entry)) }
//   // }
// 
//  	def wrap[Source, T<: Closeable](openF: Source => T)(implicit srcMf: SManifest[Source], targetMf: SManifest[T]): OpenResource[Source,T] =
//  		wrap(openF, _.close)
//  	def wrap[Source, T](openF: Source => T, closeF: T => Unit)(implicit srcMf: SManifest[Source], targetMf: SManifest[T]): OpenResource[Source,T] =
//  		new WrapOpenResource[Source, T]
//  		{
//  			def openImpl(source: Source) = openF(source)
//  			def close(t: T) = closeF(t)
//  		}
//  
//  	def resource[Source, T <: Closeable](openF: Source => T): OpenResource[Source,T] =
//  		resource(openF, _.close)
//  	def resource[Source, T](openF: Source => T, closeF: T => Unit): OpenResource[Source,T] =
//  		new OpenResource[Source,T]
//  		{
//  			def open(s: Source) = openF(s)
//  			def close(s: T) = closeF(s)
//  		}
//  	// def file[T <: Closeable](openF: File => T): OpenFile[T] = file(openF, _.close())
//  	// def file[T](openF: File => T, closeF: T => Unit): OpenFile[T] =
//  	// 	new OpenFile[T]
//  	// 	{
//  	// 		def openImpl(file: File) = openF(file)
//  	// 		def close(t: T) = closeF(t)
//  	// 	}
//  
// //def file[T <: Closeable](openF: File => T): OpenFile[T] = file(openF, _.close())
//   def file[T <: Closeable](openF: File => T): OpenFile[T] = file(openF, _.close())
//   def file[T](openF: File => T, closeF: T => Unit): OpenFile[T] = new OpenFile[T] {
//  		def openImpl(file: File) = openF(file)
//  		def close(t: T) = closeF(t)
//  	}
// 
//  	def fileOutputStream(append: Boolean) = file(f => new FileOutputStream(f, append))
//  	def fileInputStream = file(f => new FileInputStream(f))
//  	def urlInputStream = resource( (u: URL) => translate("Error opening " + u + ": ")(u.openStream))
//  	def fileOutputChannel = file(f => new FileOutputStream(f).getChannel)
//  	def fileInputChannel = file(f => new FileInputStream(f).getChannel)
//  	def fileWriter(charset: Charset, append: Boolean) =
//  		file(f => new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f, append), charset)) )
//  	def fileReader(charset: Charset) = file(f => new BufferedReader(new InputStreamReader(new FileInputStream(f), charset)) )
//  	def jarFile(verify: Boolean) = file(f => new JarFile(f, verify), (_: JarFile).close())
//  	def zipFile = file(f => new ZipFile(f), (_: ZipFile).close())
//  	def streamReader = wrap{ (_: (InputStream, Charset)) match { case (in, charset) => new InputStreamReader(in, charset) } }
//  	def gzipInputStream = wrap( (in: InputStream) => new GZIPInputStream(in) )
//  	def zipInputStream = wrap( (in: InputStream) => new ZipInputStream(in))
//  	def gzipOutputStream = wrap((out: OutputStream) => new GZIPOutputStream(out), (_: GZIPOutputStream).finish())
//  	def jarOutputStream = wrap( (out: OutputStream) => new JarOutputStream(out))
//  	def jarInputStream = wrap( (in: InputStream) => new JarInputStream(in))
//  	def zipEntry(zip: ZipFile) = resource( (entry: ZipEntry) =>
//  		translate("Error opening " + entry.getName + " in " + zip + ": ") { zip.getInputStream(entry) } )
// 
// }
// 
// 
// // object OpenResource
// // {
// // 	def wrap[Source, T<: Closeable](openF: Source => T)(implicit srcMf: SManifest[Source], targetMf: SManifest[T]): OpenResource[Source,T] =
// // 		wrap(openF, _.close)
// // 	def wrap[Source, T](openF: Source => T, closeF: T => Unit)(implicit srcMf: SManifest[Source], targetMf: SManifest[T]): OpenResource[Source,T] =
// // 		new WrapOpenResource[Source, T]
// // 		{
// // 			def openImpl(source: Source) = openF(source)
// // 			def close(t: T) = closeF(t)
// // 		}
// // 
// // 	def resource[Source, T <: Closeable](openF: Source => T): OpenResource[Source,T] =
// // 		resource(openF, _.close)
// // 	def resource[Source, T](openF: Source => T, closeF: T => Unit): OpenResource[Source,T] =
// // 		new OpenResource[Source,T]
// // 		{
// // 			def open(s: Source) = openF(s)
// // 			def close(s: T) = closeF(s)
// // 		}
// // 
// // 	def fileOutputStream(append: Boolean) = file(f => new FileOutputStream(f, append))
// // 	def fileInputStream = file(f => new FileInputStream(f))
// // 	def urlInputStream = resource( (u: URL) => translate("Error opening " + u + ": ")(u.openStream))
// // 	def fileOutputChannel = file(f => new FileOutputStream(f).getChannel)
// // 	def fileInputChannel = file(f => new FileInputStream(f).getChannel)
// // 	def fileWriter(charset: Charset, append: Boolean) =
// // 		file(f => new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f, append), charset)) )
// // 	def fileReader(charset: Charset) = file(f => new BufferedReader(new InputStreamReader(new FileInputStream(f), charset)) )
// // 	def jarFile(verify: Boolean) = file(f => new JarFile(f, verify), (_: JarFile).close())
// // 	def zipFile = file(f => new ZipFile(f), (_: ZipFile).close())
// // 	def streamReader = wrap{ (_: (InputStream, Charset)) match { case (in, charset) => new InputStreamReader(in, charset) } }
// // 	def gzipInputStream = wrap( (in: InputStream) => new GZIPInputStream(in) )
// // 	def zipInputStream = wrap( (in: InputStream) => new ZipInputStream(in))
// // 	def gzipOutputStream = wrap((out: OutputStream) => new GZIPOutputStream(out), (_: GZIPOutputStream).finish())
// // 	def jarOutputStream = wrap( (out: OutputStream) => new JarOutputStream(out))
// // 	def jarInputStream = wrap( (in: InputStream) => new JarInputStream(in))
// // 	def zipEntry(zip: ZipFile) = resource( (entry: ZipEntry) =>
// // 		translate("Error opening " + entry.getName + " in " + zip + ": ") { zip.getInputStream(entry) } )
// // }
