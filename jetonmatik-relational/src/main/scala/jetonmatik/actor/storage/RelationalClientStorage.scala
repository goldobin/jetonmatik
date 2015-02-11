package jetonmatik.actor.storage

import akka.actor._
import jetonmatik.relational.Connection

object RelationalClientStorage {
  def props(connection: Connection) = Props(new RelationalStorage with RelationalStorageWorkerProvider {
    override def relationalStorageWorkerProps: Props = RelationalClientStorageWorker.props(connection)
  })
}

object RelationalClientStorageWorker {
  def props(connection: Connection) = Props(new RelationalClientStorageWorker(connection))
}

class RelationalClientStorageWorker(connection: Connection) extends RelationalStorageWorker with ActorLogging {
  import connection.api._
  import connection.tables._

  import Storage._
  import ClientStorage._

  override def receiveStorageOperation: Receive = {
    case LoadClient(clientId) =>
      val clientOption = connection.db.withSession { implicit session =>
        {
          for (c <- clients if c.id === clientId) yield c
        }.firstOption
      } map (_.toClient()) map { case (client, _) => client }

      sender() ! ClientLoaded(clientOption)

    case SaveClient(_) =>
      log.warning("Save client operation is not supported at the moment")
      sender() ! OperationFailed
  }
}