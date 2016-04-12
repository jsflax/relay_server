package util

import java.io.File

import scala.annotation.tailrec

/**
  * @author jsflax on 4/3/16.
  */
object FileUtils {
  /**
    * Traverse all files in view directory using tail recursion.
    *
    * @param file top directory
    * @return list of all files
    */
  def listFiles(file: File): List[File] = {
    @tailrec def listFiles(files: List[File], result: List[File]): List[File] =
      files match {
        case Nil => result
        case head :: tail if head.isDirectory =>
          listFiles(Option(head.listFiles).map(
            _.toList ::: tail
          ).getOrElse(tail), result)
        case head :: tail if head.isFile =>
          listFiles(tail, head :: result)
      }
    listFiles(List(file), Nil)
  }
}
