package dao

import models.Users.users
import javax.inject.Inject
import models.User
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class UserDao @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) {
  private val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig._
  import profile.api._

  def addUser(user: User): Future[Int] = {
    db.run(users += user)
  }

  def getUserByCredentials(userName: String, email: String, password: String): Future[Option[User]] = {
    val query = users.filter(user =>
      user.userName === userName && user.email === email
    ).filter(_.pwd === password).result.headOption

    db.run(query)
  }

  def getNumberOfTablesById(id: Long): Future[Option[Long]] = {
    val query = users.filter(_.id === id).map(_.number_of_table)
    val action = query.result.headOption
    db.run(action)
  }

  def incrementNumberOfTablesById(id: Long): Future[Boolean] = {
    val query = users.filter(_.id === id).map(_.number_of_table)
    val action = for {
      currentNumberOfTables <- query.result.headOption
      updatedRows <- currentNumberOfTables.map { current =>
        val newNumberOfTables = current + 1
        query.update(newNumberOfTables)
      }.getOrElse(DBIO.successful(0))
    } yield updatedRows > 0
    db.run(action)
  }

  import scala.concurrent.Future

  def listTables(userId: Long): Future[Seq[String]] = {
    val query = sql"""SELECT table_name FROM information_schema.tables WHERE table_name LIKE '#${userId}_%';""".as[String]
    db.run(query)
  }
}
