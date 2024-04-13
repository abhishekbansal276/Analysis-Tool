package controllers

import javax.inject.Inject
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents, Request}
import dao.{EmailService, UserDao}
import models.User
import play.api.i18n.I18nSupport
import utils.CommonUtils

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class AuthenticationController @Inject()(cc: ControllerComponents, userDao: UserDao, emailService: EmailService, commonUtils: CommonUtils)(implicit ec: ExecutionContext) extends AbstractController(cc) with I18nSupport {

  def showAuthenticationForm(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    if (request.session.get("userId").isDefined) {
      Redirect(routes.HomeController.home())
    } else {
      val error: Option[String] = request.flash.get("error")
      Ok(views.html.authentication.authentication_page(error))
    }
  }

  def login(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    val formData = request.body.asFormUrlEncoded.getOrElse(Map.empty)
    val emailOption = formData.get("email").flatMap(_.headOption)
    val passwordOption = formData.get("pwd").flatMap(_.headOption)

    (emailOption, passwordOption) match {
      case (Some(email), Some(password)) =>
        userDao.getUserByCredentials(email, password).map {
          case Some(user) =>
            Redirect(routes.HomeController.home()).withSession("userId" -> user.id.toString)
          case None =>
            Redirect(routes.AuthenticationController.showAuthenticationForm).flashing("error" -> s"No user found.")
        }
      case _ =>
        Future.successful(Redirect(routes.AuthenticationController.showAuthenticationForm).flashing("error" -> s"Enter credentials again."))
    }
  }

  def sendOtp(): Action[AnyContent] = Action.async { implicit request =>
    val formData = request.body.asFormUrlEncoded.getOrElse(Map.empty)
    val emailOption = formData.get("email").flatMap(_.headOption)
    val passwordOption = formData.get("pwd").flatMap(_.headOption)

    (emailOption, passwordOption) match {
      case (Some(email), Some(password)) =>
        userDao.userExistsByEmail(email).map{ userExist =>
          if(userExist) {
            Redirect(routes.AuthenticationController.showAuthenticationForm).flashing("error" -> s"User already exist.")
          } else {
            val otp = generateOTP()
            val emailBody = s"Your OTP for registration is: $otp"
            emailService.sendEmail(email, "OTP for Registration", emailBody)
            val encryptOtp = commonUtils.encrypt(otp)
            val encryptEmail = commonUtils.encrypt(email)
            val encryptPassword = commonUtils.encrypt(password)
            Redirect(routes.AuthenticationController.showOtpForm(encryptOtp, encryptEmail, encryptPassword)).flashing("error" -> s"OTP sent.").withSession("otpSent" -> "yes")
          }
        }

      case _ =>
        Future.successful(Redirect(routes.AuthenticationController.showAuthenticationForm).flashing("error" -> s"Enter email password again."))
    }
  }

  def showOtpForm(encryptOtp: String, encryptEmail: String, encryptPassword: String): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    if (request.session.get("otpSent").isDefined) {
      val decryptEmail: String = commonUtils.decrypt(encryptEmail) match {
        case Success(value) => value
        case Failure(_) => ""
      }
      val error: Option[String] = request.flash.get("error")
      Ok(views.html.authentication.otp(encryptOtp, decryptEmail, encryptPassword, error))
    } else {
      Redirect(routes.AuthenticationController.showAuthenticationForm).flashing("error" -> "OTP not sent. Try again!").withHeaders(CACHE_CONTROL -> "no-store, must-revalidate")
    }
  }

  def processRegistration(encryptOtp: String, decryptEmail: String, encryptPassword: String): Action[AnyContent] = Action.async { implicit request =>
    val otpDigits = for {
      i <- 1 to 6
      otpDigit <- request.body.asFormUrlEncoded.flatMap(_.get(s"otp$i"))
    } yield otpDigit.headOption.getOrElse('0')

    val decryptOtp: String = commonUtils.decrypt(encryptOtp) match {
      case Success(value) => value
      case Failure(_) => ""
    }

    val decryptPassword: String = commonUtils.decrypt(encryptPassword) match {
      case Success(value) => value
      case Failure(_) => ""
    }

    val otp = otpDigits.mkString("")
    if(otp == decryptOtp) {
      userDao.addUser(User(0, decryptEmail, decryptPassword, "", verified = true)).map { _ =>
        Redirect(routes.AuthenticationController.showAuthenticationForm)
          .flashing("error" -> "User registered successfully")
      }
    } else {
      val encryptOtp = commonUtils.encrypt(decryptOtp)
      val encryptEmail = commonUtils.encrypt(decryptEmail)
      val encryptPassword = commonUtils.encrypt(decryptPassword)
      Future.successful(Redirect(routes.AuthenticationController.showOtpForm(encryptOtp, encryptEmail, encryptPassword))
        .flashing("error" -> "Wrong OTP"))
    }
  }

  def logout: Action[AnyContent] = Action {
    Redirect(routes.AuthenticationController.showAuthenticationForm).withNewSession
  }

  private def generateOTP(): String = {
    val otp = scala.util.Random.nextInt(900000) + 100000
    otp.toString
  }

  def showForgetPassword(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    val error: Option[String] = request.flash.get("error")
    Ok(views.html.authentication.forget_password(error))
  }

  def forgetPassword(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    val emailOption = request.body.asFormUrlEncoded.flatMap(_.get("email").flatMap(_.headOption))
    emailOption.map { email =>
      userDao.getPasswordByEmail(email).map {
        case Some(password) =>
          emailService.sendEmail(email, "Your Password", password)
          Redirect(routes.AuthenticationController.showAuthenticationForm).flashing("error" -> "Password sent on your email.")
        case None =>
          Redirect(routes.AuthenticationController.showForgetPassword()).flashing("error" -> "Email not found.")
      }
    }.getOrElse {
      Future.successful(Redirect(routes.AuthenticationController.showForgetPassword()).flashing("error" -> "Email parameter is missing."))
    }
  }
}