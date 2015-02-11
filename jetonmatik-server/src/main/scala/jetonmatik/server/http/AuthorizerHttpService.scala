package jetonmatik.server.http

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import jetonmatik.actor.{Authorizer, Authenticator}
import jetonmatik.model.{ClientCredentials, Client}
import jetonmatik.provider.FormattedPublicKeyProvider
import jetonmatik.server.model.oauth.{TokenResponse, TokenRequest}
import spray.routing._
import spray.routing.authentication.{BasicAuth, UserPass}
import spray.routing.directives._

import scala.concurrent.duration._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import language._

trait AuthorizerHttpService extends HttpService {
  this: FormattedPublicKeyProvider =>

  import Authorizer._
  import Authenticator._

  implicit val timeout = Timeout(5.seconds)

  val realm: String

  import FormFieldDirectives._

  def fetchTokenRequest: Directive1[TokenRequest] =
    formFields(
      'grant_type,
      'scope?).as(TokenRequest)


  def authorizerRoute(authenticator: ActorRef, authorizer: ActorRef): Route = {

    def clientAuthenticator(userPass: Option[UserPass]): Future[Option[Client]] = userPass match {
      case Some(up) =>
        for (response <- authenticator ? Authenticate(ClientCredentials(up.user, up.pass))) yield response match {
          case Authenticated(clientOption) => clientOption
          case _ => None
        }

      case None =>
        Future.successful(None)
    }

    path("token") {
      authenticate(BasicAuth(clientAuthenticator _, realm = realm)) { client =>
        (get | post) {
          fetchTokenRequest { request: TokenRequest =>
            onSuccess(authorizer ? Authorize(client, request.scopeSet)) {
              case Authorized(accessToken, scope, expiresIn) =>

                val scopeString = scope.mkString(" ")
                val scopeOption = if (scopeString.nonEmpty) Some(scopeString) else None

                import jetonmatik.json4s.Implicits._

                val tokenResponse = TokenResponse.Token(
                  "Bearer",
                  accessToken,
                  scopeOption,
                  expiresIn)

                complete(tokenResponse)
            }
          }
        }
      }
    } ~
    path("public-key") {
      get {
        complete(formattedPublicKey)
      }
    }
  }
}
