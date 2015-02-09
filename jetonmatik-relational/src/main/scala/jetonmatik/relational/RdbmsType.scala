package jetonmatik.relational

import scala.slick.driver.{JdbcDriver, MySQLDriver, PostgresDriver}

object RdbmsType extends Enumeration {
  type RdbmsType = Value
  val MySql = Value("mysql")
  val MariaDb = Value("mariadb")
  val Postgres = Value("postgres")

  def toDriver(rdbmsType: RdbmsType): JdbcDriver = rdbmsType match {
    case MySql => MySQLDriver
    case MariaDb => MySQLDriver
    case Postgres => PostgresDriver
  }

  def toJdbcDriverName(rdbmsType: RdbmsType): String = rdbmsType match {
    case MySql => "com.mysql.jdbc.Driver"
    case MariaDb => "org.mariadb.jdbc.Driver"
    case Postgres => "org.postgresql.Driver"
  }
}
