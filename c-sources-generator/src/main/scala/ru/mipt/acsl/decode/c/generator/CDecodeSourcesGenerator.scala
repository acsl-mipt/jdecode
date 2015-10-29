package ru.mipt.acsl.decode.c.generator

import java.util.Optional

import ru.mipt.acsl.generation.Generator

trait CDecodeGeneratorConfiguration
{

}

class CDecodeSourcesGenerator(val config: Option[CDecodeGeneratorConfiguration]) extends Generator[CDecodeGeneratorConfiguration]
{
  override def generate(): Unit = ???

  override def getConfiguration: Optional[CDecodeGeneratorConfiguration] = config.to[Optional]
}