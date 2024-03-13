package rdd_scala_code_for_generating_graphs

import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.rdd.RDD
import org.json4s._
import org.json4s.jackson.Serialization
import org.json4s.jackson.Serialization.write

import java.io.{File, PrintWriter, StringWriter}

class RDDCode {
  def drawGraphs(userId: Int, folderName: String): Unit = {
    val outputFolder = s"D:/scala_play_projects/analysis-tool/public/images/$userId/$folderName/graphs"
    val directory = new File(outputFolder)
    if (!directory.exists()) directory.mkdirs()

    val spark = SparkSession.builder()
      .appName("RDDAnalysis")
      .master("local[*]")
      .getOrCreate()

    import spark.implicits._

    val jsonFilePath = s"D:\\scala_play_projects\\analysis-tool\\app\\data\\$userId\\$folderName\\data.json"
    val df: DataFrame = spark.read.json(jsonFilePath)

    val rdd: RDD[Person] = df.as[Person].rdd

    if (rdd.isEmpty()) {
      println("Error: RDD is empty.")
      spark.stop()
      return
    }

    implicit val formats: AnyRef with Formats = Serialization.formats(NoTypeHints)
    val dataList = write(rdd.collect().map(person =>
      Map(
        "aadhaar_id" -> person.aadhaar_id,
        "gender" -> person.gender,
        "age" -> person.age,
        "occupation" -> person.occupation,
        "monthly_income" -> person.monthly_income,
        "category" -> person.category,
        "city" -> person.address.city,
        "state" -> person.address.state
      )
    ).toList)

    val pythonScriptPath = "D:/scala_play_projects/analysis-tool/app/python_models/python_script_to_generate_graphs.py"
    val processBuilder = new ProcessBuilder("python", pythonScriptPath, outputFolder)
    val process = processBuilder.start()
    val outputStream = process.getOutputStream
    val printWriter = new PrintWriter(outputStream)

    printWriter.println(dataList)
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
    } else {
      println("Python script executed successfully.")
    }

    spark.stop()
  }
}

object MyObject extends App {
  private val obj = new RDDCode
  obj.drawGraphs(1, "table_three")
}