import sbt.Keys._
import sbt._
import spray.revolver.RevolverPlugin._

object JetonmatikBuild extends Build {

  import Packaging._

  lazy val buildSettings = Defaults.coreDefaultSettings ++ Seq(
    organization  := "io.github.goldobin",
    version       := "0.1-SNAPSHOT",
    scalaVersion  := Dependencies.Versions.scalaVersion
  )

  lazy val root = Project(
    id = "jetonmatik",
    base = file("."),
    aggregate = Seq(server)
  )

  lazy val server = Project(
    id = "jetonmatik-server",
    base = file("jetonmatik-server"),
    settings = buildSettings ++ Revolver.settings ++ serverPackageSettings ++ Seq(
      mainClass in Compile := Some("jetonmatik.server.Boot"),
      libraryDependencies ++= Dependencies.server
    )
  )
}
