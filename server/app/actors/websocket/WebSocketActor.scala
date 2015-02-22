package actors.websocket

import actors.DockerEvent
import actors.websocket.ClientCommands.{Authenticate, GetAllDockerConnections, SaveDockerConnection, ClientCommand}
import actors.websocket.WebSocketActor.{DeregisterWebSocket, RegisterWebSocket}
import akka.actor.{Actor, ActorRef, ActorSelection, Props}
import dockarium.api.Messages._
import docker.DockerCommands.CreateContainersCommand
import org.scalajs.spickling.PicklerRegistry
import play.api.Logger
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import play.api.libs.ws.WS
import org.scalajs.spickling.playjson._

/**
 * Created by becker on 2/5/15.
 */


case class ServerEvent(name: String, payLoad: JsValue)


object WebSocketActor {

  PicklerRegistry.register[DockerHost]
  PicklerRegistry.register[SaveDockerHost]
  PicklerRegistry.register[AuthenticationRequired]


  def props(out: ActorRef, dockariumActor: ActorSelection) = Props(new WebSocketActor(out, dockariumActor))

  case class RegisterWebSocket(socket: ActorRef)
  case class DeregisterWebSocket(socket: ActorRef)
  case class SendMessageToWebSockets(message: Any)
}


class WebSocketActor(out: ActorRef, dockariumActor: ActorSelection) extends Actor {

  def dockerApiUrl = "http://localhost:2375/v1.15/"
  dockariumActor ! RegisterWebSocket(self)

  implicit val personFormat = Json.format[DockerEvent]
  implicit val eventFormat = Json.format[ServerEvent]
  import play.api.Play.current

  import scala.concurrent.ExecutionContext.Implicits.global
  import org.scalajs.spickling.playjson._

  implicit val formats = Json.format[ClientCommand]
  implicit val formats2 = Json.format[SaveDockerConnection]
  implicit val formats3 = Json.format[Authenticate]

  var authenticated = false

  out ! PicklerRegistry.pickle(AuthenticationRequired())


  def processClientCommand(clientCommand: ClientCommand) = clientCommand.command match {

    case "getAllDockerConnections" => dockariumActor ! GetAllDockerConnections(out)

    case "authenticate" =>
      val authenticationCommand = Json.fromJson[Authenticate](clientCommand.payload.get).get
      Logger.info(s"authenticating ${authenticationCommand.username} with ${authenticationCommand.password}")

      // FIXME security MD5 hash!
      authenticationCommand match {

        case Authenticate("admin", "admin") =>
          out ! Json.toJson (ServerEvent ("AuthenticationSuccessful", Json.parse ("{}") ) )
          authenticated = true

        case _ =>
          Logger.warn("Authentication failed!")
          out ! Json.toJson(ServerEvent("AuthenticationFailed", Json.parse("{}")))

      }


    case "saveDockerConnection" =>
      Logger.info("Received saveDockerConnection")
      val saveDockerConnectionCommand = Json.fromJson[SaveDockerConnection](clientCommand.payload.get).get
      dockariumActor ! saveDockerConnectionCommand

    case "getServerInfo" =>

      Logger.info("Received getServerInfo - Firing with WS")

      WS.url("http://localhost:2375/v1.15/info").get().map { r =>
        Logger.debug(s"Sending response ${r.body}")
        val value = Json.parse(r.body)
        Logger.debug(s"Parsed debug $value")
        out ! Json.toJson(ServerEvent("serverInfo", value))
      }

    case "getServerVersion" =>

      Logger.info("Received getServerVersion - Firing with WS")

      WS.url("http://localhost:2375/v1.15/version").get().map { r =>
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

      println(incomingCommand.toString())
      val value: Any = PicklerRegistry.unpickle(incomingCommand)
      println(value)
      /*


      Json.fromJson[ClientCommand](incomingCommand) match {
        case JsSuccess(cmd, _) => processClientCommand(cmd)
        case JsError(e) => Logger.error(s"Failed to parse incoming command, $e");
      }
      */

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
