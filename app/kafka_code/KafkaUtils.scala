package kafka_code

import java.sql.{Connection, DriverManager, PreparedStatement, Statement}
import java.util.Properties
import org.apache.kafka.clients.admin.{AdminClient, AdminClientConfig, NewTopic}
import org.apache.kafka.clients.consumer.{ConsumerConfig, KafkaConsumer}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig, ProducerRecord}
import org.apache.kafka.common.serialization.StringSerializer
import org.apache.spark.sql.execution.streaming.CommitMetadata.format
import org.json4s._
import org.json4s.jackson.JsonMethods._

import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters.{IterableHasAsJava, IterableHasAsScala}

class KafkaUtils {
  def createTopicAndInsertData(topicName: String, tableName: String, jsonData: String)(implicit ec: ExecutionContext): Future[Boolean] = Future {
    var connection: Connection = null
    val props = new Properties()
    props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")

    val adminClient = AdminClient.create(props)

    try {
      val newTopic = new NewTopic(topicName, 1, 1.toShort)
      adminClient.createTopics(List(newTopic).asJavaCollection).all().get()

      val producerProps = new Properties()
      producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
      producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer].getName)
      producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer].getName)

      val producer = new KafkaProducer[String, String](producerProps)

      implicit val formats: DefaultFormats.type = DefaultFormats
      val jsonList = parse(jsonData).extract[List[JObject]]

      jsonList.foreach { json =>
        val aadhaarId = (json \ "aadhaar_id").extract[String]
        val firstName = (json \ "person_name" \ "first_name").extract[String]
        val lastName = (json \ "person_name" \ "second_name").extract[String]
        val gender = (json \ "gender").extract[String]
        val age = (json \ "age").extract[Double]
        val dob = (json \ "dob").extract[String]
        val occupation = (json \ "occupation").extract[String]
        val monthlyIncome = (json \ "monthly_income").extract[Double]
        val category = (json \ "category").extract[String]
        val fatherName = (json \ "father_name").extract[String]
        val motherName = (json \ "mother_name").extract[String]
        val street = (json \ "address" \ "street").extract[String]
        val city = (json \ "address" \ "city").extract[String]
        val state = (json \ "address" \ "state").extract[String]
        val postalCode = (json \ "address" \ "postalCode").extract[String]
        val phone = (json \ "phone").extract[String]

        val value = s"""{"aadhaar_id":"$aadhaarId","first_name":"$firstName","last_name":"$lastName","gender":"$gender","age":$age,"dob":"$dob","occupation":"$occupation","monthly_income":$monthlyIncome,"category":"$category","father_name":"$fatherName","mother_name":"$motherName","street":"$street","city":"$city","state":"$state","postalCode":"$postalCode","phone":"$phone"}"""

        val record = new ProducerRecord[String, String](topicName, value)
        producer.send(record)
      }

      producer.close()

      Class.forName("com.mysql.cj.jdbc.Driver")
      val url = "jdbc:mysql://localhost:3306/population_manager?serverTimezone=UTC&useSSL=false"
      val user = "root"
      val password = ""
      connection = DriverManager.getConnection(url, user, password)

      val createTableQuery =
        s"""
           |CREATE TABLE IF NOT EXISTS population_manager.$tableName (
           |  Aadhaar_ID VARCHAR(255) PRIMARY KEY,
           |  First_Name VARCHAR(255),
           |  Last_Name VARCHAR(255),
           |  Gender VARCHAR(255),
           |  Age DOUBLE,
           |  Date_of_Birth VARCHAR(255),
           |  Occupation VARCHAR(255),
           |  Monthly_Income DOUBLE,
           |  Category VARCHAR(255),
           |  Father_Name VARCHAR(255),
           |  Mother_Name VARCHAR(255),
           |  Street VARCHAR(255),
           |  City VARCHAR(255),
           |  State VARCHAR(255),
           |  Postal_Code VARCHAR(255),
           |  Phone VARCHAR(255)
           |)
        """.stripMargin
      val statement: Statement = connection.createStatement()
      statement.executeUpdate(createTableQuery)

      connection.setAutoCommit(false)

      val insertQuery = s"INSERT INTO $tableName (Aadhaar_ID, First_Name, Last_Name, Gender, Age, Date_of_Birth, Occupation, Monthly_Income, Category, Father_Name, Mother_Name, Street, City, State, Postal_Code, Phone) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
      val preparedStatement: PreparedStatement = connection.prepareStatement(insertQuery)

      jsonList.foreach { json =>
        val aadhaarId = (json \ "aadhaar_id").extract[String]
        val firstName = (json \ "person_name" \ "first_name").extract[String]
        val lastName = (json \ "person_name" \ "second_name").extract[String]
        val gender = (json \ "gender").extract[String]
        val age = (json \ "age").extract[Double]
        val dob = (json \ "dob").extract[String]
        val occupation = (json \ "occupation").extract[String]
        val monthlyIncome = (json \ "monthly_income").extract[Double]
        val category = (json \ "category").extract[String]
        val fatherName = (json \ "father_name").extract[String]
        val motherName = (json \ "mother_name").extract[String]
        val street = (json \ "address" \ "street").extract[String]
        val city = (json \ "address" \ "city").extract[String]
        val state = (json \ "address" \ "state").extract[String]
        val postalCode = (json \ "address" \ "postalCode").extract[String]
        val phone = (json \ "phone").extract[String]

        preparedStatement.setString(1, aadhaarId)
        preparedStatement.setString(2, firstName)
        preparedStatement.setString(3, lastName)
        preparedStatement.setString(4, gender)
        preparedStatement.setDouble(5, age)
        preparedStatement.setString(6, dob)
        preparedStatement.setString(7, occupation)
        preparedStatement.setDouble(8, monthlyIncome)
        preparedStatement.setString(9, category)
        preparedStatement.setString(10, fatherName)
        preparedStatement.setString(11, motherName)
        preparedStatement.setString(12, street)
        preparedStatement.setString(13, city)
        preparedStatement.setString(14, state)
        preparedStatement.setString(15, postalCode)
        preparedStatement.setString(16, phone)
        preparedStatement.addBatch()
      }

      preparedStatement.executeBatch()
      connection.commit()
      true

    } catch {
      case ex: Exception =>
        Console.println(s"Error: ${ex.getMessage}")
        false
    } finally {
      if (connection != null) connection.close()
      adminClient.close()
    }
  }

  import scala.concurrent.Future
  import scala.concurrent.ExecutionContext.Implicits.global

  def sendDataToTopic(topicName: String, data: String): Future[Boolean] = Future {
    val producerProps = {
      val props = new Properties()
      props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
      props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer].getName)
      props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer].getName)
      props
    }

    val producer = new KafkaProducer[String, String](producerProps)

    try {
      val record = new ProducerRecord[String, String](topicName, data)
      producer.send(record)
      true
    } catch {
      case ex: Exception =>
        Console.println(s"Error sending data to topic $topicName: ${ex.getMessage}")
        false
    } finally {
      producer.close()
    }

    var connection: Connection = null
    try {
      Class.forName("com.mysql.cj.jdbc.Driver")
      val url = "jdbc:mysql://localhost:3306/population_manager?serverTimezone=UTC&useSSL=false"
      val user = "root"
      val password = ""
      connection = DriverManager.getConnection(url, user, password)

      val parsedData = parse(data)

      val aadhaarId = (parsedData \ "aadhaar_id").extract[String]
      val firstName = (parsedData \ "person_name" \ "first_name").extract[String]
      val lastName = (parsedData \ "person_name" \ "second_name").extract[String]
      val gender = (parsedData \ "gender").extract[String]
      val age = (parsedData \ "age").extract[Double]
      val dob = (parsedData \ "dob").extract[String]
      val occupation = (parsedData \ "occupation").extract[String]
      val monthlyIncome = (parsedData \ "monthly_income").extract[Double]
      val category = (parsedData \ "category").extract[String]
      val fatherName = (parsedData \ "father_name").extract[String]
      val motherName = (parsedData \ "mother_name").extract[String]
      val street = (parsedData \ "address" \ "street").extract[String]
      val city = (parsedData \ "address" \ "city").extract[String]
      val state = (parsedData \ "address" \ "state").extract[String]
      val postalCode = (parsedData \ "address" \ "postalCode").extract[String]
      val phone = (parsedData \ "phone").extract[String]

      val insertQuery = s"INSERT INTO $topicName (Aadhaar_ID, First_Name, Last_Name, Gender, Age, Date_of_Birth, Occupation, Monthly_Income, Category, Father_Name, Mother_Name, Street, City, State, Postal_Code, Phone) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
      val preparedStatement: PreparedStatement = connection.prepareStatement(insertQuery)

      preparedStatement.setString(1, aadhaarId)
      preparedStatement.setString(2, firstName)
      preparedStatement.setString(3, lastName)
      preparedStatement.setString(4, gender)
      preparedStatement.setDouble(5, age)
      preparedStatement.setString(6, dob)
      preparedStatement.setString(7, occupation)
      preparedStatement.setDouble(8, monthlyIncome)
      preparedStatement.setString(9, category)
      preparedStatement.setString(10, fatherName)
      preparedStatement.setString(11, motherName)
      preparedStatement.setString(12, street)
      preparedStatement.setString(13, city)
      preparedStatement.setString(14, state)
      preparedStatement.setString(15, postalCode)
      preparedStatement.setString(16, phone)

      preparedStatement.executeUpdate()

      true
    } catch {
      case ex: Exception =>
        Console.println(s"Error inserting data into MySQL database: ${ex.getMessage}")
        false
    } finally {
      if (connection != null) connection.close()
    }
  }
}