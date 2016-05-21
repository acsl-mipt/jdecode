package ru.mipt.acsl.geotarget

import ru.mipt.acsl.decode.model.registry.Registry
import ru.mipt.acsl.decode.parser.ModelRegistry

/**
  * @author Artem Shein
  */
object OnBoardModelRegistry {

  import ModelRegistry._

  val FoundationSource = SourceFileName("foundation")
  val FsSource = SourceFileName("fs")
  val IdentificationSource = SourceFileName("identification")
  val MccSource = SourceFileName("mcc")
  val PhotonSource = SourceFileName("photon")
  val ScriptingSource = SourceFileName("scripting")
  val SegmentationSource = SourceFileName("segmentation")
  val TmSource = SourceFileName("tm")
  val RoutingSource = SourceFileName("routing")

  val Sources = Seq(FoundationSource, FsSource, IdentificationSource, MccSource, PhotonSource, ScriptingSource,
    SegmentationSource, TmSource, RoutingSource) ++ ModelRegistry.Sources

  def registry: Registry = ModelRegistry.registryForSourceNames(Sources)

}
