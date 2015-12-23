val jetBrainsAnnotations = "org.jetbrains" % "annotations" % "13.0"
val guava = "com.google.guava" % "guava" % "18.0"
val commonsUtil = "commons-util" % "commons-util" % "final"
val commonsIo = "commons-io" % "commons-io" % "2.4"
val commonsLang = "org.apache.commons" % "commons-lang3" % "3.3.2"

lazy val commonSettings = Seq(
  organization := "ru.mipt.acsl",
  version := "0.1.0-SNAPSHOT",
  scalaVersion := "2.11.7",
  libraryDependencies ++= Seq(jetBrainsAnnotations, guava, commonsUtil, commonsIo, commonsLang)
)

lazy val common = RootProject(file("common"))

val parboiled = "org.parboiled" % "parboiled-java" % "1.1.7"
val args4j = "args4j" % "args4j" % "2.0.29"

lazy val root = (project in file(".")).
  settings(commonSettings: _*).
  settings(
    name := "decode-model",
    libraryDependencies ++= Seq(parboiled, args4j)
  ).
  dependsOn(common)