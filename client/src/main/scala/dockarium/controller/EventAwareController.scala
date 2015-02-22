package dockarium.controller

import com.greencatsoft.angularjs.Controller
import dockarium.Pickling

import scala.scalajs.js

/**
 * Created by becker on 2/22/15.
 */
trait EventAwareController extends Controller {

  def eventsToHandle: Set[Class[_]]

  type EventHandler = PartialFunction[Any, Unit]

  var eventHandler: EventHandler = _

  final def onEvent(eventHandler: EventHandler): Unit = this.eventHandler = eventHandler


  final override def initialize(): Unit = {

    super.initialize()

    eventsToHandle.foreach { e =>

      scope.$on(e.getName, (event: js.Any, args: js.Any) => {

        val e = Pickling.unpickle(args)

        if(eventHandler.isDefinedAt(e)){
          eventHandler(e)
        } else {
          System.err.println(s"[Error] Cannot handle $e (${e.getClass.getName}) because onEvent of ${getClass.getName} doesn't handle this event!")
        }

      })
    }

    init()

  }

  def init(): Unit

}