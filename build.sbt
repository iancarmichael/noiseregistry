import com.github.play2war.plugin._

name := """noiseregistry"""

version := "1.0-SNAPSHOT"

Play2WarPlugin.play2WarSettings

Play2WarKeys.servletVersion := "3.0"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.11.1"

resolvers += "Postgres Repository" at "http://mvnrepository.com/artifact"

libraryDependencies ++= Seq(
  "org.postgresql" % "postgresql" % "9.3-1102-jdbc41",
  javaJpa,
  "org.hibernate" % "hibernate-entitymanager" % "4.3.7.Final",
  cache,
  javaWs
)
