package controllers

import kafka_code.KafkaUtils
import org.json4s.DefaultFormats
import play.api.data._
import play.api.data.Forms._
import javax.inject._
import play.api.i18n.I18nSupport
import play.api.mvc._
import play.api.data.format.Formats._
import org.json4s.jackson.Serialization.write
import rdd_scala_code_for_generating_graphs.{Address, Name}
import scala.util.Try

case class InfoClass(aadhaar_id: String, person_name: Name, gender: String, age: Double, dob: String, occupation: String, monthly_income: Double, category: String, father_name: String, mother_name: String, address: Address, phone: String) {
  def toJson: String = {
    implicit val formats: DefaultFormats.type = DefaultFormats
    write(this)
  }
}

@Singleton
class AddEntryController @Inject()(val controllerComponents: ControllerComponents, kafkaUtils: KafkaUtils) extends BaseController with I18nSupport {
  private val addInfoForm: Form[InfoClass] = Form(
    mapping(
      "aadhaar_id" -> text(),
      "person_name" -> mapping(
        "first_name" -> text(),
        "second_name" -> text()
      )(Name.apply)(Name.unapply),
      "gender" -> text(),
      "age" -> of[Double],
      "dob" -> text(),
      "occupation" -> text(),
      "monthly_income" -> of[Double],
      "category" -> text(),
      "father_name" -> text(),
      "mother_name" -> text(),
      "address" -> mapping(
        "street" -> text(),
        "city" -> text(),
        "state" -> text(),
        "postalCode" -> text()
      )(Address.apply)(Address.unapply),
      "phone" -> text()
    )(InfoClass.apply)(InfoClass.unapply)
  )

  def showAddPage(folderName: String): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.add_entry(folderName, addInfoForm))
  }

  import scala.concurrent.ExecutionContext.Implicits.global // Ensure ExecutionContext is available

  import scala.concurrent.Future

  def addEntry(folderName: String): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    addInfoForm.bindFromRequest.fold(
      formWithErrors => {
        Console.println("Form error")
        Future.successful(BadRequest(views.html.add_entry(folderName, formWithErrors)))
      },
      entryData => {
        val jsonData = entryData.toJson
        Console.println(jsonData)
        val userIdOption: Option[String] = request.session.get("userId")
        val userId: Int = userIdOption.flatMap(str => Try(str.toInt).toOption).getOrElse(-1)
        val topic_name = s"${userId}_$folderName"
        kafkaUtils.sendDataToTopic(topic_name, jsonData).map {
          case true =>
            Redirect(routes.AddEntryController.showAddPage(folderName))
          case false =>
            Redirect(routes.AddEntryController.showAddPage(folderName)).flashing("error" -> "Failed to add data")
        }
      }
    )
  }
}