package ru.mipt.acsl.decode.c.generator

import java.io
import java.io.{File, OutputStreamWriter, FileOutputStream}
import java.security.MessageDigest

import com.google.common.base.{CaseFormat, Charsets}
import com.typesafe.scalalogging.LazyLogging
import ru.mipt.acsl.decode.model.domain._
import ru.mipt.acsl.decode.model.domain.impl.`type`.DecodeBerType
import ru.mipt.acsl.decode.model.domain.impl.`type`.DecodeOptionalType
import ru.mipt.acsl.decode.model.domain.impl.`type`.DecodeOrType
import ru.mipt.acsl.generation.Generator
import ru.mipt.acsl.generator.c.ast.Implicits._
import ru.mipt.acsl.generator.c.ast._

import resource._

import scala.collection.mutable
import scala.util.Random

case class CGeneratorConfiguration(outputDir: io.File, registry: DecodeRegistry, rootComponentFqn: String,
                                   namespaceAliases: Map[DecodeFqn, Option[DecodeFqn]] = Map.empty,
                                   prologEpilogPath: Option[String] = None, isSingleton: Boolean = false)

class CSourcesGenerator(val config: CGeneratorConfiguration) extends Generator[CGeneratorConfiguration] with LazyLogging {

  import CSourcesGenerator._

  override def getConfiguration: CGeneratorConfiguration = config

  override def generate() {
    val component: DecodeComponent = config.registry.getComponent(config.rootComponentFqn).get
    enumerateComponentsFrom(component)
    generateRootComponent(component)
  }

  private def ensureDirForNsExists(ns: DecodeNamespace): io.File = {
    val dir = dirForNs(ns)
    if (!(dir.exists() || dir.mkdirs()))
      sys.error(s"Can't create directory ${dir.getAbsolutePath}")
    dir
  }

  private def generateNs(ns: DecodeNamespace) {
    val nsDir = ensureDirForNsExists(ns)
    ns.subNamespaces.foreach(generateNs)
    val typesHeader = CAstElements()
    ns.types.foreach(t => generateTypeSeparateFiles(t, nsDir))
    val fileName: String = "types" + headerExt
    writeFileIfNotEmptyWithComment(new io.File(nsDir, fileName), typesHeader.protectDoubleInclude(fileName),
      s"Types of ${ns.fqn.asMangledString} namespace")
    //ns.getComponents.toTraversable.foreach(generateRootComponent)
  }

  private def generateType(t: DecodeType, nsDir: io.File): (CAstElements, CAstElements) = {
    val (h, c): (CAstElements, CAstElements) = t match {
      case t: DecodePrimitiveType => (CAstElements(t.cTypeDef(t.cType)), CAstElements())
      case t: DecodeNativeType => (CAstElements(t.cTypeDef(CVoidType.ptr)), CAstElements())
      case t: DecodeSubType => (CAstElements(t.cTypeDef(t.baseType.obj.cType)), CAstElements())
      case t: DecodeEnumType =>
        val prefixedEnumName = upperCamelCaseToUpperUnderscore(t.prefixedCTypeName)
        (CAstElements(t.cTypeDef(CEnumTypeDef(t.constants.map(c =>
          CEnumTypeDefConst(prefixedEnumName + "_" + c.name.asMangledString, c.value.toInt))))),
          CAstElements())
      case t: DecodeGenericType =>
        (CAstElements(), CAstElements())
      case t: DecodeGenericTypeSpecialized => t.genericType.obj match {
        case optional: DecodeOptionalType =>
          require(t.genericTypeArguments.size == 1)
          require(t.genericTypeArguments.head.isDefined)
          (CAstElements(t.cTypeDef(CStructTypeDef(Seq(CStructTypeDefField("flag", b8Type),
            CStructTypeDefField("value", t.genericTypeArguments.head.get.obj.cType))))), CAstElements())
        case or: DecodeOrType =>
          var index = 0
          (CAstElements(t.cTypeDef(CStructTypeDef(Seq(tag.field) ++ t.genericTypeArguments.flatMap { ot =>
            index += 1
            ot.map(t => Seq(CStructTypeDefField("_" + index, t.obj.cType))).getOrElse(Seq.empty)
          }))), CAstElements())
      }
      case t: DecodeArrayType =>
        val sizeVar: CVar = CVar("size")
        val dataVar: CVar = CVar("data")
        val arrayType: CArrayType = CArrayType(t.baseType.obj.cType, maxLengthFor(t), dataVar.name)
        val typeDef = CTypeDefStatement(t.prefixedCTypeName, CStructTypeDef(Seq(
          CStructTypeDefField(sizeVar.name, sizeTType),
          CStructTypeDefField(dataVar.name, arrayType))))
        /*val initFunc = CFuncImpl(CFuncDef(typeDef.name + cDecodeArrayTypeInitMethodNamePart,
          parameters = Seq(CFuncParam(selfVar.name, typeDef.ptr), CFuncParam(sizeVar.name, sizeTType),
            CFuncParam(dataVar.name, arrayType))), CAstElements(CStatementLine(
          CAssign(CArrow(selfVar, sizeVar), sizeVar)), CStatementLine(CAssign(CArrow(selfVar, dataVar), dataVar))))*/
        (CAstElements(typeDef),
          CAstElements())
      case t: DecodeStructType => (CAstElements(t.cTypeDef(CStructTypeDef(t.fields.map(f =>
        CStructTypeDefField(f.name.asMangledString, f.typeUnit.t.obj.cType))))), CAstElements())
      case t: DecodeAliasType =>
        if (isAliasNameTheSame(t))
          (CAstElements(), CAstElements())
        else
          (CAstElements(t.cTypeDef(t.baseType.obj.cType)), CAstElements())
      case _ => sys.error(s"not implemented $t")
    }

    if (h.nonEmpty) {
      val importTypes = t.importTypes
      val imports = CAstElements(importTypes.flatMap(t => Seq(CInclude(relPathForType(t)), CEol)): _*)

      val selfType = t.cType.ptr
      val serializeMethod = CFuncImpl(CFuncDef(methodNameFor(t, typeSerializeMethodName), resultType,
        Seq(CFuncParam(selfVar.name, selfType), writer.param)), t.serializeCode() ++ CAstElements(CStatementLine(CReturn(resultOk))))
      val deserializeMethod = CFuncImpl(CFuncDef(methodNameFor(t, typeDeserializeMethodName), resultType,
        Seq(CFuncParam(selfVar.name, selfType), reader.param)), t.deserializeCode() ++ CAstElements(CStatementLine(CReturn(resultOk))))

      h ++= Seq(CEol, serializeMethod.definition, CEol, CEol, deserializeMethod.definition)
      c ++= Seq(CEol, serializeMethod, CEol, CEol, deserializeMethod)
      if (imports.nonEmpty)
        imports += CEol
      (imports ++ externCpp(h) ++ Seq(CEol), c)
    } else {
      (CAstElements(), CAstElements())
    }
  }

