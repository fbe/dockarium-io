package actors

import actors.messages.StopMessage
import actors.websocket.ClientCommands.{GetAllDockerConnections, SaveDockerConnection}
import actors.websocket.ServerEvent
import actors.websocket.WebSocketActor.{DeregisterWebSocket, RegisterWebSocket}
import akka.actor.{Actor, ActorRef, Props}
import akka.util.Timeout
import play.Logger
import play.api.db.DB
import play.api.Play.current
import play.api.libs.json.Json

import scala.collection.immutable.Iterable
import scala.concurrent.Future

/**
 * Created by becker on 2/4/15.
 */

case class DockerConnectionInfo(name: String, address: String, port: Int, status: String)


class DockariumActor extends Actor {

  implicit val dcf = Json.format[DockerConnectionInfo]
  implicit val dcs = Json.format[ServerEvent]

  var dockerConnectionActors: Map[String, ActorRef] = Map()

  private var connectedWebsockets: Set[ActorRef] = Set()

  override def receive: Receive = {

    case GetAllDockerConnections(clientRef) =>

      import akka.pattern.ask
      import scala.concurrent.duration._
      import scala.concurrent.ExecutionContext.Implicits.global

      implicit val askTimeout = Timeout(5.second)

      val futures: Iterable[Future[DockerConnectionInfo]] = dockerConnectionActors.map { case (_, dca) =>
        Logger.debug(s"Sending ask to actor $dca")
        (dca ? "GetStatus").mapTo[DockerConnectionInfo]
      }

      Future.sequence(futures).onSuccess { case connectionInfos =>
          clientRef ! Json.toJson(ServerEvent("dockerConnections", Json.toJson(connectionInfos)))
      }

    case StopMessage =>
      connectedWebsockets.foreach(_ ! StopMessage)


    case RegisterWebSocket(actorRef) => connectedWebsockets = connectedWebsockets + actorRef
    case DeregisterWebSocket(actorRef) => connectedWebsockets = connectedWebsockets - actorRef

    case SaveDockerConnection(name, host, port) =>

      if (!dockerConnectionActors.contains(name)){
        dockerConnectionActors = dockerConnectionActors + (name -> context.actorOf(Props(classOf[DockerConnectionActor], name, host, port, self)))
      }

    case event: DockerEvent => connectedWebsockets.foreach(_ ! event)

    case x => println(s"dockarium global actor received unknown message $x")

  }
}
