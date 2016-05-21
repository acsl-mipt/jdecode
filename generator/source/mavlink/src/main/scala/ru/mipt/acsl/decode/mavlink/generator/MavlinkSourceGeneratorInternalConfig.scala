package ru.mipt.acsl.decode.mavlink.generator

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

/**
  * Created by metadeus on 21.05.16.
  */
case class MavlinkSourceGeneratorInternalConfig(inputContents: String, nsFqn: String, componentName: String,
                                                outputStream: ByteArrayOutputStream = new ByteArrayOutputStream(),
                                                includes: Map[String, String] = Map.empty)
  extends MavlinkSourceGeneratorConfig {

  override def includeContents(fileName: String): String = includes(fileName)

  override def writeOutput(contents: String): Unit = outputStream.write(contents.getBytes(StandardCharsets.UTF_8))
}
