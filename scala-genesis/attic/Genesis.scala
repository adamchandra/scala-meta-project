import sbt._
import java.util.jar._
import java.io.File
import scala.util.matching.Regex

class Genesis(info: ProjectInfo) extends DefaultProject(info) { 
  // val scalaTime = "org.scala-tools" % "time" % "2.8.0-0.2-SNAPSHOT"
  // val scalajCollection = "org.scalaj" % "scalaj-collection_2.8.0" % "1.0"
  // val mongodb = "org.mongodb" % "mongo-java-driver" % "2.0"
  
  val scalaToolsRepo = "Scala Tools Release Repository" at "http://scala-tools.org/repo-releases"
  val scalaToolsSnapRepo = "Scala Tools Snapshot Repository" at "http://scala-tools.org/repo-snapshots"
  val mavenOrgRepo = "Maven.Org Repository" at "http://repo1.maven.org/maven2/org/"

   
  val bumRepo = "Bum Networks Release Repository" at "http://repo.bumnetworks.com/releases/"
  val bumSnapsRepo = "Bum Networks Snapshots Repository" at "http://repo.bumnetworks.com/snapshots/"


  val vscaladoc = "org.scala-tools" % "vscaladoc" % "1.1"
  val casbah = "com.novus" % "casbah_2.8.0" % "1.0"
  val scalatest = "org.scalatest" % "scalatest" % "1.2-for-scala-2.8.0.final-SNAPSHOT" 

  val configgy = "net.lag" % "configgy" % "1.5.2"


  val log4j = "log4j" % "log4j" % "1.2.16"

  // override def compileOptions = super.compileOptions ++ Seq(Unchecked, ExplainTypes, Deprecation)
}

