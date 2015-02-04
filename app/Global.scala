import actors.DockariumActor
import actors.messages.{StopMessage, StartMessage}
import akka.actor.{ActorRef, Props}
import play.api.{Application, GlobalSettings}
import play.libs.Akka

import scala.concurrent.Await
import scala.concurrent.duration._

/**
 * Created by becker on 2/4/15.
 */

object Global extends GlobalSettings {

  var dockarium: ActorRef = _

  override def onStart(application: Application) {
    dockarium = Akka.system.actorOf(Props[DockariumActor], name="dockarium")
    dockarium ! StartMessage
  }

  override def onStop(app: Application): Unit ={
    dockarium ! StopMessage

    import akka.pattern.gracefulStop


    val stopped = gracefulStop(dockarium, 5.seconds)
    Await.result(stopped, 6.seconds)

    Akka.system().shutdown()
    Akka.system().awaitTermination()
  }
}


