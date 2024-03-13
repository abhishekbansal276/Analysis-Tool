package models

import slick.jdbc.PostgresProfile.api._

case class User(id: Long = 0, userName: String, email: String, pwd: String, number_of_table: Long = 0)

class Users(tag: Tag) extends Table[User](tag, "users_info") {
  def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)

  def userName = column[String]("USERNAME")

  def email = column[String]("EMAIL")

  def pwd = column[String]("PASSWORD")

  def number_of_table = column[Long]("NUMBER_OF_TABLE")

  def * = (id, userName, email, pwd, number_of_table) <> (User.tupled, User.unapply)
}

object Users {
  val users = TableQuery[Users]
}