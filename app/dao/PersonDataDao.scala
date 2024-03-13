package dao

import models.PersonDataModel
import slick.jdbc.{GetResult, JdbcProfile}

import scala.concurrent.{ExecutionContext, Future}
import javax.inject.Inject
import play.api.db.slick.DatabaseConfigProvider
import rdd_scala_code_for_generating_graphs.Person

class PersonDataDao @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext) {
  private val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig._
  import profile.api._

  def getAllPersonData(tableName: String): Future[Seq[Person]] = {
    val tableQuery = TableQuery[PersonDataModel](tag => new PersonDataModel(tag, tableName))
    db.run(tableQuery.result)
  }

  def executeQuery(sqlQuery: String): Future[Vector[Map[String, Any]]] = {
    implicit val getResult: GetResult[Map[String, String]] = GetResult(r => {
      val metaData = r.rs.getMetaData
      val columnCount = metaData.getColumnCount
      (1 to columnCount).map { i =>
        metaData.getColumnName(i) -> r.nextString()
      }.toMap
    })

    val query = sql"""#$sqlQuery"""
    db.run(query.as[Map[String, Any]])
  }
}
