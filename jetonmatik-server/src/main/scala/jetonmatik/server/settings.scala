package jetonmatik.server

import java.nio.file.{Paths, Path}
import java.time.Duration
import java.util.concurrent.TimeUnit

import jetonmatik.server.model.Client
import com.typesafe.config.{ConfigFactory, Config}
import scala.collection.JavaConverters._

trait EndpointSettings {
  val interface: String
  val port: Int
}

class DefaultEndpointSettings(config: Config) extends EndpointSettings {
  override val interface: String = config.getString("interface")
  override val port: Int = config.getInt("port")
}

trait KeyStoreSettings {
  val password: String
  val path: Path
  val keys: Map[String, String]
}

class DefaultKeyStoreSettings(config: Config) extends KeyStoreSettings {
  override val password: String = config.getString("password")
  override val path: Path = Paths.get(config.getString("path"))
  override val keys: Map[String, String] = {
    for (c <- config.getConfigList("keys").asScala)
    yield c.getString("alias") -> c.getString("password")
  }.toMap
}

trait ServerSettings {
  val realm: String
  val externalUrl: String
  val signatureKeyPairAlias: String

  val endpoint: EndpointSettings

  val tokenGeneratorPoolSize: Int

  val clientsFile: Option[Path]

  val keyStore: KeyStoreSettings
}

object ServerSettings extends ServerSettings {
  private val config: Config = ConfigFactory.load().getConfig("jetonmatik").getConfig("server")

  override val realm: String = config.getString("realm")
  override val externalUrl: String = config.getString("external-url")
  override val signatureKeyPairAlias: String = config.getString("signature-key-pair-alias")

  override val endpoint: EndpointSettings = new DefaultEndpointSettings(config.getConfig("endpoint"))

  override val tokenGeneratorPoolSize: Int = config.getInt("token-generator-pool-size")

  override val clientsFile: Option[Path] = {
    if (config.hasPath("clients-file"))
      Some(Paths.get(config.getString("clients-file")))
    else
      None
  }

  override val keyStore: KeyStoreSettings = new DefaultKeyStoreSettings(config.getConfig("key-store"))
}
