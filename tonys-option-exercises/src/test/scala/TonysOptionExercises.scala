package test

import org.scalatest.matchers.ShouldMatchers
import org.scalatest._
import org.scalacheck._

import org.scalatest.junit.AssertionsForJUnit
import scala.collection.mutable.ListBuffer
import org.junit.Assert._
import org.junit.Test
import org.junit.Before
import org.junit.BeforeClass

/*
  from: http://blog.tmorris.net/further-understanding-scalaoption/

  
  Below are 15 exercises. The task is to emulate the scala.Option API
  without using Some/None subtypes, but instead using a fold (called a
  catamorphism).
 
  A couple of functions are already done (map, get)
  to be used as an example. ScalaCheck tests are given below to
  verify the work. The desired result is to have all tests passing.
 
  The 15th exercise is not available in the existing Scala API so
  instructions are given in the comments.
 
  Revision History
  ================
 
  23/08/2010
  Initial revision
 
  ----------------
 
  23/08/2010
  Fixed prop_getOrElse. Thanks Michael Bayne.
 
  ----------------
 
  26/08/2010
  Add lazy annotation to orElse method.
 
*/

trait Optional[A] {
  // single abstract method
  def fold[X](some: A => X, none: => X): X

  import Optional._

  // Done for you.
  def map[B](f: A => B): Optional[B] =
    fold(f andThen some, none[B])

  // Done for you.
  // WARNING: undefined for None
  def get: A =
    fold(a => a, error("None.get"))

  // Exercise 1
  def flatMap[B](f: A => Optional[B]): Optional[B] =
    fold(f, none[B])

  // Exercise 2
  // Rewrite map but use flatMap, not fold.
  def mapAgain[B](f: A => B): Optional[B] =
    flatMap(f andThen some)

  // Exercise 3
  def getOrElse(e: => A): A =
    fold(a => a, e)

  // Exercise 4
  def filter(p: A => Boolean): Optional[A] =
    fold(a => if (p(a)) some(a) else none[A],
         none[A])

  // Exercise 5
  def exists(p: A => Boolean): Boolean =
    fold(a => p(a), false)

  // Exercise 6
  def forall(p: A => Boolean): Boolean =
    fold(a => p(a), true)

  // Exercise 7
  def foreach(f: A => Unit): Unit =
    map(f)

  // Exercise 8
  def isDefined: Boolean =
    fold(a => true, false)

  // Exercise 9
  def isEmpty: Boolean =
    !isDefined

  // Exercise 10
  def orElse(o: => Optional[A]): Optional[A] =
    fold(a => some(a), o)

  // Exercise 11
  def toLeft[X](right: => X): Either[A, X] =
    fold(a => Left(a), Right(right))

  // Exercise 12
  def toRight[X](left: => X): Either[X, A] =
    fold(a => Right(a), Left(left))

  // Exercise 13
  def toList: List[A] =
    fold(a => List(a), Nil)

2  // Exercise 14
  def iterator: Iterator[A] =
    toList.iterator

  // Exercise 15 The Clincher!
  // Return a none value if either this or the argument is none.
  // Otherwise apply the function to the argument in some.
  // Don't be afraid to use functions you have written.
  // Better style, more points!
  def applic[B](f: Optional[A => B]): Optional[B] =
    fold(a => f.map(x => x(a)),
      none[B])

  // Utility
  def toOption: Option[A] = fold(Some(_), None)
}

object Optional {
  // Done for you
  def none[A]: Optional[A] = new Optional[A] {
    def fold[X](some: A => X, none: => X) = none
  }

  // Done for you
  def some[A](a: A): Optional[A] = new Optional[A] {
    def fold[X](some: A => X, none: => X) = some(a)
  }

  // Utility
  def fromOption[A](o: Option[A]): Optional[A] = o match {
    case None => none
    case Some(a) => some(a)
  }
}

class OptionTests extends FunSuite with AssertionsForJUnit with BeforeAndAfterAll {

  import Arbitrary.arbitrary
  import Prop._

  import Optional._

  implicit def ArbitraryOptional[A](implicit a: Arbitrary[A]): Arbitrary[Optional[A]] =
    Arbitrary(arbitrary[Option[A]] map fromOption)

  val prop_map = forAll((o: Optional[Int], f: Int => String) =>
    (o map f).toOption == (o.toOption map f))

  val prop_get = forAll((o: Optional[Int]) =>
    o.isDefined ==> (o.get == o.toOption.get))

  val prop_flatMap = forAll((o: Optional[Int], f: Int => Optional[String]) =>
    (o flatMap f).toOption == (o.toOption flatMap (f(_).toOption)))

  val prop_mapAgain = forAll((o: Optional[Int], f: Int => String) =>
    (o mapAgain f).toOption == (o map f).toOption)

  val prop_getOrElse = forAll((o: Optional[Int], n: Int) =>
    (o getOrElse n) == (o.toOption getOrElse n))

  val prop_filter = forAll((o: Optional[Int], f: Int => Boolean) =>
    (o filter f).toOption == (o.toOption filter f))

  val prop_exists = forAll((o: Optional[Int], f: Int => Boolean) =>
    (o exists f) == (o.toOption exists f))

  val prop_forall = forAll((o: Optional[Int], f: Int => Boolean) =>
    (o forall f) == (o.toOption forall f))

  val prop_foreach = forAll((o: Optional[Int], f: Int => Unit, n: Int) => {
    var x: Int = n
    var y: Int = x

    o foreach (t => x = x + t)
    o.toOption foreach (t => y = y + t)

    x == y
  })

  val prop_isDefined = forAll((o: Optional[Int]) =>
    (o.isDefined) == (o.toOption.isDefined))

  val prop_isEmpty = forAll((o: Optional[Int]) =>
    o.isEmpty == o.toOption.isEmpty)

  val prop_orElse = forAll((o: Optional[Int], p: Optional[Int]) =>
    (o orElse p).toOption == (o.toOption orElse p.toOption))

  val prop_toLeft = forAll((o: Optional[Int], n: Int) =>
    (o toLeft n) == (o.toOption toLeft n))

  val prop_toRight = forAll((o: Optional[Int], n: Int) =>
    (o toRight n) == (o.toOption toRight n))

  val prop_toList = forAll((o: Optional[Int]) =>
    o.toList == o.toOption.toList)

  val prop_iterator = forAll((o: Optional[Int]) =>
    o.iterator sameElements o.toOption.iterator)

  // *** READ THIS COMMENT FIRST ***
  // Note that scala.Option has no such equivalent to this method
  // Therefore, reading this test may give away clues to how it might be solved.
  // If you do not wish to spoil it, look away now and follow the 
  // instruction in the Exercise comment.
  val prop_applic = forAll((o: Optional[Int => String], p: Optional[Int]) =>
    (p applic o).toOption == (for (
      f <- o.toOption;
      n <- p.toOption
    ) yield f(n)))

  val props =
    List(
      prop_map,
      prop_get,
      prop_flatMap,
      prop_mapAgain,
      prop_getOrElse,
      prop_filter,
      prop_exists,
      prop_forall,
      prop_foreach,
      prop_isDefined,
      prop_isEmpty,
      prop_orElse,
      prop_toLeft,
      prop_toRight,
      prop_toList,
      prop_iterator,
      prop_applic)
  test("properties all pass") {
    props foreach (_.check)
  }

}
