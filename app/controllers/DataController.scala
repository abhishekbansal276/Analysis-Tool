package controllers

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.types.{StringType, StructField, StructType}
import org.apache.spark.sql.{Row, SparkSession}
import play.api.i18n.I18nSupport
import play.api.mvc._
import utils.kafka_code.MyConsumer
import org.apache.spark.sql.functions._
import utils.CommonUtils

import java.nio.charset.StandardCharsets
import javax.inject._
import scala.concurrent.Future

@Singleton
class DataController @Inject()(cc: ControllerComponents, consumer: MyConsumer, commonUtils: CommonUtils) extends AbstractController(cc) with I18nSupport {
  private val spark: SparkSession = SparkSession.builder()
    .appName("SQL")
    .master("local[*]")
    .config("spark.executor.heartbeatInterval", "30s")
    .config("spark.executor.memory", "8g")
    .config("spark.executor.cores", "2")
    .config("spark.driver.memory", "4g")
    .config("spark.dynamicAllocation.enabled", "true")
    .config("spark.executor.instances", "4")
    .getOrCreate()

  private val schema = StructType(Seq(
    StructField("aadhaar_id", StringType),
    StructField("first_name", StringType),
    StructField("second_name", StringType),
    StructField("gender", StringType),
    StructField("age", StringType),
    StructField("dob", StringType),
    StructField("occupation", StringType),
    StructField("monthly_income", StringType),
    StructField("category", StringType),
    StructField("father_name", StringType),
    StructField("mother_name", StringType),
    StructField("street", StringType),
    StructField("city", StringType),
    StructField("state", StringType),
    StructField("postalCode", StringType),
    StructField("phone", StringType)
  ))

  def sqlQueryPage(tableName: String, downloadOrShow: Boolean): Action[AnyContent] = Action.async { implicit request =>
    val userIdOption: Option[String] = request.session.get("userId")
    userIdOption match {
      case Some(userId) =>
        val topicName = s"${userId}_${tableName.replaceAll(" ", "_")}".toLowerCase()
        val dataFrame = spark.read
          .format("kafka")
          .option("kafka.bootstrap.servers", "localhost:9092")
          .option("subscribe", topicName)
          .load()
          .selectExpr("CAST(key AS STRING)", "CAST(value AS STRING)")
          .select(from_json(col("value"), schema).as("data"))
          .select("data.*")
          .toDF()

        if(!dataFrame.isEmpty) {
          if (!downloadOrShow) {
            val pageDF = dataFrame.limit(200)
            val pageEntries = pageDF.collect()
            val headers: Seq[String] = pageDF.columns
            val htmlString =
              s"""
                 |<style>
                 |  .table-container {
                 |    max-width: 100%;
                 |  }
                 |  table {
                 |    border-collapse: collapse;
                 |    width: auto;
                 |  }
                 |  th, td {
                 |    border: 1px solid #dddddd;
                 |    text-align: left;
                 |    padding: 8px;
                 |    margin: 0;
                 |    white-space: nowrap;
                 |  }
                 |  th {
                 |    left: 0;
                 |    background-color: #f2f2f2;
                 |    z-index: 1;
                 |  }
                 |</style>
                 |<div class="table-container">
                 |  <table>
                 |    <thead>
                 |      <tr>
                 |        ${headers.map(header => s"<th>$header</th>").mkString("\n")}
                 |      </tr>
                 |    </thead>
                 |    <tbody>
                 |      ${
                pageEntries.map(row =>
                  s"<tr>${row.toSeq.map(value => s"<td>$value</td>").mkString("\n")}</tr>"
                ).mkString("\n")
              }
                 |    </tbody>
                 |  </table>
                 |</div>
                 |""".stripMargin
            val error: Option[String] = request.flash.get("error")
            Future.successful(Ok(views.html.data_table(htmlString, tableName, error)))
          } else {
            val csvString = commonUtils.dataFrameToCsv(dataFrame)
            val csvBytes = csvString.getBytes(StandardCharsets.UTF_8)
            val zipBytes = commonUtils.generateCSVZIP(csvBytes)

            Future.successful(Ok(zipBytes).as("application/zip").withHeaders(
              "Content-Disposition" -> s"attachment; filename=data.zip"
            ))
          }
        } else {
          Future.successful(Redirect(routes.DataController.sqlQueryPage(tableName, download = false)).flashing("error" -> s"No data. Try again."))
        }
      case None =>
        Future.successful(Redirect(routes.AuthenticationController.showAuthenticationForm))
    }
  }

