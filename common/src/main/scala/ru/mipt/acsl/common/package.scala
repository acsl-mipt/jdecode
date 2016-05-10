package ru.mipt.acsl

/**
  * Created by metadeus on 07.05.16.
  */
package object common {

  implicit class OptionHelper[T](val option: Option[T]) {
    def orElseFail: T = option.getOrElse(sys.error("not an empty Option expected"))
    def orElseFail(msg: => String): T = option.getOrElse(sys.error(msg))
  }

}