  private def generateRootComponent(comp: DecodeComponent) {
    logger.debug(s"Generating component ${comp.name.asMangledString}")
    val nsSet = mutable.HashSet.empty[DecodeNamespace]
    comp.collectNamespaces(nsSet)
    nsSet.foreach(generateNs)
    val compSet = mutable.HashSet.empty[DecodeComponent]
    collectComponentsForComponent(comp, compSet)
    compSet.foreach(generateComponent)
    if (config.isSingleton)
      generateSingleton(comp)
  }

  private def generateSingleton(component: DecodeComponent): Unit = {
    val nsDir = dirForNs(component.namespace)
    val cComponentTypeName = component.prefixedTypeName
    val cSingletonComponentName = cComponentTypeName + singletonPostfix
    val hFile = new File(nsDir, cSingletonComponentName + headerExt)
    val cFile = new File(nsDir, cSingletonComponentName + sourcesExt)
    val cComponentSingletonType = CTypeApplication(cComponentTypeName)
    val componentSingletonVar = TypedVar(upperCamelCaseToLowerCamelCase(component.typeName), cComponentSingletonType)
    val commands = component.allCommands
    val initFunc = CFuncImpl(CFuncDef(methodNameFor(cSingletonComponentName, typeInitMethodName),
      cComponentSingletonType.ptr), CAstElements(commands.map(compCmd => {
      val component: DecodeComponent = compCmd.component
      val command: DecodeCommand = compCmd.command
      CStatementLine(CAssign(CDot(componentSingletonVar.v, CVar(command.methodNamePart(component, component))),
        CRef(CVar(methodNameFor(cSingletonComponentName, command.cName)))))
    }): _*) ++ CAstElements(CStatementLine(CReturn(CRef(componentSingletonVar.v)))))
    val cmdFuncDefs = commands.map(_.command).map(command =>
      CFuncDef(methodNameFor(cSingletonComponentName, command.cName),
        command.returnType.map(_.obj.cType).getOrElse(voidType),
        Seq(CFuncParam(selfVar.name, cComponentSingletonType.ptr)) ++
          command.parameters.map(p => CFuncParam(p.cName, p.paramType.obj.cType.ptr))))
    writeFile(hFile,
      CAstElements(CEol, CInclude(includePathForComponent(component)), CEol, CEol).protectDoubleInclude(hFile.getName) ++
        externCpp(CAstElements(initFunc.definition, CEol) ++
          cmdFuncDefs.flatMap(f => CAstElements(CEol, f))) ++ CAstElements(CEol))
    writeFile(cFile, CAstElements(CInclude(includePathForNsFileName(component.namespace, hFile.getName)), CEol, CEol,
      CStatementLine(CVarDef(componentSingletonVar.name, componentSingletonVar.t, static = true)), initFunc))
  }

  private def importStatementsForComponent(comp: DecodeComponent): CAstElements = {
    val imports = comp.subComponents.flatMap(cr => Seq(CInclude(includePathForComponent(cr.component.obj)), CEol))
    if (imports.nonEmpty)
      imports += CEol
    val types = typesForComponent(comp).toSeq
    val typeIncludes = types.filterNot(_.isPrimitiveOrNative).flatMap(t => Seq(CInclude(relPathForType(t)), CEol))
    imports ++= typeIncludes
    if (typeIncludes.nonEmpty)
      imports += CEol
    imports
  }

