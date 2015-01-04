import com.typesafe.sbt.SbtNativePackager._
import com.typesafe.sbt.SbtNativePackager.autoImport._
import sbt.Keys._
import sbt._

object Packaging {
  import NativePackagerKeys._

  private lazy val commonPackageSettings = packageArchetype.java_application ++ Seq(
    publishArtifact in Universal in(Compile, packageSrc) := false,
    publishArtifact in Universal in(Compile, packageDoc) := false
  )

  lazy val dockerPackageSettings = Seq(
    maintainer in Docker := "Oleksandr Goldobin <alex.goldobin@gmail.com>",
    dockerBaseImage := "dockerfile/java:oracle-java8",
    dockerRepository := Some("goldobin"),
    dockerUpdateLatest := true
  )

  lazy val serverPackageSettings = commonPackageSettings ++ dockerPackageSettings ++ Seq(
    dockerExposedPorts in Docker := Seq(8080),
    bashScriptExtraDefines +=
      """
        |addJava "-Djetonmatik.server.external-url=$JETONMATIK_EXTERNAL_URL"
        |addJava "-Xmx512m"
        |addJava "-Xms512m"
        |""".stripMargin
  )
}