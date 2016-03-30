package ru.mipt.acsl.decode.parser

import com.typesafe.scalalogging.LazyLogging
import ru.mipt.acsl.decode.model.domain.impl.registry.Registry
import ru.mipt.acsl.decode.modeling.ErrorLevel

/**
  * @author Artem Shein
  */
object ModelRegistry extends LazyLogging {

  val provider = new DecodeSourceProvider()
  val config = new DecodeSourceProviderConfiguration("ru/mipt/acsl/decode")

  def registry(clsLoader: ClassLoader = getClass.getClassLoader): Registry = {
    val registry = provider.provide(config, clsLoader)
    val resolvingResult = registry.resolve()
    if (resolvingResult.exists(_.level == ErrorLevel))
      resolvingResult.foreach(msg => logger.error(msg.text))
    registry
  }

}
