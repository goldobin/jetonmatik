package jetonmatik.relational

import com.mchange.v2.c3p0.ComboPooledDataSource

class Connection (val settings: RelationalStorageSettings){

  val driver = RdbmsType.toDriver(settings.rdbmsType)
  val api = driver.simple
  import api._

  private lazy val ds = {
    val ds = new ComboPooledDataSource
    ds.setDriverClass(RdbmsType.toJdbcDriverName(settings.rdbmsType))
    ds.setJdbcUrl(settings.url)
    ds.setUser(settings.user)
    ds.setPassword(settings.password)
    ds.setInitialPoolSize(settings.initialPoolSize)
    ds.setMaxPoolSize(settings.maxPoolSize)
    ds
  }

  lazy val db = Database.forDataSource(ds)

  val tables = Tables(driver)
}


object Connection {
  def apply(settings: RelationalStorageSettings) = new Connection(settings)
}
