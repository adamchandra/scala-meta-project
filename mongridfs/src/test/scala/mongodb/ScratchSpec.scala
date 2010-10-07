package mongodb

import org.scalatest.matchers.ShouldMatchers
import org.scalatest._

import org.scalatest.junit.AssertionsForJUnit
import scala.collection.mutable.ListBuffer
import org.junit.Assert._
import org.junit.Test
import org.junit.Before
import cc.acs.util.StringOps._


class StringOps extends FunSuite with AssertionsForJUnit {
  test("strings do what i want")  {
    val str = """
    |92.ps 64c55535be959fa942de497b912749b1 
    |93.ps 32320228fc2c878a730a3d2c55145eae 
    |94.ps 3c5949318a9efc6ec4035e7feb65cd1c 
    |95.ps 6778e7f30f7cd5e1bdbd5ad34be87baa
    """.stripMargin
    
    val results = lines(str) map wsv map toPair
    for (r <- results) {
      println(r)
    }
  }
}

