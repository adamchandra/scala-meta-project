import com.weiglewilczek.bnd4sbt.BNDPlugin
import java.io.File
import java.util.jar.Attributes
import java.util.jar.Attributes.Name._
import sbt._
import sbt.CompileOrder._
import spde._

class MetaProject(info: ProjectInfo) extends DefaultProject(info) {

  // -------------------------------------------------------------------------------------------------------------------
  // Compile settings
  // -------------------------------------------------------------------------------------------------------------------

  override def compileOptions = super.compileOptions ++
    Seq("-deprecation",
        "-Xmigration",
        "-Xcheckinit",
        "-Xstrict-warnings",
        "-Xwarninit",
        "-encoding", "utf8")
        .map(x => CompileOption(x))

  override def javaCompileOptions = JavaCompileOption("-Xlint:unchecked") :: super.javaCompileOptions.toList

  // -------------------------------------------------------------------------------------------------------------------
  // Deploy/dist settings
  // -------------------------------------------------------------------------------------------------------------------

  lazy val deployPath = info.projectPath / "deploy"
  lazy val distPath = info.projectPath / "dist"
  def distName = "%s_%s-%s.zip".format(name, buildScalaVersion, version)

  // -------------------------------------------------------------------------------------------------------------------
  // All repositories *must* go here! See ModuleConigurations below.
  // -------------------------------------------------------------------------------------------------------------------

  object Repositories {
    lazy val AkkaRepo               = MavenRepository("Akka Repository", "http://scalablesolutions.se/akka/repository")
    lazy val CasbahRepo             = MavenRepository("Casbah Repo", "http://repo.bumnetworks.com/releases")
    lazy val CasbahSnapshotRepo     = MavenRepository("Casbah Snapshots", "http://repo.bumnetworks.com/snapshots")
    lazy val CodehausRepo           = MavenRepository("Codehaus Repo", "http://repository.codehaus.org")
    lazy val EmbeddedRepo           = MavenRepository("Embedded Repo", (info.projectPath / "embedded-repo").asURL.toString)
    lazy val FusesourceSnapshotRepo = MavenRepository("Fusesource Snapshots", "http://repo.fusesource.com/nexus/content/repositories/snapshots")
    lazy val GuiceyFruitRepo        = MavenRepository("GuiceyFruit Repo", "http://guiceyfruit.googlecode.com/svn/repo/releases/")
    lazy val JBossRepo              = MavenRepository("JBoss Repo", "https://repository.jboss.org/nexus/content/groups/public/")
    lazy val JavaNetRepo            = MavenRepository("java.net Repo", "http://download.java.net/maven/2")
    lazy val SonatypeSnapshotRepo   = MavenRepository("Sonatype OSS Repo", "http://oss.sonatype.org/content/repositories/releases")
    lazy val SunJDMKRepo            = MavenRepository("Sun JDMK Repo", "http://wp5.e-taxonomy.eu/cdmlib/mavenrepo")
    lazy val CasbahRepoReleases     = MavenRepository("Casbah Release Repo", "http://repo.bumnetworks.com/releases")
    lazy val ZookeeperRepo          = MavenRepository("Zookeeper Repo", "http://lilycms.org/maven/maven2/deploy/")
  }

  // -------------------------------------------------------------------------------------------------------------------
  // ModuleConfigurations
  // Every dependency that cannot be resolved from the built-in repositories (Maven Central and Scala Tools Releases)
  // must be resolved from a ModuleConfiguration. This will result in a significant acceleration of the update action.
  // Therefore, if repositories are defined, this must happen as def, not as val.
  // -------------------------------------------------------------------------------------------------------------------

