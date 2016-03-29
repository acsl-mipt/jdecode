package ru.mipt.acsl.decode.model.domain.impl

import ru.mipt.acsl.decode.model.domain.pure
import ru.mipt.acsl.decode.model.domain.pure.naming.Fqn

/**
  * Created by rexer on 08.03.16.
  */

package object component {

  import naming._

  implicit class ComponentRefHelper(cr: ComponentRef) {
    def aliasOrMangledName: String = cr.alias.getOrElse(cr.component.name.asMangledString)
  }

  implicit class ComponentHelper(val c: pure.component.Component) {
    def fqn: Fqn = Fqn.newFromFqn(c.namespace.fqn, c.name)
  }

}