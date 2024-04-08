package controllers

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.types.{StringType, StructField, StructType}
import org.apache.spark.sql.{DataFrame, Row, SparkSession}
import play.api.i18n.I18nSupport
import play.api.mvc._
import utils.kafka_code.MyConsumer

import javax.inject._
import scala.concurrent.Future

@Singleton
class SqlQueryPerformPageController @Inject()(cc: ControllerComponents, consumer: MyConsumer) extends AbstractController(cc) with I18nSupport {

  private val defaultPageSize: Int = 1000

  import org.apache.spark.sql.functions._

  private val spark: SparkSession = SparkSession.builder()
    .appName("SQL")
    .master("local[*]")
    .config("spark.executor.heartbeatInterval", "30s")
    .getOrCreate()

  def sqlQueryPage(tableName: String, pageNum: Int): Action[AnyContent] = Action.async { implicit request =>
    val userIdOption: Option[String] = request.session.get("userId")
    userIdOption match {
      case Some(userId) =>
        val topicName = s"${userId}_${tableName.replaceAll(" ", "_")}".toLowerCase()

        val schema = StructType(Seq(
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

        val rdd: RDD[Row] = spark.read
          .format("kafka")
          .option("kafka.bootstrap.servers", "localhost:9092")
          .option("subscribe", topicName)
          .load()
          .selectExpr("CAST(key AS STRING)", "CAST(value AS STRING)")
          .select(from_json(col("value"), schema).as("data"))
          .select("data.*")
          .rdd

        Console.println("RDD Count", rdd.count())

        val pageSize = 500
        val offset = (pageNum - 1) * pageSize

        val pageRDD = rdd.zipWithIndex().filter {
          case (_, index) => index >= offset && index < offset + pageSize
        }.map(_._1)

        val pageDF = spark.createDataFrame(pageRDD, schema)

        val pageEntries = pageDF.collect()

        rdd.unpersist()
        pageDF.unpersist()

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

        Future.successful(Ok(views.html.sql_query_perform(htmlString, tableName, pageNum, pageEntries.length)))

      case None =>
        Future.successful(Redirect(routes.AuthenticationController.showAuthenticationForm))
    }
  }

  def performSqlQuery(tableName: String, pageNumber: Int): Action[AnyContent] = Action.async { implicit request =>
    val userIdOption: Option[String] = request.session.get("userId")
    userIdOption match {
      case Some(userId) =>
        val topicName = s"${userId}_${tableName.replaceAll(" ", "_").toLowerCase()}"
        val query = request.body.asFormUrlEncoded.flatMap(_.get("sqlquery").flatMap(_.headOption))
        query match {
          case Some(q) =>
            if (!isSelectStatement(q)) {
              Future.successful(BadRequest("Only SELECT statements are allowed"))
            } else if (!isValidTable(q)) {
              Future.successful(BadRequest("SQL query is not performed on the correct table"))
            } else {
              val dataInTopic = consumer.readFormTopic(topicName)
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
                  df.createOrReplaceTempView("data")
                  val resultDF = spark.sql(q)
                  val totalEntries = resultDF.count()

                  if (!resultDF.isEmpty) {
                    val totalPages = Math.ceil(totalEntries.toDouble / defaultPageSize.toDouble).toInt
                    val validPageNumber = pageNumber.max(1).min(totalPages)
                    val pageEntries = resultDF.limit(defaultPageSize).toDF().collect()
                    val headers: Seq[String] = resultDF.columns

                    val htmlString =
                      s"""
                         |<table border="1">
                         |  <thead>
                         |    <tr>
                         |      ${headers.map(header => s"<th>$header</th>").mkString("\n")}
                         |    </tr>
                         |  </thead>
                         |  <tbody>
                         |    ${
                        pageEntries.map(row =>
                          s"<tr>${row.toSeq.map(value => s"<td>$value</td>").mkString("\n")}</tr>"
                        ).mkString("\n")
                      }
                         |  </tbody>
                         |</table>
                         |""".stripMargin

                    val navigation =
                      s"""
                         |<div>
                         |  Page $validPageNumber of $totalPages
                         |  <a href="${routes.SqlQueryPerformPageController.performSqlQuery(tableName, validPageNumber - 1).url}">Previous</a>
                         |  <a href="${routes.SqlQueryPerformPageController.performSqlQuery(tableName, validPageNumber + 1).url}">Next</a>
                         |</div>
                         |""".stripMargin

                    Future.successful(Ok(views.html.query_result(htmlString, navigation, tableName)))
                  } else {
                    println("Error: DataFrame is empty.")
                    Future.successful(Ok("Dataframe Empty"))
                  }
                } catch {
                  case ex: Exception =>
                    println(s"Error processing Kafka messages: ${ex.getMessage}")
                    ex.printStackTrace()
                    Future.successful(Ok("Kafka message processing error"))
                }
              } else {
                Future.successful(Ok("No message fetched from kafka topic"))
              }
            }
          case None =>
            Future.successful(BadRequest("Query parameter is missing"))
        }
      case None =>
        Future.successful(Redirect(routes.AuthenticationController.showAuthenticationForm))
    }
  }

  private def isSelectStatement(sql: String): Boolean = {
    sql.trim.toLowerCase.startsWith("select")
  }

  private def isValidTable(sql: String): Boolean = {
    val regex = s"\\bdata\\b".r
    regex.findFirstIn(sql.toLowerCase).isDefined
  }
}