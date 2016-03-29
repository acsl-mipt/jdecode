package ru.mipt.acsl.decode.parser

import com.intellij.lang.impl.PsiBuilderFactoryImpl
import com.intellij.lang.{DefaultASTFactory, DefaultASTFactoryImpl, LanguageParserDefinitions, PsiBuilderFactory}
import com.intellij.mock.{MockApplicationEx, MockProjectEx}
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.fileTypes.{FileTypeManager, FileTypeRegistry}
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.impl.ProgressManagerImpl
import com.intellij.openapi.util.{Disposer, Getter}
import com.intellij.openapi.vfs.encoding.{EncodingManager, EncodingManagerImpl}
import com.intellij.psi.impl.source.resolve.reference.{ReferenceProvidersRegistry, ReferenceProvidersRegistryImpl}
import com.typesafe.scalalogging.LazyLogging
import org.picocontainer.PicoContainer
import org.picocontainer.defaults.AbstractComponentAdapter
import ru.mipt.acsl.decode.model.domain.impl.registry.Registry

import scala.collection.immutable
import scala.io.Source

/**
  * @author Artem Shein
  */
class DecodeSourceProvider extends LazyLogging {

  private object ParserBoilerplate {

    Extensions.registerAreaClass("IDEA_PROJECT", null)

    private val disposable = new Disposable {
      override def dispose(): Unit = sys.error("must not dispose?")
    }

    private def application: MockApplicationEx = ApplicationManager.getApplication.asInstanceOf[MockApplicationEx]

    private def initApplication(): Unit = {
      ApplicationManager.setApplication(new MockApplicationEx(disposable),
        new Getter[FileTypeRegistry]() {
          override def get: FileTypeRegistry = FileTypeManager.getInstance
        }, disposable)
      application.registerService(classOf[EncodingManager], classOf[EncodingManagerImpl])
    }

    private val project: MockProjectEx = new MockProjectEx(disposable)

    private def registerApplicationService[T](cls: Class[T], obj: T): Unit = {
      application.registerService(cls, obj)
      Disposer.register(project, new Disposable() {
        override def dispose(): Unit = application.getPicoContainer.unregisterComponent(cls.getName)
      })
    }

    def init(): Unit = {
      initApplication()
      application.getPicoContainer.registerComponent(new AbstractComponentAdapter(classOf[ProgressManager].getName, classOf[Object]) {
        override def getComponentInstance(container: PicoContainer): ProgressManager = new ProgressManagerImpl()

        override def verify(container: PicoContainer): Unit = {}
      })
      registerApplicationService(classOf[PsiBuilderFactory], new PsiBuilderFactoryImpl())
      registerApplicationService(classOf[DefaultASTFactory], new DefaultASTFactoryImpl())
      registerApplicationService(classOf[ReferenceProvidersRegistry], new ReferenceProvidersRegistryImpl())
      LanguageParserDefinitions.INSTANCE.addExplicitExtension(DecodeLanguage.INSTANCE, new DecodeParserDefinition)
    }
  }

  ParserBoilerplate.init()

  def resourceNames(config: DecodeSourceProviderConfiguration): Seq[String] = {
    val resourcePath = config.resourcePath
    val resourcesAsStream = getClass.getResourceAsStream(resourcePath)
    require(resourcesAsStream != null, resourcePath)
    Source.fromInputStream(resourcesAsStream).getLines().filter(_.endsWith(".decode")).toSeq
  }

  def provide(config: DecodeSourceProviderConfiguration): Registry = {
    val resourcePath = config.resourcePath
    val registry = Registry()
    registry.rootNamespaces ++= resourceNames(config).map { name =>
      val resource = resourcePath + "/" + name
      logger.debug(s"Parsing $resource...")
      val parserDefinition = new DecodeParserDefinition()
      new DecodeAstTransformer().processFile(new DecodeParser().parse(DecodeParserDefinition.file, new PsiBuilderFactoryImpl().createBuilder(parserDefinition,
        parserDefinition.createLexer(null),
        Source.fromInputStream(getClass.getResourceAsStream(resource)).mkString)))
    }.to[immutable.Seq].mergeRoot
    registry
  }
}
