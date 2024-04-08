package dao

import com.google.inject.Inject
import org.mongodb.scala.MongoClient
import org.mongodb.scala.model.Filters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MongoDBService @Inject()(mongoClient: MongoClient) {
  private val database = mongoClient.getDatabase("analytics")

  def fetchInteractiveGraph(userId: String, plot_type: String, folderName: String): Future[Option[String]] = {
    val collection = database.getCollection(s"${userId}images${folderName.replaceAll(" ", "_").toLowerCase()}")
    val query = and(equal("user_id", userId), equal("plot_type", plot_type))

    collection.find(query)
      .headOption
      .map(_.flatMap(doc => doc.get("html_content").map(_.asString().getValue)))
  }
}