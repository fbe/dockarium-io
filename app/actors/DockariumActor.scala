package actors

import actors.messages.StopMessage
import akka.actor.{Props, Actor}
import play.libs.Akka

/**
 * Created by becker on 2/4/15.
 */
class DockariumActor extends Actor {

  val connections = List(Akka.system.actorOf(Props(classOf[DockerEventListenerActor], "localhost", 2375)))

  override def receive: Receive = {

    case StopMessage =>
      println("shutting down all connections")
      connections.foreach(_ ! StopMessage)

    case x => println(s"received $x")

  }
}
