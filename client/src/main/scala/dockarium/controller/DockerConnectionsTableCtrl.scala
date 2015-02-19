package dockarium.controller

import java.util

import com.greencatsoft.angularjs.{inject, Controller}
import com.greencatsoft.angularjs.core.{Scope, RootScope}

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExportAll, JSExport}

/**
 * Created by becker on 2/19/15.
 */

object DockerConnectionsTableCtrl extends Controller {

  @JSExportAll
  case class ConnectionStatus(name: String, address: String, port: String, status: String)

  @inject
  var rootScope: RootScope = _

  override def initialize(): Unit = {
    super.initialize()

    scope.connections = Set(ConnectionStatus("foo", "bar", "foobar", "barfoo"), ConnectionStatus("foo1", "bar1", "foobar", "barfoo"))

  }

  override type ScopeType = ConnectionStatusScopeType

  trait ConnectionStatusScopeType extends Scope {
    var connections: Set[ConnectionStatus] = js.native
  }

}
