import scala.collection.Seq

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.7.3"

lazy val root = (project in file("."))
  .settings(
    name := "MarimoTest"
  )

val http4sVersion = "0.23.33"
val skuberVersion = "4.0.11"
val fabric8Version = "7.4.0"
val doobieVersion = "1.0.0-RC10"

libraryDependencies ++= Seq(
  "org.http4s" %% "http4s-ember-client" % http4sVersion,
  "org.http4s" %% "http4s-ember-server" % http4sVersion,
  "org.http4s" %% "http4s-dsl" % http4sVersion,
  "org.http4s" %% "http4s-jdk-http-client" % "0.10.0",
  "io.fabric8" % "kubernetes-client" % fabric8Version,
  "com.google.inject" % "guice" % "7.0.0",
  "org.flywaydb" % "flyway-core" % "11.15.0",
  "org.flywaydb" % "flyway-database-postgresql" % "11.15.0",
  "com.typesafe" % "config" % "1.4.5",
  "org.tpolecat" %% "doobie-core" % doobieVersion,
  "org.tpolecat" %% "doobie-postgres" % doobieVersion,
)
