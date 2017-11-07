name := "calendar"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.3"

val reactiveMongoVer = "0.12.6-play26"

libraryDependencies ++= Seq(
  ws,
  guice,
  "org.reactivemongo"      %% "play2-reactivemongo" % reactiveMongoVer,
  "io.swagger"             %% "swagger-play2"       % "1.6.0",
  "org.webjars"            %  "swagger-ui"          % "3.2.2",
  "org.scalatestplus.play" %% "scalatestplus-play"  % "3.1.1" % Test
)

import play.sbt.routes.RoutesKeys

RoutesKeys.routesImport += "play.modules.reactivemongo.PathBindables._"
CoffeeScriptKeys.bare := true