package jetonmatik.relational

import com.typesafe.config.Config
import jetonmatik.StorageSettings

trait RelationalStorageSettings extends StorageSettings {
  import jetonmatik.relational.RdbmsType._

  override val readonly: Boolean = true

  def url: String
  def user: String
  def password: String

  val initialPoolSize: Int
  val maxPoolSize: Int

  val rdbmsType: RdbmsType
}

class DefaultRelationalStorageSettings(config: Config) extends RelationalStorageSettings {
  import jetonmatik.relational.RdbmsType._

  lazy val url: String = config.getString("url")
  lazy val user: String = config.getString("user")
  lazy val password: String = config.getString("pass")

  lazy val initialPoolSize: Int = config.getInt("initial-pool-size")
  lazy val maxPoolSize: Int = config.getInt("max-pool-size")

  private lazy val (_ :: rdbmsTypeName :: _) = url.split(":").toList

  lazy val rdbmsType: RdbmsType = RdbmsType.withName(rdbmsTypeName)
}