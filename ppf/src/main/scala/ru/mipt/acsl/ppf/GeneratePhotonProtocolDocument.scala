package ru.mipt.acsl.ppf

import java.io.PrintWriter

import com.google.common.base.Charsets
import com.google.common.io.Resources
import ru.mipt.acsl.ppf.html.{EmptyHtmlInfo, Html, HtmlInfo}
import ru.mipt.acsl.ppf.packet._
import ru.mipt.acsl.ppf.packet.combined.{CommandCombined, CommandResultCombined, TmCombined}

import scalacss.ProdDefaults._
import scalacss.mutable.StyleSheet
import scalatags.Text.TypedTag
import scalatags.Text.all.{html => htmlTag, Tag => _, _}
import scalatags.Text.tags2.{style => styleTag, title => titleTag}

/**
  * Created by metadeus on 04.05.16.
  */
object GeneratePhotonProtocolDocument {

  private val center = cls := "text-center"

  private val colspan2 = colspan := "2"

  private val autoWidth = style := "width:auto"

  private def pc(els: Modifier*) = p(center)(els: _*)

  private def td2(els: Modifier*) = td(colspan2)(els: _*)

  private def trTdCenter(els: Modifier*) = tr(td(pc(els: _*)))

  private def tableBordered(els: Modifier*) = table(cls := "table table-bordered")(els: _*)

  class PacketSection(sectionId: String, h: TypedTag[String], packet: Packet, additionalText: HtmlInfo = EmptyHtmlInfo) {
    def a: Modifier = scalatags.Text.all.a(href := ("#" + sectionId), packet.title)

    def toHtml: Seq[Modifier] = Seq(h(id := sectionId, packet.title), div(packet.packetInfo.toHtml ++ packet.toHtml ++ additionalText.toHtml: _*))
  }

  object MultiplexingSection extends PacketSection("streams-multiplexing-data-packet", h4, Multiplexing,
    Html(p("Пакет считается корректным при совпадении заголовка, длины пакета, номера потока.")))

  object DelimiterSection extends PacketSection("streams-exchange-cmd-delimiter", h5, Delimiter,
    Html(p("Пакет считается корректным если он полностью совпадает с требуемым.")))

  object CmdExchangeSection extends PacketSection("streams-exchange-cmd-packet", h5, CmdExchange)

  object SetCounterSection extends PacketSection("streams-exchange-cmd-counter", h5, SetCounter)

  object ConfirmationSection extends PacketSection("streams-exchange-cmd-confirmation", h5, Confirmation,
    Html(p("Пакет считается корректным при совпадении заголовка, длины пакета, а так же при корректности контрольной суммы.")))

  object CommandMessageSection extends PacketSection("streams-app-cmd-packet", h5, CommandMessage,
    Html(p("Команда кодируется в следующем виде:"), Command.toHtml))

  object CommandResultSection extends PacketSection("streams-app-control-result", h5, CommandResult)

  object TmSection extends PacketSection("streams-exchange-tm-packet", h5, Tm)

  object EventTmMessageSection extends PacketSection("streams-app-tm-event", h5, EventTmMessage)

  object StatusTmMessageSection extends PacketSection("streams-app-tm-status", h5, StatusTmMessage)

  object CombinedCommandSection extends PacketSection("streams-combined-cmd", h4, CommandCombined)

  object CombinedCommandResultSection extends PacketSection("streams-combined-result", h4, CommandResultCombined)

  object CombinedTmSection extends PacketSection("streams-combined-tm", h4, TmCombined)

  object Styles extends StyleSheet.Standalone {

    import dsl._

    //Decode highlight
    ".ln" -(color.rgb(0, 0, 0), fontWeight.normal, fontStyle.normal)
    ".s0" -(color.rgb(204, 120, 50), fontWeight.bold)
    ".s1" - (color.rgb(169, 183, 198))
    ".s2" - (color.rgb(106, 135, 89))
    ".s3" - (color.rgb(104, 151, 187))
    ".s4" - (color.rgb(204, 120, 50))

    // Headers' counters
    val rng = 2 to 6

    val counters = rng.map { i => s"h${i}counter" }

    "body" - (
      counters.map { c => ToStyleAV(counterReset := c) }: _*
      )

    for (c <- rng) {
      s"h${c - 1}" - (counterReset := s"h${c}counter")
      s"h$c".before -(
        content := (2 to c).map { i => s"counter(h${i}counter)" }.mkString(" \".\" ") + " \" \"", // interpolation bug
        counterIncrement := s"h${c}counter",
        counterReset := s"h${c + 1}counter",
        fontSize := "65%",
        fontWeight := "400px",
        lineHeight := "1px",
        color := c"#777"
        )
    }
  }