  private def generateComponent(component: DecodeComponent) {
    val dir = dirForNs(component.namespace)
    val componentStructName = component.prefixedTypeName
    val hFileName = componentStructName + headerExt
    val hFile = new io.File(dir, hFileName)
    val cFile = new io.File(dir, componentStructName + sourcesExt)
    val imports = importStatementsForComponent(component)
    val componentFunctionTableName = component.functionTableTypeName
    val componentFunctionTableNameStruct = componentFunctionTableName + structNamePostfix
    val forwardFuncTableDecl = CForwardStructDecl(componentFunctionTableNameStruct)
    val componentTypeStructName = componentStructName + structNamePostfix
    val componentSelfType = component.ptrType
    val componentTypeForwardDecl = CForwardStructTypeDef(componentStructName, componentTypeStructName)
    val componentType = componentStructType(component)
    val execCommand = CFuncImpl(CFuncDef(methodNameFor(componentStructName, "ExecuteCommand"), resultType,
      Seq(CFuncParam(selfVar.name, componentSelfType), reader.param, writer.param, commandId.param)),
      CAstElements(CIdent, CSwitch(commandId.v, casesForCommands(component),
        default = CAstElements(CStatementLine(CReturn(CVar("PhotonResult_InvalidCommandId"))))), CEol))
    val readExecCommand = CFuncImpl(CFuncDef(methodNameFor(componentStructName, "ReadExecuteCommand"), resultType,
      Seq(CFuncParam(selfVar.name, componentSelfType), reader.param, writer.param)), CStatements(
      CVarDef(commandId.name, commandId.t),
      tryCall(methodNameFor(photonBerTypeName, typeDeserializeMethodName), CRef(commandId.v), reader.v),
      CReturn(CFuncCall(execCommand.definition.name, selfVar, reader.v, writer.v, commandId.v))))
    val functionsForComponentCommands = functionsForCommands(component)
    val externedCFile = externCpp(CAstElements(forwardFuncTableDecl, CEol, CEol, componentTypeForwardDecl, CEol, CEol, componentType, CSemicolon, CEol) ++
     functionsForComponentCommands.flatMap(f => Seq(f.definition, CEol)) ++
     Seq(CEol, readExecCommand.definition, CEol))
    writeFileIfNotEmptyWithComment(hFile, (CEol +: appendPrologEpilog(imports ++ externedCFile))
      .protectDoubleInclude(hFileName), s"Component ${component.name.asMangledString} interface")
    writeFileIfNotEmptyWithComment(cFile, CAstElements(CInclude(includePathForComponent(component)), CEol, CEol) ++
      functionsForComponentCommands.flatMap(f => Seq(f, CEol, CEol)) ++
      Seq(execCommand, CEol, CEol, readExecCommand),
      s"Component ${component.name.asMangledString} implementation")
  }

  private def structTypeFieldForCommand(structComponent: DecodeComponent, component: DecodeComponent, command: DecodeCommand) = {
    val componentSelfType = structComponent.ptrType
    val methodName = upperCamelCaseToLowerCamelCase((if (structComponent == component) "" else component.cName) +
      command.mangledCName.capitalize)
    val returnType = command.returnType.map(_.obj.cType).getOrElse(voidType)
    val parameters = componentSelfType +: command.parameters.map(_.paramType.obj.cType.ptr)
    CStructTypeDefField(methodName, CFuncType(returnType, parameters, methodName))
  }

  private def componentStructType(component: DecodeComponent): CStructTypeDef = {
    val componentSelfType = component.ptrType
    CStructTypeDef(Seq(CStructTypeDefField("data", CTypeApplication(component.componentDataTypeName).ptr)) ++
      component.allCommands.map(compCmd => structTypeFieldForCommand(component, compCmd.component, compCmd.command)) ++
      component.baseType.map(_.obj.fields.map { f =>
        val name = f.mangledCName
        CStructTypeDefField(name, CFuncType(f.typeUnit.t.obj.cType, Seq(componentSelfType), name))
      }).getOrElse(Seq.empty) ++ component.commands.map(structTypeFieldForCommand(component, component, _)) ++
      component.allParameters.map(_ match { case ComponentParameterField(c, field) =>
        CStructTypeDefField(field.mangledCName, field.typeUnit.t.obj.cType)}),
      Some(component.prefixedTypeName + structNamePostfix))
  }

  private def functionsForCommands(rootComponent: DecodeComponent): Seq[CFuncImpl] = {
    val componentTypeName = rootComponent.prefixedTypeName
    val compType = rootComponent.ptrType
    val parameters = Seq(CFuncParam(selfVar.name, compType), reader.param, writer.param)
    commandsByIdFor(rootComponent).toSeq.sortBy(_._1).map(_._2).map(_ match { case ComponentCommand(component, command) =>
      val methodName = command.methodNamePart(rootComponent, component)
      val vars = command.parameters.map { p => CVar(p.mangledCName)}
      val varInits = vars.zip(command.parameters).flatMap(_ match { case (v, parameter) =>
        defineAndInitVar(v, parameter)}).to[mutable.Buffer]
      val cmdResultVar = CVar("cmdResult")
      val cmdReturnType = command.returnType.map(_.obj)
      val cmdCReturnType = command.returnType.map(_.obj.cType)
      val funcCall = CArrow(selfVar, CFuncCall(command.methodNamePart(rootComponent, component),
        selfVar +: vars.map(CRef(_)): _*))
      CFuncImpl(CFuncDef(methodNameFor(componentTypeName, methodName), resultType, parameters),
        varInits ++ cmdCReturnType.map(t => CAstElements(CStatementLine(CVarDef(cmdResultVar.name, t, Some(funcCall))),
          CStatementLine(CReturn(CFuncCall(methodNameFor(cmdReturnType.get, typeSerializeMethodName),
            CRef(cmdResultVar), writer.v))))).getOrElse(CAstElements(CStatementLine(funcCall))))
    })
  }

