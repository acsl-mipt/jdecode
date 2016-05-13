package ru.mipt.acsl.decode.c.generator

import java.{io, util}
import java.io.{ByteArrayInputStream, ByteArrayOutputStream, File}
import java.nio.charset.StandardCharsets

import com.typesafe.scalalogging.LazyLogging
import org.apache.commons.compress.compressors.xz.{XZCompressorInputStream, XZCompressorOutputStream}
import org.apache.commons.compress.utils.IOUtils
import ru.mipt.acsl.decode.c.generator.implicits._
import ru.mipt.acsl.decode.c.generator.implicits.serialization._
import ru.mipt.acsl.decode.generator.json.{DecodeJsonGenerator, DecodeJsonGeneratorConfig}
import ru.mipt.acsl.decode.model.component.message.{EventMessage => _}
import ru.mipt.acsl.decode.model.component.{Command, Component}
import ru.mipt.acsl.decode.model.naming.{ElementName, HasName, Namespace}
import ru.mipt.acsl.decode.model.types.{AliasType, ArrayType, DecodeType, EnumType, GenericType, GenericTypeSpecialized, NativeType, PrimitiveTypeInfo, StructType, SubType, TypeKind}
import ru.mipt.acsl.generator.c.ast._
import ru.mipt.acsl.generator.c.ast.implicits._

import scala.collection.{immutable, mutable}

class CSourceGenerator(val config: CGeneratorConfiguration) extends LazyLogging {

  import CSourceGenerator._

  def generate(): Unit = {
    val component = config.registry.component(config.rootComponentFqn).getOrElse(
      sys.error(s"component not found ${config.rootComponentFqn}"))
    component.generateRoot()
  }

  private def provideSources(): Unit = {
    if (config.sources.isEmpty)
      return
    val dir = new File(config.outputDir, "decode/")
    dir.mkdirs()
    config.sources.foreach(s => new File(dir, new File(s.name).getName).write(s.contents))
  }

  private def generatePrologueEpilogue(): Unit = {
    generateFile(config.prologue.isActive, prologuePath, config.prologue.contents)
    generateFile(config.epilogue.isActive, epiloguePath, config.epilogue.contents)
  }

  private def generateFile(isActive: Boolean, filePath: String, contents: Option[String]): Unit = {
    if (isActive) for (c <- contents) {
      val f = new io.File(config.outputDir, filePath)
      f.getParentFile.mkdirs()
      f.write(c)
    }
  }

  private def prologuePath: String = config.prologue.path.getOrElse("photon_prologue.h")
  private def epiloguePath: String = config.epilogue.path.getOrElse("photon_epilogue.h")

  private def appendPrologueEpilogue(file: CAstElements): CAstElements = {
    (if (config.prologue.isActive)
      CInclude(prologuePath).eol.eol
    else
      CAstElements()) ++
      file ++
      (if (config.epilogue.isActive)
        Seq(CEol) ++ CInclude(epiloguePath).eol.eol
      else
        CAstElements())
  }

  private def isAliasNameTheSame(t: AliasType): Boolean = t.cTypeName.equals(t.baseType.cTypeName)

