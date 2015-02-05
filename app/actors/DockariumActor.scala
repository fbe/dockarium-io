package actors

import actors.messages.StopMessage
import akka.actor.{ActorRef, Props, Actor}
import akka.spray.UnregisteredActorRef
import play.libs.Akka

/**
 * Created by becker on 2/4/15.
 */
class DockariumActor extends Actor {

  val connections = List(Akka.system.actorOf(Props(classOf[DockerEventListenerActor], "localhost", 2375)))

  private var connectedWebsockets: Set[ActorRef] = Set()

  override def receive: Receive = {

    case StopMessage =>
      println("shutting down all connections")
      connections.foreach(_ ! StopMessage)

    case RegisterWebSocket(actorRef) => connectedWebsockets = connectedWebsockets + actorRef
    case DeregisterWebSocket(actorRef) => connectedWebsockets = connectedWebsockets - actorRef

    case event: DockerEvent => connectedWebsockets.foreach(_ ! event)

    case x => println(s"dockarium global actor received unknown message $x")

  }
}
