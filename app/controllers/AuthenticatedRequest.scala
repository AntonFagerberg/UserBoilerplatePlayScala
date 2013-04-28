/**
 * Created by Anton Fagerberg.
 * anton@antonfagerberg.com | www.antonfagerberg.com
 */
package controllers

import play.api.mvc._

/** A request performed by an authenticated user.
  *
  * @param user     User who sent the request.
  * @param request  The request.
  */
case class AuthenticatedRequest(
  user: models.User,
  request: Request[AnyContent]
) extends WrappedRequest(request)

object AuthenticatedRequest extends Controller {
  val sessionField = "email"

  /** User sends an authenticated request to access a resource via a Controller.
   *
   * @param authenticationFunction Authentication function to use.
   * @param requestResult          Function request.
   * @return                       Action or Redirect.
   */
  def userRequest(authenticationFunction: String => Option[models.User] = models.User.authenticateEmail)(requestResult: AuthenticatedRequest => Result) = {
    Action { request =>
      request.session.get(sessionField).flatMap(requestUser => authenticationFunction(requestUser)).map { user =>
        requestResult(AuthenticatedRequest(user, request))
      }.getOrElse(Redirect(routes.Application.login).flashing("warning" -> "permissionDenied"))
    }
  }
}