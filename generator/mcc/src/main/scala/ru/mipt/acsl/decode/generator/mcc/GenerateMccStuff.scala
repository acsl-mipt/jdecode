package ru.mipt.acsl.decode.generator.mcc

import java.io.{File, FileOutputStream, OutputStream}

import ru.mipt.acsl.decode.generator.json.{DecodeJsonGenerator, DecodeJsonGeneratorConfig}
import ru.mipt.acsl.decode.parser.ModelRegistry
import ru.mipt.acsl.geotarget.OnBoardCSourceGenerator

/**
  * @author Artem Shein
  */
object GenerateMccStuff {

  def main(args: Array[String]): Unit = {
    require(args.nonEmpty)
    new FileOutputStream(new File(args(0))) { os: OutputStream =>
      new DecodeJsonGenerator(DecodeJsonGeneratorConfig(ModelRegistry.registry, os,
        OnBoardCSourceGenerator.RootComponentFqn, prettyPrint = true)).generate()
      close()
    }
  }

}
