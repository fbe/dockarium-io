package dockarium.controller

import com.greencatsoft.angularjs.{inject, Controller}
import com.greencatsoft.angularjs.core.Scope
import dockarium.WebsocketConnectionService
import dockarium.api.Messages.{SaveDockerHost, DockerHost}
import org.scalajs.spickling.PicklerRegistry

import scala.scalajs.js
import scala.scalajs.js.JSON
import scala.scalajs.js.annotation.JSExportAll

/**
 * Created by becker on 2/19/15.
 */




@JSExportAll
case class DockerHostForm(var name: String, var host: String, var port: Int)


object AdminCtrl extends Controller {

  import org.scalajs.spickling.jsany._

  @inject
  var websocketConnectionService: WebsocketConnectionService = _

  override def initialize(): Unit = {
    super.initialize()

    scope.dockerhost = DockerHostForm("localhost", "localhost", 2375)

    scope.dynamic.saveDockerConnection = () => {
      println("save docker connection")
      val pickle: js.Any = PicklerRegistry.pickle(SaveDockerHost(DockerHost(scope.dockerhost.name, scope.dockerhost.host, scope.dockerhost.port)))
      println(JSON.stringify(pickle))
      websocketConnectionService.send(pickle)
    }

  }

  override type ScopeType = AdminCtrlScope

  trait AdminCtrlScope extends Scope {
    var dockerhost: DockerHostForm = js.native
  }
}