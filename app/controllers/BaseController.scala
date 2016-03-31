package controllers

import model.{ServiceResponse, StatusCode}
import play.api.libs.json._
import play.api.mvc.{Controller, Request, Result}

/**
  * Base trait for all Controllers to be inheriting from
  *
  * Created by jasonflax on 3/30/16.
  */
trait BaseController extends Controller {

  def dispatchValidServiceResponse[B: Writes]
    (response: ServiceResponse[B]): Result = {
    // check status code and serialize return data, then return to client
    response.statusCode match {
      case StatusCode.OK =>
        Ok(
          Json.obj(
            "status" -> response.statusCode,
            "data" -> Json.toJson(response.data)
          )
        )
      case _ =>
        BadRequest(Json.obj(
          "status" -> response.statusCode,
          "message" -> response.message
        ))
    }
  }

  /**
    * Dispatch result to client
    *
    * @param serviceMethod service method to be called
    *                      (with template param types)
    * @param request       implicit request from client
    * @tparam A Object being passed in
    * @tparam B Object requested for return
    * @return service response with status code and
    *         data of type @tparam B on success
    */
  def validateModelAndFetchResult[A: Reads, B: Writes]
    (serviceMethod: A => ServiceResponse[B])
    (implicit request: Request[JsValue]) = {
    // validate that the body of the request can be de-serialized
    // into some class of type A
    val validationResult = request.body.as[JsObject].validate[A]
    validationResult.isSuccess match {
      case true =>
        // call method with passed object
        dispatchValidServiceResponse(serviceMethod(validationResult.get))
      case false =>
        BadRequest(
          Json.obj(
            "status" -> StatusCode.ImproperParameters,
            "message" -> JsError.toJson(validationResult.asInstanceOf[JsError])
        ))
    }
  }
}
