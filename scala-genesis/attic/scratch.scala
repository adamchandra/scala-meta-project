object Test {
  import java.io.{File, PrintStream, OutputStreamWriter, ByteArrayOutputStream}
  import java.io.{PrintStream, ByteArrayOutputStream}
  import java.util.regex.Pattern
  import scala.tools.scalap._
  import scala.tools.scalap.scalax.rules._
  import scala.tools.scalap.scalax.rules.scalasig._
  import scala.tools.scalap.scalax.util.StringUtil
  import scala.util.regexp._
  import tools.nsc.io.AbstractFile
  import tools.nsc.util.{ClassPath, JavaClassPath}

  def prop(p:String): String = {
    System.getProperty(p)
  } 
  // + code completion and/or hints for class methods, function names, argument lists/types/docs

  final class TestEqualityAssoc[A](val x: A) {
    @inline def ==>  [B](y: B): Tuple2[A, B] = Tuple2(x, y)
  }
  implicit def any2TestEqualityAssoc[A](x: A): TestEqualityAssoc[A] = new TestEqualityAssoc(x)

  def ex(): Unit = {
  }
  
  val s = "foo.bar.baz.Quux" ==> "f.b.b.Quux"

  def classpath(): JavaClassPath = {
    new JavaClassPath(
      prop("sun.boot.class.path"), // boot
      prop("java.ext.dirs"),       // ext
      prop("java.class.path"),     // user
      "",                          // src
      "")                          // xcode
  }

  def classfile(classname: String): ClassFile = {
    val encName = Names.encode(classname)
    val cls = classpath().findClass(encName)
    println(cls)
    if (cls.isDefined && cls.get.binary.isDefined) {
      val cfile = cls.get.binary.get
      val bytes = cfile.toByteArray
      val byteCode = ByteCode(bytes)
      ClassFileParser.parse(byteCode)
    }
    else null
  }

  def attributeNames(cf:ClassFile): Seq[Any] = {
    cf.attributes.map( attr => cf.header.constants(attr.nameIndex) )
  }

  def lspk(packagePrefix: String) : List[String] = {
    List()
  }

  // def asdf(classname: String): Unit = {
  //     val classFile = classfile(classname)xs
  //     classFile.attribute(SCALA_SIG).map(_.byteCode).map(ScalaSigAttributeParsers.parse) match {
  //       case Some(scalaSig) => parseScalaSignature(scalaSig, isPackageObject)
  //       case None => //Do nothing
  //     }
  // }

  

}
object Readers {
  private var readers: List[Pair[Reader, String => Unit]] = List()
 
  def ask(prompt: String) = new Reader(prompt)
 
  class Reader(prompt: String) extends Responder[String] {
 
    def respond(k: String => Unit): Unit = {
      print("["+(readers.length+1)+"]"+prompt+"> "); 
      val line = readLine;
      if (line == "back") {
        val Pair(rdr, k) = readers.head;
        readers = readers.tail
        rdr.respond(k)
      } else {
        readers = Pair(this, k) :: readers
        k(line)
      }
    }
  }
}

import Responder._
import Readers._

object Test1 extends Application {
  run {
    for { 
      val line1 <- ask("first number")
      val f = Integer.parseInt(line1)
      f > 0
      exec(println("first number parsed!"))
      val s <- ask("second number") map Integer.parseInt
    } yield f + s
  } match {
    case Some(x) => println("the sum is "+x)
    case None => println("(aborted)")
  }
}

// val cf = Test.classfile("scala.collection.immutable.List")
// scala> val iglobal = new scala.tools.nsc.interactive.Global(global.settings, global.reporter)
// scala> iglobal.getSourceFile("src/main/scala/iscalarc.scala")
// scala> iglobal.debugInfo(iglobal.getSourceFile("src/main/scala/iscalarc.scala"), 809, 1)
