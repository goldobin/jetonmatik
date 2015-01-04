package jetonmatik.server

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.io.IO
import akka.util.Timeout
import jetonmatik.server.actor.RouteListener
import org.slf4j.LoggerFactory
import spray.can.Http
import scala.concurrent.duration._
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global


object Boot extends App {

  val logger = LoggerFactory.getLogger(this.getClass)

  implicit val actorSystem: ActorSystem = ActorSystem("authorizer")
  implicit val timeout = Timeout(1.minute)

  val endpointActor = actorSystem.actorOf(
    RouteListener.props(ServerSettings),
    "route-listener")

  logger.info("Starting Authorizer...")

  val bind = IO(Http) ? Http.Bind(
    endpointActor,
    interface = ServerSettings.endpoint.interface,
    port = ServerSettings.endpoint.port)

  bind.onComplete {
    case Success(_) => logger.info("Authorizer started")
    case Failure(t) => logger.error("Authorizer is failed to start", t)
  }
}
