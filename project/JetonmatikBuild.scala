import sbt.Keys._
import sbt._
import spray.revolver.RevolverPlugin._

object JetonmatikBuild extends Build {

  import Packaging._

  lazy val commonSettings = Defaults.coreDefaultSettings ++ Seq(
    organization  := "io.github.goldobin",
    version       := "0.1-SNAPSHOT",
    scalaVersion  := Dependencies.Versions.scalaVersion
  )

  lazy val root = Project(
    id = "jetonmatik",
    base = file("."),
    aggregate = Seq(core, relational, server),
    settings = addCommandAlias("relational-schema", "jetonmatik-relational/runMain jetonmatik.relational.Schema")
  )

  lazy val server = Project(
    id = "jetonmatik-server",
    base = file("jetonmatik-server"),
    dependencies = Seq(
      core % "compile;test->test",
      relational % "compile"
    ),
    settings = commonSettings ++ Revolver.settings ++ serverPackageSettings ++ Seq(
      mainClass in Compile := Some("jetonmatik.server.Boot"),
      libraryDependencies ++= Dependencies.server
    )
  )

  lazy val core = Project(
    id = "jetonmatik-core",
    base = file("jetonmatik-core"),
    settings = commonSettings ++ Seq(
      libraryDependencies ++= Dependencies.core
    )
  )

  lazy val relational = Project(
    id = "jetonmatik-relational",
    base = file("jetonmatik-relational"),
    dependencies = Seq(
      core % "compile;test->test"
    ),
    settings = commonSettings ++ Seq(
      libraryDependencies ++= Dependencies.relational
    )
  )
}
