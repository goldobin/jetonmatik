import sbt.Keys._
import sbt._

object JetonmatikBuild extends Build {

  import Packaging._

  lazy val buildSettings = Defaults.coreDefaultSettings ++ Seq(
    organization  := "io.github.goldobin",
    version       := "0.1-SNAPSHOT",
    scalaVersion  := Dependencies.Versions.scalaVersion,

    resolvers     += "spray repo" at "http://repo.spray.io"
  )

  lazy val root = Project(
    id = "jetonmatik",
    base = file("."),
    aggregate = Seq(server)
  )

  lazy val server = Project(
    id = "jetonmatik-server",
    base = file("jetonmatik-server"),
    settings = buildSettings ++ serverPackageSettings ++ Seq(
      libraryDependencies ++= Dependencies.server,
      mainClass := Some("jetonmatik.server.Boot")
    )
  )
}
