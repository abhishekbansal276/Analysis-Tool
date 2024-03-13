package controllers

import javax.inject._
import play.api.i18n.I18nSupport
import play.api.mvc._

import java.io.File

@Singleton
class AnalysisController @Inject()(val controllerComponents: ControllerComponents) extends BaseController with I18nSupport {
  def showAnalysisPage(userId: Int, folderName: String): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    val folderPath = s"D:\\scala_play_projects\\analysis-tool\\public\\images\\$userId\\${folderName.replaceAll(" ","_").toLowerCase()}"
    val folderExists = new File(folderPath).isDirectory
    Ok(views.html.data_analysis(userId, folderName.replaceAll(" ", "_").toLowerCase(),folderExists))
  }
}