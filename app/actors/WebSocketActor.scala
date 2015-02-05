package actors

import akka.actor.{Props, ActorRef, Actor}
import play.api.Logger
import play.api.libs.json. Json
import play.libs.Akka

/**
 * Created by benjamin on 8/1/14.
 */

// WebSockets
case class RegisterWebSocket(socket: ActorRef)
case class DeregisterWebSocket(socket: ActorRef)
case class SendMessageToWebSockets(message: Any)
case class SendLastMessageToWebSockets(webSocketActor: ActorRef)

object WebSocketActor {
  def props(out: ActorRef) = Props(new WebSocketActor(out))
}

class WebSocketActor(out: ActorRef) extends Actor {

  Akka.system.actorSelection("/user/dockarium") ! RegisterWebSocket(self)

  implicit val personFormat = Json.format[DockerEvent]

  def receive = {
    case dockerEvent: DockerEvent => out ! Json.toJson(dockerEvent)
    case msg => Logger.warn(s"unknown message $msg")
  }

  override def postStop() = {
    Akka.system.actorSelection("/user/dockarium") ! DeregisterWebSocket(self)
  }
}

class WebSocketRegistry extends Actor {

  var webSockets = Set[ActorRef]()

  def receive = {
    case RegisterWebSocket(socket) => webSockets = webSockets + socket
    case DeregisterWebSocket(socket) => webSockets = webSockets - socket
    case SendMessageToWebSockets(message) => webSockets foreach {_ ! message}
  }
}