  private implicit class DecodeTypeHelper(val t: DecodeType) {

    def relPath: String = t.namespace.dirPath + io.File.separator + t.fileName + headerExt

    def generateSeparateFiles(nsDir: io.File): Unit = if (!t.isNative) {
      val fileName = t.fileName
      val hFileName = fileName + headerExt
      val cFileName = fileName + sourcesExt
      val (hFile, cFile) = (new io.File(nsDir, hFileName), new io.File(nsDir, cFileName))
      val (h, c) = t.generate(nsDir)
      if (h.nonEmpty)
        hFile.writeIfNotEmptyWithComment((CEol +: appendPrologueEpilogue(h).eol).protectDoubleInclude(t.relPath),
          "Type header")
      else
        logger.debug(s"Omitting type ${t.name.asMangledString}")
      if (c.nonEmpty)
        cFile.writeIfNotEmptyWithComment(CInclude(t.relPath).eol.eol ++ c, "Type implementation")
    }

    def generate(nsDir: io.File): (CAstElements, CAstElements) = {
      var (h, c): (CAstElements, CAstElements) = t match {
        case t: NativeType if t.isPrimitive => (Seq(t.cTypeDef(t.cType)), Seq.empty)
        case t: NativeType => (Seq(t.cTypeDef(CVoidType.ptr)), Seq.empty)
        case t: SubType => (Seq(t.cTypeDef(t.baseType.cType)), Seq.empty)
        case t: EnumType =>
          val prefixedEnumName = t.prefixedCTypeName.upperCamel2UpperUnderscore
          (Seq(t.cTypeDef(CEnumTypeDef(t.allConstants.toSeq.sortBy(_.value.toInt).map(c =>
            CEnumTypeDefConst(prefixedEnumName + "_" + c.name.asMangledString, c.value.toInt))))),
            Seq.empty)
        case t: GenericType =>
          (CAstElements(), CAstElements())
        case t: GenericTypeSpecialized => t.genericType match {
          case optional if optional.name.equals(ElementName.newFromMangledName("optional")) =>
            require(t.genericTypeArguments.size == 1)
            val head = t.genericTypeArguments.head.getOrElse {
              sys.error("wtf")
            }
            head match {
              // TODO: implement or remove
              // case h if h.isBasedOnEnum => (Seq(t.cTypeDef(head.obj.cType)), Seq.empty)
              case _ => (Seq(t.cTypeDef(CStructTypeDef(Seq(
                CStructTypeDefField("value", t.genericTypeArguments.head.get.cType),
                CStructTypeDefField("flag", b8Type))))), Seq.empty)
            }
          case or if or.name.equals(ElementName.newFromMangledName("or")) =>
            var index = 0
            (Seq(t.cTypeDef(CStructTypeDef(t.genericTypeArguments.flatMap { ot =>
              index += 1
              ot.map(t => Seq(CStructTypeDefField("_" + index, t.cType))).getOrElse(Seq.empty)
            } :+ tag.field))), Seq.empty)
        }
        case t: ArrayType =>
          val arrayType: CType = t.size.max match {
            case 0 => t.baseType.cType.ptr
            case _ => CArrayType(t.baseType.cType, t.size.max, dataVar.name)
          }
          val typeDef = CTypeDefStatement(t.prefixedCTypeName, CStructTypeDef(Seq(
            CStructTypeDefField(size.name, sizeTType),
            CStructTypeDefField(dataVar.name, arrayType))))
          val defineNamePrefix = t.prefixedCTypeName.upperCamel2UpperUnderscore
          (Seq(CDefine(defineNamePrefix + "_MIN_SIZE", t.size.min.toString), CEol,
            CDefine(defineNamePrefix + "_MAX_SIZE", t.size.max.toString), CEol, CEol, typeDef), CAstElements())
        case t: StructType => (Seq(t.cTypeDef(CStructTypeDef(t.fields.map(f =>
          CStructTypeDefField(f.name.asMangledString, f.typeUnit.t.cType))))), Seq.empty)
        case t: AliasType =>
          if (isAliasNameTheSame(t))
            (Seq.empty, Seq.empty)
          else
            (Seq(t.cTypeDef(t.baseType.cType)), Seq.empty)
        case _ => sys.error(s"not implemented $t")
      }

      if (h.nonEmpty) {
        val importTypes = t.importTypes
        val imports = CAstElements(importTypes.filterNot(_.isNative).flatMap(t => CInclude(t.relPath).eol): _*)

        val selfType = t.cType
        val serializeMethod = CFuncImpl(CFuncDef(t.serializeMethodName, resultType,
          Seq(CFuncParam(selfVar.name, mapIfNotSmall(selfType, t, (ct: CType) => ct.ptr.const)), writer.param)),
          t.serializeCode :+ CReturn(resultOk).line)
        val deserializeMethod = CFuncImpl(CFuncDef(t.deserializeMethodName, resultType,
          Seq(CFuncParam(selfVar.name, selfType.ptr), reader.param)), t.deserializeCode :+ CReturn(resultOk).line)

        h = h ++ Seq(CEol, serializeMethod.definition, CEol, CEol, deserializeMethod.definition)
        c = c ++ Seq(CEol, serializeMethod, CEol, CEol, deserializeMethod)

        ((if (imports.nonEmpty) imports.eol else imports) ++ h.externC.eol, c)
      } else {
        (CAstElements(), CAstElements())
      }
    }

  }

