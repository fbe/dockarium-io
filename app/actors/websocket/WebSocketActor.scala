package actors.websocket

import actors.DockerEvent
import actors.websocket.WebSocketActor.{DeregisterWebSocket, RegisterWebSocket}
import akka.actor.{ActorSelection, Actor, ActorRef, Props}
import play.Play
import play.api.Logger
import play.api.libs.json.Json
import play.api.libs.ws.WS

/**
 * Created by becker on 2/5/15.
 */


object WebSocketActor {
  def props(out: ActorRef, dockariumActor: ActorSelection) = Props(new WebSocketActor(out, dockariumActor))

  case class RegisterWebSocket(socket: ActorRef)
  case class DeregisterWebSocket(socket: ActorRef)
  case class SendMessageToWebSockets(message: Any)
}

class WebSocketActor(out: ActorRef, dockariumActor: ActorSelection) extends Actor {

  dockariumActor ! RegisterWebSocket(self)

  implicit val personFormat = Json.format[DockerEvent]
  import play.api.Play.current
  import scala.concurrent.ExecutionContext.Implicits.global

  def receive = {
    case dockerEvent: DockerEvent => out ! Json.toJson(dockerEvent)
    case "INFO" => WS.url("http://localhost:2375/v1.16/info").get().map(r => out ! r.body)
    case msg => Logger.warn(s"unknown message $msg")
  }

  override def postStop() = {
    dockariumActor ! DeregisterWebSocket(self)
  }
}
