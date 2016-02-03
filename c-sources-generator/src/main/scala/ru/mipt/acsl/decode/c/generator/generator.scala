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
    new io.File(nsDir, fileName).writeIfNotEmptyWithComment(typesHeader.protectDoubleInclude(fileName),
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
        val arrayType: CArrayType = CArrayType(t.baseType.obj.cType, t.maxLength, dataVar.name)
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
      val imports = CAstElements(importTypes.flatMap(t => CInclude(relPathForType(t)).eol): _*)

      val selfType = t.cType.ptr
      val serializeMethod = CFuncImpl(CFuncDef(t.serializeMethodName, resultType,
        Seq(CFuncParam(selfVar.name, selfType), writer.param)), t.serializeCode :+ CReturn(resultOk).line)
      val deserializeMethod = CFuncImpl(CFuncDef(t.deserializeMethodName, resultType,
        Seq(CFuncParam(selfVar.name, selfType), reader.param)), t.deserializeCode :+ CReturn(resultOk).line)

      h ++= Seq(CEol, serializeMethod.definition, CEol, CEol, deserializeMethod.definition)
      c ++= Seq(CEol, serializeMethod, CEol, CEol, deserializeMethod)
      if (imports.nonEmpty)
        imports += CEol
      (imports ++ h.externC.eol, c)
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
    val allCommands = component.allCommands
    val allParameters = component.allParameters
    val cComponentSingletonPtrType: CPtrType = cComponentSingletonType.ptr
    val initFunc = CFuncImpl(CFuncDef(cSingletonComponentName.initMethodName,
      cComponentSingletonPtrType), CAstElements(allCommands.map(_ match { case ComponentCommand(c, command) =>
      componentSingletonVar.v.dot(command.methodNamePart(component, c)._var).assign(
        cSingletonComponentName.methodName(command, component, c)._var.ref).line
    }) ++ allParameters.map(_ match { case ComponentParameterField(c, f) =>
      componentSingletonVar.v.dot(f.cStructFieldName(component, c)._var).assign(
        cSingletonComponentName.methodName(f, component, c)._var.ref).line
    }): _*) ++ CStatements(CReturn(componentSingletonVar.v.ref)))
    val funcDefs = allCommands.map(_ match { case ComponentCommand(c, command) =>
      CFuncDef(cSingletonComponentName.methodName(command, component, c),
        command.returnType.map(_.obj.cType).getOrElse(voidType),
        Seq(CFuncParam(selfVar.name, cComponentSingletonPtrType)) ++
          command.parameters.map(p => {
            val t = p.paramType.obj
            CFuncParam(p.cName, t.cType.ptrIfNotSmall(t))
          }))
    }) ++ allParameters.map(_ match { case ComponentParameterField(c, f) =>
      CFuncDef(cSingletonComponentName.methodName(f, component, c), f.typeUnit.t.obj.cType,
        Seq(CFuncParam(selfVar.name, cComponentSingletonPtrType)))
    })
    hFile.write((CAstElements(CEol, CInclude(includePathForComponent(component)), CEol, CEol) ++
      (initFunc.definition.eol ++ funcDefs.flatMap(f => CAstElements(CEol, f))).externC.eol).protectDoubleInclude(hFile.getName))
    cFile.write(CAstElements(CInclude(includePathForNsFileName(component.namespace, hFile.getName)), CEol, CEol,
      componentSingletonVar.v.define(componentSingletonVar.t, static = true).line, initFunc))
  }

  private def importStatementsForComponent(comp: DecodeComponent): CAstElements = {
    val imports = comp.subComponents.flatMap(cr => CInclude(includePathForComponent(cr.component.obj)).eol)
    if (imports.nonEmpty)
      imports += CEol
    val types = typesForComponent(comp).toSeq
    val typeIncludes = types.filterNot(_.isPrimitiveOrNative).flatMap(t => CInclude(relPathForType(t)).eol)
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
    /* TODO: Need this? Pointer to function by commandId
    val functionForCommand = CFuncImpl(CFuncDef(component.functionForCommandMethodName,
      CFuncType(resultType, Seq(componentSelfType, reader.t, writer.t))),
      CAstElements(CIdent, CSwitch(commandId.v, casesForCommandFunctions(component),
        CStatements(CReturn(CIntLiteral(0)))), CEol))*/
    val execCommand = CFuncImpl(CFuncDef(component.executeCommandMethodName, resultType,
      Seq(CFuncParam(selfVar.name, componentSelfType), reader.param, writer.param, commandId.param)),
      CAstElements(CIdent, CSwitch(commandId.v, casesForCommands(component),
        default = CStatements(CReturn(invalidCommandId))), CEol))
    val readExecCommand = CFuncImpl(CFuncDef(component.readExecuteCommandMethodName, resultType,
      Seq(CFuncParam(selfVar.name, componentSelfType), reader.param, writer.param)), CStatements(
      commandId.v.define(commandId.t),
      tryCall(photonBerTypeName.methodName(typeDeserializeMethodName), commandId.v.ref, reader.v),
      CReturn(CFuncCall(execCommand.definition.name, selfVar, reader.v, writer.v, commandId.v))))
    val writeMessage = CFuncImpl(CFuncDef(component.writeMessageMethodName, resultType,
      Seq(CFuncParam(selfVar.name, componentSelfType), writer.param, messageId.param)),
      CAstElements(CIdent, CSwitch(messageId.v, casesForMessages(component),
        default = CStatements(CReturn(invalidMessageId))), CEol))
    val functions = component.allCommandsFunctions ++ component.allMessagesFunctions
    val externedCFile = (CAstElements(forwardFuncTableDecl, CEol, CEol, componentTypeForwardDecl, CEol, CEol, componentType, CSemicolon, CEol) ++
      functions.flatMap(f => Seq(f.definition, CEol)) ++
      Seq(CEol, readExecCommand.definition, CEol, writeMessage.definition, CEol)).externC
    hFile.writeIfNotEmptyWithComment((CEol +: appendPrologEpilog(imports ++ externedCFile))
      .protectDoubleInclude(hFileName), s"Component ${component.name.asMangledString} interface")
    cFile.writeIfNotEmptyWithComment(CAstElements(CInclude(includePathForComponent(component)), CEol, CEol) ++
      functions.flatMap(f => Seq(f, CEol, CEol)) ++
      Seq(execCommand, CEol, CEol, readExecCommand, CEol, CEol, writeMessage),
      s"Component ${component.name.asMangledString} implementation")
  }

  private def structTypeFieldForCommand(structComponent: DecodeComponent, component: DecodeComponent, command: DecodeCommand): CStructTypeDefField = {
    val methodName = command.cStructFieldName(structComponent, component)
    val returnType = command.returnType.map(_.obj.cType).getOrElse(voidType)
    val parameters = command.cFuncParameterTypes(structComponent)
    CStructTypeDefField(methodName, CFuncType(returnType, parameters, methodName))
  }

  private def componentStructType(component: DecodeComponent): CStructTypeDef = {
    val componentSelfPtrType = component.ptrType
    CStructTypeDef(Seq(CStructTypeDefField("data", CTypeApplication(component.componentDataTypeName).ptr)) ++
      component.allCommands.map(_ match { case ComponentCommand(c, command) =>
        structTypeFieldForCommand(component, c, command)
      }) ++
      component.allParameters.map(_ match { case ComponentParameterField(c, f) =>
        val name = f.cStructFieldName(component, c)
        CStructTypeDefField(
          name, CFuncType(f.typeUnit.t.obj.cType, Seq(componentSelfPtrType), name))
      }) ++
      component.baseType.map(_.obj.fields.map { f =>
        val name = f.mangledCName
        CStructTypeDefField(name, CFuncType(f.typeUnit.t.obj.cType, Seq(componentSelfPtrType), name))
      }).getOrElse(Seq.empty) ++ component.commands.map(structTypeFieldForCommand(component, component, _)),
      Some(component.prefixedTypeName + structNamePostfix))
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
      hFile.writeIfNotEmptyWithComment((CAstElements(CEol) ++ appendPrologEpilog(h).eol).protectDoubleInclude(hFileName), "Type header")
    else
      logger.debug(s"Omitting type ${t.optionName.toString}")
    if (c.nonEmpty)
      cFile.writeIfNotEmptyWithComment(CAstElements(CInclude(relPathForType(t)), CEol, CEol) ++ c,
        "Type implementation")
  }
}

