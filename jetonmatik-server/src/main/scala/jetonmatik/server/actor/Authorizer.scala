package jetonmatik.server.actor

import java.time.{Duration, ZoneOffset}

import akka.actor.{Props, ActorRef, Actor, ActorLogging}
import akka.pattern.ask
import akka.pattern.pipe
import akka.util.Timeout

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.duration._

object Authorizer {
  case class GenerateToken(
    clientId: String,
    scope: Set[String]) {
    require(clientId.nonEmpty)
  }

  case class Token(
    accessToken: String,
    scope: Set[String],
    expiresIn: Long
  ) {
    require(accessToken.nonEmpty)
    require(expiresIn > 0)
  }

  def props(
    clientStorage: ActorRef,
    accessTokenGenerator: ActorRef) = Props(
    new Authorizer(clientStorage, accessTokenGenerator)
      with LocalNowProvider)
}

class Authorizer(
  clientStorage: ActorRef,
  accessTokenGenerator: ActorRef)
  extends Actor
  with ActorLogging {

  self: NowProvider =>

  import jetonmatik.server.actor.Authorizer._

  override def receive: Receive = {
    case GenerateToken(clientId, scope) =>

      val originalSender = sender()

      val accessTokenRequestFuture = {

        implicit val timeout = Timeout(10.seconds)

        for {
          ClientStorage.ReadResult(Some(client)) <- clientStorage ? ClientStorage.Read(clientId)
        }
        yield {
          val resultScope = scope.intersect(client.scope)
          val issueTime = instant().atOffset(ZoneOffset.UTC)
          val expirationTime = issueTime.plus(client.tokenTtl)

          AccessTokenGenerator.GenerateAccessToken(
            clientId,
            resultScope,
            issueTime,
            expirationTime)
        }
      }

      val accessTokenFuture = {

        implicit val timeout = Timeout(5.seconds)

        for {
          accessTokenRequest <- accessTokenRequestFuture
          AccessTokenGenerator.AccessToken(accessToken) <- accessTokenGenerator ? accessTokenRequest
        } yield {
          val validityDuration = Duration.between(
            accessTokenRequest.issueTime,
            accessTokenRequest.expirationTime)

          assume(!validityDuration.isNegative)

          Token(
            accessToken = accessToken,
            accessTokenRequest.scope,
            expiresIn = validityDuration.getSeconds
          )
        }
      }

      accessTokenFuture pipeTo originalSender
  }
}
