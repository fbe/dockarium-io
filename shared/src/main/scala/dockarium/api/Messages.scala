package dockarium.api


/**
 * Created by becker on 2/22/15.
 */
object Messages {

  // currently no case object because of highlighting issues in idea
  case class AuthenticationRequired()

  case class DockerHost(name: String, host: String, port: Int)
  case class SaveDockerHost(host: DockerHost)
}

