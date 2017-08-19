package utils

import actors.MessageDispatcherActor
import model.Match

/**
  * Created by lucch on 16/08/2017.
  */
case class TimerImpl(actor: MessageDispatcherActor) {

  def waitingFor(milliseconds: Long, currentMatch: Match): Unit ={
    val max = currentMatch.size.end
    new Thread{
      override def run(): Unit = {
        val time = System.currentTimeMillis()
        while((System.currentTimeMillis() < time+milliseconds)||(currentMatch.readyPlayer < max)){}
        actor.synchroStartMatch(currentMatch)
      }
    }.start()
  }

}