private object CSourcesGenerator {

  class WithComponent[T](val component: DecodeComponent, val _2: T) {
    def unapply(): Option[(DecodeComponent, T)] = Some(component, _2)
  }

  object WithComponent {
    def apply[T](component: DecodeComponent, _2: T) = new WithComponent[T](component, _2)
  }

  object ComponentCommand {
    def apply(component: DecodeComponent, command: DecodeCommand) = WithComponent[DecodeCommand](component, command)

    def unapply(o: WithComponent[DecodeCommand]) = o.unapply()
  }

  object ComponentMessage {
    def apply(component: DecodeComponent, message: DecodeMessage) = WithComponent[DecodeMessage](component, message)
    def unapply(o: WithComponent[DecodeMessage]) = o.unapply()
  }

  case class ComponentParameterField(component: DecodeComponent, field: DecodeStructField)

  implicit class RichString(val str: String) {
    def _var: CVar = CVar(str)

    def methodName(name: String): String = str + "_" + name.capitalize

    def initMethodName: String = methodName(typeInitMethodName)

    def methodName(command: DecodeCommand, rootComponent: DecodeComponent, component: DecodeComponent): String =
      methodName(command.methodNamePart(rootComponent, component))

    def methodName(f: DecodeStructField, rootComponent: DecodeComponent, component: DecodeComponent): String =
      methodName(f.methodNamePart(rootComponent, component))
  }

