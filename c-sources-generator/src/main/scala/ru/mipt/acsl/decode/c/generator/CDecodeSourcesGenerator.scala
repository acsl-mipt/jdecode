package ru.mipt.acsl.decode.c.generator

import java.io.File
import java.util.Optional

import com.typesafe.scalalogging.LazyLogging
import ru.mipt.acsl.decode.model.domain.DecodeRegistry
import ru.mipt.acsl.generation.Generator

class CDecodeGeneratorConfiguration(val outputDir: File, val registry: DecodeRegistry)
{

}

class CDecodeSourcesGenerator(val config: Option[CDecodeGeneratorConfiguration] = None) extends Generator[CDecodeGeneratorConfiguration] with LazyLogging
{
  override def generate(): Unit = {

  }

  override def getConfiguration: Optional[CDecodeGeneratorConfiguration] = Optional.ofNullable(config.orNull)
}