  private def nsOrAliasCppSourceParts(ns: DecodeNamespace): Seq[String] =
    config.namespaceAliases.getOrElse(ns.fqn, Some(ns.fqn)).map(_.parts.map(_.asMangledString)).getOrElse(Seq.empty)

  private def dirPathForNs(ns: DecodeNamespace): String = nsOrAliasCppSourceParts(ns).mkString(io.File.separator)

  private def includePathForNsFileName(ns: DecodeNamespace, fileName: String): String =
    dirPathForNs(ns) + io.File.separator + fileName

  private def dirForNs(ns: DecodeNamespace): io.File = new io.File(config.outputDir, dirPathForNs(ns))

  private def relPathForType(t: DecodeType): String =
    dirPathForNs(t.namespace) + io.File.separator + t.fileName + headerExt

  private def includePathForComponent(comp: DecodeComponent): String =
    includePathForNsFileName(comp.namespace, comp.prefixedTypeName + headerExt)

  private def appendPrologEpilog(file: CAstElements): CAstElements = {
    val prefix = config.prologEpilogPath.map(_ + io.File.separator).getOrElse("")
    CAstElements(CInclude(prefix + "photon_prologue.h"), CEol, CEol) ++ file ++
      Seq(CEol, CInclude(prefix + "photon_epilogue.h"), CEol, CEol)
  }

  private def generateTypeSeparateFiles(t: DecodeType, nsDir: io.File) {
    if (t.isPrimitiveOrNative)
      return
    val fileName = t.fileName
    val hFileName = fileName + headerExt
    val cFileName = fileName + sourcesExt
    val (hFile, cFile) = (new io.File(nsDir, hFileName), new io.File(nsDir, cFileName))
    val (h, c) = generateType(t, nsDir)
    if (h.nonEmpty)
      writeFileIfNotEmptyWithComment(hFile,
        (CAstElements(CEol) ++ appendPrologEpilog(h) ++ Seq(CEol)).protectDoubleInclude(hFileName), "Type header")
    else
      logger.debug(s"Omitting type ${t.optionName.toString}")
    if (c.nonEmpty)
      writeFileIfNotEmptyWithComment(cFile, CAstElements(CInclude(relPathForType(t)), CEol, CEol) ++ c,
        "Type implementation")
  }
}

private object CSourcesGenerator {

  case class ComponentParameterField(component: DecodeComponent, field: DecodeStructField)

  implicit class RichComponent(val component: DecodeComponent) {
    def allCommands: Seq[ComponentCommand] =
      allSubComponents.toSeq.flatMap(sc => sc.commands.map(ComponentCommand(sc, _)))
    def allParameters: Seq[ComponentParameterField] =
      allSubComponents.toSeq.flatMap(sc => sc.baseType.map(_.obj.fields.map(ComponentParameterField(sc, _)))
        .getOrElse(Seq.empty))

    def typeName: String = component.name.asMangledString

    def prefixedTypeName: String = typePrefix + typeName

    def componentDataTypeName: String = prefixedTypeName + "Data"

    def ptrType: CPtrType = CTypeApplication(prefixedTypeName).ptr

    def functionTableTypeName: String = prefixedTypeName + "UserFunctionTable"

    private def _allSubComponents(set: mutable.Set[DecodeComponent]): mutable.Set[DecodeComponent] = {
      component.subComponents.foreach { ref =>
        val c: DecodeComponent = ref.component.obj
        set += c
        c._allSubComponents(set)
      }
      set
    }

    def allSubComponents: mutable.Set[DecodeComponent] = {
      _allSubComponents(mutable.HashSet.empty)
    }

    def collectNamespaces(nsSet: mutable.HashSet[DecodeNamespace]) {
      component.subComponents.foreach(_.component.obj.collectNamespaces(nsSet))
      collectNsForTypes(nsSet)
    }

    def collectNsForTypes(set: mutable.Set[DecodeNamespace]) {
      if (component.baseType.isDefined)
        collectNsForType(component.baseType.get, set)
      component.commands.foreach{ cmd =>
        cmd.parameters.foreach(arg => collectNsForType(arg.paramType, set))
        if (cmd.returnType.isDefined)
          collectNsForType(cmd.returnType.get, set)
      }
    }
  }

