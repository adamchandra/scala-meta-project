package smp

import sbt._

trait RunRegex extends Project {
	def getMainClassByString(str: String): Option[String] = getMainClassByString(str, mainCompileConditional)
	def getMainClassByString(str: String, compileConditional: CompileConditional): Option[String] = {
    import scala.util.matching.Regex
    def indent(s:String, i:Int):String = {stringWrapper(" ")+s}
    def printList(ls:List[String]) { for (l <- ls) {println(indent(l, 2))} }
    def printApps(apps:List[String]) = apps match {
      case x :: xs => { println("Several apps match:"); printList(apps) }
      case Nil     => println("No apps match regex")
    }

		val applications = compileConditional.analysis.allApplications.toList
    val apps = applications.filter{ ("(?i)"+str).r findFirstIn _ isDefined }
    apps match {
      case x :: Nil  => Some(x)
      case _         => {
        printApps(apps)
        None
      }
    }
	}

	protected def runSubstringAction = task { 
    args => args.toList match {
      case x :: xs =>  runTask(getMainClassByString(x.toString), runClasspath, xs) dependsOn(compile, copyResources)
      case _       =>  runTask(getMainClass(true), runClasspath, args) dependsOn(compile, copyResources)
    }
  }
	lazy val runs = runSubstringAction
}

trait RunnableSubprojects extends Project {
  import java.io.File
  import java.net.{URL, URLClassLoader}
  import java.lang.reflect.Method

	def scala = buildScalaInstance
  val scalaJarList = scala.libraryJar :: scala.compilerJar :: Nil
  def runtimeClasspath: PathFinder = runClasspath +++ Path.finder(scalaJarList)
  def runtimeLoader: URLClassLoader = new URLClassLoader(runtimeClasspath.get.toList.map(_.asURL).toArray)
  def findRuntimeClass(className: String) = Class.forName(className, true, runtimeLoader)
  def getMainClassExt(className: String): Option[String] = Some(findRuntimeClass(className).getName)

	def runDeepAction = task { 
    args => {
      val maincls = try {
        getMainClassExt(args(0))
      } catch {
        case e: ClassNotFoundException => 
          println("class not found"); None
        case e: ArrayIndexOutOfBoundsException => 
          println("no class specified"); None
        case e: Exception => 
          println("exception" + e.getMessage); None
      } 
      runTask(maincls, runClasspath +++ mainDependencies.scalaJars, args.drop(1)) dependsOn(compile, copyResources) 
    }
  }

  lazy val runDeep = runDeepAction
}

trait Terracotta extends Project {
  lazy val runtc = runWithTerracotta
  def runWithTerracotta = task {
    args => {
		  val si = buildScalaInstance
      val scalaJarList = si.libraryJar :: si.compilerJar :: Nil
		  val fsr = new ForkScalaRun {
			  override def scalaJars = scalaJarList
			  override def workingDirectory: Option[File] = None
			  override def runJVMOptions: Seq[String] = List(
          "-Xbootclasspath/p:ext/terracotta-3.2.0/lib/dso-boot/dso-boot-hotspot_linux_160_14.jar", // TODO: use mac/linux/win version
          "-Dtc.install-root=ext/terracotta-3.2.0",  // TODO: include tc as maven-style dep, rather than subproject
          "-Dtc.config=config/terracotta/tc-config.xml", // TODO: dynamically create this config
          "-Xmx256M", "-Xms256M"
        )
		  }
      runTask(getMainClass(true), runClasspath +++ mainDependencies.scalaJars, args) (new ForkJavaRun(fsr))
    }
  }
}

trait AspectJ extends Project {
  lazy val runaj = runWithAspectJ
  def runWithAspectJ = task {
    args => {
		  val si = buildScalaInstance
      val scalaJarList = si.libraryJar :: si.compilerJar :: Nil
		  val fsr = new ForkScalaRun {
			  override def scalaJars = scalaJarList
			  override def workingDirectory: Option[File] = None
			  override def runJVMOptions: Seq[String] = List(
          "-Xmx256M", "-Xms256M",
          "-javaagent:lib_managed/scala_2.8.0.RC2/compile/aspectjweaver-1.6.8.jar"
        )
		  }
      runTask(getMainClass(true), "aspects.jar" +++ runClasspath +++ mainDependencies.scalaJars, args) (new ForkJavaRun(fsr))
    }
  }

