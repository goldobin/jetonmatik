package jetonmatik.server.actor

import java.util.concurrent.TimeoutException

import akka.actor.{ActorLogging, ActorRef, Actor, Props}
import akka.pattern.ask
import akka.util.Timeout
import jetonmatik.util.PasswordHash

import scala.util.control.NonFatal
import scala.concurrent.ExecutionContext.Implicits._
import scala.util.{Success, Failure}
import scala.concurrent.duration._

object Authenticator {
  case class Authenticate(
    clientId: String,
    clientSecret: String
  )

  case class Authentication(
    authenticated: Boolean
  )

  def props(
    clientStorage: ActorRef): Props = Props(new Authenticator(clientStorage))
}

class  Authenticator(
  clientStorage: ActorRef) extends Actor with ActorLogging {

  import jetonmatik.server.actor.Authenticator._
  import jetonmatik.server.actor.ClientStorage._

  override def receive: Receive = {
    case Authenticate(clientId, clientSecret) =>

      val originalSender = sender()

      implicit val timeout = Timeout(10.seconds)

      (clientStorage ? Read(clientId)) map {
        case ReadResult(Some(client)) => PasswordHash.validatePassword(clientSecret, client.clientSecretHash)
        case _ => false
      } onComplete {
        case Success(authenticated) => originalSender ! Authentication(authenticated = authenticated)
        case Failure(t) =>
          t match {
            case e: TimeoutException =>
              log.error(e, s"The request to for client=$clientId to client storage was timed out.")
            case NonFatal(e) =>
              log.error(e, s"An error occurred while authenticating client=$clientId")
          }

          originalSender ! Authentication(authenticated = false)
      }
  }
}

