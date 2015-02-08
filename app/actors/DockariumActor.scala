package actors

import actors.messages.StopMessage
import actors.websocket.ClientCommands.{GetAllDockerConnections, SaveDockerConnection}
import actors.websocket.ServerEvent
import actors.websocket.WebSocketActor.{DeregisterWebSocket, RegisterWebSocket}
import akka.actor.{Actor, ActorRef, Props}
import play.Logger
import play.api.db.DB
import play.api.Play.current
import play.api.libs.json.Json

/**
 * Created by becker on 2/4/15.
 */

case class DockerConnection(name: String, address: String, port: Int, status: String)


class DockariumActor extends Actor {

  implicit val dcf = Json.format[DockerConnection]
  implicit val dcs = Json.format[ServerEvent]

  var dockerEventListeners: Map[String, ActorRef] = Map()
  //List(context.actorOf(Props(classOf[DockerEventListenerActor], "localhost", 2375, self)))

  private var connectedWebsockets: Set[ActorRef] = Set()

  override def receive: Receive = {

    case GetAllDockerConnections(clientRef) =>
      clientRef ! Json.toJson(ServerEvent("dockerConnections", Json.toJson(Seq(DockerConnection("foo", "bar", 123, "connected")))))

    case StopMessage =>
      println("shutting down all connections")
      connectedWebsockets.foreach(_ ! StopMessage)


    case RegisterWebSocket(actorRef) => connectedWebsockets = connectedWebsockets + actorRef
    case DeregisterWebSocket(actorRef) => connectedWebsockets = connectedWebsockets - actorRef

    case SaveDockerConnection(name, host, port) =>
      Logger.info(s"i would save the new connection ($name) to http://$host:$port now")

      /*DB.withConnection { conn =>
        val st = conn.createStatement
        val rs = st.execute("SELECT 1 FROM DUAL")

      }*/
      if (!dockerEventListeners.contains(name)){
        dockerEventListeners + (name -> context.actorOf(Props(classOf[DockerConnectionActor], host, port, self)))
      }

    case event: DockerEvent => connectedWebsockets.foreach(_ ! event)

    case x => println(s"dockarium global actor received unknown message $x")

  }
}
