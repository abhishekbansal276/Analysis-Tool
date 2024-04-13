package controllers

import play.api.i18n.I18nSupport
import play.api.mvc._

import javax.inject._
import scala.concurrent.Future

@Singleton
class ProfileController @Inject()(val controllerComponents: ControllerComponents) extends BaseController with I18nSupport {

  def showProfilePage(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    request.session.get("userId") match {
      case Some(userIdStr) =>
        Future.successful(Ok(views.html.profile_page()))
      case None =>
        Future.successful(Redirect(routes.AuthenticationController.showAuthenticationForm))
    }
  }
}