  private implicit class NamespaceHelper(val ns: Namespace) {

    def nsOrAliasCppSourceParts: Seq[String] =
      config.namespaceAliases.getOrElse(ns.fqn, Some(ns.fqn)).map(_.parts.map(_.asMangledString)).getOrElse(Seq.empty)

    def dirPath: String = nsOrAliasCppSourceParts.mkString(io.File.separator)

    def includePathFor(fileName: String): String = dirPath + io.File.separator + fileName

    def dir: io.File = new io.File(config.outputDir, dirPath)

    def ensureDirExists: io.File = {
      val dir = ns.dir
      if (!(dir.exists() || dir.mkdirs()))
        sys.error(s"Can't create directory ${dir.getAbsolutePath}")
      dir
    }

    def generate(): Unit = {
      val nsDir = ns.ensureDirExists
      ns.subNamespaces.foreach(_.generate())
      val typesHeader = CAstElements()
      ns.types.foreach(t => t.generateSeparateFiles(nsDir))
      val fileName: String = "types" + headerExt
      new io.File(nsDir, fileName).writeIfNotEmptyWithComment(typesHeader.protectDoubleInclude(ns.dirPath +
        io.File.separator + fileName), s"Types of ${ns.fqn.asMangledString} namespace")
    }

  }

  private implicit class ComponentHelper(val component: Component) {

    def includePath: String =
      component.namespace.includePathFor(component.prefixedTypeName + headerExt)

    def structType: CStructTypeDef = {
      val componentSelfPtrType = component.ptrType
      CStructTypeDef(Seq(CStructTypeDefField("data", CTypeApplication(component.componentDataTypeName).ptr)) ++
        component.allCommands.map { case ComponentCommand(c, command) =>
          component.structTypeFieldForCommand(c, command)
        } ++
        component.allParameters.map { case ComponentParameterField(c, f) =>
          val name = f.cStructFieldName(component, c)
          CStructTypeDefField(
            name, CFuncType(f.typeUnit.t.cType, Seq(componentSelfPtrType), name))
        } ++
        component.baseType.map(_.fields.map { f =>
          val name = f.mangledCName
          CStructTypeDefField(name, CFuncType(f.typeUnit.t.cType, Seq(componentSelfPtrType), name))
        }).getOrElse(Seq.empty) ++ component.commands.map(component.structTypeFieldForCommand(component, _)),
        Some(component.prefixedTypeName + structNamePostfix))
    }

    def structTypeFieldForCommand(c: Component, command: Command): CStructTypeDefField = {
      val methodName = command.cStructFieldName(component, component)
      CStructTypeDefField(methodName, CFuncType(command.returnType.map(_.cType).getOrElse(voidType),
        command.cFuncParameterTypes(component), methodName))
    }

    def importStatements: CAstElements = {
      val imports = component.subComponents.flatMap(cr => CInclude(cr.component.includePath).eol).to[mutable.Buffer]
      if (imports.nonEmpty)
        imports += CEol
      val types = component.allTypes.toSeq
      val typeIncludes = types.filterNot(_.isNative).flatMap(t => CInclude(t.relPath).eol)
      imports ++= typeIncludes
      if (typeIncludes.nonEmpty)
        imports += CEol
      imports.to[immutable.Seq]
    }

    def typeIncludes: CAstElements =
      component.allTypes.toSeq.filter(_.isGeneratable).flatMap(t => CInclude(t.relPath).eol)

    def generateRoot(): Unit = {
      logger.debug(s"Generating component ${component.name.asMangledString}")
      provideSources()
      generatePrologueEpilogue()
      config.isSingleton match {
        case true =>
          component.generateSingleton()
        case _ =>
          sys.error("not implemented")
          val nsSet = mutable.HashSet.empty[Namespace]
          component.collectNamespaces(nsSet)
          nsSet.foreach(_.generate())
          allComponentsSetForComponent(component).foreach(_.generate())
      }
    }

    def generateSingleton(): Unit = {

      component.allTypes.foreach(t => t.generateSeparateFiles(t.namespace.ensureDirExists))

      val nsDir = component.namespace.ensureDirExists
      val componentStructName = component.prefixedTypeName
      val (hFile, cFile) = (new File(nsDir, componentStructName + headerExt),
        new File(nsDir, componentStructName + sourcesExt))

      val guidDefines = component.componentDefines

      val methods = component.allMethods ++ component.allSubComponentsMethods

      hFile.write((CEol +: appendPrologueEpilogue((component.typeIncludes ++
        "USER command implementation functions, MUST BE implemented".comment ++
        component.commandMethodImplDefs.groupBy(_.component).flatMap{ case (c, cMethods) =>
          (("Component " + c.name.asMangledString).comment ++ cMethods.flatMap(m => Seq(CEol, m.methodDef))).eol
        }.toSeq ++
        "USER parameter implementation functions, MUST BE implemented".comment ++
        component.parameterMethodImplDefs.groupBy(_.component).flatMap{ case (c, cMethods) =>
          (("Component " + c.name.asMangledString).comment ++ cMethods.flatMap(m => Seq(CEol, m.methodDef))).eol
        }.toSeq ++
        "USER service functions, MUST BE implemented".comment ++
        component.serviceMethodDefs.flatMap(m => Seq(CEol, m)).eol ++
        "Component defines".comment.eol ++ guidDefines ++
        "Message ID for component defines".comment.eol ++ component.allMessageDefines ++
        "Command ID for component defines".comment.eol ++ component.allCommandDefines ++
        "Public interface functions".comment.eol ++ methods.groupBy(_.component).flatMap{
        case (c, cMethods) => cMethods.filter(_.isPublic) match {
          case m if m.isEmpty => Seq.empty
          case m =>
            Seq(CEol) ++ ("Component " + c.name.asMangledString).comment ++
              m.flatMap(m => CAstElements(CEol, m.impl.definition))
        }
        }).externC.eol))
        .protectDoubleInclude(component.namespace.dirPath + hFile.getName))

      if (config.includeModelInfo) {
        val modelC = new StringBuilder()

        new ByteArrayOutputStream() {
          new DecodeJsonGenerator(DecodeJsonGeneratorConfig(config.registry, this, config.rootComponentFqn, prettyPrint = true)).generate()

          modelC.append("/*").append(new String(toByteArray, StandardCharsets.UTF_8)).append("*/\n\n")

          reset()

          new DecodeJsonGenerator(DecodeJsonGeneratorConfig(config.registry, this, config.rootComponentFqn)).generate()

          private val rawJsonMinified = toByteArray
          private val array = new ByteArrayOutputStream() {
            new XZCompressorOutputStream(this) {
              write(rawJsonMinified)
              finish()
              close()
            }
          }.toByteArray

          assert(util.Arrays.equals(rawJsonMinified,
            IOUtils.toByteArray(new XZCompressorInputStream(new ByteArrayInputStream(array)))))

          modelC.append(s"static uint8_t modelData[${array.length}] = {")
          for ((byte, index) <- array.zipWithIndex) {
            modelC.append("0x%02X, ".format(byte))
            if (index % 20 == 0)
              modelC.append("\n\t")
          }

          close()
        }
        modelC.append("};\n")

        new File(nsDir, "model" + sourcesExt).write(modelC.toString())
      }

      cFile.write(CInclude(component.namespace.includePathFor(hFile.getName)).eol.eol ++
        methods.flatMap(_.impl.definition.eol.eol) ++
        methods.flatMap(_.impl.eol.eol))
    }

    def generate(): Unit = {
      val dir = component.namespace.dir
      val componentStructName = component.prefixedTypeName
      val hFileName = componentStructName + headerExt
      val (hFile, cFile) = (new io.File(dir, hFileName), new io.File(dir, componentStructName + sourcesExt))
      val imports = component.importStatements
      val componentFunctionTableName = component.functionTableTypeName
      val componentFunctionTableNameStruct = componentFunctionTableName + structNamePostfix
      val forwardFuncTableDecl = CForwardStructDecl(componentFunctionTableNameStruct)
      val componentTypeStructName = componentStructName + structNamePostfix
      val componentTypeForwardDecl = CForwardStructTypeDef(componentStructName, componentTypeStructName)
      val componentType = component.structType

      val methods = component.allCommandsMethods ++ component.allStatusMessageMethods ++
        component.allMethods

      val externedCFile = (forwardFuncTableDecl.eol.eol ++ componentTypeForwardDecl.eol.eol ++
        Seq(componentType) ++ CSemicolon.eol ++ methods.flatMap(m => m.impl.definition.eol)).externC
      hFile.writeIfNotEmptyWithComment((CEol +: appendPrologueEpilogue(imports ++ externedCFile))
        .protectDoubleInclude(component.namespace.dirPath + hFileName),
        s"Component ${component.name.asMangledString} interface")
      cFile.writeIfNotEmptyWithComment(CInclude(component.includePath).eol.eol ++
        methods.flatMap(m => m.impl.eol.eol), s"Component ${component.name.asMangledString} implementation")
    }

  }

}

