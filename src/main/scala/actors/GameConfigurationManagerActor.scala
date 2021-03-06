package actors

import akka.actor.UntypedAbstractActor
import model.Match
import utils.ActorsUtils
import scala.util.parsing.json.JSONObject

/** Actor that manage the people waiting for a match and the initial configuration of the game.
  *
  * @author manuBottax
  */
class GameConfigurationManagerActor extends UntypedAbstractActor {


  private val availableRange: List[Range] = List( Range(3,5),
                                                  Range(6,9))

  private var waitingMatch: List[Match] = List()
  private var startedMatch: List[Match] = List()

  override def onReceive(message: Any): Unit = ActorsUtils.messageType(message) match {

    case "rangesRequest" => {

      println("Request for the available ranges")

      sender() ! JSONObject(Map[String, Any](
        "object" -> "ranges",
        "list" -> availableRange,
        "senderIP" -> message.asInstanceOf[JSONObject].obj("senderIP").toString))

    }

    case "selectedRange" => {

      val ip: String = message.asInstanceOf[JSONObject].obj("senderIP").toString
      val range: Range = message.asInstanceOf[JSONObject].obj("range").asInstanceOf[Range]

      println("range selected: " + range.toString())

      val selectedMatch: Option[Match] = getWaitingMatchFor(range) headOption

      if (selectedMatch.isDefined){
        if (selectedMatch.get.addPlayer(ip)) {
          println("Assigned to match n° " + selectedMatch.get.id)
          println("Player in match: " + selectedMatch.get.involvedPlayerIP.size)

          sender() ! JSONObject(Map[String, Any](
            "object" -> "newPlayerInMatch",
            "match" -> selectedMatch.get,
            "senderIP" -> ip))
        }

        else{
          System.err.println(s"Player $ip already in match, cannot add. Assign to a new match.")
          val current = new Match(List(),range)
          current.addPlayer(ip)

          println("Assigned to a new match ( n° " + current.id + " )")

          waitingMatch = List(current) ::: waitingMatch

          val selection = context.actorSelection("/user/messageDispatcher")

          selection ! JSONObject(Map[String, Any](
            "object" -> "removePlayerFromMatch",
            "match" -> selectedMatch.get,
            "senderIP" -> ip ))

          selection ! JSONObject(Map[String, Any](
            "object" -> "newPlayerInMatch",
            "match" -> current,
            "senderIP" -> ip ))
        }
      }

      else {
        println("No Available Match for this range, create a new One")
      }
    }

    // the message sent from client when configuring the P2P communication.
  case "serverIsRunning" => {

    val senderIP: String = message.asInstanceOf[JSONObject].obj("senderIP").toString

    val currentMatch: Option[Match] = getMatchFor(senderIP)

    if (currentMatch.isDefined) {
      println("Client match found !")
      currentMatch.get.addReadyPlayer(senderIP)

      println("Client P2P startup completed !")

      if (currentMatch.get canStart) {
        println("All Clients P2P startup completed, game can start !")
        sender() ! JSONObject(Map[String, Any](
          "object" -> "clientCanConnect",
          "senderIP" -> senderIP
        ))

        context.actorSelection("../characterManager") ! JSONObject(Map[String, Any]( "object" -> "clear" ))
      }
    }

    else {
      System.err.println("Error ! non connected client want to start a game ! ")
    }
  }

      // received when a friend accept a friend request
    case "updateMatch" =>{
      val currentMatch: Match = message.asInstanceOf[JSONObject].obj("match").asInstanceOf[Match]
      val m = getMatchFor(currentMatch.involvedPlayerIP.headOption.get)

      m.get.involvedPlayerIP = currentMatch.involvedPlayerIP
    }

    case "setStartedMatch" => {
      val currentMatch: Match = message.asInstanceOf[JSONObject].obj("match").asInstanceOf[Match]

      println("current match: " + waitingMatch.size)

      println("removing the selected ")

      val selected = waitingMatch.filter(x => x.id == currentMatch.id)

      waitingMatch = waitingMatch.filterNot(x => x.id == currentMatch.id)

      startedMatch = startedMatch ::: selected

      println("current match: " + waitingMatch.size)
    }

    case _ => println(getSelf() + "received unknown message: " + ActorsUtils.messageType(message))
  }


  private def getWaitingMatchFor(size: Range): List[Match] = {

    val matches: List[Match] = waitingMatch.filter((x) => size == x.size)

    if (matches.isEmpty) {
      waitingMatch = List(new Match(List(),size)) ::: waitingMatch
      return waitingMatch
    }

    matches
  }

  private def getMatchFor(clientIP: String): Option[Match] = {

    var selected = startedMatch.find((x) => x.involvedPlayerIP.contains(clientIP))

    val waiting = waitingMatch.find((x) => x.involvedPlayerIP.contains(clientIP))

    if ( waiting.isDefined ) selected = waiting

    if( selected.isDefined) {
      println("selected match n° " + selected.get.id)
    }

    selected

  }
}


