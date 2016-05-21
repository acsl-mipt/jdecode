package ru.mipt.acsl.decode.generator.mcc

import scala.collection.mutable

import java.io.File

/**
  * @author Artem Shein
  */
class VirtualFile(vfs: VirtualFs, path: String, var contents: Option[mutable.Buffer[Byte]] = None) extends File(path) {

  override def getParentFile: File = vfs.file(getParent)

}

object VirtualFile {

  def apply(vfs: VirtualFs, path: String, contents: Array[Byte]) = new VirtualFile(vfs, path, Some(contents.toBuffer))
  def apply(vfs: VirtualFs, path: String) = new VirtualFile(vfs, path)

}
