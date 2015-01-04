import sbt._

object Dependencies {
  object Versions {
    val scalaVersion      = "2.11.4"
    val scalaTestVersion  = "2.2.3"

    val akkaVersion       = "2.3.8"
    val sprayVersion      = "1.3.2"
    val json4sVersion     = "3.2.11"
    val slf4jVersion      = "1.7.7"
  }

  object Compile {
    import Versions._

    val akkaActor       = "com.typesafe.akka"     %%  "akka-actor"            % akkaVersion

    val sprayCan        = "io.spray"              %%  "spray-can"             % sprayVersion
    val sprayClient     = "io.spray"              %%  "spray-client"          % sprayVersion
    val sprayRouting    = "io.spray"              %%  "spray-routing"         % sprayVersion

    val json4sNative    = "org.json4s"            %%  "json4s-native"         % json4sVersion

    val akkaSlf4j       = "com.typesafe.akka"     %%  "akka-slf4j"            % akkaVersion
    val slf4j           = "org.slf4j"             %   "slf4j-api"             % slf4jVersion
    val logback         = "ch.qos.logback"        %   "logback-classic"       % "1.1.2"

    val nimbusJoseJwt   = "com.nimbusds"          %   "nimbus-jose-jwt"       % "3.1.2"
    val snakeYaml       = "org.yaml"              % "snakeyaml"               % "1.14"
  }

  object Test {
    import Versions._

    val mockito        = "org.mockito"            %  "mockito-all"    % "1.9.5"           % "test"
    val scalatest      = "org.scalatest"          %% "scalatest"      % scalaTestVersion  % "test"
    val akkaTestKit    = "com.typesafe.akka"      %% "akka-testkit"   % akkaVersion       % "test"
    val sprayTestKit   = "io.spray"               %% "spray-testkit"  % sprayVersion      % "test"

    val javaFaker      = "com.github.javafaker"   %   "javafaker"     % "0.3"             % "test" exclude ("org.slf4j", "slf4j-simple")
  }

  import Compile._

  lazy val server = Seq(
    akkaActor,
    akkaSlf4j, 
    slf4j,
    logback,
    sprayCan,
    sprayRouting,
    json4sNative,
    nimbusJoseJwt,
    snakeYaml,
    Test.mockito,
    Test.scalatest,
    Test.akkaTestKit,
    Test.sprayTestKit,
    Test.javaFaker
  )  
}
