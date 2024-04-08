package models

import slick.jdbc.PostgresProfile.api._

case class User(id: Long = 0, email: String, pwd: String, name_of_tables: String, verified: Boolean = false)

class Users(tag: Tag) extends Table[User](tag, "users_info") {
  def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)

  def email = column[String]("EMAIL")

  def pwd = column[String]("PASSWORD")

  def name_of_tables = column[String]("Name_Of_Tables")

  def verified = column[Boolean]("verified")

  def * = (id, email, pwd, name_of_tables, verified) <> (User.tupled, User.unapply)
}

object Users {
  val users = TableQuery[Users]
}