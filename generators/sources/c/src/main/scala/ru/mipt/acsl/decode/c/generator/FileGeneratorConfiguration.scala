package ru.mipt.acsl.decode.c.generator

/**
  * @author Artem Shein
  */
case class FileGeneratorConfiguration(isActive: Boolean = false, path: Option[String] = None,
                                      contents: Option[String] = None)
