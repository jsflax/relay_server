package controllers

import javax.inject.Inject

import model._
import play.api.mvc.Action
import service.{ChannelService, TokenService}
import model.ChannelProtocol._
import play.api.libs.json.JsError
import model.Token._

/**
  * @author jsflax on 3/30/16.
  */
class ChannelController @Inject() (messageController: MessageController)
  extends BaseController {

  def create = Action(parse.json) { implicit request =>
    validateModelAndFetchResult[ChannelCreateRequest, Long]
      (ChannelService.create)
  }

  def readAll = Action { implicit request =>
    renderServiceResponse(ChannelService.readAll())
  }

  def read(id: Long) = Action { implicit request =>
    renderServiceResponse(ChannelService.read(id))
  }

  def subscribe(id: String) = Action(parse.json) { implicit request =>
    val validationResult = validateModel[SimpleUuid](request)
    validationResult.isSuccess match {
      case true =>
            messageController.subscribe(
              id.toLong,
              validationResult.get.uuid
            )
      case false =>
        renderBadValidationResponse(validationResult.asInstanceOf[JsError])
    }
  }
}
