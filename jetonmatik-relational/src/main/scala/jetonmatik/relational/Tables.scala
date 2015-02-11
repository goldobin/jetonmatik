package jetonmatik.relational

import java.sql.Timestamp
import java.time.{OffsetDateTime, ZoneOffset}

import scala.slick.driver.{JdbcProfile, MySQLDriver, PostgresDriver}

class Tables(val driver: JdbcProfile) {
  import driver.simple._

  implicit val offsetDateTimeColumnType = MappedColumnType.base[OffsetDateTime, Timestamp](
    { offsetDateTime => Timestamp.from(offsetDateTime.toInstant) },
    { timestamp => OffsetDateTime.ofInstant(timestamp.toInstant, ZoneOffset.UTC)}
  )

  class ClientTable(tag: Tag) extends Table[StoredClient](tag, "CLIENT") {

    def id = column[String]("ID", O.PrimaryKey)
    def secretHash = column[String]("SECRET_HASH")
    def scope = column[String]("SCOPE")
    def tokenTtl = column[Long]("TOKEN_TTL")
    def lastModified = column[OffsetDateTime]("LAST_MODIFIED")
    def version = column[Long]("VERSION")

    def * = (id, secretHash, scope, tokenTtl, lastModified, version) <> (StoredClient.tupled, StoredClient.unapply)
  }

  lazy val clients = TableQuery[ClientTable]

  lazy val schema = clients.schema
}


object Tables {

  private val cachedTables: Map[JdbcProfile, Tables] = Map(
    MySQLDriver -> new Tables(MySQLDriver),
    PostgresDriver -> new Tables(PostgresDriver)
  )

  import jetonmatik.relational.RdbmsType._
  def apply(driver: JdbcProfile): Tables = cachedTables.getOrElse(driver, new Tables(driver))
  def apply(rdbmsType: RdbmsType): Tables = apply(toDriver(rdbmsType))
}

