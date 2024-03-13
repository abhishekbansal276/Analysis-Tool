package controllers

import dao.UserDao
import kafka_code.KafkaUtils
import javax.inject._
import play.api.i18n.I18nSupport
import play.api.libs.Files
import play.api.mvc._
import java.io.File
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.io.Source

@Singleton
class HomeController @Inject()(val controllerComponents: ControllerComponents, kafkaUtils: KafkaUtils, userDao: UserDao) extends BaseController with I18nSupport {

  def home(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    request.session.get("userId") match {
      case Some(userIdStr) =>
        val userId = userIdStr.toInt
        userDao.listTables(userId).map { tables =>
          val result: Seq[String] = tables.map { str =>
            val parts = str.split("_")
            if (parts.length > 1) {
              val substr = parts.tail.mkString("_")
              substr.replaceAll("_", " ").toUpperCase()
            } else {
              ""
            }
          }
          Ok(views.html.home(userId, result))
        }
      case None =>
        Future.successful(Redirect(routes.AuthenticationController.loginForm))
    }
  }

  def uploadFile: Action[MultipartFormData[Files.TemporaryFile]] = Action.async(parse.multipartFormData) { request =>
    val tableName = request.body.dataParts("tableName").headOption.getOrElse("").replace(" ", "_")
    request.body.file("jsonFile").map { filePart =>
      val userId = request.session.get("userId").map(_.toInt).getOrElse(0)
      val filename = "data.json"
      val directoryPath = s"D:\\scala_play_projects\\analysis-tool\\app\\data\\$userId\\${tableName.toLowerCase()}"
      val directory = new File(directoryPath)
      if (!directory.exists()) directory.mkdirs()
      val file = new File(directoryPath, filename)
      filePart.ref.moveTo(file)

      val modifiedContent = modifyJsonFile(file)

      val userIdFuture = Future.successful(userId)

      userIdFuture.flatMap { userId =>
        userDao.getNumberOfTablesById(userId).flatMap {
          case Some(_) =>
            val tableAndTopicName = s"${userId}_$tableName"

            val topicCreationAndInsertResult = kafkaUtils.createTopicAndInsertData(tableAndTopicName.toLowerCase(), tableAndTopicName.toLowerCase(), modifiedContent)

            topicCreationAndInsertResult.flatMap { topicCreationAndInsert =>
              if (topicCreationAndInsert) {
                userDao.incrementNumberOfTablesById(userId).map { incrementOrNot =>
                  if(incrementOrNot) {
                    Redirect(routes.HomeController.home())
                  }
                  else{
                    Redirect(routes.HomeController.home()).flashing("error" -> "Failed to increment value")
                  }
                }
              } else Future.successful(Redirect(routes.HomeController.home()).flashing("error" -> "Failed to create topic!"))
            }
          case None =>
            Future.successful(Redirect(routes.HomeController.home()).flashing("error" -> "User does not exist or has no tables"))
        }
      }
    }.getOrElse {
      Future.successful(Redirect(routes.HomeController.home()).flashing("error" -> "File upload failed!"))
    }
  }

  import scala.util.{Try, Using}

  private def modifyJsonFile(file: File): String = {
    Try {
      Using(Source.fromFile(file)) { source =>
        val fileContent = source.getLines().mkString("\n")
        val modifiedContent = if (fileContent.trim().endsWith(",")) {
          fileContent.dropRight(1)
        } else {
          fileContent
        }
        val wrappedContent = s"[$modifiedContent]"
        wrappedContent
      }.get
    }.getOrElse {
      Console.println(s"Error reading file: ${file.getAbsolutePath}")
      ""
    }
  }

}