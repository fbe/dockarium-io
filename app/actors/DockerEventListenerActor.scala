package actors

import akka.actor.{ActorRef, Actor}
import akka.io.IO
import akka.io.Tcp.Connected
import play.libs.Akka
import spray.can.Http
import spray.http._

/**
 * Created by becker on 2/4/15.
 */

case class DockerEvent(status: String, id: String, from: String, time: Long)

import play.api.libs.json._


class DockerEventListenerActor(host: String, port: Int, dockariumActor: ActorRef) extends Actor {


  implicit val personFormat = Json.format[DockerEvent]

  println("Starting DockerEventListenerActor")

  implicit val actorSystem = context.system

  IO(Http) ! Http.Connect(host, port)

  override def receive: Receive = {

    case Connected(_,_) =>
      println(s"connected to $host:$port")
      sender() ! HttpRequest(HttpMethods.GET, Uri("/v1.16/events"))
      context.become(connected)


    case x => println(x + " - " + x.getClass.getCanonicalName)
  }

  def connected: Receive = {
    case _: ChunkedResponseStart =>
      println("Chunked response started")
      context.become(receivingChunks)

    case x => println(x)
  }

  def receivingChunks: Receive = {
    case MessageChunk(data, _) =>
      println(new String(data.toByteArray))
      val jsValue = Json.parse(data.toByteArray)
      Json.fromJson[DockerEvent](jsValue).foreach(event => dockariumActor ! event)


    case unknown =>
      println("Unknown data received. Data: " + unknown + " - Class: " + unknown.getClass.getCanonicalName)
  }

}