  implicit class RichComponent(val component: DecodeComponent) {
    def allCommands: Seq[WithComponent[DecodeCommand]] =
      allSubComponents.toSeq.flatMap(sc => sc.commands.map(ComponentCommand(sc, _)))

    def allParameters: Seq[ComponentParameterField] =
      allSubComponents.toSeq.flatMap(sc => sc.baseType.map(_.obj.fields.map(ComponentParameterField(sc, _)))
        .getOrElse(Seq.empty))

    def typeName: String = component.name.asMangledString

    def functionForCommandMethodName: String = component.prefixedTypeName.methodName("FunctionForCommand")

    def executeCommandMethodName: String = component.prefixedTypeName.methodName("ExecuteCommand")

    def readExecuteCommandMethodName: String = component.prefixedTypeName.methodName("ReadExecuteCommand")

    def writeMessageMethodName: String = component.prefixedTypeName.methodName("WriteMessage")

    def prefixedTypeName: String = typePrefix + typeName

    def componentDataTypeName: String = prefixedTypeName + "Data"

    def ptrType: CPtrType = CTypeApplication(prefixedTypeName).ptr

    def functionTableTypeName: String = prefixedTypeName + "UserFunctionTable"

    private def _allSubComponents(set: mutable.Set[DecodeComponent]): mutable.Set[DecodeComponent] = {
      component.subComponents.foreach { ref =>
        val c: DecodeComponent = ref.component.obj
        set += c
        RichComponent(c)._allSubComponents(set)
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
      component.commands.foreach { cmd =>
        cmd.parameters.foreach(arg => collectNsForType(arg.paramType, set))
        if (cmd.returnType.isDefined)
          collectNsForType(cmd.returnType.get, set)
      }
    }

    def allMessagesFunctions: Seq[CFuncImpl] = {
      val componentPtrType = component.ptrType
      allMessagesById.toSeq.sortBy(_._1).map(_._2).map(_ match { case ComponentMessage(c, message) =>
        CFuncImpl(CFuncDef(message.fullMethodName(component, c), resultType,
          Seq(CFuncParam(selfVar.name, componentPtrType), writer.param)),
          CAstElements(message.parameters.map(p => {
            val v = p.varName._var
            val parameterRef = p.ref(c)
            if (parameterRef.structField.isDefined) {
              val structField = parameterRef.structField.get
              val t = structField.typeUnit.t.obj
              v.define(t.cType, Some(selfVar ->
                CFuncCall(structField.cStructFieldName(component, c), selfVar))).line
            } else {
              sys.error("not implemented")
            }
          }): _*) ++
            CAstElements(message.parameters.flatMap(p => {
              val v = p.varName._var
              val parameterRef = p.ref(c)
              if (parameterRef.structField.isDefined) {
                val t = parameterRef.t
                if (parameterRef.subTokens.isEmpty)
                  t.serializeCallCode(v.refIfNotSmall(t))
                else
                  sys.error("not implemented")
              } else {
                sys.error("not implemented")
              }
            }): _*) :+
            CReturn(resultOk).line)
      })
    }

    def allCommandsFunctions: Seq[CFuncImpl] = {
      val componentTypeName = component.prefixedTypeName
      val compType = component.ptrType
      val parameters = Seq(CFuncParam(selfVar.name, compType), reader.param, writer.param)
      component.allCommandsById.toSeq.sortBy(_._1).map(_._2).map(_ match { case ComponentCommand(subComponent, command) =>
        val methodNamePart = command.methodNamePart(component, subComponent)
        val vars = command.parameters.map { p => CVar(p.mangledCName) }
        val varInits = vars.zip(command.parameters).flatMap(_ match { case (v, parameter) =>
          defineAndInitVar(v, parameter)
        }).to[mutable.Buffer]
        val cmdResultVar = CVar("cmdResult")
        val cmdReturnType = command.returnType.map(_.obj)
        val cmdCReturnType = command.returnType.map(_.obj.cType)
        val funcCall = selfVar -> CFuncCall(command.methodNamePart(component, subComponent),
          selfVar +: vars.map(_.ref): _*)
        CFuncImpl(CFuncDef(componentTypeName.methodName(methodNamePart), resultType, parameters),
          varInits ++ cmdCReturnType.map(t => CStatements(cmdResultVar.define(t, Some(funcCall)),
            CReturn(CFuncCall(cmdReturnType.get.methodName(typeSerializeMethodName),
              cmdResultVar.ref, writer.v)))).getOrElse(CStatements(funcCall)))
      })
    }

    // todo: optimize: memoize
    private def makeMapById[T <: DecodeHasOptionId](seq: Seq[T], subSeq: DecodeComponent => Seq[T]): mutable.Map[Int, WithComponent[T]] = {
      var nextId = 0
      val mapById = mutable.HashMap.empty[Int, WithComponent[T]]
      seq.filter(_.id.isDefined).foreach(el => assert(mapById.put(el.id.get, WithComponent[T](component, el)).isEmpty))
      seq.filter(_.id.isEmpty).foreach { el =>
        // todo: optimize: too many contain checks
        while (mapById.contains(nextId))
          nextId += 1
        assert(mapById.put(el.id.getOrElse {
          nextId += 1
          nextId - 1
        }, WithComponent[T](component, el)).isEmpty)
      }
      component.allSubComponents.toSeq.sortBy(_.fqn.asMangledString).filterNot(_ == component).foreach(subComponent =>
        subSeq(subComponent).foreach { el =>
          assert(mapById.put(nextId, WithComponent[T](subComponent, el)).isEmpty)
          nextId += 1
        })
      mapById
    }

    def allCommandsById: mutable.Map[Int, WithComponent[DecodeCommand]] = makeMapById(component.commands, _.commands)

    def allMessagesById: mutable.Map[Int, WithComponent[DecodeMessage]] = makeMapById(component.messages, _.messages)
  }

  implicit class RichParameter(val parameter: DecodeMessageParameter) {
    def varName: String = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL,
      parameter.value.replaceAll("[\\.\\[\\]]", "_").replaceAll("__", "_"))
  }

