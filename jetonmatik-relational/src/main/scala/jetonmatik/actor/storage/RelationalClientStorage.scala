package jetonmatik.actor.storage

import java.io.IOException
import java.sql.SQLException

import akka.actor.SupervisorStrategy.{Escalate, Stop}
import akka.actor._
import jetonmatik.actor.storage.ClientStorage.StorageFailure
import jetonmatik.relational.Tables
import jetonmatik.relational.Connection

object RelationalClientStorage {
  def props(connection: Connection) = Props(new RelationalClientStorage with RelationalClientStorageWorkerProvider {
    override def relationalClientStorageWorkerProps: Props = RelationalClientStorageWorker.props(connection)
  })
}

class RelationalClientStorage() extends Actor with ActorLogging {
  this: RelationalClientStorageWorkerProvider =>

  var senderMap: Map[ActorRef, ActorRef] = Map.empty

  def receive: Receive = {
    case Terminated(worker) =>
      context.unwatch(worker)
      senderMap -= worker

    case msg =>
      val worker = context.actorOf(relationalClientStorageWorkerProps)
      context.watch(worker)
      worker forward msg
  }

  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
    case e @ (_: SQLException | _: IOException) =>
      log.error(e, "Failed to perform DB operation")

      val worker = sender()

      for (originalSender <- senderMap.get(worker)) {
        originalSender ! StorageFailure
      }

      Stop

    case _ => Escalate
  }
}

trait RelationalClientStorageWorkerProvider {
  def relationalClientStorageWorkerProps: Props
}

object RelationalClientStorageWorker {
  def props(connection: Connection) = Props(new RelationalClientStorageWorker(connection))
}

class RelationalClientStorageWorker(connection: Connection) extends Actor with ActorLogging {
  import connection.api._
  import connection.tables._

  import ClientStorage._

  override def receive: Receive = {
    case LoadClient(clientId) =>
      val clientOption = connection.db.withSession { implicit session =>
        {
          for (c <- clients if c.id === clientId) yield c
        }.firstOption
      } map (_.toClient()) map { case (client, _) => client }

      sender() ! ClientLoaded(clientOption)
      context.stop(self)

    case SaveClient(_) =>
      log.warning("Save client operation is not supported at the moment")
      sender() ! StorageFailure
  }
}