  import Repositories._
  lazy val atmosphereModuleConfig  = ModuleConfiguration("org.atmosphere", SonatypeSnapshotRepo)
  lazy val jettyModuleConfig       = ModuleConfiguration("org.eclipse.jetty", sbt.DefaultMavenRepository)
  lazy val guiceyFruitModuleConfig = ModuleConfiguration("org.guiceyfruit", GuiceyFruitRepo)
  // lazy val hawtdispatchModuleConfig  = ModuleConfiguration("org.fusesource.hawtdispatch", FusesourceSnapshotRepo)
  lazy val jbossModuleConfig       = ModuleConfiguration("org.jboss", JBossRepo)
  lazy val jdmkModuleConfig        = ModuleConfiguration("com.sun.jdmk", SunJDMKRepo)
  lazy val jmsModuleConfig         = ModuleConfiguration("javax.jms", SunJDMKRepo)
  lazy val jmxModuleConfig         = ModuleConfiguration("com.sun.jmx", SunJDMKRepo)
  lazy val jerseyContrModuleConfig = ModuleConfiguration("com.sun.jersey.contribs", JavaNetRepo)
  lazy val jerseyModuleConfig      = ModuleConfiguration("com.sun.jersey", JavaNetRepo)
  lazy val jgroupsModuleConfig     = ModuleConfiguration("jgroups", JBossRepo)
  lazy val multiverseModuleConfig  = ModuleConfiguration("org.multiverse", CodehausRepo)
  lazy val nettyModuleConfig       = ModuleConfiguration("org.jboss.netty", JBossRepo)
  lazy val scalaTestModuleConfig   = ModuleConfiguration("org.scalatest", ScalaToolsSnapshots)
  lazy val logbackModuleConfig     = ModuleConfiguration("ch.qos.logback",sbt.DefaultMavenRepository)
  lazy val atomikosModuleConfig    = ModuleConfiguration("com.atomikos",sbt.DefaultMavenRepository)
  lazy val casbahRelease           = ModuleConfiguration("com.novus",CasbahRepoReleases)
  lazy val zookeeperRelease        = ModuleConfiguration("org.apache.hadoop.zookeeper",ZookeeperRepo)
  lazy val casbahModuleConfig      = ModuleConfiguration("com.novus", CasbahRepo)
  lazy val timeModuleConfig        = ModuleConfiguration("org.scala-tools", "time", CasbahSnapshotRepo)
  lazy val embeddedRepo            = EmbeddedRepo // This is the only exception, because the embedded repo is fast!

  // -------------------------------------------------------------------------------------------------------------------
  // Versions
  // -------------------------------------------------------------------------------------------------------------------

  lazy val ATMO_VERSION          = "0.6.1"
  lazy val CAMEL_VERSION         = "2.4.0"
  lazy val CASSANDRA_VERSION     = "0.6.1"
  lazy val DISPATCH_VERSION      = "0.7.4"
  lazy val HAWT_DISPATCH_VERSION = "1.0"
  lazy val JACKSON_VERSION       = "1.2.1"
  lazy val JERSEY_VERSION        = "1.2"
  lazy val MULTIVERSE_VERSION    = "0.6.1"
  lazy val SCALATEST_VERSION     = "1.2-for-scala-2.8.0.final-SNAPSHOT"
  lazy val LOGBACK_VERSION       = "0.9.24"
  lazy val SLF4J_VERSION         = "1.6.0"
  lazy val SPRING_VERSION        = "3.0.3.RELEASE"
  lazy val ASPECTWERKZ_VERSION   = "2.2.1"
  lazy val JETTY_VERSION         = "7.1.4.v20100610"
  lazy val AKKA_VERSION          = "0.10"
  lazy val SCALACHECK_VERSION    = "1.7"

  // -------------------------------------------------------------------------------------------------------------------
  // Dependencies
  // -------------------------------------------------------------------------------------------------------------------

  object Dependencies {
    def akkaModule(module: String) = "se.scalablesolutions.akka" %% ("akka-" + module) % AKKA_VERSION

    lazy val akkaAMQP                  = akkaModule("amqp")
    lazy val akkaCamel                 = akkaModule("camel")
    lazy val akkaCassandra             = akkaModule("persistence-cassandra")
    lazy val akkaCore                  = akkaModule("core")
    lazy val akkaHttp                  = akkaModule("http")
    lazy val akkaJTA                   = akkaModule("jta")
    lazy val akkaKernel                = akkaModule("kernel")
    lazy val akkaMongo                 = akkaModule("persistence-mongo")
    lazy val akkaRedis                 = akkaModule("persistence-redis")
    lazy val akkaSpring                = akkaModule("spring")