  def performSqlQuery(tableName: String, downloadOrShow: Boolean): Action[AnyContent] = Action { implicit request =>
    val userIdOption: Option[String] = request.session.get("userId")
    userIdOption match {
      case Some(userId) =>
        val topicName = s"${userId}_${tableName.replaceAll(" ", "_").toLowerCase()}"
        val query = if(!downloadOrShow) request.body.asFormUrlEncoded.flatMap(_.get("sqlquery").flatMap(_.headOption)) else request.session.get("query")
        query match {
          case Some(q) =>
            if (!commonUtils.isSelectStatement(q)) {
              Redirect(routes.DataController.sqlQueryPage(tableName, download = false)).flashing("error" -> s"Query not correct. Only select statements are allowed.")
            } else if (!commonUtils.isValidTable(q)) {
              Redirect(routes.DataController.sqlQueryPage(tableName, download = false)).flashing("error" -> s"Use data as your table name.")
            } else {
              val data = consumer.readFromTopic(topicName)
              if (data.nonEmpty) {
                import spark.implicits._
                val df = spark.read.json(spark.createDataset(data))
                df.createOrReplaceTempView("data")
                val resultDF = spark.sql(q)

                if(!downloadOrShow) {
                  val pageDF = resultDF.limit(200)
                  val pageEntries = pageDF.collect()
                  val headers: Seq[String] = pageDF.columns
                  val htmlString =
                    s"""
                       |<style>
                       |  .table-container {
                       |    max-width: 100%;
                       |  }
                       |  table {
                       |    border-collapse: collapse;
                       |    width: auto;
                       |  }
                       |  th, td {
                       |    border: 1px solid #dddddd;
                       |    text-align: left;
                       |    padding: 8px;
                       |    margin: 0;
                       |    white-space: nowrap;
                       |  }
                       |  th {
                       |    left: 0;
                       |    background-color: #f2f2f2;
                       |    z-index: 1;
                       |  }
                       |</style>
                       |<div class="table-container">
                       |  <table>
                       |    <thead>
                       |      <tr>
                       |        ${headers.map(header => s"<th>$header</th>").mkString("\n")}
                       |      </tr>
                       |    </thead>
                       |    <tbody>
                       |      ${
                      pageEntries.map(row =>
                        s"<tr>${row.toSeq.map(value => s"<td>$value</td>").mkString("\n")}</tr>"
                      ).mkString("\n")
                    }
                       |    </tbody>
                       |  </table>
                       |</div>
                       |""".stripMargin

                  Ok(views.html.query_result(htmlString, tableName)).withSession(request.session + ("query" -> q))
                } else {
                  if(!resultDF.isEmpty) {
                    val csvString = commonUtils.dataFrameToCsv(resultDF)
                    val csvBytes = csvString.getBytes(StandardCharsets.UTF_8)
                    val zipBytes = commonUtils.generateCSVZIP(csvBytes)

                    Ok(zipBytes).as("application/zip").withHeaders(
                      "Content-Disposition" -> s"attachment; filename=data.zip"
                    )
                  } else {
                    Redirect(routes.DataController.performSqlQuery(tableName, false)).flashing("error" -> s"No data.")
                  }
                }
              } else {
                Redirect(routes.DataController.sqlQueryPage(tableName, download = false)).flashing("error" -> s"No data. Try again.")
              }
            }
          case None =>
            Redirect(routes.DataController.sqlQueryPage(tableName, download = false)).flashing("error" -> s"Query parameter is missing.")
        }
      case None =>
        Redirect(routes.AuthenticationController.showAuthenticationForm)
    }
  }
}