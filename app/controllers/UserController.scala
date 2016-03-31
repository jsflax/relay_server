package controllers

import model.{User, UserRequest}
import play.api.mvc.Action
import service.UserService

import model.UserRequestProtocol._
import model.UserProtocol._

/**
  * @author jsflax on 3/31/16.
  */
class UserController extends BaseController {
  def create = Action(parse.json) { implicit request =>
    validateModelAndFetchResult[UserRequest, Option[String]](UserService.create)
  }

  def login = Action(parse.json) { implicit request =>
    validateModelAndFetchResult[UserRequest, User](
      UserService.readByEmailAndPassword
    )
  }
}
