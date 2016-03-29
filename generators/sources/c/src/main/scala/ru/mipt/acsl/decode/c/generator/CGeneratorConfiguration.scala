package ru.mipt.acsl.decode.c.generator

import java.io.File

import ru.mipt.acsl.decode.model.domain.impl.registry.Registry
import ru.mipt.acsl.decode.model.domain.pure.naming.Fqn

/**
  * @author Artem Shein
  */
case class CGeneratorConfiguration(outputDir: File, registry: Registry, rootComponentFqn: String,
                                   namespaceAliases: Map[Fqn, Option[Fqn]] = Map.empty,
                                   sources: Seq[GeneratorSource] = Seq.empty,
                                   prologue: FileGeneratorConfiguration = FileGeneratorConfiguration(),
                                   epilogue: FileGeneratorConfiguration = FileGeneratorConfiguration(),
                                   isSingleton: Boolean = false)
