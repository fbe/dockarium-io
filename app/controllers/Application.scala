package controllers

import actors.WebSocketActor
import play.api._
import play.api.libs.json.JsValue
import play.api.mvc._
object Application extends Controller {

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  import play.api.Play.current

  def websocket = WebSocket.acceptWithActor[String, JsValue] { request => out =>
    WebSocketActor.props(out)
  }

}