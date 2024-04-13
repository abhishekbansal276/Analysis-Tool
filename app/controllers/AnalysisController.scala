package controllers

import dao.MongoDBService
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.mongodb.scala._
import org.mongodb.scala.bson.BsonBinary
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import utils.CommonUtils
import utils.analysis_using_spark.AnalysisGenerateGraphsRDD
import utils.kafka_code.MyConsumer

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.{ZipEntry, ZipOutputStream}
import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AnalysisController @Inject()(
                                    val controllerComponents: ControllerComponents,
                                    analysisGenerateGraphsRDD: AnalysisGenerateGraphsRDD,
                                    override val messagesApi: MessagesApi,
                                    mongoClient: MongoClient,
                                    consumer: MyConsumer,
                                    mongoDBService: MongoDBService,
                                    commonUtils: CommonUtils
                                  )(implicit ec: ExecutionContext) extends BaseController with I18nSupport {

  def showAnalysisPage(folderName: String): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    val userId = request.session.get("userId").map(_.toInt).getOrElse(0)
    val topicName = s"${userId}_${folderName.replaceAll(" ", "_").toLowerCase()}"
    val db = mongoClient.getDatabase("analytics")
    val collection = db.getCollection(s"${userId}images${folderName.replaceAll(" ", "_").toLowerCase()}")

    import org.mongodb.scala.model.Filters._
    val graphExistFuture: Future[Boolean] = collection.find(and(equal("user_id", userId.toString), equal("done", true)))
      .headOption()
      .map {
        case Some(_) => true
        case None => false
      }

    import scala.concurrent.Await
    import scala.concurrent.duration._
    val graphsExist: Boolean = Await.result(graphExistFuture, Duration.Inf)
    if (graphsExist) {
      Console.println("Graph Exist")
      val imagesFuture = collection.find().toFuture()
      imagesFuture.map { images =>
        val imageBytesList = images.flatMap(_.get[BsonBinary]("image_data")).map(_.getData())
        Ok(views.html.data_analysis(imageBytesList, analysisDone = true, folderName))
      }.recover {
        case e: Throwable =>
          InternalServerError("An error occurred while fetching images: " + e.getMessage)
          Console.println("Graph not exist")
          Ok(views.html.data_analysis(Seq.empty, analysisDone = false, folderName))
      }
    } else {
      analysisGenerateGraphsRDD.drawGraphs(topicName, userId.toString, folderName.replaceAll(" ", "_").toLowerCase())
      Console.println("Graph not exist")
      Future.successful(Ok(views.html.data_analysis(Seq.empty, analysisDone = false, folderName)))
    }
  }

  def reload(folderName: String): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    val userId = request.session.get("userId").map(_.toInt).getOrElse(0)
    val topicName = s"${userId}_${folderName.replaceAll(" ", "_").toLowerCase()}"
    analysisGenerateGraphsRDD.drawGraphs(topicName, userId.toString, folderName.replaceAll(" ", "_").toLowerCase()).map { done =>
      if (done) {
        Redirect(routes.AnalysisController.showAnalysisPage(folderName))
      } else {
        Ok(views.html.data_analysis(Seq.empty, analysisDone = false, folderName))
      }
    }.recover {
      case ex: Exception =>
        InternalServerError("An error occurred: " + ex.getMessage)
    }
  }

  def showInteractivePlot(plot_type: String, folderName: String): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    val userIdOption = request.session.get("userId")
    userIdOption match {
      case Some(userId) =>
        mongoDBService.fetchInteractiveGraph(userId, plot_type, folderName).map {
          case Some(htmlContent) => Ok(views.html.interactive_graph(htmlContent))
          case None => NotFound("HTML content not found")
        }
      case None =>
        Future.successful(Unauthorized("User not authenticated"))
    }
  }

  def downloadCSV(tableName: String): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    val userIdOption: Option[String] = request.session.get("userId")
    userIdOption match {
      case Some(userId) =>
        val topicName = s"${userId}_${tableName.replaceAll(" ", "_").toLowerCase()}"
        val dataInTopic = consumer.readFromTopic(topicName)
        if (dataInTopic.nonEmpty) {
          try {
            val spark = SparkSession.builder()
              .appName("GetData")
              .master("local[*]")
              .config("spark.executor.memory", "8g")
              .config("spark.executor.cores", "2")
              .config("spark.driver.memory", "4g")
              .config("spark.dynamicAllocation.enabled", "true")
              .config("spark.executor.instances", "4")
              .getOrCreate()
            import spark.implicits._

            val df: DataFrame = spark.read.json(spark.createDataset(dataInTopic))
            val csvString = commonUtils.dataFrameToCsv(df)
            val csvBytes = csvString.getBytes(StandardCharsets.UTF_8)
            val zipBytes = commonUtils.generateCSVZIP(csvBytes)

            Future.successful(Ok(zipBytes).as("application/zip").withHeaders(
              "Content-Disposition" -> s"attachment; filename=data.zip"
            ))
          } catch {
            case ex: Exception =>
              println(s"Error processing Kafka messages: ${ex.getMessage}")
              ex.printStackTrace()
              Future.successful(InternalServerError("Kafka message processing error"))
          }
        } else {
          Future.successful(Ok("No message fetched from Kafka topic"))
        }
      case None =>
        Future.successful(Redirect(routes.AuthenticationController.showAuthenticationForm))
    }
  }

  def downloadImages(folderName: String): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    val userId = request.session.get("userId").map(_.toInt).getOrElse(0)
    val db = mongoClient.getDatabase("analytics")
    val collection = db.getCollection(s"${userId}images${folderName.replaceAll(" ", "_").toLowerCase()}")

    val imagesFuture = collection.find().toFuture()

    imagesFuture.map { images =>
      val imageBytesList = images.flatMap(_.get[BsonBinary]("image_data")).map(_.getData())

      val zipBytes = commonUtils.generateZIPImage(imageBytesList)

      Ok(zipBytes).as("application/zip").withHeaders(
        "Content-Disposition" -> s"attachment; filename=images.zip"
      )
    }.recover {
      case e: Throwable =>
        InternalServerError("An error occurred while fetching images: " + e.getMessage)
    }
  }
}