  implicit class RichNamed(val named: DecodeNamed) {
    def methodNamePart(rootComponent: DecodeComponent, component: DecodeComponent): String =
      upperCamelCaseToLowerCamelCase((if (rootComponent == component) "" else component.typeName) +
        cName.capitalize)

    def methodName(rootComponent: DecodeComponent, component: DecodeComponent): String =
      methodNameFor(rootComponent.prefixedTypeName, methodNamePart(rootComponent, component))

    def cName: String = named.name.asMangledString

    def mangledCName: String = {
      var methodName = cName
      if (keywords.contains(methodName))
        methodName = "_" + methodName
      methodName
    }
  }

  implicit class RichType(val t: DecodeType) {
    def cType: CType = CTypeApplication(t.prefixedCTypeName)
    def isPrimitiveOrNative = t.isInstanceOf[DecodePrimitiveType] || t.isInstanceOf[DecodeNativeType]
    def fileName: String = t.prefixedCTypeName
    def prefixedCTypeName: String = t match {
        case _: DecodePrimitiveType => cTypeName
        case _: DecodeNativeType => "Photon" + cTypeName
        case _ => "PhotonGt" + cTypeName
      }
    def cTypeName: String = t match {
        case t: DecodeNamed => lowerUnderscoreToUpperCamel(t.name.asMangledString)
        case t: DecodePrimitiveType => primitiveTypeToCTypeApplication(t).name
        case t: DecodeArrayType =>
          val baseCType: String = t.baseType.obj.cTypeName
          val min = t.size.minLength
          val max = t.size.maxLength
          "Arr" + baseCType + ((t.isFixedSize, min, max) match {
            case (true, 0, _) | (false, 0, 0) => ""
            case (true, _, _) => s"Fixed$min"
            case (false, 0, _) => s"Max$max"
            case (false, _, 0) => s"Min$min"
            case (false, _, _) => s"Min${min}Max$max"
          })
        case t: DecodeGenericTypeSpecialized =>
          t.genericType.obj.cTypeName +
            t.genericTypeArguments.map(tp => if (tp.isDefined) tp.get.obj.cTypeName else "Void").mkString
        case t: DecodeOptionNamed => lowerUnderscoreToUpperCamel(cTypeNameFromOptionName(t.optionName))
        case _ => sys.error("not implemented")
      }
    def serializeCallCode(src: CExpression): CAstElements = t match {
      case _: DecodeArrayType | _: DecodeStructType => CStatements(tryCall(
        methodNameFor(t, typeSerializeMethodName), src, writer.v))
      case _ => serializeCode(src)
    }

    def deserializeCallCode(dest: CExpression): CAstElements = t match {
      case _: DecodeArrayType | _: DecodeStructType => CStatements(tryCall(
        methodNameFor(t, typeDeserializeMethodName), dest, reader.v))
      case _ => deserializeCode(dest)
    }

    def serializeCode(src: CExpression = selfVar): CAstElements = t match {
      case t: DecodeStructType =>
        CAstElements(t.fields.flatMap(f =>
          f.typeUnit.t.obj.serializeCallCode(CRef(CArrow(src, CVar(f.cName))))): _*)
      case t: DecodeArrayType => serializeCodeForArraySize(src) ++ serializeCodeForArrayElements(t, src)
      case t: DecodeAliasType => t.baseType.obj.serializeCode(src)
      case t: DecodeSubType => t.baseType.obj.serializeCallCode(src)
      case t: DecodeEnumType => t.baseType.obj.serializeCallCode(src)
      /* CStatements(CFuncCall(tryMacroName,
         CFuncCall(methodNameFor(prefixedCTypeNameFor(t), typeSerializeMethodName), src, writer.v)))*/
      case t: DecodePrimitiveType => serializeCodeFor(t, src)
      case t: DecodeNativeType => serializeCodeFor(t, src)
      case t: DecodeGenericTypeSpecialized => serializeCodeFor(t, src)
      case _ => sys.error(s"not implemented for $t")
    }

    def deserializeCode(dest: CExpression = selfVar): CAstElements = t match {
      case t: DecodeStructType =>
        CAstElements(t.fields.flatMap(f =>
          f.typeUnit.t.obj.deserializeCallCode(CRef(CArrow(dest, CVar(f.cName))))): _*)
      case t: DecodeArrayType => deserializeCodeForArraySize(dest) ++ deserializeCodeForArrayElements(t, dest)
      case t: DecodeAliasType => t.baseType.obj.deserializeCode(dest)
      case t: DecodeSubType => t.baseType.obj.deserializeCallCode(dest)
      case t: DecodeEnumType => t.baseType.obj.deserializeCallCode(dest)
      /*CStatements(CFuncCall(tryMacroName,
        CFuncCall(methodNameFor(prefixedCTypeNameFor(t), typeDeserializeMethodName), dest, reader.v)))*/
      case t: DecodePrimitiveType => deserializeCodeFor(t, dest)
      case t: DecodeNativeType => deserializeCodeFor(t, dest)
      case t: DecodeGenericTypeSpecialized => deserializeCodeFor(t, dest)
      case _ => sys.error(s"not implemented for $t")
    }

    def cTypeDef(cType: CType) = CTypeDefStatement(t.prefixedCTypeName, cType)

    def collectNamespaces(set: mutable.Set[DecodeNamespace]) {
      set += t.namespace
      t match {
        case t: BaseTyped => collectNsForType(t.baseType, set)
        case t: DecodeStructType => t.fields.foreach(f => collectNsForType(f.typeUnit.t, set))
        case t: DecodeGenericTypeSpecialized => t.genericTypeArguments
          .filter(_.isDefined).foreach(a => collectNsForType(a.get, set))
        case _ =>
      }
    }

    def importTypes: Seq[DecodeType] = t match {
      case t: DecodeStructType => t.fields.flatMap { f =>
        val t = f.typeUnit.t.obj
        if (t.isPrimitiveOrNative)
          Seq.empty
        else
          Seq(t)
      }
      case s: DecodeGenericTypeSpecialized =>
        s.genericType.obj match {
          case optional: DecodeOptionalType =>
            Seq(s.genericTypeArguments.head.get.obj)
          case or: DecodeOrType =>
            s.genericTypeArguments.flatMap(_.map(p => Seq(p.obj)).getOrElse(Seq.empty))
        }
      case t: BaseTyped =>
        if (t.baseType.obj.isPrimitiveOrNative)
          Seq.empty
        else
          Seq(t.baseType.obj)
      case _ => Seq.empty
    }
  }

