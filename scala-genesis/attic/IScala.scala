import scala.util.regexp._

object Util {
  def color(c: String, s: String): String = {
    // e.g., Console.RED, "foo" ==> "[\03foo[\00"
    c + s + Console.RESET
  }

  def shortenFQN(fqn:String): String = {
    // e.g., {"foo.bar.baz.Quux" => "f.b.b.Quux", 
    //        "Quux" => "Quux"}
    if (fqn contains ".") {
      val fqnparts = fqn.split('.')
      val (pkg, cn) = (fqnparts.dropRight(1), fqnparts.last)
      val initials = pkg.map {_.substring(0,1)} 
      var sb = new StringBuilder()
      return initials.addString(sb, "", ".", "."+cn).toString
    }
    fqn
  }
  var accum:List[String] = List()
  def printAcc(s:String) {
    accum = accum ::: List(s)
  }
}

object Scalap {
  import java.io.{PrintStream, ByteArrayOutputStream}
  import java.io.{File, PrintStream, OutputStreamWriter, ByteArrayOutputStream}
  import scala.tools.scalap.scalax.rules.scalasig._
  import tools.nsc.io.AbstractFile
  import tools.nsc.util.{ClassPath, JavaClassPath}
  import scala.tools.scalap._
  import Util._

  val SCALA_SIG = "ScalaSig"
  val versionMsg = "Scala classfile decoder " +
  Properties.versionString + " -- " +
  Properties.copyrightString + "\n"

  /**Verbose program run?
   */
  var verbose = false
  var printPrivates = false

  def isScalaFile(bytes: Array[Byte]): Boolean = {
    val byteCode = ByteCode(bytes)
    val classFile = ClassFileParser.parse(byteCode)
    classFile.attribute("ScalaSig").isDefined
  }

  /**Processes the given Java class file.
   *
   * @param clazz the class file to be processed.
   */
  def processJavaClassFile(clazz: Classfile) {
    // construct a new output stream writer
    val out = new OutputStreamWriter(Console.out)
    val writer = new MyJavaWriter(clazz, out)
    // print the class
    writer.printClass
    out.flush()
  }

  def isPackageObjectFile(s: String) = s != null && (s.endsWith(".package") || s == "package")

  def parseScalaSignature(scalaSig: ScalaSig, isPackageObject: Boolean) = {
    val baos = new ByteArrayOutputStream
    val stream = new PrintStream(baos)
    val syms = scalaSig.topLevelClasses ::: scalaSig.topLevelObjects
    syms.head.parent match {
      //Partial match
      case Some(p) if (p.name != "<empty>") => {
        val path = p.path
        if (!isPackageObject) {
          stream.print("package ");
          stream.print(path);
          stream.print("\n")
        } else {
          val i = path.lastIndexOf(".")
          if (i > 0) {
            stream.print("package ");
            stream.print(path.substring(0, i))
            stream.print("\n")
          }
        }
      }
      case _ =>
    }
    // Print classes
    val printer = new MyScalaSigPrinter(stream, printPrivates)
    for (c <- syms) {
      Console.println("c: " + c.toString)
      printer.printSymbol(c)
    }
    Console.println("baos ****************")
    Console.println(baos.toString())
  }

  def decompileScala(bytes: Array[Byte], isPackageObject: Boolean) = {
    val byteCode = ByteCode(bytes)
    val classFile = ClassFileParser.parse(byteCode)
    classFile.attribute(SCALA_SIG).map(_.byteCode).map(ScalaSigAttributeParsers.parse) match {
      case Some(scalaSig) => parseScalaSignature(scalaSig, isPackageObject)
      case None => //Do nothing
    }
  }

  def _linearizeHierarchy(classes: Array[String]): List[String] = {
    if (classes == null || classes.length == 0 ) {
      return Nil
    }
    else if ( classes.length > 1 ) {
      return _linearizeHierarchy(classes.last) ::: _linearizeHierarchy(classes.init)
    }
    _linearizeHierarchy(classes.head)
  }

  def _linearizeHierarchy(classname: String): List[String] = {
    val cls = java.lang.Class.forName(classname)
    if (cls != null && cls.getName() != "java.lang.Object" && cls.getName() != "scala.ScalaObject") {
      val supertype = java.lang.Class.forName(classname).getSuperclass()
      val interfaces = cls.getInterfaces().map {_.getName()}
      if (supertype == null) {
        val lin = cls.getName :: _linearizeHierarchy(interfaces)
        return lin
      }
      else {
        val lin = cls.getName :: _linearizeHierarchy(interfaces) ::: _linearizeHierarchy(supertype.getName)
        return lin
      }
    }
    Nil
  }

  def removeDuplicates(l:List[String]): List[String] = {
    l match {
      case a :: rest => a :: removeDuplicates(rest.filter(_ != a))
      case _         => l
    }
  }

  def linearizeHierarchy(classname: String): List[String] = removeDuplicates(_linearizeHierarchy(classname))

  def processHierarchy(args: Arguments, path: ClassPath[AbstractFile])(classname: String): Unit = {
    println(classname)
    val classes = linearizeHierarchy(classname)
    for (c <- classes) {
      process(args, path)(c)
    }
  }


  /**Executes scalap with the given arguments and classpath for the
   *  class denoted by <code>classname</code>.
   *
   * @param args...
   * @param path...
   * @param classname...
   */
  def process(args: Arguments, path: ClassPath[AbstractFile])(classname: String): Unit = {
    // find the classfile
    val encName = Names.encode(
      if (classname == "scala.AnyRef") "java.lang.Object"
      else classname)
    val cls = path.findClass(encName)
    // println ("source: " + cls)
    if (cls.isDefined && cls.get.binary.isDefined) {
      val cfile = cls.get.binary.get
      if (verbose) {
        Console.println(Console.BOLD + "FILENAME" + Console.RESET + " = " + cfile.path)
      }
      val bytes = cfile.toByteArray
      if (isScalaFile(bytes)) {
        decompileScala(bytes, isPackageObjectFile(encName))
      } else {
        // construct a reader for the classfile content
        val reader = new ByteArrayReader(cfile.toByteArray)
        // parse the classfile
        val clazz = new Classfile(reader)
        processJavaClassFile(clazz)
      }
    }
  }

  object EmptyClasspath extends ClassPath[AbstractFile] {
    import tools.nsc.util.ClassRep
    /**
     * The short name of the package (without prefix)
     */
    def name: String = ""
    val classes: List[ClassRep[AbstractFile]] = Nil
    val packages: List[ClassPath[AbstractFile]] = Nil
    val sourcepaths: List[AbstractFile] = Nil
  }


}
