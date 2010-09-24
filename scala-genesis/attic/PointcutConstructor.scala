// Sample aspectJ for scala stuff
// Pointcutting a constructor
package acs


// example code copied from 
//   http://blog.jayway.com/2010/04/28/intercepting-scala-trait-constructors
  
trait MyTrait {
    var someStuff = "someStuff"
    def someMethod() : String = {
        "do something"
    }
}

class SomeClass extends MyTrait

// public aspect MyAspect {
// 	// public pointcut newInstance() : execution(MyTrait+.new(..));
//   public pointcut newInstance() : execution(void MyTrait$class.$init$(..));
// 	public pointcut someInvocation() : execution(String MyTrait+.someMethod(..));
// 	after() : newInstance() {
// 	    System.out.println("after MyTrait constructor invocation");
// 	}
// 	after() : someInvocation() {
// 	    System.out.println("after someMethod invocation");
// 	}
// }
// 

case class Complex(real: Double, imaginary: Double) {
  def +(that: Complex) =
    new Complex(real + that.real, imaginary + that.imaginary)
  def -(that: Complex) =
    new Complex(real - that.real, imaginary - that.imaginary)
}

object ComplexMain {
  def main(args: Array[String]) {
    val c1 = Complex(1.0, 2.0)
    val c2 = Complex(3.0, 4.0)
    val c12 = c1 + c2
    println(c12)
  }
}
