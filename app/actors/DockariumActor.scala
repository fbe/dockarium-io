package actors

import actors.messages.StopMessage
import actors.websocket.ClientCommands.SaveDockerConnection
import actors.websocket.WebSocketActor.{DeregisterWebSocket, RegisterWebSocket}
import akka.actor.{Actor, ActorRef, Props}
import play.Logger
import play.api.db.DB
import play.api.Play.current

/**
 * Created by becker on 2/4/15.
 */
class DockariumActor extends Actor {

  val connections = List(context.actorOf(Props(classOf[DockerEventListenerActor], "localhost", 2375, self)))

  private var connectedWebsockets: Set[ActorRef] = Set()

  override def receive: Receive = {

    case StopMessage =>
      println("shutting down all connections")
      connections.foreach(_ ! StopMessage)

    case RegisterWebSocket(actorRef) => connectedWebsockets = connectedWebsockets + actorRef
    case DeregisterWebSocket(actorRef) => connectedWebsockets = connectedWebsockets - actorRef

    case SaveDockerConnection(name, host, port) =>
      Logger.info(s"i would save the new connection ($name) to http://$host:$port now")

      DB.withConnection { conn =>
        val st = conn.createStatement
        val rs = st.execute("SELECT 1 FROM DUAL")

      }

    case event: DockerEvent => connectedWebsockets.foreach(_ ! event)

    case x => println(s"dockarium global actor received unknown message $x")

  }
}
