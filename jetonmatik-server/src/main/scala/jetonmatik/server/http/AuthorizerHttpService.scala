package jetonmatik.server.http

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import jetonmatik.server.actor.{Authorizer, Authenticator}
import jetonmatik.server.model.oauth.{TokenResponse, TokenRequest}
import spray.routing._
import spray.routing.authentication.{BasicAuth, UserPass}
import spray.routing.directives._

import scala.concurrent.duration._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global


trait AuthorizerHttpService extends HttpService {
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

    def clientAuthenticator(userPass: Option[UserPass]): Future[Option[String]] = userPass match {
      case Some(up) =>
        authenticator ? Authenticate(up.user, up.pass) map {
          case Authentication(true) => Some(up.user)
          case Authentication(false) => None
        }
      case None => Future.successful(None)
    }

    path("token") {
      authenticate(BasicAuth(clientAuthenticator _, realm = realm)) { clientId =>
        (get | post) {
          fetchTokenRequest { request: TokenRequest =>
            onSuccess(authorizer ? GenerateToken(clientId, request.scopeSet)) {
              case Token(accessToken, scope, expiresIn) =>

                val scopeString = scope.mkString(" ")
                val scopeOption = if (scopeString.nonEmpty) Some(scopeString) else None

                import jetonmatik.json4s.Implicits._

                complete(
                  TokenResponse.Token(
                    "Bearer",
                    accessToken,
                    scopeOption,
                    expiresIn))
            }
          }
        }
      }
    } ~
    path("public-key") {
      get {
        complete {
          (authorizer ? RetrieveFormattedPublicKey) map {
            case FormattedPublicKey(key) => key
          }
        }
      }
    }
  }
}
