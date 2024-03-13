package controllers

import javax.inject.Inject
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents, Request}
import dao.UserDao
import models.User
import play.api.data.Form
import play.api.data.Forms.{ignored, mapping, text}
import play.api.i18n.I18nSupport

import scala.concurrent.{ExecutionContext, Future}

class AuthenticationController @Inject()(cc: ControllerComponents, userDao: UserDao)(implicit ec: ExecutionContext) extends AbstractController(cc) with I18nSupport {

  private val userForm: Form[User] = Form(
    mapping(
      "id" -> ignored(0L),
      "userName" -> text(),
      "email" -> text(),
      "pwd" -> text(),
      "number_of_table" -> ignored(0L)
    )(User.apply)(User.unapply)
  )

  def loginForm(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    if (request.session.get("userId").isDefined) {
      Redirect(routes.HomeController.home())
    } else {
      Ok(views.html.authentication.login(userForm))
    }
  }

  def login(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    userForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(views.html.authentication.login(formWithErrors)))
      },
      loginData => {
        userDao.getUserByCredentials(loginData.userName, loginData.email, loginData.pwd).map {
          case Some(user) =>
            Redirect(routes.HomeController.home()).withSession("userId" -> user.id.toString)
          case None =>
            val formWithErrors = userForm.withGlobalError("Invalid username/email or password")
            BadRequest(views.html.authentication.login(formWithErrors))
        }
      }
    )
  }

  def registerForm: Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    if (request.session.get("userId").isDefined) {
      Redirect(routes.HomeController.home())
    } else {
      Ok(views.html.authentication.register(userForm))
    }
  }

  def register: Action[AnyContent] = Action.async { implicit request =>
    userForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(views.html.authentication.register(formWithErrors)))
      },
      userData => {
        userDao.addUser(userData).map { _ =>
          Redirect(routes.AuthenticationController.loginForm)
            .flashing("success" -> "User registered successfully")
        }
      }
    )
  }

  def logout: Action[AnyContent] = Action {
    Redirect(routes.AuthenticationController.loginForm)
      .withNewSession
  }
}
