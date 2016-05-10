package ru.mipt.acsl.decode.c.generator.implicits

import ru.mipt.acsl.decode.c.generator._
import ru.mipt.acsl.decode.model.domain.impl.component.message.EventMessage
import ru.mipt.acsl.decode.model.domain.impl.component.message._
import ru.mipt.acsl.decode.model.domain.impl.component.{Command, Component}
import ru.mipt.acsl.decode.model.domain.impl.naming.Namespace
import ru.mipt.acsl.decode.model.domain.impl.types.{DecodeType, NativeType, StructField}
import ru.mipt.acsl.decode.model.domain.pure.component.message.{MessageParameter, StatusMessage, TmMessage}
import ru.mipt.acsl.generator.c.ast.implicits._
import ru.mipt.acsl.decode.c.generator.implicits.serialization._
import ru.mipt.acsl.decode.c.generator.CSourceGenerator._
import ru.mipt.acsl.decode.model.domain.HasOptionId
import ru.mipt.acsl.decode.model.domain.impl.registry.Registry
import ru.mipt.acsl.generator.c.ast._

import scala.collection.{immutable, mutable}

/**
  * @author Artem Shein
  */
private[generator] case class ComponentHelper(component: Component) {

  def typeName: String = component.name.asMangledString

  def prefixedTypeName: String = typePrefix + typeName

  def allCommands: Seq[WithComponent[Command]] =
    allSubComponents.toSeq.flatMap(sc => sc.commands.map(ComponentCommand(sc, _)))

  def allParameters: Seq[ComponentParameterField] =
    component.baseType.map(_.fields.map(ComponentParameterField(component, _))).getOrElse(Seq.empty) ++
      allSubComponents.toSeq.flatMap(sc => sc.baseType.map(_.fields.map(ComponentParameterField(sc, _)))
        .getOrElse(Seq.empty))

  def executeCommandMethodNamePart: String = "ExecuteCommand"

  def functionForCommandMethodName: String = component.prefixedTypeName.methodName("FunctionForCommand")

  def executeCommandMethodName(rootComponent: Component): String =
    if (component == rootComponent)
      component.prefixedTypeName.methodName(executeCommandMethodNamePart)
    else
      rootComponent.prefixedTypeName.methodName(component.cName + executeCommandMethodNamePart)

  def readExecuteCommandMethodNamePart: String = "ReadExecuteCommand"

  def readExecuteCommandMethodName(rootComponent: Component): String =
    if (component == rootComponent)
      component.prefixedTypeName.methodName(readExecuteCommandMethodNamePart)
    else
      rootComponent.prefixedTypeName.methodName(component.cName + readExecuteCommandMethodNamePart)

  def writeStatusMessageMethodName(rootComponent: Component): String =
    if (component == rootComponent)
      component.prefixedTypeName.methodName("WriteStatusMessage")
    else
      rootComponent.prefixedTypeName.methodName(component.cName + "WriteMessage")

  def isStatusMessageMethodName: String = component.prefixedTypeName.methodName("IsStatusMessage")

  def isEventAllowedMethodName: String = component.prefixedTypeName.methodName("IsEventAllowed")

  def componentDataTypeName: String = prefixedTypeName + "Data"

  def ptrType: CPtrType = CTypeApplication(prefixedTypeName).ptr

  def functionTableTypeName: String = prefixedTypeName + "UserFunctionTable"

  def guidDefineName: String = component.prefixedTypeName.upperCamel2UpperUnderscore + "_GUID"

  def idDefineName: String = component.prefixedTypeName.upperCamel2UpperUnderscore + "_ID"

  def executeMethodNamePart(c: Component): String =
    "Execute" + methodNamePart(c).capitalize

  def executeMethodNamePart(command: Command, c: Component): String =
    "Execute" + methodNamePart(command, c).capitalize

  def executeMethodName(c: Component): String =
    component.prefixedTypeName.methodName(executeMethodNamePart(c))

  def executeMethodName(command: Command, c: Component): String =
    component.prefixedTypeName.methodName(executeMethodNamePart(command, c))

  def methodNamePart(c: Component): String =
    ((if (component == c) "" else c.typeName) +
      component.cName.capitalize).upperCamel2LowerCamel

  def methodNamePart(part: String, c: Component): String =
    ((if (component == c) "" else c.typeName) + part).upperCamel2LowerCamel

  def methodNamePart(command: Command, c: Component): String =
    methodNamePart(command.cName.capitalize, c)

  def methodNamePart(message: TmMessage, c: Component): String =
    methodNamePart(message.cName.capitalize, c)

  def methodNamePart(field: StructField, c: Component): String =
    methodNamePart(field.cName.capitalize, c)

  def methodName(part: String): String = component.prefixedTypeName.methodName(part)

  def methodName(c: Component): String = methodName(methodNamePart(c))

  def methodName(command: Command, c: Component): String = methodName(methodNamePart(command, c))

  def methodName(field: StructField, c: Component): String = methodName(methodNamePart(field, c))

  def beginNewEventMethodName: String = prefixedTypeName.methodName("BeginNewEvent")

  def endEventMethodName: String = prefixedTypeName.methodName("EndEvent")

  def allSubComponents: immutable.Set[Component] =
    component.subComponents.flatMap { ref =>
      val c = ref.component
      c.allSubComponents + c
    }.toSet

  def collectNamespaces(nsSet: mutable.HashSet[Namespace]) {
    component.subComponents.foreach(_.component.collectNamespaces(nsSet))
    collectNsForTypes(nsSet)
  }

  def collectNsForTypes(set: mutable.Set[Namespace]) {
    for (baseType <- component.baseType)
      collectNsForType(baseType, set)
    component.commands.foreach { cmd =>
      cmd.parameters.foreach(arg => collectNsForType(arg.paramType, set))
      for (returnType <- cmd.returnType)
        collectNsForType(returnType, set)
    }
  }

  def writeStatusMessageMethod: CFuncImpl = writeStatusMessageMethod(component)

  def writeStatusMessageMethod(rootComponent: Component): CFuncImpl = {
    CFuncImpl(CFuncDef(component.writeStatusMessageMethodName(rootComponent), resultType,
      Seq(writer.param, messageId.param)),
      Seq(messageId.v.serializeBer._try.line, CIndent, CSwitch(messageId.v,
        casesForMap(component.allStatusMessagesById, { (message: TmMessage, c: Component) => message match {
          case message: StatusMessage =>
            Some(CStatements(CReturn(message.fullImplMethodName(rootComponent, c).call(writer.v))))
          case _ => None
        }
        }),
        default = CStatements(CReturn(invalidMessageId))), CEol))
  }

  def executeCommandMethod: CFuncImpl = executeCommandMethod(component)

  def executeCommandMethod(rootComponent: Component): CFuncImpl = {
    CFuncImpl(CFuncDef(component.executeCommandMethodName(rootComponent), resultType,
      Seq(reader.param, writer.param, commandId.param)),
      Seq(CIndent, CSwitch(commandId.v, casesForMap(component.allCommandsById,
        (command: Command, c: Component) =>
          Some(CStatements(CReturn(rootComponent.executeMethodName(command, c).call(reader.v, writer.v))))),
        default = CStatements(CReturn(invalidCommandId))), CEol))
  }

  def readExecuteCommandMethod: CFuncImpl = readExecuteCommandMethod(component)

  def readExecuteCommandMethod(rootComponent: Component): CFuncImpl = {
    CFuncImpl(CFuncDef(component.readExecuteCommandMethodName(rootComponent), resultType,
      Seq(reader.param, writer.param)),
      CStatements(commandId.v.define(commandId.t),
        tryCall(photonBerTypeName.methodName(typeDeserializeMethodName), commandId.v.ref, reader.v),
        CReturn(CFuncCall(component.executeCommandMethodName(rootComponent), reader.v, writer.v, commandId.v))))
  }

  def executeCommandForComponentMethodNamePart: String = "ExecuteCommandForComponent"

  def executeCommandForComponentMethodName(rootComponent: Component): String =
    if (component == rootComponent)
      component.prefixedTypeName.methodName(executeCommandForComponentMethodNamePart)
    else
      rootComponent.prefixedTypeName.methodName(executeCommandForComponentMethodNamePart + component.cName)

  def executeCommandForComponentMethod(rootComponent: Component): CFuncImpl = {
    CFuncImpl(CFuncDef(component.executeCommandForComponentMethodName(rootComponent), resultType,
      Seq(reader.param, writer.param, commandId.param)),
      Seq(CIndent, CSwitch(commandId.v, component.allComponentsById.toSeq.sortBy(_._1).map { case (id, c) =>
        CCase(CIntLiteral(id), CStatements(CReturn(c.readExecuteCommandMethodName(rootComponent)
          .call(reader.v, writer.v))))
      }, default = CStatements(CReturn(invalidCommandId))), CEol))
  }

  def executeCommandForComponentMethod: CFuncImpl = {
    CFuncImpl(CFuncDef(component.executeCommandForComponentMethodName(component), resultType,
      Seq(reader.param, writer.param, componentId.param, commandId.param)),
      Seq(CIndent, CSwitch(componentId.v, component.allComponentsById.toSeq.sortBy(_._1).map { case (id, c) =>
        CCase(CIntLiteral(id), CStatements(CReturn(
          (if (c == component)
            c.executeCommandMethodName(component)
          else
            c.executeCommandForComponentMethodName(component))
            .call(reader.v, writer.v, commandId.v))))
      }, default = CStatements(CReturn(invalidComponentId))), CEol))
  }

  def allMethods: Seq[MethodInfo] = component.allCommandsMethods ++
    component.allStatusMessageMethods ++ component.allEventMessageMethods ++
    Seq(MethodInfo(component.executeCommandMethod, component),
      MethodInfo(component.readExecuteCommandMethod, component, isPublic = true),
      MethodInfo(component.writeStatusMessageMethod, component, isPublic = true),
      MethodInfo(component.executeCommandForComponentMethod, component)) ++
    component.allSubComponents.map(c => MethodInfo(c.executeCommandForComponentMethod(component), c))

  def parameterMethodName(parameter: MessageParameter, rootComponent: Component): String =
    rootComponent.prefixedTypeName.methodName(parameter.ref(component).structField
      .getOrElse {
        sys.error("not implemented")
      }.cStructFieldName(rootComponent, component))

  def allEventMessageMethods: Seq[MethodInfo] = {
    allEventMessagesById.toSeq.sortBy(_._1).flatMap {
      case (id, ComponentEventMessage(c, eventMessage)) =>
        val eventVar = "event"._var
        val methodName = eventMessage.fullImplMethodName(component, c)
        val fullMethodDef = eventMessage.fullMethodDef(component, c)
        val eventParams: Seq[CFuncParam] = fullMethodDef.parameters
        val eventParam = eventParams.head
        val idLiteral = CIntLiteral(id)
        Seq(MethodInfo(CFuncImpl(CFuncDef(methodName, resultType, writer.param +: eventParams),
          CAstElements(CIndent, CIf(CEq(CIntLiteral(0),
            component.isEventAllowedMethodName.call(idLiteral, CTypeCast(eventVar, berType))),
            CAstElements(CEol, CIndent, CReturn(eventIsDenied), CSemicolon, CEol)),
            eventMessage.baseType.serializeCallCode(eventVar).line) ++
            eventMessage.fields.flatMap {
              case Left(p) =>
                p.serializeCallCode(component, c)
              case Right(p) =>
                CStatements(p.paramType.serializeCallCode(p.cName._var))
            } :+ CReturn(resultOk).line
        ), c),
          MethodInfo(CFuncImpl(fullMethodDef,
            CStatements(
              writer.v.define(writer.t, Some(component.beginNewEventMethodName.call(idLiteral, eventParam.name._var))),
              methodName.call(writer.v +: eventParams.map(_.name._var): _*)._try,
              CReturn(component.endEventMethodName.call()))), c, isPublic = true))
    }
  }

  def allStatusMessageMethods: Seq[MethodInfo] = {
    allStatusMessagesById.toSeq.sortBy(_._1).map(_._2).map {
      case ComponentStatusMessage(c, statusMessage) =>
        MethodInfo(CFuncImpl(CFuncDef(statusMessage.fullImplMethodName(component, c), resultType,
          Seq(writer.param)),
          statusMessage.parameters.flatMap(p =>
            CComment(p.toString).line +: p.serializeCallCode(component, c)) :+ CReturn(resultOk).line), c)
    }
  }

  def allCommandDefines: CAstElements = allCommandDefines(component)

  def allCommandDefines(rootComponent: Component): CAstElements =
    component.commandDefines(rootComponent) ++ component.allSubComponents.toSeq.flatMap(_.commandDefines(component))

  def commandDefines(rootComponent: Component): CAstElements = {
    val defineName: String = (rootComponent.prefixedTypeName +
      (if (rootComponent == component) "" else component.cName) + "CommandIds").upperCamel2UpperUnderscore
    val commandById = component.allCommandsById
    CDefine(defineName + "_LEN", commandById.size.toString).eol ++
      CDefine(defineName, "{" + commandById.toSeq.map(_._1).sorted.mkString(", ") + "}").eol
  }

  def componentDefines: CAstElements = {
    Seq(CDefine("PHOTON_COMPONENTS_SIZE", allComponentsById.size.toString), CEol,
      CDefine("PHOTON_COMPONENT_IDS", '{' + allComponentsById.keys.toSeq.sorted.mkString(", ") + '}'), CEol) ++
      allComponentsById.flatMap { case (id, c) =>
        val guidDefineName = c.guidDefineName
        Seq(CDefine(guidDefineName, '"' + c.fqn.asMangledString + '"'), CEol,
          CDefine("PHOTON_COMPONENT_" + id + "_GUID", guidDefineName), CEol,
          CDefine(c.idDefineName, id.toString), CEol)
      } ++
      Seq(CDefine("PHOTON_COMPONENT_GUIDS", '{' + allComponentsById.toSeq.sortBy(_._1).map(_._2)
        .map(_.guidDefineName).mkString(", ") + '}'), CEol)
  }

  def allMessageDefines: CAstElements = allMessageDefines(component)

  def allMessageDefines(rootComponent: Component): CAstElements =
    component.messageDefines(rootComponent) ++ component.allSubComponents.toSeq.flatMap(_.messageDefines(component))

  def messageDefines(rootComponent: Component): CAstElements = {
    val prefix = (rootComponent.prefixedTypeName +
      (if (rootComponent == component) "" else component.cName)).upperCamel2UpperUnderscore
    val eventIdsDefineName = prefix + "_EVENT_MESSAGE_IDS"
    val statusIdsDefineName = prefix + "_STATUS_MESSAGE_IDS"
    val statusMessageIdPrioritiesDefineName = prefix + "_STATUS_MESSAGE_ID_PRIORITIES"
    val statusPrioritiesDefineName = prefix + "_STATUS_MESSAGE_PRIORITIES"
    val statusMessageById = component.allStatusMessagesById
    val statusMessagesSortedById = statusMessageById.toSeq.sortBy(_._1)
    val eventMessageById = component.allEventMessagesById
    val eventMessagesSortedById = eventMessageById.toSeq.sortBy(_._1)
    CDefine(eventIdsDefineName + "_SIZE", eventMessageById.size.toString).eol ++
      CDefine(eventIdsDefineName, "{" + eventMessagesSortedById.map(_._1).mkString(", ") + "}").eol ++
      CDefine(statusIdsDefineName + "_SIZE", statusMessagesSortedById.size.toString).eol ++
      CDefine(statusIdsDefineName, '{' + statusMessagesSortedById.map(_._1.toString).mkString(", ") + '}').eol ++
      CDefine(statusPrioritiesDefineName, "{" + statusMessagesSortedById.map(_._2._2.priority.getOrElse(0)).mkString(", ") + "}").eol ++
      CDefine(statusMessageIdPrioritiesDefineName, "{\\\n" + statusMessagesSortedById.map {
        case (id, ComponentStatusMessage(c, m)) => s"  {$id, ${m.priority.getOrElse(0)}}"
        case _ => sys.error("assertion error")
      }.mkString("\\\n") + "\\\n}").eol
  }

  def parameterMethodImplDefs: Seq[MethodDefInfo] = {
    val parameters: Seq[ComponentParameterField] = component.allParameters
    val res = parameters.map { case ComponentParameterField(c, f) =>
      val fType = f.typeUnit.t
      MethodDefInfo(CFuncDef(component.methodName(f, c), fType.cMethodReturnType, fType.cMethodReturnParameters), c)
    }
    res
  }

  def beginNewEventMethodDef: CFuncDef =
    CFuncDef(component.beginNewEventMethodName, writer.t, Seq(messageId.param, eventId.param))

  def endEventMethodDef: CFuncDef = CFuncDef(component.endEventMethodName, resultType)

  def isEventAllowedMethodDef: CFuncDef =
    CFuncDef(component.isEventAllowedMethodName, b8Type, Seq(messageId.param, eventId.param))

  def serviceMethodDefs: Seq[CFuncDef] =
    Seq(component.beginNewEventMethodDef, component.endEventMethodDef, component.isEventAllowedMethodDef)


  def commandMethodImplDefs: Seq[MethodDefInfo] = {
    component.allCommands.map { case ComponentCommand(c, command) =>
      MethodDefInfo(CFuncDef(component.methodName(command, c),
        command.returnType.map(_.cMethodReturnType).getOrElse(voidType),
        command.parameters.map(p => {
          val t = p.paramType
          CFuncParam(p.cName, mapIfNotSmall(t.cType, t, (ct: CType) => ct.ptr.const))
        }) ++ command.returnType.map(_.cMethodReturnParameters).getOrElse(Seq.empty)), c)
    }
  }

  def allSubComponentsMethods: Seq[MethodInfo] = {
    component.allSubComponents.toSeq.flatMap { subComponent =>
      Seq(subComponent.executeCommandMethod(component),
        subComponent.readExecuteCommandMethod(component),
        subComponent.writeStatusMessageMethod(component))
        .map(f => MethodInfo(f, subComponent))
    }
  }

  def allCommandsMethods: Seq[MethodInfo] = {
    val componentTypeName = component.prefixedTypeName
    val parameters = Seq(reader.param, writer.param)
    component.allCommandsById.toSeq.sortBy(_._1).map(_._2).map { case ComponentCommand(subComponent, command) =>
      val methodNamePart = component.executeMethodNamePart(command, subComponent)
      val vars = command.parameters.map { p => CVar(p.mangledCName) }
      val varInits = vars.zip(command.parameters).flatMap { case (v, parameter) =>
        val paramType = parameter.paramType
        CStatements(v.define(paramType.cType), paramType.deserializeCallCode(v.ref)._try)
      }
      val cmdReturnType = command.returnType
      val funcCall = component.methodName(command, subComponent).call(
        (for ((v, t) <- vars.zip(command.parameters.map(_.paramType))) yield v.refIfNotSmall(t)): _*)
      MethodInfo(CFuncImpl(CFuncDef(componentTypeName.methodName(methodNamePart), resultType, parameters),
        varInits ++ cmdReturnType.map {
          case n: NativeType if n.isPrimitive => CStatements(n.serializeCallCode(funcCall), CReturn(resultOk))
          case rt => CStatements(CReturn(rt.serializeCallCode(funcCall)))
        }.getOrElse(CStatements(funcCall, CReturn(resultOk)))), subComponent)
    }
  }

  def allTypes: immutable.Set[DecodeType] =
    (component.commands.flatMap(cmd => cmd.returnType.map(_.typeWithDependentTypes).getOrElse(Seq.empty) ++
      cmd.parameters.flatMap(_.paramType.typeWithDependentTypes)) ++
      component.eventMessages.map(_.baseType) ++
      component.baseType.map(_.fields.flatMap(_.typeUnit.t.typeWithDependentTypes)).getOrElse(Seq.empty)).toSet ++
      component.allSubComponents.flatMap(_.allTypes)

  // todo: optimize: memoize
  private def makeMapById[T <: HasOptionId](seq: Seq[T], subSeq: Component => Seq[T])
  : immutable.HashMap[Int, WithComponent[T]] = {
    var nextId = 0
    val mapById = mutable.HashMap.empty[Int, WithComponent[T]]
    // fixme: remove Option.get
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
    immutable.HashMap(mapById.toSeq: _*)
  }

  def allCommandsById: immutable.HashMap[Int, WithComponent[Command]] =
    makeMapById(component.commands, _.commands)

  def allStatusMessagesById: immutable.HashMap[Int, WithComponent[StatusMessage]] =
    makeMapById(component.statusMessages, _.statusMessages)

  def allEventMessagesById: immutable.HashMap[Int, WithComponent[EventMessage]] =
    makeMapById(component.eventMessages, _.eventMessages)

  def allComponentsById: immutable.HashMap[Int, Component] = {
    val map = mutable.HashMap.empty[Int, Component]
    var nextId = 0
    val components = component +: component.allSubComponents.toSeq
    val (withId, withoutId) = (components.filter(_.id.isDefined), components.filter(_.id.isEmpty))
    withId.foreach { c => assert(map.put(c.id.getOrElse(sys.error("wtf")), c).isEmpty) }
    map ++= withoutId.map { c =>
      while (map.contains(nextId))
        nextId += 1
      nextId += 1
      (nextId - 1, c)
    }
    immutable.HashMap(map.toSeq: _*)
  }
}
