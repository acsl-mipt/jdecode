package ru.mipt.acsl.decode.generator.model.doc.html

import java.io
import java.io.{FileOutputStream, OutputStreamWriter}
import java.net.URLEncoder

import scala.collection.JavaConversions._
import com.google.common.base.Charsets
import ru.mipt.acsl.decode.model.component.{Component, StatusParameter}
import ru.mipt.acsl.decode.model.naming.{Fqn, Namespace}
import ru.mipt.acsl.decode.model.registry.{Language, Registry}
import ru.mipt.acsl.decode.model.types.{DecodeType, StructType, _}
import ru.mipt.acsl.decode.model.{Parameter, StatusParameter}

import scalatags.Text.all._
import scalatags.Text.tags2.{title => titleTag}

/**
  * @author Artem Shein
  */
case class HtmlModelDocGeneratorConfiguration(outputFile: io.File, registry: Registry, exclude: Set[Fqn] = Set.empty,
                                              language: String = "ru")

class HtmlModelDocGenerator(val config: HtmlModelDocGeneratorConfiguration) {

  private val lang: Language = Language.newInstance(config.language)

  def generate(): Unit = {
    val allComponents = config.registry.allComponents.filterNot(c =>
      config.exclude.contains(c.fqn) || config.exclude.contains(c.namespace.fqn))
    val allNamespaces = config.registry.allNamespaces.filterNot(ns => config.exclude.contains(ns.fqn))
    config.outputFile.write(
      html(
        head(
          meta(httpEquiv := "content-type", content := "text/html; charset=UTF-8"),
          meta(charset := "utf-8"),
          titleTag("Описание компонентов"),
          link(rel := "stylesheet", href := "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.5/css/bootstrap.min.css"),
          script(src := "https://ajax.googleapis.com/ajax/libs/jquery/1.11.3/jquery.min.js"),
          script(src := "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.5/js/bootstrap.min.js"),
          raw(
            """
              |<style type="text/css">
              |        .ln { color: rgb(0,0,0); font-weight: normal; font-style: normal; }
              |        .s0 { color: rgb(204,120,50); font-weight: bold; }
              |        .s1 { color: rgb(169,183,198); }
              |        .s2 { color: rgb(106,135,89); }
              |        .s3 { color: rgb(204,120,50); }
              |        .s4 { color: rgb(104,151,187); }
              |        .s5 { color: rgb(128,128,128); }
              |    </style>
              |
              |    <style type="text/css">
              |        body {
              |            counter-reset: h2counter;
              |            counter-reset: h3counter;
              |            counter-reset: h4counter;
              |            counter-reset: h5counter;
              |            counter-reset: h6counter;
              |        }
              |        h1 {
              |            counter-reset: h2counter;
              |        }
              |        h2 {
              |            counter-reset: h3counter;
              |        }
              |        h3 {
              |            counter-reset: h4counter;
              |        }
              |        h4 {
              |            counter-reset: h5counter;
              |        }
              |        h5 {
              |            counter-reset: h6counter;
              |        }
              |        h2:before {
              |            content: counter(h2counter) " ";
              |            counter-increment: h2counter;
              |            counter-reset: h3counter;
              |            font-size: 65%;
              |            font-weight:400;
              |            line-height:1;
              |            color:#777
              |        }
              |        h3:before {
              |            content: counter(h2counter) "." counter(h3counter) " ";
              |            counter-increment: h3counter;
              |            font-size: 65%;
              |            font-weight:400;
              |            line-height:1;
              |            color:#777
              |        }
              |        h4:before {
              |            content: counter(h2counter) "." counter(h3counter) "." counter(h4counter) " ";
              |            counter-increment: h4counter;
              |            font-size: 65%;
              |            font-weight:400;
              |            line-height:1;
              |            color:#777
              |        }
              |        h5:before {
              |            content: counter(h2counter) "." counter(h3counter) "." counter(h4counter) "." counter(h5counter) " ";
              |            counter-increment: h5counter;
              |            font-size: 65%;
              |            font-weight:400;
              |            line-height:1;
              |            color:#777
              |        }
              |        h6:before {
              |            content: counter(h2counter) "." counter(h3counter) "." counter(h4counter) "." counter(h5counter) "." counter(h6counter) " ";
              |            counter-increment: h6counter;
              |            font-size: 65%;
              |            font-weight:400;
              |            line-height:1;
              |            color:#777
              |        }
              |    </style>
            """.stripMargin)
        ),
        body(
          div(cls := "container")(
            Seq(h1("Описание компонентов"), h2("Оглавление"), ol(
              li("Описание компонентов", ol(allComponents.map(c =>
                li(a(href := "#" + htmlId(c))(c.name.mangledNameString()))))),
              li("Описание пространств имен и типов", ol(allNamespaces.map(ns =>
                li(a(href := "#" + htmlId(ns))(ns.fqn.mangledNameString())))))), hr) ++
            Seq(h2("Описание компонентов")) ++
              allComponents.flatMap(generateComponent) ++
            Seq(h2("Описание пространств имен и типов")) ++
            allNamespaces.flatMap(generateNamespace)))))
  }

