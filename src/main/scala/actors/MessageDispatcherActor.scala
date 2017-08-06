package actors

import akka.actor.{ActorRef, ActorSelection, Props, UntypedAbstractActor}
import model.{Client, ClientImpl, MatchResult}

import scala.util.parsing.json.JSONObject

/** Actor that receive message from the server's actors, and send it trough the net to the addressee client.
  *
  * @author manuBottax
  */
class MessageDispatcherActor extends UntypedAbstractActor {

  var onlineClientIP: List[String] = List()

  override def onReceive(message: Any): Unit = ActorsUtils.messageType(message) match {

    case "onlineClient" => {
      val ip: String = message.asInstanceOf[JSONObject].obj("senderIP").toString
      onlineClientIP = onlineClientIP ::: List (ip)
    }

    case "registrationResult" => {

      val result: String = message.asInstanceOf[JSONObject].obj("result").toString
      val ip: String = message.asInstanceOf[JSONObject].obj("senderIP").toString

      println("registration has " + result)

      if (result == "success"){
        val reply = JSONObject(Map[String, Any](
          "object" -> "registrationResult",
          "result" -> true ))

        sendRemoteMessage(ip, reply)
      }

      else {
        val reply = JSONObject(Map[String, Any](
          "object" -> "registrationResult",
          "result" -> false ))

        sendRemoteMessage(ip, reply)
      }
    }

    case "loginError" => {
      val ip: String = message.asInstanceOf[JSONObject].obj("senderIp").toString

      println("Login has failed ")

      val reply: JSONObject = JSONObject(Map[String, Any](
        "object" -> "matches",
        "list" -> Option.empty[List[MatchResult]]))

      sendRemoteMessage(ip, reply)
    }

    case "previousMatchResult" => {
      val ip: String = message.asInstanceOf[JSONObject].obj("senderIP").toString

      println("Login ok ! Previous result loaded !")

      val reply: JSONObject = JSONObject(Map[String, Any](
        "object" -> "matches",
        "list" -> message.asInstanceOf[JSONObject].obj("list")))

      sendRemoteMessage(ip, reply)
    }

    case "ranges" => {
      val ip: String = message.asInstanceOf[JSONObject].obj("senderIP").toString

      println("sending available ranges")

      val reply: JSONObject = JSONObject(Map[String, Any](
        "object" -> "ranges",
        "list" -> message.asInstanceOf[JSONObject].obj("list")))

      sendRemoteMessage(ip, reply)
    }

    case "characterToChoose" => {
      val ip: String = message.asInstanceOf[JSONObject].obj("senderIP").toString

      println("sending available characters")

      sendRemoteMessage(ip, message)
    }

    case "availableCharacter" => {
      val available: Boolean = message.asInstanceOf[JSONObject].obj("available").asInstanceOf[Boolean]
      val ip: String = message.asInstanceOf[JSONObject].obj("senderIp").toString

      println("sending: character is available: " + available)

      sendRemoteMessage(ip, message)
    }

    case "notifySelection" => {
      val ip: String = message.asInstanceOf[JSONObject].obj("senderIp").toString


      notifyOtherClient(ip, message)
    }

    case "AvailablePlaygrounds" => {

      val ip: String = message.asInstanceOf[JSONObject].obj("senderIp").toString

      println("sending available playgrounds")


      val reply: JSONObject = JSONObject(Map[String, Any](
        "object" -> "playgrounds",
        "list" -> message.asInstanceOf[JSONObject].obj("list")))

      sendRemoteMessage(ip, reply)

    }

    case "playgroundChosen" => broadcastMessage(message.asInstanceOf[JSONObject])

    case "otherPlayerIP" => {
      val ip: String = message.asInstanceOf[JSONObject].obj("senderIp").toString
      println("sending other player IPs")

      sendConfigurationMessage(ip, message)
    }

    case "resultSaved" =>  {
      val ip: String = message.asInstanceOf[JSONObject].obj("senderIP").toString

      println("result saved in DB")

      sendRemoteMessage(ip, message)
    }

      ///// client bootstrap ///////////////////
    case "clientCanConnect" => {
      val reply: JSONObject = JSONObject(Map[String, Any](
        //result" -> PeerBootstrapMessages.CLIENT_CAN_START_RUNNING,
        "result" -> "ClientCanStartRunning"))

      broadcastConfigurationMessage(reply)
    }

    case _ => println(getSelf() + "received unknown message: " + ActorsUtils.messageType(message))
  }



  private def sendRemoteMessage(ipAddress: String, message: Any): Unit = {
    //val clientActorName = "fakeReceiver"
    val clientActorName = "messageReceiver"
    //val receiver: ActorSelection = context.actorSelection("akka.tcp://ClientSystem@" + to.ipAddress + "/user/" + clientActorName)
    //todo come siamo rimasti per le porte ? -> conviene mandare nel messaggio (Ip + porta)
    //val receiver: ActorSelection = context.actorSelection("akka.tcp://DpacServer@" + to.ipAddress + ":4552" + "/user/" + clientActorName)
    val receiver: ActorSelection = context.actorSelection("akka.tcp://DpacClient@" + ipAddress + ":2554" + "/user/" + clientActorName)
    receiver ! message
  }

  //todo: invio il messaggio ad uno degli attori che è quello della fede (basta beccarlo con la selection).
  private def sendConfigurationMessage(ipAddress: String, message: Any): Unit = {

    //todo come siamo rimasti per le porte ? -> come faccio a trovare il tuo Thread per mandargli i messaggi ?
    //val receiver: ActorSelection = context.actorSelection("akka.tcp://DpacClient@" + to.ipAddress + ":4552" + "/ClientWorkerThread")
    val receiver: ActorSelection = context.actorSelection("akka.tcp://DpacServer@" + ipAddress + ":4552" + "/user/" + "fakeReceiver")
    receiver ! message
  }

  //todo: questo va fatto solo per la partita interessata
  private def broadcastMessage(message: JSONObject): Unit = {

    onlineClientIP.foreach((x) => sendRemoteMessage(x,message))
  }

  //todo: questo va fatto solo per la partita interessata
  private def broadcastConfigurationMessage(message: JSONObject): Unit = {
    onlineClientIP.foreach((x) => sendConfigurationMessage(x, message))
  }

  //todo: questo va fatto solo per la partita interessata
  private def notifyOtherClient(excludedClient: String, message: Any): Unit =  {
    onlineClientIP.foreach((x) => if (x != excludedClient) sendRemoteMessage(x,message))
  }


}