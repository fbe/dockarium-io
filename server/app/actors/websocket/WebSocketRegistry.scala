package actors.websocket

import actors.websocket.WebSocketActor.{SendMessageToWebSockets, DeregisterWebSocket, RegisterWebSocket}
import akka.actor.{ActorRef, Actor}

/**
 * Created by becker on 2/5/15.
 */

class WebSocketRegistry extends Actor {

  var webSockets = Set[ActorRef]()

  def receive = {
    case RegisterWebSocket(socket) => webSockets = webSockets + socket
    case DeregisterWebSocket(socket) => webSockets = webSockets - socket
    case SendMessageToWebSockets(message) => webSockets foreach {_ ! message}
  }
}