package ru.mipt.acsl.decode.mavlink.generator

import java.io._
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

import com.google.common.base.CaseFormat
import com.google.common.collect.{Multimap, MultimapBuilder}
import org.apache.commons.lang3.StringUtils

import scala.collection.mutable
import scala.xml.{Node, XML}

/**
  * @author Artem Shein
  */
class MavlinkSourceGenerator(val config: MavlinkSourceGeneratorConfig) {

  import MavlinkSourceGenerator._

  private val types: mutable.Set[String] = mutable.Set.empty
  private val messages = mutable.Buffer.empty[Message]
  private val enums = mutable.Map.empty[String, MavEnum]
  private val concreteEnums: Multimap[String, String] = MultimapBuilder.hashKeys().hashSetValues().build()

  def generate(): Unit = {

    val output = new ByteArrayOutputStream()
    new OutputStreamWriter(output) {
      this: Appendable =>

      def _eol(): Unit = eol(this)

      append("namespace " + config.getNamespaceFqn)
      _eol()
      append("import decode.(u8, u16, u32, u64, i8, i16, i32, i64, f32, f64, array, unit)")
      _eol()
      _eol()
      append("language en")
      _eol()
      processFile(config.getInputContents)
      _eol()
      append("alias ^float f32")
      _eol()
      append("alias char u8")
      _eol()
      types.foreach(generateType(_, this))
      messages.foreach(_.generate(this))
      val it = concreteEnums.entries().iterator()
      while (it.hasNext) {
        val e = it.next()
        generateConcreteEnum(e.getKey, e.getValue, this)
      }

      // Component
      _eol()
      append("component ").append(makeComponentName(config.getComponentName))
      append(" {")
      _eol()
      append("\tparameters (")
      _eol()
      messages.foreach { msg =>
        val typeName = msg.typeName
        append("\t\t")
        val id = msg.id.toString
        append("_").append(id)
        append(StringUtils.repeat(" ", Math.max(1, 40 - id.length())))
        append(": ").append(typeName)
        append(",")
        eol(this)
      }
      append("\t)")
      _eol()
      messages.find(_.name == CommandLong).foreach { commandMessage =>
        val cmdEnum = enums.get(MavCmd)
        assert(cmdEnum.isDefined, "enum for command not found")
        cmdEnum.get.constants.foreach { constant =>
          _eol()
          val cmdNameOriginal = if (constant.name.startsWith(MavCmd))
            constant.name.substring(MavCmd.length() + 1)
          else constant.name
          append("\tcommand ")
            .append(CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, cmdNameOriginal))
            .append(" @id(").append(constant.value.get.toString).append(") (")
          _eol()
          commandMessage.fields.foreach { field =>
            val fieldName = field.name
            val typeName = field.typeName
            if (fieldName.startsWith("param")) {
              val paramNum = fieldName.substring("param".length()).toInt
              val parameter = constant.params.find(_.index == paramNum)
              if (parameter.orElse(field.info).isDefined) {
                append("\t\t'")
                  .append(escapeUnaryQuotesString(parameter.map(_.info).getOrElse(field.info.get)))
                  .append("'")
                _eol()
              }
            }
            append("\t\t")
            append(fieldName)
            append(StringUtils.repeat(" ", Math.max(1, 20 - fieldName.length())))
            append(": ")
            append(typeName)
            append(",")
            _eol()
          }
          append("\t): unit")
          _eol()
        }
      }
      _eol()
      messages.foreach { msg =>
        val originalName = msg.name
        originalName.equals(CommandLong) match {
          case false =>
            val idString = msg.id.toString
            val name = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, originalName)
            append("\tstatus ").append(name)
              .append(StringUtils.repeat(" ",
                Math.max(1, 40 - name.length())))
              .append(" @id(").append(idString)
              .append(") (").append("_").append(idString).append(")")
            _eol()
          case _ =>
        }
      }
      append("}")
      _eol()

