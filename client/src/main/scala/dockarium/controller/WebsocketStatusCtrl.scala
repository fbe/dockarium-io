package dockarium.controller

import com.greencatsoft.angularjs.core.Scope
import com.greencatsoft.angularjs.{inject, Controller}
import dockarium.{ServerConnectionStatus, WebsocketConnectionService}

import scala.scalajs.js

/**
 * Created by becker on 2/19/15.
 */
object WebsocketStatusCtrl extends Controller {

  @inject
  var websocketConnectionService: WebsocketConnectionService = _

  override def initialize(): Unit = {
    super.initialize()

    scope.status = websocketConnectionService.serverConnectionStatus

    scope.$on("WebSocketStatusChange", { () =>
      scope.status = websocketConnectionService.serverConnectionStatus
    })


  }

  override type ScopeType = StatusScope

  trait StatusScope extends Scope {
    var status: ServerConnectionStatus = js.native
  }
}



/*

    $scope.status = serverConnection.status;

    $scope.$on('WebSocketStatusChange', function(){
        $scope.status = serverConnection.status;
    });
 */