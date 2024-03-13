package models

import rdd_scala_code_for_generating_graphs.{Address, Name, Person}
import slick.jdbc.PostgresProfile.api._

class PersonDataModel(tag: Tag, tableName: String) extends Table[Person](tag, tableName) {
  def aadhaar_id = column[String]("Aadhaar_ID", O.PrimaryKey)
  def first_name = column[String]("First_Name")
  def second_name = column[String]("Last_Name")
  def gender = column[String]("Gender")
  def age = column[Double]("Age")
  def dob = column[String]("Date_of_Birth")
  def occupation = column[String]("Occupation")
  def monthly_income = column[Double]("Monthly_Income")
  def category = column[String]("Category")
  def father_name = column[String]("Father_Name")
  def mother_name = column[String]("Mother_Name")
  def street = column[String]("Street")
  def city = column[String]("City")
  def state = column[String]("State")
  def postalCode = column[String]("Postal_Code")
  def phone = column[String]("Phone")

  def * = (
    aadhaar_id,
    (first_name, second_name).mapTo[Name],
    gender,
    age,
    dob,
    occupation,
    monthly_income,
    category,
    father_name,
    mother_name,
    (street, city, state, postalCode).mapTo[Address],
    phone
  ).mapTo[Person]
}

object PersonDataModel {
  def apply(tableName: String): TableQuery[PersonDataModel] = {
    TableQuery(tag => new PersonDataModel(tag, tableName))
  }
}
