package ru.mipt.acsl.geotarget

import java.io.File

import com.typesafe.scalalogging.LazyLogging
import ru.mipt.acsl.decode.c.generator.{CGeneratorConfiguration, CSourcesGenerator, FileGeneratorConfiguration, GeneratorSource}
import ru.mipt.acsl.decode.model.domain.impl.naming.Fqn
import ru.mipt.acsl.decode.model.domain.pure.naming.Fqn
import ru.mipt.acsl.decode.parser.ModelRegistry

import scala.collection.immutable.HashMap
import scala.io.Source

/**
  * @author Artem Shein
  */
object OnBoardCSourcesGenerator extends LazyLogging {

  def fqn(str: String): Fqn = Fqn.newFromSource(str)

  def main(args : Array[String]) = {
    val config = new CGeneratorConfiguration(new File("gen/"),
      ModelRegistry.registry,
      "ru.mipt.acsl.photon.Main",
      HashMap(
        fqn("decode") -> Some(fqn("photon.decode")),
        fqn("ru.mipt.acsl.photon") -> Some(fqn("photon")),
        fqn("ru.mipt.acsl.foundation") -> Some(fqn("photon.foundation")),
        fqn("ru.mipt.acsl.fs") -> Some(fqn("photon.fs")),
        fqn("ru.mipt.acsl.identification") -> Some(fqn("photon.identification")),
        fqn("ru.mipt.acsl.mcc") -> Some(fqn("photon")),
        fqn("ru.mipt.acsl.routing") -> Some(fqn("photon.routing")),
        fqn("ru.mipt.acsl.scripting") -> Some(fqn("photon.scripting")),
        fqn("ru.mipt.acsl.segmentation") -> Some(fqn("photon.segmentation")),
        fqn("ru.mipt.acsl.tm") -> Some(fqn("photon.tm"))),
      sources = ModelRegistry.provider.resourceNames(ModelRegistry.config).map(r =>
        GeneratorSource(r, Source.fromInputStream(getClass.getResourceAsStream(
          ModelRegistry.config.resourcePath + "/" + r)))),
      isSingleton = true,
      prologue = FileGeneratorConfiguration(isActive = true, path = Some("photon/prologue.h"),
        contents = Some(
          """
            |#ifndef __PHOTON_PROLOGUE__
            |#define __PHOTON_PROLOGUE__
            |
            |#include "photon/Result.h"
            |#include "photon/Reader.h"
            |#include "photon/Writer.h"
            |#include "photon/Ber.h"
            |
            |typedef uint8_t PhotonGtB8;
            |typedef uint8_t PhotonGtU8;
            |typedef uint16_t PhotonGtU16;
            |typedef uint32_t PhotonGtU32;
            |typedef uint64_t PhotonGtU64;
            |typedef PhotonBer PhotonGtBer;
            |
            |typedef struct {
            |   void* _stub;
            |} PhotonGcMain;
            |
            |#endif
          """.stripMargin)))
    logger.debug(s"Generating on-board sources to ${config.outputDir.getAbsolutePath}...")
    new CSourcesGenerator(config).generate()
  }
}