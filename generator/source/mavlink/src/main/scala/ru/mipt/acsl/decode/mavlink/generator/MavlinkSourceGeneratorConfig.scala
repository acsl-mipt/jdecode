package ru.mipt.acsl.decode.mavlink.generator

/**
  * Created by metadeus on 21.05.16.
  */
trait MavlinkSourceGeneratorConfig {

  def includeContents(fileName:  String): String

  def inputContents: String

  def writeOutput(contents: String): Unit

  def nsFqn: String

  def componentName: String

}
