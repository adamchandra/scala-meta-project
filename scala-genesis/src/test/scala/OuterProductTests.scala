package cc.factorie


// object OuterProductTests {
//   def main(args: Array[String]) : Unit = {
//     outer1()
//   }
// 
//   import cc.factorie.la._
// 
//   import cc.factorie.OuterProduct.{computeMatrix => computeMatrixJ}
// 
//   protected def flatOuter(vector1: Vector, vector2: Vector) : Vector = (vector1, vector2) match {
//     case (v1:SingletonBinaryVector, v2:SingletonBinaryVector)    => singletonXsingleton(v1, v2)
//     case (v1:SingletonBinaryVector, v2:SparseBinaryVector)       => singletonXsparse(v1, v2)
//     case (v1:SparseBinaryVector,    v2:SingletonBinaryVector)    => sparseXsingleton(v1, v2)
//     case (v1:SparseBinaryVector,    v2:SparseBinaryVector)       => sparseXsparse(v1, v2)
//   }
// 
//   def singletonXsingleton(v1:SingletonBinaryVector, v2:SingletonBinaryVector): SingletonBinaryVector =
//     new SingletonBinaryVector(v1.size * v2.size, v1.singleIndex * v2.size + v2.singleIndex)
// 
//   def singletonXsparse(v1:SingletonBinaryVector, v2:SparseBinaryVector):SparseBinaryVector = 
//     new SparseBinaryVector(v1.size * v2.size, computeMatrixJ(Array(v1.singleIndex), v2.ind, v2.size)) 
// 
//   def sparseXsparse(v1:SparseBinaryVector, v2:SparseBinaryVector): SparseBinaryVector = 
//     new SparseBinaryVector(v1.size * v2.size, computeMatrixJ(v1.ind, v2.ind, v2.size))
// 
//   def sparseXsingleton(v1:SparseBinaryVector, v2:SingletonBinaryVector):SparseBinaryVector = 
//     new SparseBinaryVector(v1.size * v2.size, computeMatrixJ(v1.ind, Array(v2.singleIndex), v2.size))
// 
//   // Reference version of computeMatrix - reimplemented in java for speed 
//   def computeMatrix(a1: Iterable[Int], a2: Iterable[Int], a2width:Int) = {
//     val arr = new Array[Int](a1.size * a2.size);
//     var i = 0;
//     for (i1 <- a1; i2 <- a2) {
//       arr(i) = i1 * a2width + i2;
//       i += 1;
//     }
//     arr
//   }
// 
//   def computeMatrix2(a1: Array[Int], a2: Array[Int], a2width:Int) = {
//     val arr = new Array[Int](a1.size * a2.size)
//     var i = 0; var n = 0
//     while (i<a1.size) {
//       var j = 0
//       while (j<a2.size) {
//         arr(i) = i * a2width + j
//         n += 1
//         j +=1
//       }
//       i += 1
//     }
//     arr
//   }
// 
//   def sample[T](name:String, runs: Int)(f: => T):T = {
//     val times:Array[Long] = Array.ofDim(runs)
//     var ret:Option[T] = None
//     for (i <- 0 to runs-1) {
//       var sw = new StopWatch
//       ret = Some(f)
//       times(i) = sw.getElapsedTime
//     }
// 
//     val mean = times.sum.toDouble / times.size
//     val diffs = times map (_ - mean)
//     val diffsq = diffs map {f => f*f}
//     val stddev = math.sqrt(diffsq.sum / diffsq.size)
//     val str = """
//     |results from %s:
//     |  runs      : %s
//     |  times     : %s
//     |  avg       : %s
//     |  std dev   : %s
//     |
//     """.stripMargin.format(name, runs, times.mkString("[", ", ", "]"), mean, stddev)
//     println(str)
// 
//     ret.get
//   }
// 
//   def outer1() {
// 
//     val a:Array[Int] = 1 to 1011 toArray
//     val b:Array[Int] = 1 to 2021 toArray
//     val n = a.length * b.length
// 
//     sample("computeMatrix in scala", 30) {
//       computeMatrix(a, b, n)
//     }
//     sample("computeMatrix in scala (while loop)", 30) {
//       computeMatrix2(a, b, n)
//     }
//     sample("computeMatrix in java", 30) {
//       computeMatrixJ(a, b, n)
//     }
//   }
// }
// 
// class StopWatch {
//   val startTime = System.currentTimeMillis()
//   def getElapsedTime():Long = System.currentTimeMillis() - startTime
// }
