package utils.analysis_using_spark

import org.json4s._
import org.json4s.jackson.JsonMethods.{compact, parse, render}
import org.mongodb.scala.MongoClient
import org.mongodb.scala.bson.collection.immutable.Document
import redis.clients.jedis.Jedis
import utils.kafka_code.MyConsumer
import java.io.{PrintWriter, StringWriter}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AnalysisGenerateGraphsRDD @Inject()(consumer: MyConsumer)(implicit ec: ExecutionContext) {

  def drawGraphs(topicName: String, user_id: String, folderName: String): Future[Boolean] = {
    val jedis = new Jedis("localhost", 6379)
    val lockKey = s"drawGraphsLock:$user_id"
    val lockAcquired = jedis.setnx(lockKey, "locked") == 1
    if (lockAcquired) {
      try {
        jedis.expire(lockKey, 600L)
        val resultFuture = performAnalysis(topicName, user_id, folderName)
        resultFuture.onComplete { result =>
          if (result.isSuccess) {
            println("Fine")
          }
          if (jedis.isConnected) {
            jedis.del(lockKey)
            jedis.close()
          }
        }
        resultFuture
      } catch {
        case ex: Exception =>
          if (jedis.isConnected) {
            jedis.del(lockKey)
            jedis.close()
          }
          Future.failed(ex)
      }
    } else {
      Future.failed(new RuntimeException("Wait another analysis is in progress!"))
    }
  }

  private def performAnalysis(topicName: String, user_id: String, folderName: String): Future[Boolean] = Future {
    val result = consumer.readFormTopic(topicName)
    println(s"Result length: ${result.length}")
    if (result.nonEmpty) {
      try {
        val dataList = JArray(result.map(parse(_).asInstanceOf[JObject]))
        val pythonScriptPath = "D:\\Training Project\\analytics\\app\\utils\\python_models\\python_script_to_generate_graphs.py"
        val processBuilder = new ProcessBuilder("python", pythonScriptPath, user_id, folderName)
        val process = processBuilder.start()
        val outputStream = process.getOutputStream
        val printWriter = new PrintWriter(outputStream)
        printWriter.println(compact(render(dataList)))
        printWriter.flush()
        printWriter.close()
        val exitCode = process.waitFor()
        if (exitCode != 0) {
          val errorStream = process.getErrorStream
          val errorString = new StringWriter()
          val errorPrintWriter = new PrintWriter(errorString)
          errorPrintWriter.println(s"Error: Python script execution failed with exit code $exitCode.")
          scala.io.Source.fromInputStream(errorStream).getLines.foreach(line => errorPrintWriter.println(line))
          errorPrintWriter.flush()
          errorPrintWriter.close()
          println(errorString.toString)
          false
        } else {
          val db = MongoClient().getDatabase("analytics")
          val collection = db.getCollection(s"${user_id}images$folderName")
          collection.insertOne(Document("user_id" -> user_id, "done" -> true)).toFuture().map { result =>
            if (result.wasAcknowledged()) {
              true
            } else {
              false
            }
          }.recover {
            case ex: Throwable =>
              println(s"Error inserting document: ${ex.getMessage}")
              false
          }
          println("Python script executed successfully.")
          true
        }
      } catch {
        case ex: Exception =>
          println(s"Error processing Kafka messages: ${ex.getMessage}")
          ex.printStackTrace()
          false
      }
    } else {
      println("No messages fetched from Kafka topic.")
      false
    }
  }
}