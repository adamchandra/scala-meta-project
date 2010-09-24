package acs

import Element.elem

abstract class Element { 
  def content: Array[String]
  def width: Int = if (height==0) 0 else content(0).length 
  def height: Int = content.length

  def above(that:Element) : Element = {  
    val e1 = this widen that.width
    val e2 = that widen this.width
    elem(e1.content ++ e2.content)
  }

  def beside(that:Element) : Element = {  
    val e1 = this heighten that.height
    val e2 = that heighten this.height
    elem(
      for { (a, b) <- e1.content zip e2.content
      } yield a + b
    )
  }

  def widen(n: Int) : Element = {  
    if (n <= width) 
      this
    else {
      val left = elem(' ', (n-width)/2, height)
      val right = elem(' ', n-left.width-width, height)
      left beside this beside right
    }
  }

  def heighten(h: Int) : Element = {  
    if (h <= height) 
      this
    else {
      val top = elem(' ', width, (h-height)/2)
      val bottom = elem(' ', width, h-top.height-height)
      top above this above bottom
    }
  }
  override def toString = content mkString "\n"
}

object ElementRoot {
  var share = elem("none")

  def init(ss:String) = {
    share = elem(ss) 
  }
  def beside(ss:String) = {
    share = share beside elem(ss)
  }
  def above(ss:String) = {
    share = share above elem(ss)
  }
  def show() = {
    println(share)
  }
}

object Element {
  var sharedElement = ElementRoot

  class UniformElement(c:Char, width:Int, height:Int) extends Element {
    override val content = Array(c.toString*width) // Array.fill(height)(c.toString*width)
  }

  class StringElement(s:String) extends Element {
    override val content = Array(s)
  }

  class ArrayElement(ss:Array[String]) extends Element {
    override val content = ss
  }

  def elem(s:String): Element = {
    new StringElement(s)
  }

  def elem(c:Char, width:Int, height:Int): Element = {
    new UniformElement(c, width, height)
  }

  def elem(ss:Array[String]): Element = {
    new ArrayElement(ss)
  }

  import Console._
  def userInput(): Array[String] = {
    readLine().split("\\s+", 2) match {
      case Array(a) => Array(a, "");
      case Array(a, b) => Array(a, b);
      case _ => Array("", "");
    }
  }

  def readloop(): Unit = {
    var cmd, arg = ""
    while (cmd != "quit") {
      var Array(cmd, arg) = userInput()
      if (cmd == "init") {
        sharedElement.init(arg)
      }
      else if (cmd == "beside") {
        sharedElement.beside(arg)
      }
      else if (cmd == "above") {
        sharedElement.above(arg)
      }
      else if (cmd == "show") {
        sharedElement.show()
      }
    }
  }

  def main(args: Array[String]) = {
    readloop()
  }
}