      close()
    }
    config.writeOutput(new String(output.toByteArray, StandardCharsets.UTF_8))
  }

  private def generateConcreteEnum(typeName: String, enumName: String, appendable: Appendable): Unit = {

    def append(s: String): Appendable = appendable.append(s)
    def _eol(): Unit = eol(appendable)

    _eol()
    val anEnum = enums.get(enumName).get
    for (i <- anEnum.info) {
      append("'").append(escapeUnaryQuotesString(i)).append("'")
    }
    append("enum ").append(makeTypeNameForEnum(typeName, enumName))
      .append(" ").append(typeName)
    append(" (")
    _eol()
    anEnum.constants.foreach { c =>
      append("\t").append(c.name)
      var width = c.name.length
      for (v <- c.value) {
        val literal = v.toString
        append(" = ").append(literal)
        width += literal.length() + 3
      }
      for (i <- c.info) {
        append(StringUtils.repeat(" ", Math.max(1, 40 - width)))
        append("'").append(escapeUnaryQuotesString(i)).append("'")
      }
      append(",")
      _eol()
    }
    append(")")
    _eol()
  }

  private def processFile(inputContents: String): Unit = {
    val document = XML.loadString(inputContents)
    (document \ "_").foreach { element =>
      element.label match {
        case "include" =>
          processFile(config.getIncludeContents(element.text))
        case "version" | "dialect" =>
        case "enums" =>
          (element \ "enum").map(processEnum).foreach { e =>
            val name = e.name
            if (enums.contains(name)) {
              enums(name) += e
            }
            else {
              enums(name) = e
            }
          }
        case "messages" =>
          messages ++= (element \ "message").map(processMessage)

        case _ =>
          throw new RuntimeException(s"Unexpected tag '${element.label}'");
      }
    }
  }

  private def processEnum(element: Node): MavEnum =
    MavEnum(element \@ "name",
      element.find(_.label == "description").map(_.text),
      (element \ "entry")
        .map(e => MavEnumConstant(e \@ "name",
          e.attribute("value").map(_.text.toInt),
          e.find(_.label == "description").map(_.text),
          (e \ "param").map { p => MavEnumConstantParam(
            (p \@ "index").toInt, p.text)
          })).toBuffer)

  implicit class Regex(sc: StringContext) {
    def r = new util.matching.Regex(sc.parts.mkString, sc.parts.tail.map(_ => "x"): _*)
  }

  private def generateType(typeName: String, appendable: Appendable): Unit = {
    typeName match {
      case r"([u]?int)$t([1-9][0-9]?)${bitLength}_t[a-zA-Z_0-9]*" =>
        appendable.append("alias ").append(typeName).append(" ").append(if (t.equals("uint")) "u" else "i")
          .append(bitLength)
        eol(appendable);
      case _ =>
    }
  }

  private def processMessage(message: Node): Message = {
    val id = (message \@ "id").toInt

    val name = message \@ "name"
    val description = message.find(_.label == "description")

    val fields = mutable.Buffer.empty[StructField]
    (message \ "field").map { field =>
      var t = field \@ "type"
      val fieldName = field \@ "name"
      val fieldDescription = if (field.text.isEmpty) None else Some(field.text)
      val fieldNameFormatted =
        escapeIfKeyword(CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, fieldName))
      if (t.contains("[")) {
        val index = t.indexOf('[')
        val baseType = escapeIfKeyword(t.substring(0, index))
        t = "array[" + baseType + ", " + t.substring(index + 1)
        types += baseType
      }
      else {
        t = escapeIfKeyword(t)
        types += t
      }
      val anEnum = field.attribute("enum").map(_.text)
      for (e <- anEnum) {
        concreteEnums.put(t, e)
      }
      fields += StructField(t, fieldNameFormatted, fieldDescription.map(escapeUnaryQuotesString), anEnum)
    }

    Message(name, escapeIfKeyword(CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_UNDERSCORE, name)),
      id, description.map(d => escapeUnaryQuotesString(d.text)), fields)
  }

}

object MavlinkSourceGenerator {
  val CommandLong = "COMMAND_LONG"
  val MavCmd = "MAV_CMD"

  def main(args: Array[String]): Unit = {
    val parser = MavlinkSourceGeneratorConfigCliParser()
    parser.parse(args) match {
      case Some(config) => apply(config).generate()
      case _ =>
        System.exit(1)
    }
  }

  def apply(config: MavlinkSourceGeneratorConfig) = new MavlinkSourceGenerator(config)

  private def eol(appendable: Appendable): Unit = appendable.append("\n")

  private def makeTypeNameForEnum(typeName: String, enumName: String): String =
    CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_UNDERSCORE, enumName) + "_" + typeName

  private def escapeUnaryQuotesString(text: String): String = text.replaceAll(Pattern.quote("'"), "\\\\'")

  private def escapeIfKeyword(name: String): String = name match {
    case "type" | "float" | "command" | "id" => "^" + name
    case _ => name
  }

  private def makeComponentName(name: String): String = name.capitalize

  private case class MavEnum(name: String, info: Option[String],
                             constants: mutable.Buffer[MavEnumConstant] = mutable.Buffer.empty) {

    def +=(e: MavEnum): Unit = constants ++= e.constants

  }

  private case class MavEnumConstant(name: String, value: Option[Int], info: Option[String], params: Seq[MavEnumConstantParam])

  private case class MavEnumConstantParam(index: Int, info: String)

  private case class MavComponent(name: String, subComponents: mutable.Set[MavComponent] = mutable.Set.empty) {

    def subComponents_+=(subComponent: MavComponent): Unit = subComponents += subComponent

  }

  private case class StructField(t: String, name: String, info: Option[String], enum: Option[String]) {

    def generate(appendable: Appendable): Unit = try {

      def append(s: String): Appendable = appendable.append(s)

      val tName = typeName
      for (i <- info) {
        append("  '").append(i).append("'")
        eol(appendable)
      }
      append("  ").append(name)
      append(StringUtils.repeat(" ", Math.max(40 - name.length(), 1)))
      append(": ").append(tName)
        //.append(StringUtils.repeat(" ", Math.max(20 - tName.length(), 1)))

      append(",")
      eol(appendable);
    }
    catch {
      case e: IOException => throw new RuntimeException(e)
    }

    def typeName: String = enum.map(e => makeTypeNameForEnum(t, e)).getOrElse(t)

  }

  private case class Message(name: String, typeName: String, id: Int, info: Option[String], var fields: Seq[StructField]) {

    reorderFields()

    private def reorderFields(): Unit = {
      fields = fields.sortWith { (left, right) =>
        val priorityLists = Seq(
          Seq("int64_t", "double", "uint64_t"),
          Seq("int32_t", "float", "uint32_t"),
          Seq("int16_t", "uint16_t"))

        val priority = { (field: StructField) =>
          val t = field.t
          priorityLists.zipWithIndex.find { case (types, idx) => types.contains(t) }.map(_._2).getOrElse(0)
        }

        priority(left) < priority(right)
      }

    }

    def generate(appendable: Appendable): Unit = {

      def append(s: String): Appendable = appendable.append(s)
      def _eol(): Unit = eol(appendable)

      _eol()
      for (i <- info) {
        append("'").append(i).append("'")
        _eol()
      }
      append("struct ").append(typeName).append(" ")
      append("(")
      _eol()

      fields.foreach(_.generate(appendable))

      append(")")
      _eol()
    }
  }

}