  // Run scala code via java command. This differs from running code using scala in that scala jars (lib/compiler) are
  // placed on the normal classpath, rather than the bootclasspath, which is necessary when running a customized
  // classloader (e.g., terracotta, aspectJ), which needs to intercept and instrument some of the library classes as they
  // are loaded.
  class ForkJavaRun(config: ForkScalaRun) extends ScalaRun {
    def run(mainClass: String, classpath: Iterable[Path], options: Seq[String], log: Logger): Option[String] = {
      val alloptions: Seq[String] = config.runJVMOptions.toList ::: classpathOption(classpath) ::: mainClass :: options.toList
      val exitCode = config.outputStrategy match {
        case Some(strategy) => Fork.java(config.javaHome, alloptions, config.workingDirectory, strategy)
        case None           => Fork.java(config.javaHome, alloptions, config.workingDirectory, LoggedOutput(log))
      }
      processExitCode(exitCode, "runner")
    }
    private def classpathOption(classpath: Iterable[Path]) = {
      "-cp" :: Path.makeString(classpath) :: Nil
    }
    private def processExitCode(exitCode: Int, label: String) = {
      if (exitCode == 0) None
      else Some("Nonzero exit code returned from " + label + ": " + exitCode)
    }
  }


  // todo make ajc output to proper target dir

  // todo run this from sbt:
  // java -classpath project/boot/scala-2.8.0.RC1/lib/scala-library.jar:
  //                 lib_managed/scala_2.8.0.RC1/compile/aspectjrt-1.6.8.jar:
  //                 lib_managed/scala_2.8.0.RC1/compile/aspectjtools-1.6.8.jar:
  //                 target/scala_2.8.0.RC1/classes
  //                 org.aspectj.tools.ajc.Main -outxml -outjar aj.jar src/main/scala/acs/LogComplex.aj
  //  aj.jar needs to be on classpath
  //
  // todo make sure resources are copied properly
  // todo make -javaagent:... pick up correct aspectjweaver-*.jar without specifying explicitly
  // todo create new action 'aspect ...'



  import ScalaProject.{optionsAsString, javaOptionsAsString}
  object cconfig extends BaseCompileConfig {
    def baseCompileOptions = compileOptions
    def label = "ajc"
    def sourceRoots = mainSourcePath / "scala";
    def sources = descendents(sourceRoots, "*.aj")
    def outputDirectory = outputPath / "resources"
    def classpath = compileClasspath
    def analysisPath = outputPath / "ajc-analysis"
		def fingerprints = Fingerprints(Nil, Nil)
		def javaOptions = javaOptionsAsString(javaCompileOptions)
  }

  def ajcc = new CompileConditional(cconfig, buildCompiler)

	def ajccAction = task {
    args => {
      val ajcMain = "org.aspectj.tools.ajc.Main"
		  val si = buildScalaInstance

      class MyForkRun extends ScalaRun {
        object config extends ForkScalaRun {
	        override def javaHome: Option[File] = None
	        override def outputStrategy: Option[OutputStrategy] = None
	        override def scalaJars: Iterable[File] = si.libraryJar :: si.compilerJar :: Nil
	        override def workingDirectory: Option[File] = None
	        override def runJVMOptions: Seq[String] = javaOptionsAsString(javaCompileOptions)
        }
	      def run(mainClass: String, classpath: Iterable[Path], options: Seq[String], log: Logger): Option[String] = {
          val sourceList:Seq[String] = ajcc.config.sources.get.toList.map (_.toString)
          val ajcMain = "org.aspectj.tools.ajc.Main"
		      val alloptions:Seq[String] = ("-cp" :: 
                                        Path.makeString(classpath) :: 
                                        ajcMain :: 
                                        "-outxml" :: "-outjar" :: "aspects.jar" ::
                                        options.toList ::: sourceList.toList)
		      val exitCode = config.outputStrategy match {
            case Some(strategy) => Fork.java(None, alloptions, config.workingDirectory, strategy)
            case None           => Fork.java(None, alloptions, config.workingDirectory, LoggedOutput(log))
		      }
		      if (exitCode == 0) None
		      else Some("Nonzero exit code returned from ajc" + exitCode)
	      }
      }
      runTask(Some(ajcMain), runClasspath +++ mainDependencies.scalaJars, args) (new MyForkRun)
    }
  } 

  lazy val ajc = ajccAction
}
