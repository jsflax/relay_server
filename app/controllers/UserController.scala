package controllers

import model.{User, UserCreateRequest, UserLoginRequest}
import play.api.mvc.Action
import service.UserService

import model.UserProtocol._

/**
  * @author jsflax on 3/31/16.
  */
class UserController extends BaseController {
  def create = Action(parse.json) { implicit request =>
    validateModelAndFetchResult[UserCreateRequest, Option[String]](UserService.create)
  }

  def login = Action(parse.json) { implicit request =>
    validateModelAndFetchResult[UserLoginRequest, User](
      UserService.readByEmailAndPassword
    )
  }
}
