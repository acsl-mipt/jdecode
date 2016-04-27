package ru.mipt.acsl.decode.parser

import java.nio.charset.StandardCharsets._

import com.google.common.io.Resources
import com.typesafe.scalalogging.LazyLogging
import ru.mipt.acsl.decode.model.domain.impl.registry.Registry
import ru.mipt.acsl.decode.modeling.ErrorLevel

/**
  * @author Artem Shein
  */
object ModelRegistry extends LazyLogging {

  val provider = new DecodeSourceProvider()
  val config = new DecodeSourceProviderConfiguration("ru/mipt/acsl/decode")

  val sourceName = { n: String => n + ".decode" }
  val sourceResourcePath = { n: String => "ru/mipt/acsl/decode/" + sourceName(n) }
  val sourceContents = { n: String => Resources.toString(Resources.getResource(sourceResourcePath(n)), UTF_8) }

  val sources = Seq("runtime", "foundation", "fs", "identification", "mcc", "photon", "scripting", "segmentation",
    "tm", "routing")

  def registry: Registry = registryForSourceNames(sources)

  def registryForSourceNames(sourceNames: Seq[String]): Registry = registry(sourceNames.map(sourceContents))

  def registry(sources: Seq[String]): Registry = {
    val registry = provider.provide(config, sources)
    val resolvingResult = registry.resolve()
    if (resolvingResult.exists(_.level == ErrorLevel))
      resolvingResult.foreach(msg => logger.error(msg.text))
    registry
  }

}
