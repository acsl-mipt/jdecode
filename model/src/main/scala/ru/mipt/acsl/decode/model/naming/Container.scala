package ru.mipt.acsl.decode.model.naming

import ru.mipt.acsl.decode.model.Referenceable

/**
  * Created by metadeus on 27.05.16.
  */
trait Container {

  def objects: Seq[Referenceable]

  def objects_=(objects: Seq[Referenceable]): Unit

}