  implicit class RichCommand(val command: DecodeCommand) {
    def cFuncParameterTypes(component: DecodeComponent): Seq[CType] = {
      component.ptrType +: command.parameters.map(p => {
        val t = p.paramType.obj
        t.cType.ptrIfNotSmall(t)
      })
    }
  }

  implicit class RichMessage(val message: DecodeMessage) {
    def fullMethodName(rootComponent: DecodeComponent, component: DecodeComponent): String =
      rootComponent.prefixedTypeName.methodName("Write" + message.methodNamePart(rootComponent, component).capitalize)
  }

  implicit class RichNamed(val named: DecodeNamed) {
    def methodNamePart(rootComponent: DecodeComponent, component: DecodeComponent): String =
      upperCamelCaseToLowerCamelCase((if (rootComponent == component) "" else component.typeName) +
        cName.capitalize)

    def methodName(rootComponent: DecodeComponent, component: DecodeComponent): String =
      rootComponent.prefixedTypeName.methodName(methodNamePart(rootComponent, component))

    def cName: String = named.name.asMangledString

    def mangledCName: String = {
      var methodName = cName
      if (keywords.contains(methodName))
        methodName = "_" + methodName
      methodName
    }

    def cStructFieldName(structComponent: DecodeComponent, component: DecodeComponent): String =
      upperCamelCaseToLowerCamelCase((if (structComponent == component) "" else component.cName) +
        named.mangledCName.capitalize)
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
      case t: DecodeOptionNamed => lowerUnderscoreToUpperCamel(t.asInstanceOf[DecodeOptionNamed].cTypeName)
      case _ => sys.error("not implemented")
    }

