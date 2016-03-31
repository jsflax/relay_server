package model

/**
  * Possible Status Codes to be sent back to client
  *
  * @author jsflax on 3/30/16.
  */
object StatusCode extends Enumeration {
  type StatusCode = Int

  val OK = 200
  val Unauthorized = 401
  val ResourceNotFound = 404
  val ImproperParameters = 420
  val DatabaseError = 422
}

import model.StatusCode._
import play.api.libs.json.Writes

/**
  * Main envelope to be passed around from service to controller,
  * and then serialized and passed to the client.
  *
  * @param statusCode from enum StatusCode
  * @param data    data to return on success
  * @param message    message on error
  * @tparam A class to be serialized and returned
  */
case class ServiceResponse[A: Writes](statusCode: StatusCode,
                                      data: A = null,
                                      message: String = null)
