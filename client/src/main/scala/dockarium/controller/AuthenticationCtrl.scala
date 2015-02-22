package dockarium.controller

import dockarium.api.Messages.AuthenticationRequired

/**
 * Created by becker on 2/19/15.
 */

object AuthenticationCtrl extends EventAwareController {

  onEvent {
    case AuthenticationRequired() => println("Received an authentication Required")
  }

  override def eventsToHandle = Set(classOf[AuthenticationRequired])

  override def init(): Unit = {}




}
