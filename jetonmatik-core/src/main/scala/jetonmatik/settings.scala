package jetonmatik

import java.nio.file.{Paths, Path}

import com.typesafe.config.Config

import scala.collection.JavaConverters._

trait StorageSettings {
  val readonly: Boolean
}

trait FileStorageSettings extends StorageSettings {
  override val readonly: Boolean = true
  val file: Path
}

class DefaultFileStorageSettings(config: Config) extends FileStorageSettings {
  override val file: Path = Paths.get(config.getString("file"))
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
