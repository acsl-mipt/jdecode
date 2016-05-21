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

  val PixhawkComponentFqn = Fqn.newFromSource("pixhawk.Pixhawk")

  val PixhawkSourceResource = "pixhawk/pixhawk.xml"
  val PixhawkIncludes = Seq("pixhawk/common.xml")

  def main(args: Array[String]): Unit = {
    require(args.nonEmpty)
    new FileOutputStream(new File(args(0))) { os: OutputStream =>

      val pixhawkOutput = new ByteArrayOutputStream()

      def resourceContents(path: String): String = Resources.toString(Resources.getResource(path),
        StandardCharsets.UTF_8)

      MavlinkSourceGenerator(MavlinkSourceGeneratorInternalConfig(resourceContents(PixhawkSourceResource),
        PixhawkComponentFqn.copyDropLast.asMangledString, PixhawkComponentFqn.last.asMangledString, pixhawkOutput,
        PixhawkIncludes.map(i => (new File(i).getName, resourceContents(i))).toMap))
        .generate()

      DecodeJsonGenerator(DecodeJsonGeneratorConfig(
        ModelRegistry.registry(
          OnBoardModelRegistry.Sources.map(ModelRegistry.sourceContents) :+
            new String(pixhawkOutput.toByteArray, StandardCharsets.UTF_8)), os,
        Seq(OnBoardCSourceGenerator.RootComponentFqn, PixhawkComponentFqn.asMangledString), prettyPrint = true))
        .generate()

      close()
    }
  }

}