  def main(args: Array[String]): Unit = {

    require(args.nonEmpty, "provide output file path")

    val fileContents =
      raw("<!DOCTYPE html>").render +
        htmlTag(
          head(meta(httpEquiv := "content-type", content := "text/html; charset=UTF-8"),
            meta(charset := "utf-8"),
            titleTag("Протокол информационного обмена борт-земля Фотон (Photon Protocol)"),
            link(rel := "stylesheet", href := "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.5/css/bootstrap.min.css"),
            script(src := "https://ajax.googleapis.com/ajax/libs/jquery/1.11.3/jquery.min.js"),
            script(src := "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.5/js/bootstrap.min.js"),
            styleTag(`type` := "text/css", raw(Styles.render))
          ),
          body(
            div(cls := "container",
              h1("Протокол информационного обмена борт-земля Фотон (Photon Protocol)"),

              h2(id := "about", "Назначение протокола"),
              div(
                p("Протокол борт-земля предназначен для организации обмена и обеспечения управления беспилотными аппаратами, как наземными, так и воздушными, водными, космическими и другими типами аппаратов как с постоянной, так и сеансовой связью."),
                p("Протокол позволяет организовать информационный обмен между одним или несколькими распределенными наземными или мобильными пунктами управления и отдельным аппаратом или распределенной группой разнородных аппаратов, а так же между пунктами управления, отдельными аппаратами и группами аппаратов."),
                p("Использование протокола позволяет избежать множества ошибок допускаемых при проектировании и переиспользовать накопленный опыт проектирования протоколов в различных направлениях. Кроме того, протокол в своем составе предлагает полный набор инструментов для создания наземных и бортовых систем управления, отладки и тестирования обменов, организации бортовой системы исполнения кода и средства для его написания и отладки. А так же комплекс средств по хранению и анализу телеметрической информации и обеспечению научных расчетов."),
                p("Кроме того, при разработке протокола были учтены требования к обеспечению возможности эволюционного изменения и дополнения на основе будущих требований от перспективных авиа-космических и наземных комплексов, а так же обесечения обратной совместимости и возможностей управления аппаратами с разными версиями и набором доступной функциональности.")),
              hr,

              h2(id := "streams", "Информационный обмен"),
              div(
                h3(id := "streams-types", "Описание типов данных"),
                div(
                  p(raw("Протокол использует типы из &laquo;"), a(href := "Base_types.html", "Пространства базовых типов данных"), raw("&raquo;."))), hr,
                h3(id := "streams-stack", "Стек обменных протоколов"),
                div(
                  p("С целью упрощения реализации и внесения изменений в структуру информационного обмена протокол информационного обмена представляет собой иерархическую структуру &mdash; стек протоколов, где каждый уровень имеет выделенный смысл и возложенные на него функции, а так же полностью регламентирует структуру пакетов и место в иерархии. Каждый нижележащий уровень стека может включать только пакеты ближайшего к нему верхнего уровня стека."),
                  p("Начиная с обменного уровня происходит деление на телеметрический и командный потоки данных, оно обсуловлено различиями в обработке данных из этих потоков: командный поток подразумевает гарантированную доставку пакетов с командами и ответами на команды и контроль прохождения пакетов в строго заданном порядке, телеметрический поток не накладывает таких ограничений. Командный поток работает в режиме запрос-ответ, в то время, как телеметрический в режиме вещания."),
                  table(cls := "table", autoWidth,
                    thead(
                      tr(th(colspan2, "Название уровня стека протоколов"),
                        th("Соответствующий уровень модели OSI"),
                        th("Назначение уровня"))),
                    tbody(
                      tr(td(rowspan := "3", a(href := "#streams-combined", "Комбинированный уровень (компактный вид)")),
                        td(a(href := "#streams-app", "Прикладной уровень")),
                        td(ul(li("Прикладной"),
                          li("Представительский"),
                          li("Сеансовый"))),
                        td("Используется для передачи команд, ответов на команды и телеметрической информации.")),
                      tr(td(a(href := "#streams-exchange", "Обменный уровень")),
                        td(ul(li("Транспортный"),
                          li("Сетевой"))),
                        td("Используется для адресации устройств и компонентов устройства в обменной сети.")),
                      tr(td(a(href := "#streams-multiplexing", "Мультиплексирование обменных потоков")),
                        td("Нет прямого соответствия"),
                        td("Опциональный уровень, может быть опущен при использовании разных каналов связи для потоков. Используется для мультиплексирования (смешивания) данных разных потоков в том случае, когда требуется организовать обмен командными и телеметрическими данными в одном выделенном канале.")),
                      tr(td2(a(href := "#streams-datalink", "Канальный уровень")),
                        td("Канальный"),
                        td("Обеспечивает физическую адресацию и обмен кадрами данных между абонентами")))),
                  p("Информационный обмен спроектирован таким образом, что позволяет реализовать библиотеку для разбора информационных потоков без необходимости внесения изменений в неё даже в случае эволюционного изменения протокола при условии соблюдения правил эволюции протокола."),
                  hr,

                  h4(id := "streams-stack-tcpip", "Использование протокола совместно с TCP/IP-стеком"),
                  div(
                    p("При использовании поверх TCP/IP-стека возможно несколько вариантов использования протокола."),
                    p("В случае замены адресации протокола на адресацию протокола IP возможно заменить все уровни ниже прикладного. В этом случае командный поток будет использовать TCP/IP-cтек, а телеметрический поток &mdash; UDP/IP:"),
                    tableBordered(
                      thead(th("Командный поток"), th("Телеметрический поток")),
                      tbody(
                        tr(td(pc(a(href := "#streams-app-cmd", "Командный поток прикладного уровня"))),
                          td(pc(a(href := "#streams-app-tm", "Телеметрический поток прикладного уровня")))),
                        tr(td(pc("TCP")), td(pc("UDP"))),
                        tr(td2(pc("IP"))),
                        tr(td2(pc("Ethernet, ..."))))),
                    p("В случае, когда адресация протокола IP не соответствует требованиям и/или требуется объединение командного и телеметрического потоков в один, то возможно использование UDP или TCP вместо канального уровня протокола:"),
                    tableBordered(autoWidth, tbody(
                      tr(td2(pc(a(href := "#streams-app", "Прикладной уровень")))),
                      tr(td2(pc(a(href := "#streams-exchange", "Обменный уровень")))),
                      tr(td2(pc(a(href := "#streams-multiplexing", "Мультиплексирование обменных потоков")))),
                      tr(td(pc("UDP")), td(pc("TCP"))),
                      tr(td2(pc("IP"))),
                      tr(td2(pc("Ethernet, ...")))))), hr,

                  h4(id := "streams-stack-802", "Использование совместно со стандартом IEEE 802.11"),
                  div(
                    p("Стандарт меш-сети IEEE 802.11 подразумевает реализацию физического и канального уровней сети, поэтому совместное использование протокола со стандартом реализуется заменой канального уровня протокола:"),
                    tableBordered(autoWidth, tbody(
                      trTdCenter(a(href := "#streams-app", "Прикладной уровень")),
                      trTdCenter(a(href := "#streams-exchange", "Обменный уровень")),
                      trTdCenter(a(href := "#streams-multiplexing", "Мультиплексирование обменных потоков")),
                      trTdCenter("802.11"))),
                    p("Так же в случае организованной IP-сети поверх 802.11 возможен вариант использования TCP и UDP, как и в случае с использованием TCP/IP-стека (с теми же ограничениями):"),
                    tableBordered(
                      thead(th("Командный поток"), th("Телеметрический поток")),
                      tbody(
                        tr(td(pc(a(href := "#streams-app-cmd", "Командный поток прикладного уровня"))),
                          td(pc(a(href := "#streams-app-tm", "Телеметрический поток прикладного уровня")))),
                        tr(td(pc("TCP")),
                          td(pc("UDP"))),
                        tr(td2(pc("802.11"))))))
                ), hr,


                h3(id := "streams-datalink", "Канальный уровень"),
                div(p("Канальный уровень состоит из передачи информационных кадров между абонентами. Размер информационного кадра определяется аппаратными возможностями."),
                  hr, h4(id := "streams-crypt-multiplex-packet", Frame.title), div(Frame.toHtml: _*)), hr,

                h3(id := "streams-multiplexing", "Мультиплексирование потоков"),
                div(p("При использовании мультиплексирования поток включает пакеты от каждого из обменных потоков."),
                  tableBordered(autoWidth,
                    tbody(tr(td(a(href := "#streams-multiplexing-data-packet", "Пакет обменного потока данных")),
                      td(a(href := "#streams-multiplexing-data-packet", "Пакет обменного потока данных")),
                      td("...")))), hr,
                  MultiplexingSection.toHtml),
                hr,

                h3(id := "streams-exchange", "Обменные потоки данных"),
                div(
                  p("Обменный поток делится на командный и на телеметрический обменный поток."),

                  h4(id := "streams-exchange-cmd", "Командный обменный поток данных"),
                  div(
                    p("Командный обменный поток данных имеет вид:"),
                    tableBordered(autoWidth,
                      tbody(
                        tr(
                          td(DelimiterSection.a),
                          td(CmdExchangeSection.a, " | ",
                            SetCounterSection.a, " | ",
                            ConfirmationSection.a),
                          td(a(href := "#streams-exchange-cmd-delimiter", "Разделитель командного обменного потока")),
                          td(a(href := "#streams-exchange-cmd-packet", "Адресный пакет командного обменного потока данных"),
                            " | ", a(href := "#streams-exchange-cmd-counter", "Пакет согласования счетчика"),
                            " | ", a(href := "#streams-exchange-cmd-confirmation", "Пакет подтверждения получения")),
                          td("...")))),
                    p("Разбор командного обменного потока производится по ", a(href := "#streams-delimiter-parsing",
                      "правилам разбора потоков с разделителем"), "."),
                    hr,
                    DelimiterSection.toHtml, hr,
                    CmdExchangeSection.toHtml, hr,
                    SetCounterSection.toHtml, hr,
                    ConfirmationSection.toHtml, hr,

                    h4(id := "streams-exchange-tm", "Телеметрический обменный поток данных"),
                    div(
                      p("Телеметрический обменный поток имеет вид:"),
                      tableBordered(autoWidth, tbody(tr(
                        td(TmSection.a),
                        td(TmSection.a),
                        td("...")))),
                      hr,
                      TmSection.toHtml)),
                  hr,
                  h3(id := "streams-app", "Поток данных прикладного уровня"),
                  div(
                    p("Поток данных прикладного уровня делится на командный и телеметрический."),
                    hr,

                    h4(id := "streams-app-cmd", "Командный поток данных прикладного уровня"),
                    div(
                      p("Входящий командный поток:"),
                      tableBordered(autoWidth, tbody(tr(
                        td(a(href := "#streams-app-cmd-packet", "Командное сообщение")),
                        td(a(href := "#streams-app-cmd-packet", "Командное сообщение")),
                        td("...")))),
                      p("Обратный командный поток:"),
                      tableBordered(autoWidth, tbody(tr(
                        td(a(href := "#streams-app-cmd-result", "Результат исполнения команды")),
                        td(a(href := "#streams-app-cmd-result", "Результат исполнения команды")),
                        td("...")))), hr,
                      CommandMessageSection.toHtml, hr,
                      CommandResultSection.toHtml),

                    hr,

                    h4(id := "streams-app-tm", "Телеметрический поток данных прикладного уровня"),
                    div(
                      p("ТМ-информация состоит из пакетов ТМ-сообщений. ТМ-сообщение может быть одного из двух типов:",
                        ul(
                          li("Событийное ТМ-сообщение предназначено для уведомления наземного комплекса управления (НКУ) о наступлении определенного события на борту, например о возникновении ошибки, смене режимов, выхода параметров за допустимые пределы, получении и исполнении команды и т.п. Событийные сообщения привязываются к точному бортовому времени возникновения события."),
                          li("Статусное ТМ-сообщение предназначено для передачи информации о внутреннем состоянии бортового комплекса и может формироваться как циклически, так и в соответствии с внутренней логикой работы компонентов."))),
                      p("ТМ-сообщение включает значения бортовых параметров в данных ТМ-сообщения. Перечень и тип передаваемых параметров определяается на основе базы данных бортовых параметров по номеру компонента и номеру ТМ-сообщения."),

                      p("Телеметрический исходящий поток:"),
                      tableBordered(autoWidth,
                        tbody(
                          tr(
                            td(EventTmMessageSection.a, " | ", StatusTmMessageSection.a),
                            td(EventTmMessageSection.a, " | ", StatusTmMessageSection.a),
                            td("...")))),
                      p("Обратный телеметрический поток имеет вид совпадающий с исходящим."),
                      EventTmMessageSection.toHtml, hr,
                      StatusTmMessageSection.toHtml)),
                  hr,

                  h3(id := "streams-combined", "Комбинированный уровень"),
                  div(
                    p("Комбинированный уровень стека протоколов это логическое представление компактного вида обменных потоков, который может быть использован в случае узкого или медленного канала связи, когда компактный вид покрывает все потребности и снижение накладных расходов при передаче данных имеет приоритет над гибкостью и расширяемостью."),
                    p("Пакет комбинированного уровня предназначен непосредственно для передачи данных трех обобщаемых уровней: уровня мультиплексирования, обменного и прикладного уровня."),
                    p("Поток комбинированного уровня:"),
                    tableBordered(autoWidth,
                      tbody(
                        tr(
                          td(CombinedCommandSection.a, " | ", CombinedCommandResultSection.a, " | ", CombinedTmSection.a),
                          td(CombinedCommandSection.a, " | ", CombinedCommandResultSection.a, " | ", CombinedTmSection.a),
                          td("...")))), hr,
                    CombinedCommandSection.toHtml, hr,
                    CombinedCommandResultSection.toHtml, hr,
                    CombinedTmSection.toHtml),
                  hr,

                  h3(id := "streams-delimiter-parsing", "Алгоритм разбора потока с разделителем"),
                  div(
                    p("Последовательность разбора потока с разделителем следующая:"),
                    ol(`type` := "A",
                      li("найти в потоке разделитель;",
                        li("попытаться прочесть пакет следующий сразу за разделителем:",
                          ol(`type` := "1",
                            li("если пакет считан успешно, то попытаться прочесть разделитель следующий сразу за пакетом:",
                              ol(`type` := "a",
                                li("если разделитель считан успешно, то продолжать разбор с пункта B;"),
                                li("если разделитель не был считан успешно, то попытаться считать пакет данных со смещением в длину разделителя от конца предыдущего пакета (разделитель может быть испорчен при передаче):",
                                  ol(`type` := "i",
                                    li("если пакет считан успешно, то продолжить с пункта 1;"),
                                    li("если пакет не был считан, то начать с самого начала (пункт A) в поисках нового разделителя."))))),
                            li("если пакет не был считан, то продолжить с пункта A.")))))),
                  hr)),
              raw(Resources.toString(Resources.getResource("raw.html"), Charsets.UTF_8)),
              hr,
              p(raw("&copy; 2015&ndash;2016 "), a(href := "http://acsl.mipt.ru/", "ЛПСУ"))
            )))

    new PrintWriter(args(0)) {
      write(fileContents)
      close()
    }
  }

