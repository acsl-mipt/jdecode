package ru.mipt.acsl.decode

import scala.collection.mutable
import scala.collection.immutable

/**
  * @author Artem Shein
  */
object ScalaUtil {
  def newSeq[T]: immutable.Seq[T] = immutable.Seq.empty[T]
  def appendToBuffer[T](buffer: mutable.Buffer[T], elem: T) = buffer += elem
  def append[T](seq: immutable.Seq[T], el: T): immutable.Seq[T] = seq :+ el
  def append[T](set: immutable.Set[T], el: T): immutable.Set[T] = set + el
  def append[T](seq: immutable.Seq[T], seq2: Seq[T]): immutable.Seq[T] = seq ++ seq2
}
