package utils.akka_code

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import org.apache.kafka.clients.consumer.{ConsumerConfig, KafkaConsumer}

import java.time.Duration
import java.util.Properties
import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContextExecutor, Future}
import play.api.libs.json._
import play.api.mvc.{AnyContent, Request}

case class P(
              aadhaarId: String,
              firstName: String,
              secondName: String,
              gender: String,
              age: Double,
              dob: String,
              occupation: String,
              monthlyIncome: Double,
              category: String,
              fatherName: String,
              motherName: String,
              street: String,
              city: String,
              state: String,
              postalCode: String,
              phone: String
            )

class KafkaConsumerHelper {
  def readFromTopic(topicName: String, offset: Int, limit: Int): List[String] = {
    val props = new Properties()
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
    props.put(ConsumerConfig.GROUP_ID_CONFIG, "my-group-my")
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false")
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

    var consumer: KafkaConsumer[String, String] = null
    try {
      consumer = new KafkaConsumer[String, String](props)
      consumer.subscribe(List(topicName).asJava)

      val result = scala.collection.mutable.ListBuffer[String]()

      while (true) {
        val records = consumer.poll(Duration.ofMillis(1000))
        val recordIterator = records.iterator()
        var count = 0
        while (recordIterator.hasNext && count < offset + limit) {
          val record = recordIterator.next()
          if (count >= offset) {
            result += record.value()
          }
          count += 1
        }
        if (records.isEmpty || result.length >= offset + limit)
          return result.slice(offset, offset + limit).toList
      }
      result.slice(offset, offset + limit).toList
    } catch {
      case ex: Exception =>
        println(s"Error fetching data from Kafka topic: ${ex.getMessage}")
        ex.printStackTrace()
        List.empty[String]
    } finally {
      if (consumer != null) consumer.close()
    }
  }
}

class AkkaRoutes {
  implicit val system: ActorSystem = ActorSystem("akka-http-server")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  private val kafkaConsumerHelper = new KafkaConsumerHelper()
  private val pageSize = 20

  def showDataAsTable(topicName: String, request: Request[AnyContent]): Unit = {
    val userIdOption: Option[String] = request.session.get("userId")
    userIdOption match {
      case Some(userId) =>
        val routes = path("initial-data" / topicName) {
          get {
            parameters("page".as[Int].withDefault(1), "pageSize".as[Int].withDefault(pageSize)) { (page, pageSize) =>
              val offset = (page - 1) * pageSize
              val recordsFuture = Future {
                kafkaConsumerHelper.readFromTopic(topicName, offset, pageSize)
              }
              onComplete(recordsFuture) {
                case scala.util.Success(records) =>
                  val people = records.flatMap(parseRecord)
                  complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, createTableWithNavigation(people, page, pageSize, topicName)))
                case scala.util.Failure(ex) =>
                  complete(HttpResponse(StatusCodes.InternalServerError, entity = s"Failed to fetch records: ${ex.getMessage}"))
              }
            }
          }
        } ~ path("sql-query") {
          post {
            formField("query") { query =>
              val result = performSqlQuery(query)
              complete(HttpEntity(ContentTypes.`application/json`, s"$result"))
            }
          }
        }

        Http().bindAndHandle(routes, "localhost", 8080)

      case None =>
    }
  }

  private def parseRecord(record: String): Option[P] = {
    try {
      val cleanedRecord = record.replaceAll(",+$", "")
      val json = Json.parse(cleanedRecord)
      val person = P(
        (json \ "aadhaar_id").as[String],
        (json \ "first_name").as[String],
        (json \ "second_name").as[String],
        (json \ "gender").as[String],
        (json \ "age").as[Double],
        (json \ "dob").as[String],
        (json \ "occupation").as[String],
        (json \ "monthly_income").as[Double],
        (json \ "category").as[String],
        (json \ "father_name").as[String],
        (json \ "mother_name").as[String],
        (json \ "street").as[String],
        (json \ "city").as[String],
        (json \ "state").as[String],
        (json \ "postalCode").as[String],
        (json \ "phone").as[String]
      )
      Some(person)
    } catch {
      case ex: JsResultException =>
        println(s"Error parsing record: ${ex.errors}")
        None
      case ex: Exception =>
        println(s"Error parsing record: ${ex.getMessage}")
        None
    }
  }

  private def createTable(people: Seq[P]): String = {
    val headers = List(
      "Aadhaar ID",
      "First Name",
      "Second Name",
      "Gender",
      "Age",
      "Date of Birth",
      "Occupation",
      "Monthly Income",
      "Category",
      "Father's Name",
      "Mother's Name",
      "Street",
      "City",
      "State",
      "Postal Code",
      "Phone"
    )
    val headerRow = headers.map(header => s"<th>$header</th>").mkString
    val dataRows = people.map { person =>
      val values = List(
        person.aadhaarId,
        person.firstName,
        person.secondName,
        person.gender,
        person.age,
        person.dob,
        person.occupation,
        person.monthlyIncome,
        person.category,
        person.fatherName,
        person.motherName,
        person.street,
        person.city,
        person.state,
        person.postalCode,
        person.phone
      )
      val dataRow = values.map(value => s"<td>$value</td>").mkString
      s"<tr>$dataRow</tr>"
    }.mkString
    s"<table><tr>$headerRow</tr>$dataRows</table>"
  }

  private def createTableWithNavigation(people: Seq[P], currentPage: Int, pageSize: Int, topicName: String): String = {
    val table = createTable(people)
    val nextPage = currentPage + 1
    val previousPage = currentPage - 1
    s"""
       |$table
       |<br/>
       |<form action="/initial-data/$topicName" method="GET">
       |  <input type="hidden" name="page" value="$nextPage">
       |  <input type="hidden" name="pageSize" value="$pageSize">
       |  <button type="submit">Next Page</button>
       |</form>
       |<form action="/initial-data/$topicName" method="GET">
       |  <input type="hidden" name="page" value="$previousPage">
       |  <input type="hidden" name="pageSize" value="$pageSize">
       |  <button type="submit">Previous Page</button>
       |</form>
       |""".stripMargin
  }

  private def performSqlQuery(query: String): Seq[String] = {
    Seq("Query result 1", "Query result 2")
  }
}