  def generateNamespace(namespace: Namespace): Seq[Tag] = {
    Seq(h3(id := htmlId(namespace))("Пространство имен " + namespace.fqn.mangledNameString())) ++
      Option(namespace.info.get(lang)).map(div(_)).toSeq ++
      (if (namespace.subNamespaces.isEmpty)
        Seq.empty
      else
        Seq(p("Внутренние пространства имен: ")(raw(namespace.subNamespaces.map(ns =>
          a(href := "#" + htmlId(ns))(ns.name.mangledNameString())).mkString(", ") + ".")))) ++
      (if (namespace.types.isEmpty)
        Seq.empty
      else
        Seq(h4("Типы")) ++
        Seq(table(cls := "table table-bordered")(style := "width: auto")(
          thead(tr(th("Имя типа"), th("Описание"), th("Вид типа"))),
          tbody(namespace.types.map(t =>
            tr(td(id := htmlId(t))(typeName(t)), td(raw(t.info.getOrElse(lang, ""))), typeKind(t))): _*)))) ++
      Seq(hr)
  }

  def typeKind(t: DecodeType): Tag = t match {
    case array if array.isArray => td("Массив " + typeName(ArrayTypeInfo(array).baseType) + " элементов")
    case a: Alias.Type => td("Псевдоним для ", typeNameWithLink(a.obj))
    case s: SubType => td("Расширение типа ", typeNameWithLink(s.baseType))
    case s: StructType => td("Структура", br, "Поля:",
      table(cls := "table", style := "width: auto")(
        thead(tr(th("Имя поля"), th("Тип"), th("Описание"))),
        tbody(s.fields.map(f => tr(td(f.name.mangledNameString()), td(typeNameWithLink(f.typeMeasure.t)),
          td(StringFrag(f.info.getOrElse(lang, ""))))): _*)))
    case e: EnumType => td("Перечисление" +
      Option(e.extendsTypeOption.orElse(null)).map(ext => " расширяющее " + typeName(ext)).getOrElse(""),
      br, "Базовый тип: ", typeNameWithLink(e.baseType), br, "Константы:",
      ul(e.allConstants.toSeq.sortBy(_.value.toString).map(c => li(c.name.mangledNameString() + " = " + c.value)): _*))
    case n: NativeType =>
      val p = PrimitiveTypeInfo.typeInfoByFqn.get(Option(n.fqn.orElse(null)).getOrElse(sys.error("not implemented")))
      td(StringFrag(p.map(pi => pi.bitLength + "-битный " + TypeKind.nameForTypeKind(pi.kind))
        .getOrElse("Системный встроенный тип")))
    /*case g: GenericType => td("Обобщенный тип ")(g.name.asMangledString)('<' +
      g.typeParameters.map(_.map(_.asMangledString).getOrElse("")).mkString(", ") + '>')*/
    case s: GenericTypeSpecialized =>
      td("Специализированный обобщенный тип ", typeNameWithLink(s.genericType), "<",
        raw(s.genericTypeArguments.map(typeNameWithLink(_).toString).mkString(", ")), ">")
  }

  def htmlId(ns: Namespace): String = "n" + URLEncoder.encode(ns.fqn.mangledNameString(), Charsets.UTF_8.name)
  def htmlId(c: Component): String = "c" + URLEncoder.encode(c.fqn.mangledNameString(), Charsets.UTF_8.name)
  def htmlId(t: DecodeType): String =  "t" + typeName(t)

  def typeNameWithLink(t: DecodeType): Tag =
    a(href := "#" + htmlId(t))(typeName(t))

  def typeName(t: DecodeType): String = t match {
    case array if array.isArray => "[" + typeName(ArrayTypeInfo(array).baseType) + "]"
    case t: GenericTypeSpecialized => typeName(t.genericType) + "<" +
      t.genericTypeArguments.map(a => typeName(a)) .mkString(", ")+ ">"
    case _ => sys.error("not implemented") //t.name.asMangledString
  }

