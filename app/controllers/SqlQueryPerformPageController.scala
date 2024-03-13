package controllers

import javax.inject._
import play.api._
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}
import dao.PersonDataDao
import play.api.i18n.I18nSupport

import java.util

@Singleton
class SqlQueryPerformPageController @Inject()(cc: ControllerComponents, personDataDao: PersonDataDao)(implicit ec: ExecutionContext) extends AbstractController(cc) with I18nSupport {

  def sqlQueryPage(tableName: String): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    val userIdOption: Option[String] = request.session.get("userId")
    userIdOption match {
      case Some(userId) =>

        val tn = s"${userId}_$tableName"
        personDataDao.getAllPersonData(tn).map { peopleInfoList =>
          Ok(views.html.sql_query_perform(peopleInfoList, Vector.empty[Map[String, Any]], tableName))
        }
      case None =>
        Future.successful(Redirect(routes.AuthenticationController.loginForm))
    }
  }

  def performSqlQuery(folderName: String): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    val userIdOption: Option[String] = request.session.get("userId")
    userIdOption match {
      case Some(userId) =>
        val tableName = s"${userId}_$folderName"
        val query = request.body.asFormUrlEncoded.flatMap(_.get("sqlquery").flatMap(_.headOption))
        query match {
          case Some(q) =>
            if (!isSelectStatement(q)) {
              Future.successful(BadRequest("Only SELECT statements are allowed"))
            } else if (!isValidTable("table_name", q)) {
              Future.successful(BadRequest("SQL query is not performed on the correct table"))
            } else {
              val correctQuery = getCorrectQuery(tableName, q)
              val queryResultFuture = personDataDao.executeQuery(correctQuery)
              val peopleInfoFuture = personDataDao.getAllPersonData(tableName)
              for {
                queryResult <- queryResultFuture
                peopleInfoList <- peopleInfoFuture
              } yield {
                Ok(views.html.sql_query_perform(peopleInfoList, queryResult, folderName))
              }
            }
          case None =>
            Future.successful(BadRequest("Query parameter is missing"))
        }
      case None =>
        Future.successful(Redirect(routes.AuthenticationController.loginForm))
    }
  }

  private def isSelectStatement(sql: String): Boolean = {
    sql.trim.toLowerCase.startsWith("select")
  }

  private def isValidTable(tableName: String, sql: String): Boolean = {
    val regex = s"\\b$tableName\\b".r
    regex.findFirstIn(sql.toLowerCase).isDefined
  }

  private def getCorrectQuery(tableName: String, query: String): String = {
    query.replaceAll("table_name", tableName)
  }
}