    def isSmall: Boolean = t match {
      case _: DecodePrimitiveType | _: DecodeNativeType | _: DecodeEnumType => true
      case t: DecodeAliasType => t.baseType.obj.isSmall
      case t: DecodeSubType => t.baseType.obj.isSmall
      case _ => false
    }

    def methodName(name: String): String = t.prefixedCTypeName.methodName(name)

    def serializeMethodName: String = methodName(typeSerializeMethodName)

    def deserializeMethodName: String = methodName(typeDeserializeMethodName)

    def serializeCallCode(src: CExpression): CAstElements = t match {
      case _: DecodeArrayType | _: DecodeStructType => CStatements(tryCall(
        methodName(typeSerializeMethodName), src, writer.v))
      case _ => serializeCode(src)
    }

    def deserializeCallCode(dest: CExpression): CAstElements = t match {
      case _: DecodeArrayType | _: DecodeStructType => CStatements(tryCall(
        methodName(typeDeserializeMethodName), dest, reader.v))
      case _ => deserializeCode(dest)
    }

    def serializeCode: CAstElements = serializeCode(selfVar)

    def serializeCode(src: CExpression): CAstElements = t match {
      case t: DecodeStructType =>
        CAstElements(t.fields.flatMap(f => {
          val fType = f.typeUnit.t.obj
          fType.serializeCallCode((src -> CVar(f.cName)).refIfNotSmall(fType))
        }): _*)
      case t: DecodeArrayType => src.serializeCodeForArraySize ++ t.serializeCodeForArrayElements(src)
      case t: DecodeAliasType => t.baseType.obj.serializeCode(src)
      case t: DecodeSubType => t.baseType.obj.serializeCallCode(src)
      case t: DecodeEnumType => t.baseType.obj.serializeCallCode(src)
      case t: DecodePrimitiveType => t.serializeCode(src)
      case t: DecodeNativeType => t.serializeCode(src)
      case t: DecodeGenericTypeSpecialized => t.serializeCode(src)
      case _ => sys.error(s"not implemented for $t")
    }

    def deserializeCode: CAstElements = deserializeCode(selfVar)

