package controllers

import java.io.File

import model.{ServiceResponse, StatusCode}
import play.api.libs.json.{JsArray, JsObject, JsString, Json}
import play.api.mvc.Action
import util.FileUtils

/**
  * @author jsflax on 4/3/16.
  */
class AssetController extends BaseController {
  def avatars() = Action { implicit request =>
    renderServiceResponse(
      ServiceResponse[JsArray](
        StatusCode.OK,
        JsArray(FileUtils.listFiles(
          new File("./public/images/avatars")
        ).map(name =>
          JsObject(
            Seq("path" -> JsString(s"/assets/avatars/${name.getName}"),
              "name" -> JsString(
                name.getName.replaceAll("\\.[^.]*$", "").replaceAll("_", " ")
              ))
          )
        ))
      )
    )
  }
}
