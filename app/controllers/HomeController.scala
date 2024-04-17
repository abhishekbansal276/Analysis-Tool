package controllers

import dao.UserDao

import javax.inject._
import play.api.i18n.I18nSupport
import play.api.libs.Files
import play.api.mvc._
import redis.clients.jedis.Jedis
import utils.analysis_using_spark.AnalysisGenerateGraphsRDD
import utils.kafka_code.MyProducer

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HomeController @Inject()(val controllerComponents: ControllerComponents,
                               myProducer: MyProducer,
                               userDao: UserDao,
                               analysisGenerateGraphsRDD: AnalysisGenerateGraphsRDD)
                              (implicit ec: ExecutionContext) extends BaseController with I18nSupport {

  def home(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    request.session.get("userId") match {
      case Some(userIdStr) =>
        val userId = userIdStr.toInt
        userDao.getNameOfTablesById(userId).map { tablesOption =>
          val result: Seq[String] = tablesOption match {
            case Some(tables) =>
              tables.flatMap { table =>
                val parts = table.split("_")
                if (parts.length > 1) {
                  Some(parts.tail.mkString("_").toUpperCase().replaceAll("_", " "))
                } else {
                  None
                }
              }
            case None =>
              Seq.empty[String]
          }
          val error: Option[String] = request.flash.get("error")
          Ok(views.html.home(result, error))
        }
      case None =>
        Future.successful(Redirect(routes.AuthenticationController.showAuthenticationForm))
    }
  }

  def uploadFile: Action[MultipartFormData[Files.TemporaryFile]] = Action.async(parse.multipartFormData) { request =>
    val tableName = request.body.dataParts("tableName").headOption.getOrElse("").replace(" ", "_").toLowerCase()
    val userId = request.session.get("userId").map(_.toLong).getOrElse(0L)
    request.session.get("uploading") match {
      case Some(_) =>
        Future.successful(Redirect(routes.HomeController.home()).flashing("error" -> "Another file is uploading!"))
      case None =>
        request.body.file("jsonFile").map { filePart =>
          val topicName = s"${userId}_$tableName"
          val updatedSession = request.session + ("uploading" -> "true")

          userDao.getNameOfTablesById(userId).flatMap {
            case Some(tableNames) if tableNames.contains(topicName) =>
              Future.successful(Redirect(routes.HomeController.home()).flashing("error" -> s"This name already exists! Choose another name.").withSession(updatedSession))

            case _ =>
              val topicCreationAndInsertResult = myProducer.createTopicAndInsertData(topicName, filePart.ref.path.toFile.getAbsolutePath)
              val updateUserTablesResult = userDao.updateUserTables(userId, topicName)

              for {
                topicResult <- topicCreationAndInsertResult
                userUpdateResult <- updateUserTablesResult
              } yield {
                if (topicResult && userUpdateResult) {
                  Console.println("File Upload Successful")
                  analysisGenerateGraphsRDD.drawGraphs(topicName, userId.toString, tableName.replaceAll(" ", "_").toLowerCase())
                  Redirect(routes.HomeController.home()).flashing("error" -> "File uploaded successfully.").withSession(request.session - "uploading")
                } else {
                  Redirect(routes.HomeController.home()).flashing("error" -> "Failed to create topic or update user!").withSession(request.session - "uploading")
                }
              }
          }
        }.getOrElse {
          Future.successful(Redirect(routes.HomeController.home()).flashing("error" -> "File upload failed!"))
        }
    }
  }
}