    lazy val annotation                = "javax.annotation"            % "jsr250-api" % "1.0" % "compile"
    lazy val aopalliance               = "aopalliance"                 % "aopalliance" % "1.0" % "compile"
    lazy val atmo                      = "org.atmosphere"              % "atmosphere-annotations"     % ATMO_VERSION % "compile"
    lazy val atmo_jbossweb             = "org.atmosphere"              % "atmosphere-compat-jbossweb" % ATMO_VERSION % "compile"
    lazy val atmo_jersey               = "org.atmosphere"              % "atmosphere-jersey"          % ATMO_VERSION % "compile"
    lazy val atmo_runtime              = "org.atmosphere"              % "atmosphere-runtime"         % ATMO_VERSION % "compile"
    lazy val atmo_tomcat               = "org.atmosphere"              % "atmosphere-compat-tomcat"   % ATMO_VERSION % "compile"
    lazy val atmo_weblogic             = "org.atmosphere"              % "atmosphere-compat-weblogic" % ATMO_VERSION % "compile"
    lazy val atomikos_transactions     = "com.atomikos"                % "transactions"     % "3.2.3" % "compile"
    lazy val atomikos_transactions_api = "com.atomikos"                % "transactions-api" % "3.2.3" % "compile"
    lazy val atomikos_transactions_jta = "com.atomikos"                % "transactions-jta" % "3.2.3" % "compile"
    lazy val camel_core                = "org.apache.camel"            % "camel-core" % CAMEL_VERSION % "compile"
    lazy val camel_spring              = "org.apache.camel"            % "camel-spring"        % CAMEL_VERSION     % "compile;test"
    lazy val casbah                    = "com.novus"                   % "casbah_2.8.0" % "1.0.8.5" % "compile"
    lazy val cassandra                 = "org.apache.cassandra"        % "cassandra" % CASSANDRA_VERSION % "compile"
    lazy val commons_codec             = "commons-codec"               % "commons-codec" % "1.4" % "compile"
    lazy val commons_coll              = "commons-collections"         % "commons-collections" % "3.2.1"           % "compile;test"
    lazy val commons_io                = "commons-io"                  % "commons-io" % "1.4" % "compile"
    lazy val commons_pool              = "commons-pool"                % "commons-pool" % "1.5.4" % "compile"
    lazy val configgy                  = "net.lag"                     % "configgy" % "2.8.0-1.5.5" % "compile"
    lazy val dispatch_http             = "net.databinder"              % "dispatch-http_2.8.0" % DISPATCH_VERSION % "compile"
    lazy val dispatch_json             = "net.databinder"              % "dispatch-json_2.8.0" % DISPATCH_VERSION % "compile"
    lazy val google_coll               = "com.google.collections"      % "google-collections"  % "1.0"             % "compile;test"
    lazy val guicey                    = "org.guiceyfruit"             % "guice-all" % "2.0" % "compile"
    lazy val h2_lzf                    = "voldemort.store.compress"    % "h2-lzf" % "1.0" % "compile"
    lazy val hadoop_core               = "org.apache.hadoop"           % "hadoop-core" % "0.20.2" % "compile"
    lazy val hawtdispatch              = "org.fusesource.hawtdispatch" % "hawtdispatch-scala" % HAWT_DISPATCH_VERSION % "compile"
    lazy val hbase_core                = "org.apache.hbase"            % "hbase-core" % "0.20.6" % "compile"
    lazy val jackson                   = "org.codehaus.jackson"        % "jackson-mapper-asl" % JACKSON_VERSION % "compile"
    lazy val jackson_core              = "org.codehaus.jackson"        % "jackson-core-asl"   % JACKSON_VERSION % "compile"
    lazy val jackson_core_asl          = "org.codehaus.jackson"        % "jackson-core-asl"   % JACKSON_VERSION % "compile"
    lazy val jersey                    = "com.sun.jersey"              % "jersey-core"   % JERSEY_VERSION % "compile"
    lazy val jersey_contrib            = "com.sun.jersey.contribs"     % "jersey-scala"  % JERSEY_VERSION % "compile"
    lazy val jersey_json               = "com.sun.jersey"              % "jersey-json"   % JERSEY_VERSION % "compile"
    lazy val jersey_server             = "com.sun.jersey"              % "jersey-server" % JERSEY_VERSION % "compile"
    lazy val jetty                     = "org.eclipse.jetty"           % "jetty-server"  % JETTY_VERSION % "compile"
    lazy val jetty_servlet             = "org.eclipse.jetty"           % "jetty-servlet" % JETTY_VERSION % "compile"
    lazy val jetty_util                = "org.eclipse.jetty"           % "jetty-util"    % JETTY_VERSION % "compile"
    lazy val jetty_xml                 = "org.eclipse.jetty"           % "jetty-xml"     % JETTY_VERSION % "compile"
    lazy val jgroups                   = "jgroups"                     % "jgroups" % "2.9.0.GA" % "compile"
    lazy val jsr166x                   = "jsr166x"                     % "jsr166x" % "1.0" % "compile"
    lazy val jsr250                    = "javax.annotation"            % "jsr250-api" % "1.0" % "compile"
    lazy val jsr311                    = "javax.ws.rs"                 % "jsr311-api" % "1.1" % "compile"
    lazy val jta_1_1                   = "org.apache.geronimo.specs"   % "geronimo-jta_1.1_spec" % "1.1.1" % "compile" intransitive
    lazy val log4j                     = "log4j"                       % "log4j"               % "1.2.16"          % "compile;test"
    lazy val logback                   = "ch.qos.logback"              % "logback-classic" % LOGBACK_VERSION % "compile"
    lazy val logback_core              = "ch.qos.logback"              % "logback-core" % LOGBACK_VERSION % "compile"
    lazy val mongoj                    = "org.mongodb"                 % "mongo-java-driver" % "2.1" % "compile;runtime;test"
    lazy val mongos                    = "Osinka.com"                  % "mongo-scala-driver_2.8.0" % "0.8.5" % "compile;runtime;test"
    lazy val multiverse                = "org.multiverse"              % "multiverse-alpha" % MULTIVERSE_VERSION % "compile" intransitive
    lazy val netty                     = "org.jboss.netty"             % "netty" % "3.2.2.Final" % "compile"
    lazy val osgi_core                 = "org.osgi"                    % "org.osgi.core" % "4.2.0"
    lazy val protobuf                  = "com.google.protobuf"         % "protobuf-java" % "2.3.0" % "compile"
    lazy val rabbit                    = "com.rabbitmq"                % "amqp-client" % "1.8.1" % "compile"
    lazy val redis                     = "com.redis"                   % "redisclient" % "2.8.0-2.0" % "compile"
    lazy val sbinary                   = "sbinary"                     % "sbinary" % "2.8.0-0.3.1" % "compile"
    lazy val sbt_process               = "org.scala-tools.sbt"         % "process_2.8.0" % "0.1" % "compile;runtime;test"
    lazy val sjson                     = "sjson.json"                  % "sjson" % "0.8-2.8.0" % "compile"
    lazy val slf4j                     = "org.slf4j"                   % "slf4j-api"     % SLF4J_VERSION % "compile"
    lazy val spring_beans              = "org.springframework"         % "spring-beans"   % SPRING_VERSION % "compile"
    lazy val spring_context            = "org.springframework"         % "spring-context" % SPRING_VERSION % "compile"
    lazy val stax_api                  = "javax.xml.stream"            % "stax-api" % "1.0-2" % "compile"
    lazy val tagsoup                   = "org.ccil.cowan"              % "tagsoup" % "1.2" % "compile"
    lazy val thrift                    = "com.facebook"                % "thrift" % "r917130" % "compile"
    lazy val werkz                     = "org.codehaus.aspectwerkz"    % "aspectwerkz-nodeps-jdk5" % ASPECTWERKZ_VERSION % "compile"
    lazy val werkz_core                = "org.codehaus.aspectwerkz"    % "aspectwerkz-jdk5"        % ASPECTWERKZ_VERSION % "compile"
    lazy val zookeeper                 = "org.apache.hadoop.zookeeper" % "zookeeper" % "3.2.2" % "compile"

