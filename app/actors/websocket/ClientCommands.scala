package actors.websocket

import play.api.libs.json.JsValue


/**
 * Created by becker on 2/7/15.
 */

object ClientCommands {

  case object GetServerInfo
  case object GetMemInfo
  case object GetServerVersion

  case class ClientCommand(command: String, payload: Option[JsValue])
  case class SaveDockerConnection(name: String, host: String, port: Int)

}

