package ru.mipt.acsl.decode.generator.mcc

import java.io.{ByteArrayOutputStream, File, FileOutputStream, OutputStream}
import java.nio.charset.StandardCharsets

import com.google.common.io.Resources
import ru.mipt.acsl.decode.generator.json.{DecodeJsonGenerator, DecodeJsonGeneratorConfig}
import ru.mipt.acsl.decode.mavlink.generator.{MavlinkSourceGenerator, MavlinkSourceGeneratorInternalConfig}
import ru.mipt.acsl.decode.model.naming.Fqn
import ru.mipt.acsl.decode.parser.ModelRegistry
import ru.mipt.acsl.geotarget.{OnBoardCSourceGenerator, OnBoardModelRegistry}

/**
  * @author Artem Shein
  */
object GenerateMccStuff {

  val PixhawkComponentFqn = Fqn.newInstance("mavlink.Pixhawk")

  val PixhawkSourceResource = "pixhawk/pixhawk.xml"
  val PixhawkIncludes = Seq("pixhawk/common.xml")

  val ModelFilePath = "src/mcc/core/db/db/model.json"

  def main(args: Array[String]): Unit = {

    new FileOutputStream(new File(ModelFilePath)) { os: OutputStream =>

      val pixhawkOutput = new ByteArrayOutputStream()

      def resourceContents(path: String): String = Resources.toString(Resources.getResource(path),
        StandardCharsets.UTF_8)

      MavlinkSourceGenerator(MavlinkSourceGeneratorInternalConfig(resourceContents(PixhawkSourceResource),
        PixhawkComponentFqn.copyDropLast.mangledNameString(), PixhawkComponentFqn.last.mangledNameString(), pixhawkOutput,
        PixhawkIncludes.map(i => (new File(i).getName, resourceContents(i))).toMap))
        .generate()

      DecodeJsonGenerator(DecodeJsonGeneratorConfig(
        ModelRegistry.registry(
          OnBoardModelRegistry.Sources.map(ModelRegistry.sourceContents) :+
            new String(pixhawkOutput.toByteArray, StandardCharsets.UTF_8)), os,
        Seq(OnBoardCSourceGenerator.RootComponentFqn, PixhawkComponentFqn.mangledNameString()), prettyPrint = true))
        .generate()

      close()
    }
  }

}