  implicit class RichCAstElements(val els: CAstElements) {
    private val rand = new Random()

    def protectDoubleInclude(fileName: String): CAstElements = {
      val bytes = new Array[Byte](20)
      rand.nextBytes(bytes)
      val uniqueName = "__" + upperCamelCaseToUpperUnderscore(fileName).replaceAll("\\.", "_") + "_" +
        MessageDigest.getInstance("MD5").digest(bytes).map("%02x".format(_)).mkString + "__"
      CAstElements(CIfNDef(uniqueName), CEol, CDefine(uniqueName)) ++ els :+ CEndIf
    }
  }

  private case class TypedVar(name: String, t: CType) {
    val v = CVar(name)
    val param = CFuncParam(name, t)
    val field = CStructTypeDefField(name, t)
  }

  private val headerExt = ".h"
  private val sourcesExt = ".c"
  private val structNamePostfix = "_s"
  private val cppDefine = "__cplusplus"
  private val typePrefix = "PhotonGc"

  private val tryMacroName = "PHOTON_TRY"
  private val typeInitMethodName = "Init"
  private val typeSerializeMethodName = "Serialize"
  private val typeDeserializeMethodName = "Deserialize"
  private val singletonPostfix = "Singleton"
  private val photonBerTypeName = "PhotonBer"
  private val photonWriterTypeName = "PhotonWriter"
  private val photonReaderTypeName = "PhotonReader"

  private val b8Type = CTypeApplication("PhotonGtB8")
  private val berType = CTypeApplication("PhotonBer")
  private val voidType = CTypeApplication("void")
  private val sizeTType = CTypeApplication("size_t")
  private val stringType = CTypeApplication("char").ptr
  private val cDecodeArrayType = CTypeApplication("PhotonArr")

  private val cDecodeArrayTypeInitMethodNamePart = "_Init"

  private val cDecodeArrayTypeInitMethodName = cDecodeArrayType.name + cDecodeArrayTypeInitMethodNamePart

  private val resultType = CTypeApplication("PhotonResult")
  private val resultOk = CVar(resultType.name + "_Ok")

  private val selfVar = CVar("self")
  private val dataVar = CVar("data")
  private val itemVar = CVar("item")

  private val tag = TypedVar("tag", berType)
  private val size = TypedVar("size", berType)
  private val i = TypedVar("i", sizeTType)
  private val reader = TypedVar("reader", CTypeApplication("PhotonReader").ptr)
  private val writer = TypedVar("writer", CTypeApplication("PhotonWriter").ptr)
  private val commandId = TypedVar("commandId", sizeTType)

  private val executeCommandMethodName = "executeCommand"
  private val executeCommandParameters = Seq(CFuncParam(commandId.name, commandId.t), reader.param, writer.param)

  private val sizeOfStatement = "sizeof"
  private val userFunctionTableStructFieldName = "userFunctionTable"

