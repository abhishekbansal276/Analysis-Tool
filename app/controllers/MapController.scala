package controllers

import dao.MongoDBService
import javax.inject.Inject
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MapController @Inject()(cc: ControllerComponents,
                              mongoDBService: MongoDBService) extends AbstractController(cc) {

  def showMap(folderName: String): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    val userIdOption = request.session.get("userId")
    userIdOption match {
      case Some(userId) =>
        mongoDBService.fetchInteractiveGraph(userId, "state_counts_map", folderName).map {
          case Some(htmlContent) => Ok(views.html.map_view(htmlContent))
          case None => NotFound("HTML content not found")
        }
      case None =>
        Future.successful(Unauthorized("User not authenticated"))
    }
  }
}
