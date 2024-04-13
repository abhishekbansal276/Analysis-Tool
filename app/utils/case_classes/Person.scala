package utils.case_classes

import play.api.libs.json.{Json, Reads}

case class Person(aadhaar_id: String,
                  first_name: String,
                  second_name: String,
                  gender: String,
                  age: String,
                  dob: String,
                  occupation: String,
                  monthly_income: String,
                  category: String,
                  father_name: String,
                  mother_name: String,
                  street: String,
                  city: String,
                  state: String,
                  postalCode: String,
                  phone: String)

object Person {
  implicit val personReads: Reads[Person] = Json.reads[Person]
}