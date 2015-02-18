package docker

/**
 * Created by becker on 2/7/15.
 */
object DockerCommands {

  /// """{"Image": "busybox", "Cmd": [ "cat", "/proc/meminfo" ] }

  case class CreateContainersCommand(Image: String, Cmd: Seq[String])



}
