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

  def renderServiceResponse[B: Writes]
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
        Ok(Json.obj(
          "status" -> response.statusCode,
          "message" -> response.message
        ))
    }
  }

  def renderBadValidationResponse(jsError: JsError): Result =
    Ok(
      Json.obj(
        "status" -> StatusCode.ImproperParameters,
        "message" -> JsError.toJson(jsError)
      )
    )

  /**
    * Validate that the body of the request can be de-serialized
    * into some class of type A
    *
    * @param request request object from client
    * @tparam A json-coercable domain
    * @return result of validation
    */
  def validateModel[A: Reads](request: Request[JsValue]): JsResult[A] =
    request.body.as[JsObject].validate[A]

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
    val validationResult = validateModel(request)
    validationResult.isSuccess match {
      case true =>
        // call method with passed object
        renderServiceResponse(serviceMethod(validationResult.get))
      case false =>
        renderBadValidationResponse(validationResult.asInstanceOf[JsError])
    }
  }
}