    // lazy val scalaTime = "org.scala-tools" % "time" % "2.8.0-0.2-SNAPSHOT"
    // lazy val scalajCollection = "org.scalaj" % "scalaj-collection_2.8.0" % "1.0"

    // Test
    lazy val cassandra_clhm   = "org.apache.cassandra"    % "clhm-production"     % CASSANDRA_VERSION % "test"
    lazy val hadoop_test      = "org.apache.hadoop"       % "hadoop-test"         % "0.20.2"          % "test"
    lazy val hbase_test       = "org.apache.hbase"        % "hbase-test"          % "0.20.6"          % "test"
    lazy val high_scale       = "org.apache.cassandra"    % "high-scale-lib"      % CASSANDRA_VERSION % "test"
    lazy val junit            = "junit"                   % "junit"               % "4.8.1"           % "test"
    lazy val mockito          = "org.mockito"             % "mockito-all"         % "1.8.1"           % "test"
    lazy val scalatest        = "org.scalatest"           % "scalatest"           % SCALATEST_VERSION % "test"
    lazy val scalacheck       = "org.scala-tools.testing" % "scalacheck_2.8.0"    % SCALACHECK_VERSION

  }

  // ------------------------------------------------------------
  class DefaultSubProject(info: ProjectInfo, val deployPath: Path) extends DefaultProject(info) with RunRegex {
    lazy val sourceArtifact = Artifact(this.artifactID, "sources", "jar", Some("sources"), Nil, None)
    lazy val docsArtifact = Artifact(this.artifactID, "docs", "jar", Some("docs"), Nil, None)
    override def runClasspath = super.runClasspath +++ (MetaProject.this.info.projectPath / "config")
    override def testClasspath = super.testClasspath +++ (MetaProject.this.info.projectPath / "config")
    override def packageDocsJar = this.defaultJarPath("-docs.jar")
    override def packageSrcJar  = this.defaultJarPath("-sources.jar")
    override def packageToPublishActions = super.packageToPublishActions ++ Seq(this.packageDocs, this.packageSrc)
  }

