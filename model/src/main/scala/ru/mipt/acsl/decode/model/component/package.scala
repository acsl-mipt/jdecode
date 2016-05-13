package ru.mipt.acsl.decode.model

import ru.mipt.acsl.decode.model.naming.Fqn

/**
  * Created by rexer on 08.03.16.
  */

package object component {

  implicit class ComponentRefHelper(cr: ComponentRef) {
    def aliasOrMangledName: String = cr.alias.getOrElse(cr.component.name.asMangledString)
  }

  implicit class ComponentHelper(val c: Component) {
    def fqn: Fqn = Fqn.newFromFqn(c.namespace.fqn, c.name)
  }

}