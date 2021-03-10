package opencl.executor

import java.awt.image.BufferedImage
import java.io.{File, IOException}

import org.junit.Assume

import scala.collection.parallel.CollectionConverters._
import javax.imageio.ImageIO

object LongTestsEnabled:
  def apply(): Unit =
    Assume.assumeTrue("Needs long tests enabled.", areEnabled)

  def areEnabled: Boolean =
    System.getenv("LIFT_LONG_TESTS") != null

object Utils:

  def isApplePlatform: Boolean = Executor.getPlatformName == "Apple"

  def isAmdGpu: Boolean =
    Executor.getPlatformName == "AMD Accelerated Parallel Processing" && 
    Executor.getDeviceType == "GPU"

  def writeMD(width: Int, height: Int, data: Array[Float], name: String): Unit =
    val out = new File(name + ".png")
    val img = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY)
    val r = img.getRaster

    for (i <- 0 until height)
      for (j <- 0 until width)
        r.setSample(j, i, 0, data(i * height + j).toByte)

    try
      ImageIO.write(img, "png", out)
    catch
      case e: IOException => e.printStackTrace()
  end writeMD

  /*
   * Matrix printing methods
   */

  def myPrint(m: Array[Array[Array[Float]]]): Unit =
    m.foreach( r =>
      println(r.map( e =>
        "(" + e.map(x => f"$x%2.0f").reduce(_ + ", " + _) + ")"
      ).reduce(_ + " " + _))
    )

  def myPrint(m: Array[Array[Float]]): Unit =
    m.foreach( r =>
      println(r.map(x => f"$x%2.0f").reduce(_ + " " + _))
    )

  @scala.annotation.tailrec
  def myPrint(m: Array[Float], cols: Int): Unit =
    val (row, rest) = m.splitAt(cols)
    if (row.nonEmpty) println(row.map(x => f"$x%2.0f").reduce(_ + " " + _))
    if (rest.nonEmpty) myPrint(rest, cols)

  @scala.annotation.tailrec
  def printRow(r: Array[Float], elems: Int): Unit =
    val (elem, rest) = r.splitAt(elems)
    if (elem.nonEmpty) print("(" + elem.map(x => f"$x%2.0f").reduce(_ + ", " + _) + ") ")
    if (rest.nonEmpty) printRow(rest, elems)

  @scala.annotation.tailrec
  def myPrint(m: Array[Float], cols: Int, elems: Int): Unit =
    val (row, rest) = m.splitAt(cols*elems)
    if (row.nonEmpty) printRow(row, elems); println("")
    if (rest.nonEmpty) myPrint(rest, cols, elems)


  def add(A: Array[Array[Float]], B: Array[Array[Float]]): Array[Array[Float]] =
    if (A.length != B.length || A.head.length != B.length)
      throw new IllegalArgumentException
    A.lazyZip(B).map((x, y) => x.lazyZip(y).map(_+_))

  /*
   * Matrix multiplication methods
   */

  def matrixMatrixPatternMultiply(A: Array[Array[Float]], B: Array[Array[Float]]): Array[Array[Float]] =
    val Bt = B.transpose
    A.map( Arow =>
      Bt.map( Bcol => Arow.lazyZip(Bcol).map(_ * _).sum )
    )

  def matrixMatrixMultiply(A: Array[Array[Float]], B: Array[Array[Float]]) :  Array[Array[Float]] =
    val aCols = A(0).length
    val aRows = A.length
    val bCols = B(0).length
    val res =  Array.ofDim[Float](aRows, bCols)

    if (A.head.length != B.length)
      throw new IllegalArgumentException

    @inline def computeRow(row: Int): Unit =
      // while statements are much faster than for statements
      var col = 0
      while(col < bCols)
        var i = 0; var sum = 0.0f
        while(i < aCols)
          sum += A(row)(i) * B(i)(col)
          i += 1
        res(row)(col) = sum
        col += 1

    (0 until aRows).par.foreach( computeRow )

    res
  end matrixMatrixMultiply

  def matrixMatrixMultiply(A: Array[Array[Float]],
                           B: Array[Array[Float]],
                           C: Array[Array[Float]],
                           alpha: Float,
                           beta: Float) :  Array[Array[Float]] =
    val aCols = A(0).length
    val aRows = A.length
    val bCols = B(0).length
    val res =  Array.ofDim[Float](aRows, bCols)

    if (A.head.length != B.length)
      throw new IllegalArgumentException

    @inline def computeRow(row: Int): Unit =
      // while statements are much faster than for statements
      var col = 0
      while(col < bCols)
        var i = 0; var sum = 0.0f
        while(i < aCols)
          sum += A(row)(i) * B(i)(col)
          i += 1
        res(row)(col) =  alpha * sum + C(row)(col) * beta
        col += 1

    (0 until aRows).par.foreach( computeRow )

    res
  end matrixMatrixMultiply

  def matrixVector(matrix: Array[Array[Float]], vector: Array[Float]): Array[Float] =
    matrix.map(
      row => row.lazyZip(vector).map(_ * _).sum
    )

  def matrixVector(matrix: Array[Array[Float]], vectorX: Array[Float], vectorY: Array[Float]): Array[Float] =
    val tmp = matrix.map(
      row => row.lazyZip(vectorX).map(_ * _).sum
    )
    tmp.lazyZip(vectorY).map(_ + _)

  def matrixVector(matrix: Array[Array[Float]], vector: Array[Float], alpha: Float): Array[Float] =
    matrix.map(
      row => row.lazyZip(vector).map(_ * _).sum * alpha
    )

  def matrixVector(matrix: Array[Array[Float]], vectorX: Array[Float], vectorY: Array[Float], alpha: Float, beta: Float): Array[Float] =
    val tmp = matrix.map(
      row => row.lazyZip(vectorX).map(_ * _).sum * alpha
    )
    val scaledY = vectorY.map(_ * beta)
    tmp.lazyZip(scaledY).map(_ + _)
  

  /*
   * Stencil util functions
   */
  def scalaCompute1DStencil(data: Array[Float],
                            size: Int, step: Int,
                            left: Int, right: Int,
                            weights: Array[Float],
                            boundary: (Int, Int) => Int): Array[Float] =
    val leftPadding = Array.tabulate(left)(x => data(boundary((x + 1) * -1, data.length))).reverse
    val rightPadding = Array.tabulate(right)(x => data(boundary(x + data.length, data.length)))
    val paddedInput = leftPadding ++ data ++ rightPadding

    val neighbourhoodArray = paddedInput.sliding(size, step).toArray
    neighbourhoodArray.map(_.zip(weights).foldLeft(0.0f)((acc, p) => acc + p._1 * p._2))

  def scalaCompute2DStencil(data: Array[Array[Float]],
                            size1: Int, step1: Int,
                            size2: Int, step2: Int,
                            top: Int, bottom: Int,
                            left: Int, right: Int,
                            weights: Array[Float],
                            boundary: (Int, Int) => Int): Array[Float] =
    val neighbours = scalaGenerate2DNeighbours(data, size1, step1, size2, step2,
      top, bottom, left, right, boundary)
    val result = neighbours.map(x => x.map(y => y.flatten.zip(weights).foldLeft(0.0f)((acc, p) => acc + p._1 * p._2)))
    def clamp(i: Float) = Math.max(0.0f, i)
    result.flatten.map(clamp)

  def scalaGenerate2DNeighbours(data: Array[Array[Float]],
                                size1: Int, step1: Int,
                                size2: Int, step2: Int,
                                top: Int, bottom: Int,
                                left: Int, right: Int,
                                boundary: (Int, Int) => Int): Array[Array[Array[Array[Float]]]] =
    //padding
    val topPadding = Array.tabulate(top)(x => data(boundary((x + 1) * -1, data.length))).reverse
    val bottomPadding = Array.tabulate(bottom)(x => data(boundary(x + data.length, data.length)))
    val verticalPaddedInput = (topPadding ++ data ++ bottomPadding).transpose
    val leftPadding = Array.tabulate(left)(
      x => verticalPaddedInput(
        boundary((x + 1) * -1, verticalPaddedInput.length))).reverse
    val rightPadding = Array.tabulate(right)(
      x => verticalPaddedInput(
        boundary(x + data.length, verticalPaddedInput.length)))
    val paddedInput = (leftPadding ++ verticalPaddedInput ++ rightPadding).transpose
    //paddedInput.map(x => println(x.mkString(",")))

    //sliding
    val firstSlide = paddedInput.sliding(size1, step1).toArray
    val secondSlide = firstSlide.map(x => x.transpose.sliding(size2, step2).toArray)
    val neighbours = secondSlide.map(x => x.map(y => y.transpose))
    neighbours
  end scalaGenerate2DNeighbours

  // Boundary conditions implemented as scala functions for gold versions
  val scalaClamp: (Int, Int) => Int = (idx: Int, length: Int) =>
    if (idx < 0) 0 else if (idx > length - 1) length - 1 else idx

  val scalaWrap: (Int, Int) => Int = (idx: Int, length: Int) =>
    (idx % length + length) % length

  val scalaMirror: (Int, Int) => Int = (idx: Int, length: Int) =>
    val id = (if (idx < 0) -1 - idx else idx) % (2 * length)
    if (id >= length) length + length - id - 1 else id

end Utils
