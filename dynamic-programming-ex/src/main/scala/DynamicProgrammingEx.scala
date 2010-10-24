// taken from : http://people.csail.mit.edu/bdean/6.046/dp

object MaximumValueContiguousSubsequence {
  //  1. Maximum Value Contiguous Subsequence. Given a sequence of n real numbers A(1) ... A(n),
  //  determine a contiguous subsequence A(i) ... A(j) for which the sum of elements in the subsequence
  //  is maximized.
  def mvcs(seq: Seq[Int]): (Int, Int, Int) = {
    // subsequence is triple(start, end, sum)
    var max = (0, 1, seq(0))
    var curr = (0, 1, seq(0))
    for (i <- 1 to seq.length-1) {
      val extcurr = (curr._1, i, curr._3 + seq(i))
      val newseq = (i, i+1, seq(i))
      if (sum(newseq) > sum(extcurr))
        curr = newseq
      else
        curr = extcurr
      if (sum(curr) > sum(max))
        max = curr
    }
    max
  }

  def sum(triple: Tuple3[Int, Int, Int]): Int = triple._3

  def main(args: Array[String]) = {
    val trials = List(
      List(1, 2, 3),
      List(1, -1, 1),
      List(-1, 3, -1, -1, 3, -1))
    for (t <- trials) {
      println("mvcs = " + mvcs(t))
    }
  }
}

object MakingChange {
  //  2. Making Change. You are given n types of coin denominations of values v(1) < v(2) < ... < v(n)
  //  (all integers). Assume v(1) = 1, so you can always make change for any amount of money C. Give an
  //  algorithm which makes change for an amount of money C with as few coins as possible.

  def bestv_for(v: List[Int], m: Int, changeFor: List[List[Int]]): List[Int] = {
    var best = 1 :: changeFor(m - 1)
    for (d <- v.view(1, v.size)) {
      if (m - d >= 0 && changeFor(m - d).size + 1 < best.size)
        best = d :: changeFor(m - d)
    }
    best
  }

  def bestv_fold(v: List[Int], m: Int, changeFor: List[List[Int]]): List[Int] = {
    v.foldLeft(1 :: changeFor(m - 1))({ (best, d) =>
      {
        if (m - d >= 0 && changeFor(m - d).size + 1 < best.size)
          d :: changeFor(m - d)
        else
          best
      }
    })
  }

  def makeChange(v: List[Int], money: Int): List[Int] = {
    var changeFor: List[List[Int]] = List(List())
    for (m <- 1 to money) {
      changeFor = changeFor ++ List(bestv_fold(v, m, changeFor))
    }
    changeFor.last
  }

  def main(args: Array[String]) {
    val tests: List[(List[Int], Int)] = List(
      (List(1, 5, 10, 25), 127),
      (List(1, 5, 10, 25), 33),
      (List(1, 3), 7))
    tests map {
      case (denom: List[Int], money: Int) => {
        println("coins = " + denom.mkString(", "))
        println("change for " + money + " == " + makeChange(denom, money))
      }
      case n => println(n)
    }
  }
}

object LongestIncreasingSubsequence {
  //  3. Longest Increasing Subsequence. Given a sequence of n real numbers A(1) ... A(n), determine a
  //  subsequence (not necessarily contiguous) of maximum length in which the values in the subsequence
  //  form a strictly increasing sequence.

  // running time O(n^2), should be able to do this 
  // in n log n
  def longest(seq: List[Int]): List[Int] = {
    var lis = List(List(0))

    for (i <- 1 to seq.size - 1) {
      var best = List(i)
      for (j <- 0 to lis.size - 1) {
        if (seq(i) > seq(lis(j).last)) {
          if (lis(j).size + 1 > best.size)
            best = lis(j) ++ List(i)
        }
      }
      lis = lis ++ List(best)
    }
    lis.foldLeft(lis.head) {case (l1, l2) => if (l1.length > l2.length) l1 else l2 }
  }

  def main(args: Array[String]) {
    val tests: List[List[Int]] = List(
      List(1, 5, -10, 25),
      List(1, -5, 10, -25),
      List(-1, -3))
    tests map { (seq) =>
      {
        println("seq = " + seq.mkString(", "))
        println("lcs = " + longest(seq).mkString(", "))
      }
    }
  }

}

object BoxStacking {
  //  4. Box Stacking. You are given a set of n types of rectangular 3-D boxes, where the i^th box has
  //  height h(i), width w(i) and depth d(i) (all real numbers). You want to create a stack of boxes
  //  which is as tall as possible, but you can only stack a box on top of another box if the dimensions
  //  of the 2-D base of the lower box are each strictly larger than those of the 2-D base of the higher
  //  box. Of course, you can rotate a box so that any side functions as its base. It is also allowable
  //  to use multiple instances of the same type of box.
  
  case class Box(h:Int, w:Int, d:Int)