  private def upperCamelCaseToLowerCamelCase(str: String) = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, str)

  private def collectComponentsForComponent(comp: DecodeComponent, compSet: mutable.HashSet[DecodeComponent]): Unit = {
    compSet += comp
    comp.subComponents.foreach(cr => collectComponentsForComponent(cr.component.obj, compSet))
  }

  private def maxLengthFor(t: DecodeArrayType): Long = {
    val size = t.size
    if (t.isFixedSize)
      size.minLength.min(256)
    if (size.maxLength == 0)
      256
    else
      size.maxLength.min(256)
  }

  private def collectNsForType[T <: DecodeType](t: DecodeMaybeProxy[T], set: mutable.Set[DecodeNamespace]) {
    require(t.isResolved, s"Proxy not resolved error for ${t.proxy.toString}")
    t.obj.collectNamespaces(set)
  }

  private def methodNameFor(t: String, name: String): String = t + "_" + name.capitalize

  private def methodNameFor(t: DecodeType, name: String): String = methodNameFor(t.prefixedCTypeName, name)

  private def lowerUnderscoreToUpperCamel(str: String) = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, str)

  var fileNameId: Int = 0
  var typeNameId: Int = 0

  private def fileNameFromOptionName(name: Option[DecodeName]): String = {
    if (name.isDefined) {
      name.get.asMangledString
    } else {
      fileNameId += 1
      "type" + fileNameId
    }
  }

  private def cTypeNameFromOptionName(name: Option[DecodeName]): String = {
    if (name.isDefined) {
      name.get.asMangledString
    } else {
      typeNameId += 1
      "type" + typeNameId
    }
  }

  private def writeFile(file: io.File, cppFile: CAstElements) {
    for (typeHeaderStream <- managed(new OutputStreamWriter(new FileOutputStream(file)))) {
      cppFile.generate(CGenState(typeHeaderStream))
    }
  }

  private def writeFileIfNotEmptyWithComment(file: io.File, cppFile: CAstElements, comment: String) {
    if (cppFile.nonEmpty)
      writeFile(file, CAstElements(CComment(comment), CEol) ++ cppFile)
  }

  private def primitiveTypeToCTypeApplication(primitiveType: DecodePrimitiveType): CTypeApplication = {
    import TypeKind._
    (primitiveType.kind, primitiveType.bitLength) match {
      case (Bool, 8) => CUnsignedCharType
      case (Bool, 16) => CUnsignedShortType
      case (Bool, 32) => CUnsignedIntType
      case (Bool, 64) => CUnsignedLongType
      case (Float, 32) => CFloatType
      case (Float, 64) => CDoubleType
      case (Int, 8) => CSignedCharType
      case (Int, 16) => CSignedShortType
      case (Int, 32) => CSignedIntType
      case (Int, 64) => CSignedLongType
      case (Uint, 8) => CUnsignedCharType
      case (Uint, 16) => CUnsignedShortType
      case (Uint, 32) => CUnsignedIntType
      case (Uint, 64) => CUnsignedLongType
      case _ => sys.error("illegal bit length")
    }
  }

  private def upperCamelCaseToUpperUnderscore(s: String) = CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, s)

  private def isAliasNameTheSame(t: DecodeAliasType): Boolean = t.cTypeName == t.baseType.obj.cTypeName

  private def defineAndInitVar(v: CVar, parameter: DecodeCommandParameter): CAstElements = {
    CAstElements(CStatementLine(CDefVar(v.name, parameter.paramType.obj.cType)),
      CStatementLine(CFuncCall(tryMacroName, CFuncCall(methodNameFor(parameter.paramType.obj,
        typeDeserializeMethodName), CRef(v), reader.v))))
  }

  private case class ComponentCommand(component: DecodeComponent, command: DecodeCommand)

  // todo: memoize
  private def commandsByIdFor(comp: DecodeComponent): mutable.Map[Int, ComponentCommand] = {
    var commandNextId = 0
    val commandsById = mutable.HashMap.empty[Int, ComponentCommand]
    comp.commands.foreach(cmd =>
      assert(commandsById.put(cmd.id.getOrElse {
        commandNextId += 1; commandNextId - 1
      }, ComponentCommand(comp, cmd)).isEmpty))
    comp.allSubComponents.toSeq.sortBy(_.fqn.asMangledString).filterNot(_ == comp).foreach(comp =>
      comp.commands.foreach { cmd =>
        assert(commandsById.put(commandNextId, ComponentCommand(comp, cmd)).isEmpty)
        commandNextId += 1
      })
    commandsById
  }

  private val keywords = Seq("return")

  private def casesForCommands(comp: DecodeComponent): Seq[CCase] =
    commandsByIdFor(comp).toSeq.sortBy(_._1).map(_ match { case (id, ComponentCommand(component, command)) =>
      CCase(CIntLiteral(id), CAstElements(CStatementLine(CReturn(CFuncCall(
        command.methodName(comp, component), selfVar, reader.v, writer.v)))))
    })

  private def externCpp(file: CAstElements): CAstElements =
    CAstElements(CIfDef(cppDefine), CEol, CPlainText("extern \"C\" {"), CEol, CEndIf, CEol, CEol) ++ file ++
      Seq(CEol, CEol, CIfDef(cppDefine), CEol, CPlainText("}"), CEol, CEndIf)

  private def typesForComponent(comp: DecodeComponent,
                                typesSet: mutable.Set[DecodeType] = mutable.HashSet.empty): mutable.Set[DecodeType] = {
    typesSet ++= comp.commands.flatMap(cmd =>
      cmd.returnType.map(rt => Seq(rt.obj)).getOrElse(Seq.empty) ++ cmd.parameters.map(_.paramType.obj))
    typesSet ++= comp.baseType.map(_.obj.fields.map(_.typeUnit.t.obj)).getOrElse(Seq.empty)
    typesSet
  }

  private var nextComponentId = 0
  private val componentByComponentId = mutable.HashMap.empty[Int, DecodeComponent]
  private val componentIdByComponent = mutable.HashMap.empty[DecodeComponent, Int]

  private def enumerateComponentsFrom(component: DecodeComponent): Unit = {
    if (component.id.isDefined) {
      assert(componentIdByComponent.put(component, component.id.get).isEmpty)
      assert(componentByComponentId.put(component.id.get, component).isEmpty)
    }
    component.subComponents.foreach {cr => enumerateComponentsFrom(cr.component.obj)}
    if (component.id.isEmpty) {
      while (componentByComponentId.contains(nextComponentId))
        nextComponentId += 1
      componentByComponentId.put(nextComponentId, component)
      componentIdByComponent.put(component, nextComponentId)
      nextComponentId += 1
    }
  }

  private def serializeCodeForArraySize(src: CExpression): CAstElements =
    CStatements(CFuncCall(tryMacroName, CFuncCall(methodNameFor(photonBerTypeName, typeSerializeMethodName),
      CArrow(src, size.v), writer.v)))

  private def deserializeCodeForArraySize(dest: CExpression): CAstElements =
    CStatements(CFuncCall(tryMacroName, CFuncCall(methodNameFor(photonBerTypeName, typeDeserializeMethodName),
      CRef(CArrow(dest, size.v)), reader.v)))

  private def serializeCodeForArrayElements(t: DecodeArrayType, src: CExpression): CAstElements =
    CAstElements(CIdent, CForStatement(CAstElements(CDefVar(i.name, i.t, Some(CIntLiteral(0))), CComma,
      CAssign(size.v, CArrow(src, size.v))), CAstElements(CLess(i.v, size.v)),
      CAstElements(CIncBefore(i.v)), t.baseType.obj.serializeCallCode(CRef(CIndex(CArrow(src, dataVar), i.v)))), CEol)

  private def deserializeCodeForArrayElements(t: DecodeArrayType, src: CExpression): CAstElements =
    CAstElements(CIdent, CForStatement(CAstElements(CDefVar(i.name, i.t, Some(CIntLiteral(0))), CComma,
      CAssign(size.v, CArrow(src, size.v))), CAstElements(CLess(i.v, size.v)),
      CAstElements(CIncBefore(i.v)), t.baseType.obj.deserializeCallCode(CRef(CIndex(CArrow(src, dataVar), i.v)))), CEol)

  private def serializeCodeFor(t: DecodePrimitiveType, src: CExpression): CAstElements = {
    import TypeKind._
    CStatements(CFuncCall(methodNameFor(photonWriterTypeName, (t.kind, t.bitLength) match {
      case (_, 8) => "WriteUint8"
      case (Bool, 16) | (Uint, 16) => "WriteUint16Le"
      case (Bool, 32) | (Uint, 32) => "WriteUint32Le"
      case (Bool, 64) | (Uint, 64) => "WriteUint64Le"
      case (Int, 16) => "WriteInt16Le"
      case (Int, 32) => "WriteInt32Le"
      case (Int, 64) => "WriteInt64Le"
      case (Float, 32) => "WriteFloat32Le"
      case (Float, 64) => "WriteFloat64Le"
      case _ => sys.error(s"not implemented $t")
    }), writer.v, src))
  }

  private def wrapTry(exprs: CExpression*): CAstElement = CFuncCall(tryMacroName, exprs: _*)

  private def tryCall(methodName: String, exprs: CExpression*): CAstElement = wrapTry(CFuncCall(methodName, exprs: _*))

  private def serializeCodeFor(t: DecodeNativeType, src: CExpression): CAstElements = t match {
    case t: DecodeBerType => CStatements(tryCall(
      methodNameFor(photonBerTypeName, typeSerializeMethodName), src, writer.v))
    case _ => sys.error(s"not implemented $t")
  }

  private def deserializeCodeFor(t: DecodeNativeType, dest: CExpression): CAstElements = t match {
    case t: DecodeBerType => CStatements(tryCall(
      methodNameFor(photonBerTypeName, typeDeserializeMethodName), dest, reader.v))
    case _ => sys.error(s"not implemented $t")
  }

  private def serializeCodeFor(t: DecodeGenericTypeSpecialized, src: CExpression): CAstElements =
    t.genericType.obj match {
      case gt: DecodeOrType => CAstElements(CComment("todo"))
      case gt: DecodeOptionalType => CAstElements(CComment("todo"))
      case _ => sys.error(s"not implemented $t")
    }

  private def deserializeCodeFor(t: DecodePrimitiveType, dest: CExpression): CAstElements = {
    import TypeKind._
    CStatements(CAssign(CDeref(dest), CFuncCall(methodNameFor(photonReaderTypeName, (t.kind, t.bitLength) match {
      case (_, 8) => "ReadUint8"
      case (Bool, 16) | (Uint, 16) => "ReadUint16Le"
      case (Bool, 32) | (Uint, 32) => "ReadUint32Le"
      case (Bool, 64) | (Uint, 64) => "ReadUint64Le"
      case (Int, 16) => "ReadInt16Le"
      case (Int, 32) => "ReadInt32Le"
      case (Int, 64) => "ReadInt64Le"
      case (Float, 32) => "ReadFloat32Le"
      case (Float, 64) => "ReadFloat64Le"
      case _ => sys.error(s"not implemented $t")
    }), reader.v)))
  }

  private def deserializeCodeFor(t: DecodeGenericTypeSpecialized, dest: CExpression): CAstElements =
    t.genericType.obj match {
      case gt: DecodeOrType => CAstElements(CComment("todo"))
      case gt: DecodeOptionalType => CAstElements(CComment("todo"))
      case _ => sys.error(s"not implemented $t")
    }
}
