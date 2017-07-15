package actors

import actors.GameMasterActor.FriendMessage
import akka.actor.Actor

/** Actor that manage the research of friends for a private match.
  *
  *  @author manuBottax
  */
class FriendManagerActor extends Actor {


  override def preStart(): Unit = {

    //todo: init()

  }

  def receive = {

    case FriendMessage => println(s"new Friend message !")

    case _  => println ("received unknown message")
  }

}

object FriendManagerActor {

  // todo: messaggi di risposta

}