  def stack(boxes:List[Box]): List[Box] = {
    // init tower to inf sized table top + zero-dim box
    var tower:List[Box] = List(boxes(0), Box(0, 0, 0))
    // Box(Int.MaxValue, Int.MaxValue, Int.MaxValue), 
    for {box  <- boxes;
         brot <- rotations(box)} {
           tower = addBox(brot, tower)
         }
    tower.dropRight(1)
  }

  def rotations(box:Box):List[Box] = {
    val d = List(box.h, box.w, box.d).sorted
    List((0, 1, 2), 
         (1, 0, 2), 
         (2, 0, 1)) map { x => x match {
           case (a,b,c) => Box(d(a),d(b),d(c)) 
         }}
  }

  def addBox(box:Box, tower:List[Box]):List[Box] = {
    def baseGT(a:Box, b:Box) = a.w > b.w && a.d > b.d
    def baseLT(a:Box, b:Box) = a.w < b.w && a.d < b.d

    var n = 0
    while (baseLT(box, tower(n))) n += 1
    val min = n
    while (!baseLT(tower(n), box)) n += 1
    val max = n

    if (height(tower.view(min, max)) < box.h) {
      val str = """
              |   %s
              | --> %s
              |   %s
              """.stripMargin.format(
                tower.view(max, tower.size).reverse.mkString("\n   "),
                box,  
                tower.view(0, min).reverse.mkString("\n   "))
      println(str)

      tower.view(0, min).toList ++ List(box) ++ tower.view(max, tower.size)
    }
    else tower
  }

  import scala.collection.Seq

  def height(boxes:Seq[Box]):Int = boxes.foldLeft(0)({(h:Int,b:Box) => h + b.h})

  def main(args: Array[String]) = {
    val tests = 
      List(
        List(Box(1, 2, 3), 
             Box(2, 3, 4), 
             Box(3, 4, 5)),
        List(Box(3, 6, 1), 
             Box(5, 11, 8), 
             Box(6, 12, 12), 
             Box(9, 18, 1), 
             Box(5, 10, 12)))
    for (t <- tests) {
      println(stack(t))
    }
  }
}


//  5. Building Bridges. Consider a 2-D map with a horizontal river passing through its center. There
//  are n cities on the southern bank with x-coordinates a(1) ... a(n) and n cities on the northern
//  bank with x-coordinates b(1) ... b(n). You want to connect as many north-south pairs of cities as
//  possible with bridges such that no two bridges cross. When connecting cities, you can only connect
//  city i on the northern bank to city i on the southern bank.
object BuildingBridges {
  // same as longest increasing subsequence
  def bridges(cities:Seq[Int]): Seq[Int] = {
    List(0)
  }

  def main(args:Array[String]) {
    val tests:List[List[Int]] = List(
      List(1, 5, -10, 25),
      List(1, -5, 10, -25),
      List(-1, -3)
    )
    tests map { 
      (seq) => {
        println("seq = " + seq.mkString(", "))
        println("bridges = " + bridges(seq).mkString(", "))
      }
    }
  }
}


//  6. Integer Knapsack Problem (Duplicate Items Forbidden). This is the same problem as the example
//  above, except here it is forbidden to use more than one instance of each type of item.
// 
//  7. Balanced Partition. You have a set of n integers each in the range 0 ... K. Partition these
//  integers into two subsets such that you minimize |S1 - S2|, where S1 and S2 denote the sums of the
//  elements in each of the two subsets.
// 
//  8. Edit Distance. Given two text strings A of length n and B of length m, you want to transform A
//  into B with a minimum number of operations of the following types: delete a character from A,
//  insert a character into A, or change some character in A into a new character. The minimal number
//  of such operations required to transform A into B is called the edit distance between A and B.

//  9. Counting Boolean Parenthesizations. You are given a boolean expression consisting of a string of
//  the symbols 'true', 'false', 'and', 'or', and 'xor'. Count the number of ways to parenthesize the
//  expression such that it will evaluate to true. For example, there is only 1 way to parenthesize
//  'true and false xor true' such that it evaluates to true.
// 
// 10. Optimal Strategy for a Game. Consider a row of n coins of values v(1) ... v(n), where n is
// even. We play a game against an opponent by alternating turns. In each turn, a player selects either
// the first or last coin from the row, removes it from the row permanently, and receives the value of
// the coin. Determine the maximum possible amount of money we can definitely win if we move first.
// 
// 11. Two-Person Traversal of a Sequence of Cities. You are given an ordered sequence of n cities, and
// the distances between every pair of cities. You must partition the cities into two subsequences (not
// necessarily contiguous) such that person A visits all cities in the first subsequence (in order),
// person B visits all cities in the second subsequence (in order), and such that the sum of the total
// distances travelled by A and B is minimized. Assume that person A and person B start initially at
// the first city in their respective subsequences.
// 
// 12. Bin Packing (Simplified Version). You have n1 items of size s1, n2 items of size s2, and n3
// items of size s3. You'd like to pack all of these items into bins each of capacity C, such that the
// total number of bins used is minimized.


