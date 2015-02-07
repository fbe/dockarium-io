package actors.websocket

import actors.DockerEvent
import actors.websocket.ClientCommands.{SaveDockerConnection, ClientCommand}
import actors.websocket.WebSocketActor.{DeregisterWebSocket, RegisterWebSocket}
import akka.actor.{Actor, ActorRef, ActorSelection, Props}
import docker.DockerCommands.CreateContainersCommand
import play.api.Logger
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import play.api.libs.ws.WS

/**
 * Created by becker on 2/5/15.
 */


case class ServerEvent(name: String, payLoad: JsValue)


object WebSocketActor {
  def props(out: ActorRef, dockariumActor: ActorSelection) = Props(new WebSocketActor(out, dockariumActor))

  case class RegisterWebSocket(socket: ActorRef)
  case class DeregisterWebSocket(socket: ActorRef)
  case class SendMessageToWebSockets(message: Any)
}


class WebSocketActor(out: ActorRef, dockariumActor: ActorSelection) extends Actor {

  def dockerApiUrl = "http://localhost:2375/v1.16/"
  dockariumActor ! RegisterWebSocket(self)

  implicit val personFormat = Json.format[DockerEvent]
  implicit val eventFormat = Json.format[ServerEvent]
  import play.api.Play.current

  import scala.concurrent.ExecutionContext.Implicits.global

  implicit val formats = Json.format[ClientCommand]
  implicit val formats2 = Json.format[SaveDockerConnection]


  def processClientCommand(clientCommand: ClientCommand) = clientCommand.command match {

    case "saveDockerConnection" =>
      Logger.info("Received saveDockerConnection")
      val saveDockerConnectionCommand = Json.fromJson[SaveDockerConnection](clientCommand.payload.get).get
      dockariumActor ! saveDockerConnectionCommand

    case "getServerInfo" =>

      Logger.info("Received getServerInfo - Firing with WS")

      WS.url("http://localhost:2375/v1.16/info").get().map { r =>
        Logger.debug(s"Sending response ${r.body}")
        val value = Json.parse(r.body)
        Logger.debug(s"Parsed debug $value")
        out ! Json.toJson(ServerEvent("serverInfo", value))
      }

    case "getServerVersion" =>

      Logger.info("Received getServerVersion - Firing with WS")

      WS.url("http://localhost:2375/v1.16/version").get().map { r =>
        Logger.debug(s"Sending response ${r.body}")
        val value = Json.parse(r.body)
        Logger.debug(s"Parsed debug $value")
        out ! Json.toJson(ServerEvent("serverVersion", value))
      }

    case "getMemInfo" =>

      implicit val containerCommandWrites = Json.writes[CreateContainersCommand]

      postJson("/containers/create", Some(Json.toJson(CreateContainersCommand("busybox", Seq("cat", "/proc/meminfo")))))
        .map { r =>
        Logger.info(s"Create Meminfo container: ${r.body}")
        val id = (Json.parse(r.body) \ "Id").validate[String].get
        Logger.info(s"Container id: $id")

        postJson(s"/containers/$id/start", None).map { _ =>
          // TODO wait for container termination!
          WS.url(s"$dockerApiUrl/containers/$id/logs?stdout=1").get().map(r => {
            Logger.info(s"Meminfo: ${r.body}")

            import play.api.libs.json.Json.toJson

            val jsonKv = r.body.replace(" kB", "").replace(" ", "").split("\n").map { l =>
              val kv = l.split(":") // split MemFree:2519168
              (kv(0).trim, toJson(kv(1).toLong))
            }.toMap

            println(jsonKv)


            out ! Json.toJson(ServerEvent("meminfo",  toJson(jsonKv)))

          })

        }
      }

    case _ => Logger.error(s"Received unknown command $clientCommand")
  }

  def receive = {

    case dockerEvent: DockerEvent => out ! Json.toJson(ServerEvent("event", Json.toJson(dockerEvent)))

    case incomingCommand: JsValue =>

      Json.fromJson[ClientCommand](incomingCommand) match {
        case JsSuccess(cmd, _) => processClientCommand(cmd)
        case JsError(e) => Logger.error(s"Failed to parse incoming command, $e");
      }

    case msg => Logger.error(s"unknown message $msg")
  }

  override def postStop() = {
    dockariumActor ! DeregisterWebSocket(self)
  }

  def postJson(path: String, value: Option[JsValue]) = {
    val url = WS.url(s"$dockerApiUrl/$path")
    value match {
      case None => url.post("")
      case Some(v) => url.withHeaders(("Content-Type", "application/json")).post(v.toString())
    }
  }
}