    def deserializeCode(dest: CExpression): CAstElements = t match {
      case t: DecodeStructType =>
        CAstElements(t.fields.flatMap(f => {
          f.typeUnit.t.obj.deserializeCallCode((dest -> f.cName._var).ref)
        }): _*)
      case t: DecodeArrayType => dest.deserializeCodeForArraySize ++ t.deserializeCodeForArrayElements(dest)
      case t: DecodeAliasType => t.baseType.obj.deserializeCode(dest)
      case t: DecodeSubType => t.baseType.obj.deserializeCallCode(dest)
      case t: DecodeEnumType => t.baseType.obj.deserializeCallCode(dest)
      case t: DecodePrimitiveType => t.deserializeCode(dest)
      case t: DecodeNativeType => t.deserializeCode(dest)
      case t: DecodeGenericTypeSpecialized => t.deserializeCode(dest)
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

  implicit class RichArrayType(val t: DecodeArrayType) {

    private def codeForArrayElements(expr: CExpression, codeGen: CExpression => CAstElements, ref: Boolean): CAstElements = {
      val dataExpr = expr -> dataVar(i.v)
      CAstElements(CIdent, CForStatement(CAstElements(i.v.define(i.t, Some(CIntLiteral(0))), CComma,
        size.v.assign(expr -> size.v)), CAstElements(CLess(i.v, size.v)),
        CAstElements(CIncBefore(i.v)), codeGen(if (ref) dataExpr.ref else dataExpr)), CEol)
    }

    def serializeCodeForArrayElements(src: CExpression): CAstElements = {
      val baseType = t.baseType.obj
      codeForArrayElements(src, baseType.serializeCallCode, ref = !baseType.isSmall)
    }

    def deserializeCodeForArrayElements(dest: CExpression): CAstElements = {
      val baseType = t.baseType.obj
      codeForArrayElements(dest, baseType.deserializeCallCode, ref = true)
    }

    def maxLength: Long = {
      val size = t.size
      if (t.isFixedSize)
        size.minLength.min(256)
      if (size.maxLength == 0)
        256
      else
        size.maxLength.min(256)
    }
  }

  implicit class RichNativeType(val t: DecodeNativeType) {

    private def code(methodNamePart: String, exprs: CExpression*): CAstElements = t match {
      case t: DecodeBerType => CStatements(tryCall(
        photonBerTypeName.methodName(methodNamePart), exprs: _*))
      case _ => sys.error(s"not implemented $t")
    }

    def serializeCode(src: CExpression): CAstElements = code(typeSerializeMethodName, src, writer.v)

    def deserializeCode(dest: CExpression): CAstElements = code(typeDeserializeMethodName, dest, reader.v)
  }

  implicit class RichPrimitiveType(val t: DecodePrimitiveType) {
    private def code(src: CExpression, typeName: String, methodPrefix: String, exprs: CExpression*): CAstElements = {
      import TypeKind._
      CStatements(CFuncCall(typeName.methodName(methodPrefix + ((t.kind, t.bitLength) match {
        case (_, 8) => "Uint8"
        case (Bool, 16) | (Uint, 16) => "Uint16Le"
        case (Bool, 32) | (Uint, 32) => "Uint32Le"
        case (Bool, 64) | (Uint, 64) => "Uint64Le"
        case (Int, 16) => "Int16Le"
        case (Int, 32) => "Int32Le"
        case (Int, 64) => "Int64Le"
        case (Float, 32) => "Float32Le"
        case (Float, 64) => "Float64Le"
        case _ => sys.error(s"not implemented $t")
      })), exprs: _*))
    }

    def serializeCode(src: CExpression): CAstElements = code(src, photonWriterTypeName, "Write", writer.v, src)

    def deserializeCode(dest: CExpression): CAstElements = code(dest, photonReaderTypeName, "Read", reader.v)
  }

  implicit class RichDecodeGenericTypeSpecialized(val t: DecodeGenericTypeSpecialized) {

    private def code(src: CExpression): CAstElements = t.genericType.obj match {
      case gt: DecodeOrType => CAstElements(CComment("todo"))
      case gt: DecodeOptionalType => CAstElements(CComment("todo"))
      case _ => sys.error(s"not implemented $t")
    }

    def serializeCode(src: CExpression): CAstElements = code(src)

    def deserializeCode(dest: CExpression): CAstElements = code(dest)
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

    def externC: CAstElements =
      CAstElements(CIfDef(cppDefine), CEol, CPlainText("extern \"C\" {"), CEol, CEndIf, CEol, CEol) ++ els ++
        Seq(CEol, CEol, CIfDef(cppDefine), CEol, CPlainText("}"), CEol, CEndIf)

    def eol: CAstElements = els += CEol
  }

  implicit class RichAstElement(val el: CAstElement) {
    def line: CStatementLine = CStatementLine(el)

    def eol: CAstElements = CAstElements(el, CEol)
  }

  implicit class RichExpression(val expr: CExpression) {
    def _try: CFuncCall = CFuncCall(tryMacroName, expr)

    def ->(expr2: CExpression): CArrow = CArrow(expr, expr2)

    def apply(indexExpr: CExpression): CIndex = CIndex(expr, indexExpr)

    def ref: CRef = CRef(expr)

    def refIfNotSmall(t: DecodeType): CExpression = if (t.isSmall) expr else expr.ref

    def assign(right: CExpression) = CAssign(expr, right)

    def dot(right: CExpression) = CDot(expr, right)

    private def _codeForArraySize(methodName: String, expr2: CExpression, ref: Boolean): CAstElements = {
      val sizeExpr = expr -> size.v
      CStatements(CFuncCall(photonBerTypeName.methodName(methodName),
        Seq(if (ref) sizeExpr.ref else sizeExpr, expr2): _*)._try)
    }

    def serializeCodeForArraySize: CAstElements =
      _codeForArraySize(typeSerializeMethodName, writer.v, ref = false)

    def deserializeCodeForArraySize: CAstElements =
      _codeForArraySize(typeDeserializeMethodName, reader.v, ref = true)
  }

  implicit class RichVar(val v: CVar) {
    def define(t: CType, init: Option[CExpression] = None, static: Boolean = false) = CVarDef(v.name, t, init, static)
  }

  implicit class RichCType(val ct: CType) {
    def ptrIfNotSmall(t: DecodeType) = if (t.isSmall) ct else ct.ptr
  }

  private var fileNameId: Int = 0
  private var typeNameId: Int = 0

  implicit class RichOptionNamed(val optionNamed: DecodeOptionNamed) {
    def fileName: String = {
      if (optionNamed.optionName.isDefined) {
        optionNamed.optionName.get.asMangledString
      } else {
        fileNameId += 1
        "type" + fileNameId
      }
    }

    def cTypeName: String = {
      if (optionNamed.optionName.isDefined) {
        optionNamed.optionName.get.asMangledString
      } else {
        typeNameId += 1
        "type" + typeNameId
      }
    }
  }

  implicit class RichFile(val file: io.File) {
    def write(contents: CAstElements) {
      for (typeHeaderStream <- managed(new OutputStreamWriter(new FileOutputStream(file)))) {
        contents.generate(CGenState(typeHeaderStream))
      }
    }

    def writeIfNotEmptyWithComment(contents: CAstElements, comment: String) {
      if (contents.nonEmpty)
        file.write(CComment(comment).eol ++ contents)
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
  private val invalidMessageId = CVar("PhotonResult_InvalidMessageId")
  private val invalidCommandId = CVar("PhotonResult_InvalidCommandId")

  private val tag = TypedVar("tag", berType)
  private val size = TypedVar("size", berType)
  private val i = TypedVar("i", sizeTType)
  private val reader = TypedVar("reader", CTypeApplication("PhotonReader").ptr)
  private val writer = TypedVar("writer", CTypeApplication("PhotonWriter").ptr)
  private val commandId = TypedVar("commandId", sizeTType)
  private val messageId = TypedVar("messageId", sizeTType)

  private val executeCommandMethodName = "executeCommand"
  private val executeCommandParameters = Seq(CFuncParam(commandId.name, commandId.t), reader.param, writer.param)

  private val sizeOfStatement = "sizeof"
  private val userFunctionTableStructFieldName = "userFunctionTable"

  private def upperCamelCaseToLowerCamelCase(str: String) = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, str)

  private def collectComponentsForComponent(comp: DecodeComponent, compSet: mutable.HashSet[DecodeComponent]): Unit = {
    compSet += comp
    comp.subComponents.foreach(cr => collectComponentsForComponent(cr.component.obj, compSet))
  }

  private def collectNsForType[T <: DecodeType](t: DecodeMaybeProxy[T], set: mutable.Set[DecodeNamespace]) {
    require(t.isResolved, s"Proxy not resolved error for ${t.proxy.toString}")
    t.obj.collectNamespaces(set)
  }

  private def lowerUnderscoreToUpperCamel(str: String) = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, str)

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
    CStatements(v.define(parameter.paramType.obj.cType),
      CFuncCall(parameter.paramType.obj.deserializeMethodName, v.ref, reader.v)._try)
  }