  // -------------------------------------------------------------------------------------------------------------------
  // Subprojects
  // -------------------------------------------------------------------------------------------------------------------

  // lazy val subproj = project("proj-name", "proj-path", new SubProject(_), dep_proj, dep_proj,...)
  lazy val scalaGenesis         = project("scala-genesis", "scala-genesis", new ScalaGenesisProject(_))

  class ScalaGenesisProject(info: ProjectInfo) extends DefaultSubProject(info, distPath) {
    val mongoj = Dependencies.mongoj
    val mongos = Dependencies.mongos
    val process = Dependencies.sbt_process
    val junit = Dependencies.junit
    val scalatest = Dependencies.scalatest
    val akkaCore = Dependencies.akkaCore
  }

  lazy val spidie               = project("spidie", "spidie", new SpidieProject(_))
  class SpidieProject(info: ProjectInfo) extends DefaultSubProject(info, distPath) {
    val mongoj = Dependencies.mongoj
    val mongos = Dependencies.mongos
    val tagsoup = Dependencies.tagsoup
  }

  lazy val robotRulesParser     = project("robot-rules-parser", "robot-rules-parser", new RobotRulesParserProject(_))
  class RobotRulesParserProject(info: ProjectInfo) extends DefaultSubProject(info, distPath) {
    val log4j = Dependencies.log4j
  }

  lazy val tonysOptionExercises = project("tonys-option-exercises", "tonys-option-exercises", new TonysOptionExercisesProject(_))
  class TonysOptionExercisesProject(info: ProjectInfo) extends DefaultSubProject(info, distPath) {
    val junit = Dependencies.junit
    val scalatest = Dependencies.scalatest
    val scalacheck = Dependencies.scalacheck
    val log4j = Dependencies.log4j
  }

  lazy val dynamicProgrammingEx = project("dynamic-programming-ex", "dynamic-programming-ex", new DynamicProgrammingEx(_))
  class DynamicProgrammingEx(info: ProjectInfo) extends DefaultSubProject(info, distPath) {
    val junit = Dependencies.junit
    val scalatest = Dependencies.scalatest
  }

  //class PlaygroundProject(info: ProjectInfo) extends DefaultSubProject(info, distPath) {
  //  val junit = Dependencies.junit
  //  val scalatest = Dependencies.scalatest
  //  val log4j = Dependencies.log4j
  //}

}

trait RunRegex {
  self: DefaultProject => 
	def getMainClassByRegex(str: String): Option[String] = getMainClassByRegex(str, mainCompileConditional)
	def getMainClassByRegex(str: String, compileConditional: CompileConditional): Option[String] = {
    import scala.util.matching.Regex
    def indent(s:String, i:Int):String = {stringWrapper(" ")+s}
    def printList(ls:List[String]) { for (l <- ls) { println(indent(l, 2))} }
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

	protected def runByRegexAction = task { 
    args => args.toList match {
      case x :: xs =>  runTask(getMainClassByRegex(x.toString), runClasspath, xs) dependsOn(compile, copyResources)
      case _       =>  runTask(getMainClass(true), runClasspath, args) dependsOn(compile, copyResources)
    }
  }
	lazy val runs = runByRegexAction describedAs "select and run an application based on case-insensitive regex, e.g., runs mypack.*Myapp$ arg1 arg2"
}