  /*
<!--p>Общий вид потоков данных борт-земля и земля-борт совпадают. В общем информационном обмене участвуют 2 потока данных:
  <ul>
      <li>поток с подтверждением доставки, используется для доставки командных сообщений и результаты исполнения команд;
      <li>поток без подтверждения доставки, используется для передачи телеметрической информации.
  </ul>
<p>При наличии технической возможности и необходимости потоки могут быть разнесены по отдельным радиоканалам или радиосредствам, в частности, при наличии медленного, но надежного канала и быстрого, но ненадежного, потоки с управляющей информацией могут использовать более надежные каналы связи.
<p>При использовании одного канала для передачи потока с подтверждением и без подтверждения доставки призводится мультиплексирование потоков по определенным стандартом правилам.-->

<!-- Пакет запроса повтороной передачи используется только для управления потоком обеспечивающим гарантированную передачу (командный и обратный командному потоки). Пакет согласования счетчика пакетов используется в специальных случаях, например, с НКУ на борт в случае рассогласования обмена по командным потокам, все эти случаи и последовательность действий в таких случаях, а так же участие в них оператора должны быть явно регламентированы <sup><span class="label label-warning">будет уточнено в следующих редакциях</span></sup>.-->

<!--Он использует возможности активного обменного окна, что позволяет гарантировать получение пакета адресатом. До получения подтверждения доставки пакета в случае потока с гарантией на доставку отправитель обязан обеспечить хранение уже отправленных пакетов в рамках установленного активного окна обмена.
  <p>Для достижения максимальной скорости обмена и заполнения канала полезной информацией получатель должен обеспечить возможность получения и хранения количества пакетов равного как минимум размеру активного окна обмена.-->

<!-- Так же проверяется счетчик пакета &mdash; он должен быть на единицу больше предыдущего значения пакета (в случае последнего пакета со значением счетчика 65535 единственным допустимым значением является 0). В случае потока без гарантии на доставку пакета счетчик пакета не контролируется. В случае получения пакета согласования счетчика &mdash; его значение не сверяется с существующим, вместо этого текущее значение ожидаемого счетчика устанавливается в новое значение.-->
*/

}