  private val keywords = Seq("return")

  private def casesForCommandFunctions(component: DecodeComponent): Seq[CCase] =
    component.allCommandsById.toSeq.sortBy(_._1).map(_ match { case (id, ComponentCommand(c, command)) =>
      CCase(CIntLiteral(id), CStatements(CReturn(command.methodName(component, c)._var.ref)))
    })

  private def casesForCommands(component: DecodeComponent): Seq[CCase] =
    component.allCommandsById.toSeq.sortBy(_._1).map(_ match { case (id, ComponentCommand(c, command)) =>
      CCase(CIntLiteral(id), CStatements(CReturn(CFuncCall(
        command.methodName(component, c), selfVar, reader.v, writer.v))))
    })

  private def casesForMessages(component: DecodeComponent): Seq[CCase] =
    component.allMessagesById.toSeq.sortBy(_._1).map(_ match { case (id, ComponentMessage(c, message)) =>
      CCase(CIntLiteral(id), CStatements(CReturn(CFuncCall(message.fullMethodName(component, c), selfVar, writer.v))))
    })

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
    component.subComponents.foreach { cr => enumerateComponentsFrom(cr.component.obj) }
    if (component.id.isEmpty) {
      while (componentByComponentId.contains(nextComponentId))
        nextComponentId += 1
      componentByComponentId.put(nextComponentId, component)
      componentIdByComponent.put(component, nextComponentId)
      nextComponentId += 1
    }
  }

  private def tryCall(methodName: String, exprs: CExpression*): CAstElement = CFuncCall(methodName, exprs: _*)._try
}
