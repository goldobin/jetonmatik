package jetonmatik.server.model.oauth

object GrantType extends Enumeration {
  val ClientCredentials = Value("client_credentials") // Only this grant type is supported at the moment
  val RefreshToken = Value("refresh_token")           // Planned to be supported

  val AuthorizationCode = Value("authorization_code") // Not Supported
  val Password = Value("password")                    // Not Supported
  val Implicit = Value("implicit")                    // Not Supported

  type GrantType = Value
}

case class TokenRequest(
  grant_type: String /*GrantType*/,
  scope: Option[String]) {

  require(grant_type == "client_credentials", "Only client_credentials grant_type is supported")

  val scopeSet = scope match {
    case Some(s) => s.split(" ").toSet
    case None => Set[String]()
  }
}

object TokenResponse {
  sealed trait Response

  case class Token(
    token_type: String,
    access_token: String,
    scope: Option[String],
    expires_in: Long) extends Response

  case class Error(
    error: String,
    error_description: Option[String]) extends Response
}