name := "short-url"

version := "0.1"

scalaVersion := "2.13.1"

val slf4jVersion = "2.0.0-alpha1"
val tapirVersion = "0.12.18"
val doobieVersion = "0.8.6"
val monixVersion = "3.1.0"
val pureConfigVersion = "0.12.2"
val log4catsVersion = "1.0.1"
val http4sVersion = "0.21.0-M6"
val testcontainersVersion = "0.35.0"
val flywayVersion = "6.2.1"
val scalaTestVersion = "3.0.8"
val http4sVerion = "0.21.0-RC2"
val newtypeVersion = "0.4.3"

libraryDependencies ++= Seq(
  "io.monix" %% "monix-eval" % monixVersion,
  "org.tpolecat" %% "doobie-core" % doobieVersion,
  "org.tpolecat"%% "doobie-hikari" % doobieVersion,
  "org.tpolecat" %% "doobie-postgres" % doobieVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % tapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-openapi-docs" % tapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-openapi-circe-yaml" % tapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-http4s" % tapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % tapirVersion,
  "org.http4s" %% "http4s-client" % http4sVerion,
  "org.http4s" %% "http4s-circe" % http4sVersion,
  "org.slf4j" % "slf4j-api" % slf4jVersion,
  "org.slf4j" % "slf4j-log4j12" % slf4jVersion,
  "com.github.pureconfig" %% "pureconfig" % pureConfigVersion,
  "io.chrisdavenport" %% "log4cats-core" % log4catsVersion,
  "io.chrisdavenport" %% "log4cats-slf4j" % log4catsVersion,
  "org.flywaydb" % "flyway-core" % flywayVersion,
  "io.estatico" %% "newtype" % newtypeVersion,
  "com.dimafeng" %% "testcontainers-scala-scalatest" % testcontainersVersion % Test,
  "com.dimafeng" %% "testcontainers-scala-postgresql" % testcontainersVersion % Test,
  "org.scalatest" %% "scalatest" % scalaTestVersion % Test
)

scalacOptions ++= Seq("-Ymacro-annotations")

addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.11.0" cross CrossVersion.full)

enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)
enablePlugins(AshScriptPlugin)

mainClass in Compile := Some("ru.finochenko.short.url.Server")

dockerBaseImage := "openjdk:jre-alpine"