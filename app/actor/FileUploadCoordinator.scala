package actors

import akka.actor._

import java.io._
import dao.UserDao
import redis.clients.jedis.Jedis
import utils.analysis_using_spark.AnalysisGenerateGraphsRDD
import utils.kafka_code.MyProducer

object FileUploadCoordinator {
  def props(myProducer: MyProducer, userDao: UserDao, actorSystem: ActorSystem, analysisGenerateGraphsRDD: AnalysisGenerateGraphsRDD): Props = Props(new FileUploadCoordinator(myProducer, userDao, actorSystem, analysisGenerateGraphsRDD))

  // Message to start file upload process
  case class UploadFile(userId: Long, tableName: String, file: File)
}

class FileUploadCoordinator(myProducer: MyProducer, userDao: UserDao, actorSystem: ActorSystem, analysisGenerateGraphsRDD: AnalysisGenerateGraphsRDD) extends Actor {
  import FileUploadCoordinator._
  import context.dispatcher // Import execution context

  def receive: Receive = {
    case UploadFile(userId, tableName, file) =>
      val topicName = s"${userId}_$tableName"
      val lockKey = s"uploadFileLock:$userId"
      val jedis = new Jedis("localhost", 6379)

      if (jedis.setnx(lockKey, "locked") == 1) {
        try {
          jedis.expire(lockKey, 600L)
          // Process the file using injected dependencies
          val topicCreationAndInsertResult = myProducer.createTopicAndInsertData(topicName, file.getAbsolutePath)
          val updateUserTablesResult = userDao.updateUserTables(userId, topicName)

          for {
            topicResult <- topicCreationAndInsertResult
            userUpdateResult <- updateUserTablesResult
          } yield {
            if (topicResult && userUpdateResult) {
              println("File Upload Successful")
              analysisGenerateGraphsRDD.drawGraphs(topicName, userId.toString, tableName.replaceAll(" ", "_").toLowerCase())
              sender() ! "File uploaded successfully."
            } else {
              sender() ! "Failed to create topic or update user!"
            }
          }
        } catch {
          case ex: Exception =>
            sender() ! Status.Failure(ex)
        } finally {
          jedis.del(lockKey)
          jedis.close()
        }
      } else {
        sender() ! "Another file is uploading!"
      }
  }
}