  def generateComponent(component: Component): Seq[Tag] = {
    Seq(h3(id := htmlId(component))("Компонент " + component.name.mangledNameString())) ++
      (if (component.subComponents.isEmpty)
        Seq.empty
      else
        Seq(p("Включает компоненты: ", raw(component.subComponents.map(cr =>
          a(href := "#" + htmlId(cr.obj().obj))(cr.obj().obj.name.mangledNameString()) +
            (if (cr.name == cr.obj().obj.name) " под именем " + cr.name.mangledNameString() else "")).mkString(", ")), "."))) ++
      Option(component.info.get(lang)).map(div(_)).toSeq ++
      Option(component.baseType.orElse(null)).map(bt => Seq(h4("Параметры"),
        table(cls := "table table-bordered", style := "width: auto")(
          thead(tr(th("Имя параметра"), th("Тип параметра"), th("Единицы измерения"), th("Описание параметра"))),
          tbody(bt.fields.map(f =>
            tr(
              td(f.name.mangledNameString()),
              td(typeNameWithLink(f.typeMeasure.t)),
              td(StringFrag(Option(f.typeMeasure.measure.orElse(null)).map(_.name.mangledNameString()).getOrElse(""))),
              td(StringFrag(f.info.getOrElse(lang, ""))))): _*))))
        .getOrElse(Seq.empty) ++
      (if (component.eventMessages.isEmpty)
        Seq.empty
      else
        Seq(h4("Событийные сообщения")) ++
          component.eventMessages.flatMap(e => Seq(h5(e.name.mangledNameString())) ++ Seq(Option(e.info.get(lang)).map(div(_))).flatten ++
            Seq(table(cls := "table table-bordered", style := "width: auto")(
              thead(tr(th("Параметр или переменная"), th("Тип параметра"), th("Описание параметра"))),
              tbody(e.parameters.map{
                case s: StatusParameter => trForMessageParameter(s, component)
                case p: Parameter => trForParameter(p)
              }: _*))))) ++
      (if (component.statusMessages.isEmpty)
        Seq.empty
      else
        Seq(h4("Статусные сообщения")) ++
          component.statusMessages.flatMap(s => Seq(h5(s.name.mangledNameString())) ++ Seq(Option(s.info.get(lang)).map(div(_))).flatten ++
            Seq(table(cls := "table table-bordered", style := "width: auto")(
              thead(tr(th("Параметр"), th("Тип параметра"), th("Описание параметра"))),
              tbody(s.parameters.map(trForMessageParameter(_, component)): _*))
          ))) ++
      (if (component.commands.isEmpty)
        Seq.empty
      else
        Seq(h4("Команды")) ++
          component.commands.flatMap{ c =>
            Seq(h4(c.name.mangledNameString()), p("Возвращаемое значение: ",
              c.returnType match {
                case t if t.isUnit => "нет"
                case rt => a(href := "#" + htmlId(rt))(typeName(rt))
              }, ".")) ++
              Seq(Option(c.info.get(lang)).map(div(_))).flatten ++
              (if (c.parameters.isEmpty)
                Seq.empty
              else
                Seq(table(cls := "table table-bordered", style := "width: auto")(
                  thead(tr(th("Параметр команды"), th("Тип параметра"), th("Описание параметра"))),
                  tbody(c.parameters.map(trForParameter): _*))))
          }) ++
    Seq(hr)
  }

  def trForMessageParameter(mp: StatusParameter, component: Component): Tag =
    tr(
      td("Параметр " + mp.path),
      td(typeNameWithLink(mp.ref(component).resultType)),
      td(StringFrag(mp.info.getOrElse(lang, ""))))

  def trForParameter(p: Parameter): Tag =
    tr(
      td(p.name.mangledNameString()),
      td(typeNameWithLink(p.parameterType)),
      td(StringFrag(p.info.getOrElse(lang, ""))))

  implicit class RichFile(val file: io.File) {
    def write(contents: Tag): Unit = write(Seq(contents))
    def write(contents: Seq[Tag]) {
      val stream = new OutputStreamWriter(new FileOutputStream(file))
      try {
        val sb = new StringBuilder
        contents.foreach(_.writeTo(sb))
        stream.write(sb.toString())
      } finally {
        stream.close()
      }
    }
  }
}
