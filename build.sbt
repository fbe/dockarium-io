name := """dockarium"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  ws
)

libraryDependencies += "io.spray" % "spray-can_2.11" % "1.3.2"

libraryDependencies ++= Seq(
  "org.webjars" %% "webjars-play" % "2.3.0-2",
  "org.webjars" % "bootstrap" % "3.3.2",
  "org.webjars" % "angularjs" % "1.3.11",
  "org.webjars" % "reconnecting-websocket" % "23d2fbc",
  "org.webjars" % "font-awesome" % "4.3.0-1",
  "org.webjars" % "angular-ui-bootstrap" % "0.12.0"
)