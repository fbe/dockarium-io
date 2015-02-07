package controllers

import actors.websocket.WebSocketActor
import play.api.libs.json.JsValue
import play.api.mvc._
import play.libs.Akka

object Application extends Controller {

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  import play.api.Play.current

  def websocket = WebSocket.acceptWithActor[JsValue, JsValue] { request => out =>
    WebSocketActor.props(out, Akka.system().actorSelection("/user/dockarium"))
  }

}