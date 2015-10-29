val jetBrainsAnnotations = "org.jetbrains" % "annotations" % "13.0"
val guava = "com.google.guava" % "guava" % "18.0"
val commonsUtil = "commons-util" % "commons-util" % "final"
val commonsIo = "commons-io" % "commons-io" % "2.4"
val commonsLang = "org.apache.commons" % "commons-lang3" % "3.3.2"

lazy val commonSettings = Seq(
  organization := "ru.mipt.acsl",
  version := "0.1.0",
  scalaVersion := "2.11.7",
  libraryDependencies ++= Seq(jetBrainsAnnotations, guava, commonsUtil, commonsIo, commonsLang)
)

lazy val common = project.
  settings(commonSettings: _*).
  settings(
    name := "decode-common"
  )

val parboiled = "org.parboiled" % "parboiled-java" % "1.1.7"
val args4j = "args4j" % "args4j" % "2.0.29"

lazy val model = project.
  settings(commonSettings: _*).
  settings(
    name := "decode-model",
    libraryDependencies ++= Seq(parboiled, args4j)
  ).
  dependsOn(common)

lazy val javaSourcesGenerator = (project in file("java-sources-generator")).
  settings(commonSettings: _*).
  settings(
    name := "decode-java-sources-generator"
  ).
  dependsOn(model)

val jdom = "org.jdom" % "jdom2" % "2.0.6"

lazy val mavlinkSourcesGenerator = (project in file("mavlink-sources-generator")).
  settings(commonSettings: _*).
  settings(
    name := "decode-mavlink-sources-geneerator",
    libraryDependencies ++= Seq(args4j, jdom)
  ).
  dependsOn(common)

val logback = "ch.qos.logback" % "logback-classic" % "1.1.2"

lazy val parser = project.
  settings(commonSettings: _*).
  settings(
    name := "decode-parser",
    libraryDependencies ++= Seq(parboiled, logback)
  ).
  dependsOn(model)

lazy val root = project.
  aggregate(common, javaSourcesGenerator, mavlinkSourcesGenerator, model, parser).
  settings(commonSettings: _*).
  settings(
    name := "decode"
  )
