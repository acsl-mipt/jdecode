package ru.mipt.acsl.decode.parser

import java.nio.charset.StandardCharsets._

import com.google.common.io.Resources
import com.typesafe.scalalogging.LazyLogging
import ru.mipt.acsl.decode.model.registry.Registry
import ru.mipt.acsl.modeling.ErrorLevel

/**
  * @author Artem Shein
  */
object ModelRegistry extends LazyLogging {

  val Provider = new DecodeSourceProvider()
  val Config = new DecodeSourceProviderConfiguration("ru/mipt/acsl/decode")

  val RuntimeSource = SourceFileName("runtime")
  val Sources = Seq(RuntimeSource)

  def sourceName(nameWithoutExt: SourceFileName): String = nameWithoutExt.toFileNameWithoutExt + ".decode"

  def sourceResourcePath(nameWithoutExt: SourceFileName): String = "ru/mipt/acsl/decode/" + sourceName(nameWithoutExt)

  def sourceContents(nameWithoutExt: SourceFileName): String =
    Resources.toString(Resources.getResource(sourceResourcePath(nameWithoutExt)), UTF_8)

  def registryForSourceNames(sourceNames: Seq[SourceFileName]): Registry = registry(sourceNames.map(sourceContents))

  def registry(sources: Seq[String]): Registry = {
    val registry = Provider.provide(Config, sources)
    val resolvingResult = registry.resolve()
    if (resolvingResult.exists(_.level == ErrorLevel))
      resolvingResult.foreach(msg => logger.error(msg.text))
    registry
  }

  case class SourceFileName(name: String) {

    def toFileNameWithoutExt: String = name

    override def toString: String = toFileNameWithoutExt

  }

}
