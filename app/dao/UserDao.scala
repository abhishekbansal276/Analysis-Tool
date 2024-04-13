package dao

import models.Users.users
import javax.inject.Inject
import models.{User, Users}
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import scala.concurrent.{ExecutionContext, Future}

class UserDao @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) {
  private val dbConfig = dbConfigProvider.get[JdbcProfile]
  import dbConfig._
  import profile.api._

  def addUser(user: User): Future[Int] = {
    db.run(users += user)
  }

  def getUserByCredentials(email: String, password: String): Future[Option[User]] = {
    val query = users.filter(user =>
      user.email === email && user.pwd === password && user.verified
    ).result.headOption

    db.run(query)
  }

  def getNameOfTablesById(userId: Long)(implicit ec: ExecutionContext): Future[Option[List[String]]] = {
    val query = Users.users.filter(_.id === userId).map(_.name_of_tables).result

    db.run(query).map {
      case Nil => None
      case list =>
        val tableNames = list.head.split(",").map(_.trim).toList
        Some(tableNames)
    }
  }

  def updateUserTables(userId: Long, newTopicName: String)(implicit ec: ExecutionContext): Future[Boolean] = {
    val query = for {
      user <- Users.users.filter(_.id === userId)
    } yield user.name_of_tables

    val updatedAction = for {
      existingTables <- query.result.headOption
      updated <- existingTables match {
        case Some(existing) => query.update(existing + "," + newTopicName)
        case None => query.update(newTopicName)
      }
    } yield updated

    db.run(updatedAction.map(_ > 0))
  }

  def getPasswordByEmail(email: String): Future[Option[String]] = {
    val query = users.filter(_.email === email).map(_.pwd).result.headOption
    db.run(query)
  }

  import scala.concurrent.ExecutionContext.Implicits.global

  def userExistsByEmail(email: String): Future[Boolean] = {
    val query = users.filter(_.email === email).result.headOption
    db.run(query).map(_.isDefined)
  }
}