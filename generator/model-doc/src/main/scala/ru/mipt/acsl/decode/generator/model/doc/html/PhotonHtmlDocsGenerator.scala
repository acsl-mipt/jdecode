package ru.mipt.acsl.decode.generator.model.doc.html

import java.io.File

import ru.mipt.acsl.decode.model.domain.impl.naming.Fqn
import ru.mipt.acsl.decode.parser.ModelRegistry

/**
  * @author Artem Shein
  */
object PhotonHtmlDocsGenerator {
  def main(args: Array[String]) = {
    val config = HtmlModelDocGeneratorConfiguration(new File("Photon_components.html"),
      ModelRegistry.registry,
      exclude = Set(Fqn.newFromSource("test")))
    new HtmlModelDocGenerator(config).generate()
  }
}