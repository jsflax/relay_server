package controllers

import model.{Channel, ServiceResponse, SimpleToken, StatusCode}
import play.api.mvc.Action
import service.{ChannelService, TokenService}
import model.ChannelProtocol._
import play.api.libs.json.JsError
import play.api.Play.current
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import model.Token._

import scala.collection.mutable.ListBuffer

/**
  * @author jsflax on 3/30/16.
  */
class ChannelController extends BaseController {

  def create = Action(parse.json) { implicit request =>
    validateModelAndFetchResult[Channel, Long](ChannelService.create)
  }

  def readAll = Action { implicit request =>
    renderServiceResponse(ChannelService.readAll())
  }

  def read(id: String) = Action { implicit request =>
    renderServiceResponse(ChannelService.read(id))
  }

  def subscribe(id: String) = Action(parse.json) { implicit request =>
    val validationResult = validateModel[SimpleToken](request)
    validationResult.isSuccess match {
      case true =>
        TokenService.findByToken(validationResult.get.token) match {
          case Some(user) =>
            MessageController.actorMap.get(user.id) match {
              case Some(actor) =>
                MessageController.channelActorMap.getOrElseUpdate(
                  id.toLong, ListBuffer(actor)
                ) += actor
                renderServiceResponse[SimpleToken](
                  ServiceResponse(
                    StatusCode.OK
                  )
                )
              case None =>
                renderServiceResponse[SimpleToken](
                  ServiceResponse(
                    StatusCode.Unauthorized,
                    message = Messages("user.not.connected.error")
                  )
                )
            }
          case None =>
            renderServiceResponse[SimpleToken](
              ServiceResponse(
                StatusCode.ResourceNotFound,
                message = Messages("user.does.not.exist.error")
              )
            )
        }
      case false =>
        renderBadValidationResponse(validationResult.asInstanceOf[JsError])
    }
  }
}
