import sbt._
import java.util.jar._
import java.io.File
import scala.util.matching.Regex
import smp.RunRegex

class Genesis(info: ProjectInfo) extends DefaultProject(info) 
    with AutoCompilerPlugins 
    with RunRegex 
    with AkkaProject 
{ 

  // val akkaCamel = akkaModule("camel")
  // val akkaHttp = akkaModule("http")
  // val akkaKernel = akkaModule("kernel")
  // val akkaMongo = akkaModule("persistence-mongo")
  // val akkaAMQP = akkaModule("amqp")
  // val akkaJTA = akkaModule("jta")
  // val akkaCassandra = akkaModule("persistence-cassandra")
  // val akkaRedis = akkaModule("persistence-redis")
  // val akkaSpring = akkaModule("spring")

  // val scalaTime = "org.scala-tools" % "time" % "2.8.0-0.2-SNAPSHOT"
  // val scalajCollection = "org.scalaj" % "scalaj-collection_2.8.0" % "1.0"
  
  val scalaToolsRepo = "Scala Tools Release Repository" at "http://scala-tools.org/repo-releases"
  val scalaToolsSnapRepo = "Scala Tools Snapshot Repository" at "http://scala-tools.org/repo-snapshots"
  val mavenOrgRepo = "Maven.Org Repository" at "http://repo1.maven.org/maven2/org/"

  val bumRepo = "Bum Networks Release Repository" at "http://repo.bumnetworks.com/releases/"
  val bumSnapsRepo = "Bum Networks Snapshots Repository" at "http://repo.bumnetworks.com/snapshots/"

  // val vscaladoc = "org.scala-tools" % "vscaladoc" % "1.1" % "compile;runtime;test"
  // val casbah = "com.novus" % "casbah_2.8.0" % "1.0" % "compile;runtime;test"
  // val configgy = "net.lag" % "configgy" % "2.8.0-1.5.5" % "compile;runtime;test"
  val mongodb = "org.mongodb" % "mongo-java-driver" % "2.1" % "compile;runtime;test"
  val mongodbDriver = "Osinka.com" % "mongo-scala-driver_2.8.0" % "0.8.5" % "compile;runtime;test"
  val log4j = "log4j" % "log4j" % "1.2.16" % "compile;runtime;test"
  val scalatest = "org.scalatest" % "scalatest" % "1.2-for-scala-2.8.0.final-SNAPSHOT"  % "compile;test"
  val junit4 = "junit" % "junit" % "4.8.1" % "compile;test"

  val sbtProcess =  "org.scala-tools.sbt" % "process_2.8.0" % "0.1" % "compile;runtime;test"

  // proguard configuration

  // todo: put this in a plugin
  //       make proguard task take main class and write jar file like: my.main.Class.jar
  //       figure out if compileClasspath should be runClasspath
  override def mainClass = Some("cc.acs.mongofs.gridfs.GridFSUI")

  /* ******* Proguard ****** */
  lazy val outputJar = outputPath / (name + "-" + version + "-standalone.jar")

  val proguardJar = "net.sf.proguard" % "proguard" % "4.3" % "tools->default"
  val toolsConfig = config("tools")
  def rootProjectDirectory = rootProject.info.projectPath
  val proguardConfigurationPath: Path = outputPath / "proguard.pro"
  lazy val proguard = proguardTask dependsOn(`package`, writeProguardConfiguration)
  private lazy val writeProguardConfiguration = writeProguardConfigurationTask dependsOn `package`
  //lazy val pack = packTask dependsOn(proguard)

  private def proguardTask = task {
    FileUtilities.clean(outputJar :: Nil, log)
    val proguardClasspathString = Path.makeString(managedClasspath(toolsConfig).get)
    val configFile = proguardConfigurationPath.toString
    val exitValue = Process("java", List("-Xmx256M", "-cp", proguardClasspathString, "proguard.ProGuard", "@" + configFile)) ! log
    if(exitValue == 0) None else Some("Proguard failed with nonzero exit code (" + exitValue + ")")
  }

  private def writeProguardConfigurationTask =
    task {
      /* the template for the proguard configuration file
       * You might try to remove "-keep class *" and "-keep class *", but this might break dynamic classloading.
       */
      val outTemplate = """
      |-dontskipnonpubliclibraryclasses
      |-dontskipnonpubliclibraryclassmembers
      |-dontoptimize
      |-dontobfuscate
      |-dontshrink
      |-dontpreverify
      |-dontnote
      |-dontwarn
      |-libraryjars %s
      |%s
      |-outjars %s
      |-ignorewarnings
      |-keep class *
      |-keep class %s$ { *** main(...); }
      |"""

      val defaultJar = (outputPath / defaultJarName).asFile.getAbsolutePath
      log.debug("proguard configuration using main jar " + defaultJar)

      val externalDependencies = Set() ++ (
        mainCompileConditional.analysis.allExternals ++ compileClasspath.get.map { _.asFile }
      ) map { _.getAbsoluteFile } filter { _.getName.endsWith(".jar") }

      def quote(s: Any) = '"' + s.toString + '"'
      log.debug("proguard configuration external dependencies: \n\t" + externalDependencies.mkString("\n\t"))
      // partition jars from the external jar dependencies of this project by whether they are located in the project directory
      // if they are, they are specified with -injars, otherwise they are specified with -libraryjars
      val (externalJars, libraryJars) = externalDependencies.toList.partition(jar => Path.relativize(rootProjectDirectory, jar).isDefined)
      log.debug("proguard configuration library jars locations: " + libraryJars.mkString(", "))
      // exclude properties files and manifests from scala-library jar
      val inJars = (quote(defaultJar) :: externalJars.map(quote(_) + "(!META-INF/**,!*.txt)")).map("-injars " + _).mkString("\n")

      val proguardConfiguration = outTemplate.stripMargin.format(libraryJars.map(quote).mkString(File.pathSeparator), inJars, quote(outputJar.absolutePath), mainClass.get)
      log.debug("Proguard configuration written to " + proguardConfigurationPath)
      FileUtilities.write(proguardConfigurationPath.asFile, proguardConfiguration, log)
    }
}
