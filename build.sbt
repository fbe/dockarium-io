import sbt.Project.projectToRef

lazy val clients = Seq(dockariumClient)
lazy val scalaV = "2.11.5"

lazy val dockariumServer = (project in file("server")).settings(
  resolvers += Resolver.url("scala-js-releases", url("http://dl.bintray.com/content/scala-js/scala-js-releases"))(Resolver.ivyStylePatterns),
  scalaVersion := scalaV,
  scalaJSProjects := clients,
  pipelineStages := Seq(scalaJSProd/*, gzip*/),
  libraryDependencies ++= Seq(
    jdbc,
    anorm,
    cache,
    ws,
    "com.vmunier" %% "play-scalajs-scripts" % "0.1.0",
    "org.webjars" % "jquery" % "1.11.1",
    "org.webjars" %% "webjars-play" % "2.3.0-2",
    "org.webjars" % "bootstrap" % "3.3.2",
    "org.webjars" % "angularjs" % "1.3.11",
    "org.webjars" % "reconnecting-websocket" % "23d2fbc",
    "org.webjars" % "font-awesome" % "4.3.0-1",
    "org.webjars" % "angular-ui-bootstrap" % "0.12.0",
    "org.webjars" % "angular-ui-router" % "0.2.13",
    "io.spray" % "spray-can_2.11" % "1.3.2",
    "org.scalajs" %% "scalajs-pickling-play-json" % "0.3.1"
  ),
  EclipseKeys.skipParents in ThisBuild := false).
  enablePlugins(PlayScala).
  aggregate(clients.map(projectToRef): _*).
  dependsOn(dockariumSharedJvm)

lazy val dockariumClient = (project in file("client")).settings(
  scalaVersion := scalaV,
  persistLauncher := true,
  persistLauncher in Test := false,
  sourceMapsDirectories += dockariumSharedJs.base / "..",
  unmanagedSourceDirectories in Compile := Seq((scalaSource in Compile).value),
  libraryDependencies ++= Seq(
    "org.scala-js" %%% "scalajs-dom" % "0.8.0",
    "com.greencatsoft" %%% "scalajs-angular" % "0.3",
    "org.scalajs" %%% "scalajs-pickling" % "0.4-SNAPSHOT"
  )).
  enablePlugins(ScalaJSPlugin, ScalaJSPlay).
  dependsOn(dockariumSharedJs)

lazy val dockariumShared = (crossProject.crossType(CrossType.Pure) in file("shared")).
  settings(scalaVersion := scalaV).
  jsConfigure(_ enablePlugins ScalaJSPlay).
  jsSettings(sourceMapsBase := baseDirectory.value / "..")

lazy val dockariumSharedJvm = dockariumShared.jvm
lazy val dockariumSharedJs = dockariumShared.js

// loads the jvm project at sbt startup
onLoad in Global := (Command.process("project dockariumServer", _: State)) compose (onLoad in Global).value