package controllers.api

import build.DocumentationFactory
import play.api.mvc.Action

object SystemApiController extends BaseApiController {

  def checkIndex = Action { implicit request =>
    withApiKeyProtection { _ =>
      DocumentationFactory.indexService.checkIndex
      Ok
    }
  }

}
