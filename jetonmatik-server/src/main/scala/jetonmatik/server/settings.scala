package jetonmatik.server

import com.typesafe.config.{ConfigFactory, Config}
import jetonmatik.relational.DefaultRelationalStorageSettings
import jetonmatik.{DefaultKeyStoreSettings, KeyStoreSettings, DefaultFileStorageSettings, StorageSettings}

trait EndpointSettings {
  val interface: String
  val port: Int
}

class DefaultEndpointSettings(config: Config) extends EndpointSettings {
  override val interface: String = config.getString("interface")
  override val port: Int = config.getInt("port")
}

trait ServerSettings {
  val realm: String
  val externalUrl: String
  val signatureKeyPairAlias: String

  val endpoint: EndpointSettings

  val tokenGeneratorPoolSize: Int

  val keyStore: KeyStoreSettings

  val storage: StorageSettings
}

object ServerSettings extends ServerSettings {
  private val config: Config = ConfigFactory.load().getConfig("jetonmatik").getConfig("server")

  override val realm: String = config.getString("realm")
  override val externalUrl: String = config.getString("external-url")
  override val signatureKeyPairAlias: String = config.getString("signature-key-pair-alias")

  override val endpoint: EndpointSettings = new DefaultEndpointSettings(config.getConfig("endpoint"))
  override val tokenGeneratorPoolSize: Int = config.getInt("token-generator-pool-size")
  override val keyStore: KeyStoreSettings = new DefaultKeyStoreSettings(config.getConfig("key-store"))
  override val storage: StorageSettings = {
    val storageConfig = config.getConfig("storage")
    val storageType = storageConfig.getString("storage-type")

    storageType match {
      case "memory" =>
        new DefaultFileStorageSettings(storageConfig)
      case "relational" =>
        new DefaultRelationalStorageSettings(storageConfig)
      case _ =>
        throw new RuntimeException(s"Unsupported storage type: $storageType")
    }
  }
}
