/*
 * todo:
 *   make this run from sbt s.t. other projects can list this as
 *   dependency and use the console
 *
 *   get debugger working
 *     breakpoints
 *     var inspections
 *     @report
 *
 *   edit command (visit def/class/file)
 *
  * figure out how sbt managed deps work
 */


object ScalaRC {
  import scala.collection.JavaConversions._
  import scala.tools.nsc.util.{ClassPath, JavaClassPath}
  import Scalap._


  /**
   * prints out:
   *     type(s), mixins, bases
   *     package/namespace
   *     source file, if available
   *     scala/javadocs
   *   For objects:
   *     toString (or mkString, if available)
   * 
   */
  def info(o:Object) {
    pcls(o.getClass().getName())
  }

  /**
   * prints out a dependency tree for the given function, e.g.
   * pdep( pdefs )
   * -- pdefs
   *    | -- java.lang.Object.getClass   
   *    | -- java.lang.Class.getMethods
   *    | -- scala...println
   *
   */
  def deps() {
    
  }

  /**
   * return current package
   */
  def pk() {
  }

  /**
   * set current package
   */
  def chpk() {
  }
  
  /**
   * list definitions in scope
   */
  def defs() {

  }

  /**
   * converts number to value at index of most
   * recently output list
   * e.g.,
   * > defs()
   * 1. foo
   * 2. bar
   * 3. baz
   * > edit(3) is equivalent to edit("baz")
   */
  implicit def listIndexToString(n:Int): String = {
    ""
  }

  /**
   * edit the function/class with given name
   * possibly use prefix matching on name
   */
  def edit(fname:String) {
    
  }

  def pcls(classname: String)  {
    import scala.tools.scalap.Arguments
    val args = Arguments.parse("")(Array(""))
    processHierarchy(args, classpath())(classname)
  }

  def props() {
    for (i <- System.getProperties().entrySet()) 
      println(i)
  } 

  def prop(p:String): String = {
    System.getProperty(p)
  } 

  def classpath(): JavaClassPath = {
    new JavaClassPath(
      prop("sun.boot.class.path"), // boot
      prop("java.ext.dirs"),       // ext
      prop("java.class.path"),     // user
      "",                          // src
      "")                          // xcode
  }
}

