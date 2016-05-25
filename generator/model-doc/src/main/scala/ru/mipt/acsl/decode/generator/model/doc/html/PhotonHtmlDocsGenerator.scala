package ru.mipt.acsl.decode.generator.model.doc.html

import java.io.File

import ru.mipt.acsl.decode.model.naming.Fqn
import ru.mipt.acsl.decode.parser.ModelRegistry
import ru.mipt.acsl.geotarget.OnBoardModelRegistry

/**
  * @author Artem Shein
  */
object PhotonHtmlDocsGenerator {
  def main(args: Array[String]) = {
    val config = HtmlModelDocGeneratorConfiguration(new File("Photon_components.html"),
      OnBoardModelRegistry.registry,
      exclude = Set(Fqn.newFromSource("test")))
    new HtmlModelDocGenerator(config).generate()
  }
}