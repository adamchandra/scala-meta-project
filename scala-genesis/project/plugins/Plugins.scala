import sbt._

class Plugins(info: ProjectInfo) extends PluginDefinition(info)
{
  val a = "acs" % "sbt-extensions" % "0.1"
  val akkaPlugin = "se.scalablesolutions.akka" % "akka-sbt-plugin" % "0.10"
}

