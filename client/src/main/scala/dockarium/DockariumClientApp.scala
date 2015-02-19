package dockarium

import com.greencatsoft.angularjs.core.RootScope
import com.greencatsoft.angularjs.{inject, injectable, Factory, Angular}
import dockarium.controller._
import org.scalajs.dom
import org.scalajs.dom.Event
import org.scalajs.dom.raw.MessageEvent

import scala.scalajs.js.{JSON, JSApp}
import scala.scalajs.js.annotation.{JSExportAll, JSExport}

/**
 * Created by becker on 2/18/15.
 */
@JSExport
object DockariumClientApp extends JSApp {

  override def main(): Unit = {

    val module = Angular.module("dockariumclient", Seq("ui.bootstrap", "chart.js", "ui.router", "dockarium.util"))
    module.config(RoutingConfig)

    module.controller(DockerConnectionsTableCtrl)
    module.controller(DashboardCtrl)
    module.controller(EventLogCtrl)
    module.controller(AuthenticationCtrl)
    module.controller(AuthenticationWindowCtrl)
    module.controller(AdminCtrl)
    module.controller(WebsocketStatusCtrl)
    module.controller(ServerInfoCtrl)
    module.controller(ServerVersionCtrl)

    module.factory(WebsocketConnectionServiceFactory)

  }
}


@JSExportAll
case class ServerConnectionStatus(name: String, icon: String)

@injectable("$websocketConnectionService")
class WebsocketConnectionService(rootScope: RootScope) {

  require(rootScope != null, "RootScope not given")

  var statusIcons: Map[String, String] = Map(
    "initializing" -> "glyphicon-question-sign",
    "waiting" -> "glyphicon-ok-sign icon-warning",
    "receiving" -> "glyphicon-ok-sign icon-success",
    "error" -> "glyphicon-remove-sign icon-danger"
  )

  var serverConnectionStatus = ServerConnectionStatus("initializing", statusIcons("initializing"))

  println("oh hey, websocket connection service initialized!")

  val socketType = dom.document.location.protocol match {
    case "https:" => "wss"
    case "http:" => "ws"
  }

  val ws = new dom.WebSocket(s"$socketType://${dom.document.location.host}/websocket")

  def broadcastStatus(statusName: String, statusIcon: String) = {
    println(s"broadcasting status")
    serverConnectionStatus = ServerConnectionStatus(statusName, statusIcon)

    rootScope.$apply {
      rootScope.$broadcast("WebSocketStatusChange")
    }
  }

  def send(args: Any) = {
    println("Sending " + JSON.stringify(args.toString))
    ws.send(JSON.stringify(args.toString))
  }

  ws.onopen = (e: Event) => broadcastStatus("connected", statusIcons("receiving"))
  ws.onclose = (e: Event) => broadcastStatus("disconnected", statusIcons("error"))
  ws.onmessage = (e: MessageEvent) => {

    rootScope.$apply {
      println(s"Broadcasting message ${e.data.toString}")
      rootScope.$broadcast("serverEvent", JSON.parse(e.data.toString))
    }

  }
}

object WebsocketConnectionServiceFactory extends Factory[WebsocketConnectionService] {

  @inject
  var rootScope: RootScope = _

  override val name = "$websocketConnectionService"
  override def apply(): WebsocketConnectionService = new WebsocketConnectionService(rootScope)
}