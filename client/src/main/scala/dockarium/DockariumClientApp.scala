package dockarium

import com.greencatsoft.angularjs.Angular
import dockarium.controller._

import scala.scalajs.js.JSApp
import scala.scalajs.js.annotation.JSExport

/**
 * Created by becker on 2/18/15.
 */
@JSExport
object DockariumClientApp extends JSApp {

  override def main(): Unit = {

    val module = Angular.module("dockariumclient", Seq("ui.bootstrap", "chart.js", "ui.router"))
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


  }
}
