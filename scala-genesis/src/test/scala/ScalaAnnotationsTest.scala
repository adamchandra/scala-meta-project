package test

import org.junit.Assert._
import org.junit.Before
import org.junit.Test
import org.scalatest.junit.JUnitSuite

import scala.reflect.Manifest


class AnnotationsInScalaTest extends JUnitSuite {

  trait DeferredDomain { 
    // root:DeferredDomain[T] =>
    // def domainClasses(implicit m:Manifest[T]) = Nil
  }


  trait DeclaredDomain[A] {
    def domainClasses(implicit m:Manifest[A]) = m
  }

  class A extends DeferredDomain
  class B extends A 
  class C extends B with DeclaredDomain[C]
  class D extends C with DeclaredDomain[C]


  @Test def manifesto:Unit = {
    val v = new D
    printClassInfo((new D).getClass)
    println("v.domainClasses: " + v.domainClasses)
  }
  
  def smokeScreen:Unit = {
    printClassInfo((new A).getClass)
    printClassInfo((new B).getClass)
    printClassInfo((new C).getClass)
  }

  def getClassHierarchy(clazz: Class[_]):List[(Class[_], List[Class[_]])] = {
    var c = clazz
    var h = List[(Class[_], List[Class[_]])]()
    while (c != null) {
      h = (c, c.getInterfaces.toList) :: h
      c = c.getSuperclass
    }
    h
  }


  def printClassInfo[T](clazz: Class[T]) {
    println("********************")
    println("clazz            :" +  clazz)
    println("fields           :" +  clazz.getFields.mkString(", "))
    println("annos            :" +  clazz.getAnnotations.mkString(", "))
    println("name             :" +  clazz.getName)
    println("parentInterfaces :" +  clazz.getInterfaces.mkString(", "))
    println("superClass       :" +  clazz.getSuperclass)
    println("typeParams       :" +  clazz.getTypeParameters.mkString(", "))
    println("classHierarchy   :" +  getClassHierarchy(clazz).mkString("(\n  ", "\n  ", "\n)"))
    
  }


  object WhichList {
    def apply[B](value: List[B])(implicit m: Manifest[B]) = println("List[%s]".format(m.toString))
  }

  def whichlist: Unit = {
    WhichList(List(1, 2, 3))
    WhichList(List(1.1, 2.2, 3.3))
    WhichList(List("one", "two", "three"))

    List(List(1, 2, 3), List(1.1, 2.2, 3.3), List("one", "two", "three")) foreach {
      WhichList(_)
    }

  }

}
