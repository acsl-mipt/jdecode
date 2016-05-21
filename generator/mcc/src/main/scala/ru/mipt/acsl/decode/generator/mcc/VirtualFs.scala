package ru.mipt.acsl.decode.generator.mcc

import scala.collection.mutable

/**
  * @author Artem Shein
  */
class VirtualFs {

  val binds = mutable.Map.empty[String, VirtualFile]

  def bind(path: String, contents: Array[Byte]): Unit = {
    binds(path) = VirtualFile(this, path, contents)
  }

  def file(path: String): VirtualFile = binds.getOrElse(path, VirtualFile(this, path))

}

object VirtualFs {

  def apply() = new VirtualFs()

}