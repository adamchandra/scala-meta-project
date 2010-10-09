// Most exercises taken from : http://people.csail.mit.edu/bdean/6.046/dp/

/*  
object MaximumValueContiguousSubsequence {
  //  1. Maximum Value Contiguous Subsequence. Given a sequence of n real numbers A(1) ... A(n),
  //  determine a contiguous subsequence A(i) ... A(j) for which the sum of elements in the subsequence
  //  is maximized.
  case class ContiguousSeq(start:Int, end:Int, sum:Int) {
    def + (that:ContiguousSeq): ContiguousSeq = ContiguousSeq(this.start, that.end, this.sum + that.sum)
    def > (that:ContiguousSeq): Boolean = this.sum > that.sum
    def == (that:ContiguousSeq): Boolean = this.sum == that.sum
    def < (that:ContiguousSeq): Boolean = this.sum < that.sum
  }

  case class Sequences(max:ContiguousSeq, maxToCurrent:ContiguousSeq, tail:ContiguousSeq)

  def mvcs(seq:Seq[Int]):Sequences = seq.length match {
    case 0 => null
    case 1 => {
      val c = ContiguousSeq(0,1,seq.head)
      Sequences(c, c, c)
    }
    case n => {
      val prev = mvcs(seq.view(0, seq.length-1))
      val curr = ContiguousSeq(n-1, n, seq.last)

      val bestTail = 
        if (prev.tail.end == n-1 && prev.tail + curr > prev.tail)
          prev.tail + curr
        else 
          curr

      val maxToCurrent = prev.maxToCurrent + curr

      val m1 = if (prev.max > bestTail) prev.max else bestTail
      val max = if (m1 > maxToCurrent) m1 else 

        if (prev.max > bestTail) prev.max else bestTail
        maxToCurrent
      Sequences(m2, maxToCurrent, bestTail)
    }
  }

  def main(args:Array[String]) = {
    val trials = List(
      List(1, 2, 3), 
      List(1, -1, 1), 
      List(-1, 3, -1, -1, 3, -1), 
      Nil
    )
    for (t <- trials) {
      println("mvcs = " + mvcs(t))
    }
  }

}
*/


object MaximumValueContiguousSubsequence {
  //  1. Maximum Value Contiguous Subsequence. Given a sequence of n real numbers A(1) ... A(n),
  //  determine a contiguous subsequence A(i) ... A(j) for which the sum of elements in the subsequence
  //  is maximized.
  case class ContiguousSeq(start:Int, end:Int, sum:Int) {
    def ++ (value:Int) = ContiguousSeq(start, end+1, sum + value)
    def > (that:ContiguousSeq): Boolean = this.sum > that.sum
    def == (that:ContiguousSeq): Boolean = this.sum == that.sum
    def < (that:ContiguousSeq): Boolean = this.sum < that.sum
  }

  def mvcs(seq:Seq[Int]):Sequences = {
    var max = ContiguousSeq(0, 0, seq(1))
    var current  = ContiguousSeq(0, 0, seq(1))
    for (i <- seq.drop(1)) {
      val newcurr = current ++ i
      if (i > current.sum) 
        current = ContiguousSeq(curr.end
      if (current > max) 
        max = ContiguousSeq(current.start, current.end, current.sum)
    }
  }

  def main(args:Array[String]) = {
    val trials = List(
      List(1, 2, 3), 
      List(1, -1, 1), 
      List(-1, 3, -1, -1, 3, -1), 
      Nil
    )
    for (t <- trials) {
      println("mvcs = " + mvcs(t))
    }
  }

}


//  2. Making Change. You are given n types of coin denominations of values v(1) < v(2) < ... < v(n)
//  (all integers). Assume v(1) = 1, so you can always make change for any amount of money C. Give an
//  algorithm which makes change for an amount of money C with as few coins as possible. [on problem
//  set 4]
// 
//  3. Longest Increasing Subsequence. Given a sequence of n real numbers A(1) ... A(n), determine a
//  subsequence (not necessarily contiguous) of maximum length in which the values in the subsequence
//  form a strictly increasing sequence. [on problem set 4]
// 
//  4. Box Stacking. You are given a set of n types of rectangular 3-D boxes, where the i^th box has
//  height h(i), width w(i) and depth d(i) (all real numbers). You want to create a stack of boxes
//  which is as tall as possible, but you can only stack a box on top of another box if the dimensions
//  of the 2-D base of the lower box are each strictly larger than those of the 2-D base of the higher
//  box. Of course, you can rotate a box so that any side functions as its base. It is also allowable
//  to use multiple instances of the same type of box.
// 
//  5. Building Bridges. Consider a 2-D map with a horizontal river passing through its center. There
//  are n cities on the southern bank with x-coordinates a(1) ... a(n) and n cities on the northern
//  bank with x-coordinates b(1) ... b(n). You want to connect as many north-south pairs of cities as
//  possible with bridges such that no two bridges cross. When connecting cities, you can only connect
//  city i on the northern bank to city i on the southern bank. (Note: this problem was incorrectly
//  stated on the paper copies of the handout given in recitation.)
// 
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
// 
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

