package controllers

import model.Channel
import play.api.mvc.Action
import service.ChannelService
import model.ChannelProtocol._

/**
  * @author jsflax on 3/30/16.
  */
class ChannelController extends BaseController {

  def create = Action(parse.json) { implicit request =>
    validateModelAndFetchResult[Channel, Long](ChannelService.create)
  }

  def readAll = Action { implicit request =>
    dispatchValidServiceResponse(ChannelService.readAll())
  }

  def read(id: String) = Action { implicit request =>
    dispatchValidServiceResponse(ChannelService.read(id))
  }
}
