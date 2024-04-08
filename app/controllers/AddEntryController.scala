package controllers

import play.api.i18n.I18nSupport
import play.api.libs.Files
import play.api.mvc._
import utils.kafka_code.MyProducer

import javax.inject._

@Singleton
class AddEntryController @Inject()(val controllerComponents: ControllerComponents, myProducer: MyProducer) extends BaseController with I18nSupport {
  def showAddPage(folderName: String): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.add_entry(folderName))
  }

  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent.Future

  def uploadFile(folderName: String): Action[MultipartFormData[Files.TemporaryFile]] = Action.async(parse.multipartFormData) { request =>
    val userId = request.session.get("userId").map(_.toInt).getOrElse(0)
    val topicName = s"${userId}_${folderName.replaceAll(" ", "_").toLowerCase()}"
    request.body.file("jsonFile").map { filePart =>
        val topicCreationAndInsertResult = myProducer.createTopicAndInsertData(topicName, filePart.ref.path.toFile.getAbsolutePath)
        for {
          topicResult <- topicCreationAndInsertResult
        } yield {
          if (topicResult) {
            Redirect(routes.AnalysisController.showAnalysisPage(folderName)).flashing("error" -> "Data added successfully.")
          } else {
            Redirect(routes.HomeController.home()).flashing("error" -> "Failed to create topic or update user!")
          }
        }
      }
      .getOrElse {
        Future.successful(Redirect(routes.HomeController.home()).flashing("error" -> "File upload failed!"))
      }
  }

  def addEntry(folderName: String): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    val userId = request.session.get("userId").map(_.toInt).getOrElse(0)
    val topic_name: String = s"${userId}_${folderName.replaceAll(" ", "_").toLowerCase()}"

    val formData = request.body.asFormUrlEncoded.getOrElse(Map.empty[String, Seq[String]])
    val aadhaarId = formData.get("aadhaar_id").flatMap(_.headOption).getOrElse("")
    val firstName = formData.get("first_name").flatMap(_.headOption).getOrElse("")
    val secondName = formData.get("second_name").flatMap(_.headOption).getOrElse("")
    val gender = formData.get("gender").flatMap(_.headOption).getOrElse("")
    val age = formData.get("age").flatMap(_.headOption).getOrElse(0L)
    val dob = formData.get("dob").flatMap(_.headOption).getOrElse("")
    val occupation = formData.get("occupation").flatMap(_.headOption).getOrElse("")
    val monthlyIncome = formData.get("monthly_income").flatMap(_.headOption).getOrElse(0)
    val category = formData.get("category").flatMap(_.headOption).getOrElse("")
    val fatherName = formData.get("father_name").flatMap(_.headOption).getOrElse("")
    val motherName = formData.get("mother_name").flatMap(_.headOption).getOrElse("")
    val street = formData.get("street").flatMap(_.headOption).getOrElse("")
    val city = formData.get("city").flatMap(_.headOption).getOrElse("")
    val state = formData.get("state").flatMap(_.headOption).getOrElse("")
    val postalCode = formData.get("postalCode").flatMap(_.headOption).getOrElse("")
    val phone = formData.get("phone").flatMap(_.headOption).getOrElse("")

    val data = s"""{"aadhaar_id":"$aadhaarId","first_name":"$firstName","second_name":"$secondName","gender":"$gender","age":$age,"dob":"$dob","occupation":"$occupation","monthly_income":$monthlyIncome,"category":"$category","father_name":"$fatherName","mother_name":"$motherName","street":"$street","city":"$city","state":"$state","postalCode":"$postalCode","phone":"$phone"}"""

    myProducer.sendDataToTopic(topic_name, data).map {
      case true =>
        Redirect(routes.AddEntryController.showAddPage(folderName))
      case false =>
        Redirect(routes.AddEntryController.showAddPage(topic_name)).flashing("error" -> "Failed to add data")
    }
  }
}