private[generator] object CSourceGenerator {

  val PtrSize = 4
  val VaruintByteSize = 8

  val headerExt = ".h"
  val sourcesExt = ".c"
  val structNamePostfix = "_s"
  val cppDefine = "__cplusplus"
  val typePrefix = "PhotonGc"

  val tryMacroName = "PHOTON_TRY"
  val typeInitMethodName = "Init"

  val photonBerTypeName = "PhotonBer"
  val b8Type = CTypeApplication("PhotonGtB8")
  val berType = CTypeApplication("PhotonBer")
  val voidType = CTypeApplication("void")
  val sizeTType = CTypeApplication("size_t")

  val resultType = CTypeApplication("PhotonResult")
  val resultOk = CVar(resultType.name + "_Ok")

  val selfVar = CVar("self")
  val invalidMessageId = CVar("PhotonResult_InvalidMessageId")
  val invalidCommandId = CVar("PhotonResult_InvalidCommandId")
  val invalidComponentId = CVar("PhotonResult_InvalidComponentId")
  val eventIsDenied = CVar("PhotonResult_EventIsDenied")

  val reader = TypedVar("reader", CTypeApplication("PhotonReader").ptr)
  val writer = TypedVar("writer", CTypeApplication("PhotonWriter").ptr)
  private val tag = TypedVar("tag", berType)
  val size = TypedVar("size", berType)
  val i = TypedVar("i", berType)

  val componentId = TypedVar("componentId", berType)
  val commandId = TypedVar("commandId", berType)
  val messageId = TypedVar("messageId", berType)
  val eventId = TypedVar("eventId", berType)

  def tryCall(methodName: String, exprs: CExpression*): CFuncCall = methodName.call(exprs: _*)._try

  def collectNsForType[T <: DecodeType](t: T, set: mutable.Set[Namespace]): Unit =
    t.collectNamespaces(set)

  def mapIfSmall[A <: B, B <: CAstElement](el: A, t: DecodeType, f: A => B): B = if (t.isSmall) f(el) else el

  def mapIfNotSmall[A <: B, B <: CAstElement](el: A, t: DecodeType, f: A => B): B = if (t.isSmall) el else f(el)

  private def allComponentsSetForComponent(component: Component,
                                           componentsSet: immutable.HashSet[Component] = immutable.HashSet.empty)
  : immutable.HashSet[Component] = {
    componentsSet ++ Seq(component) ++ component.subComponents.flatMap(cr =>
      allComponentsSetForComponent(cr.component, componentsSet))
  }

  private def primitiveTypeToCTypeApplication(primitiveType: PrimitiveTypeInfo): CTypeApplication = {
    import TypeKind._
    (primitiveType.kind, primitiveType.bitLength) match {
      case (Bool, 8) => CUint8TType
      case (Bool, 16) => CUint16TType
      case (Bool, 32) => CUint32TType
      case (Bool, 64) => CUint64TType
      case (Float, 32) => CFloatType
      case (Float, 64) => CDoubleType
      case (Int, 8) => CInt8TType
      case (Int, 16) => CInt16TType
      case (Int, 32) => CInt32TType
      case (Int, 64) => CInt64TType
      case (Uint, 8) => CUint8TType
      case (Uint, 16) => CUint16TType
      case (Uint, 32) => CUint32TType
      case (Uint, 64) => CUint64TType
      case _ => sys.error("illegal bit length")
    }
  }

  val keywords = Seq("return")

  def casesForMap[T <: HasName](map: immutable.HashMap[Int, WithComponent[T]],
                                        apply: (T, Component) => Option[CAstElements]): Seq[CCase] =
    map.toSeq.sortBy(_._1).flatMap { case (id, WithComponent(c, _2)) => apply(_2, c).map(els => CCase(CIntLiteral(id), els)) }
}
