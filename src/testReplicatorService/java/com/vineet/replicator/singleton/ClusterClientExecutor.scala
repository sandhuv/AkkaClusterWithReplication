package testReplicatorService.java.com.vineet.replicator.singleton

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.contrib.pattern.ClusterClient.SendToAll
import testReplicatorService.java.com.vineet.replicator.singleton.proto.hello

import scala.concurrent.duration.{FiniteDuration, _}

/**
  * Created by Vineet Sandhu on 09/05/15.
  */
class ClusterClientExecutor(clusterClient: ActorRef,registerInterval: FiniteDuration) extends Actor with ActorLogging{


   import context.dispatcher
   val registerTask = context.system.scheduler.schedule(5.seconds, registerInterval, clusterClient,
     SendToAll("/user/master/active", hello(append())))



   def append() : Set[String] ={
     Set("1", "2", "3")
   }

   override def receive: Receive = {
     case x => println("----" +x)
   }
 }


object ClusterClientExecutor{

  def props(clusterClient: ActorRef, registerInterval: FiniteDuration = 5.seconds): Props =
    return Props(classOf[ClusterClientExecutor], clusterClient, registerInterval)
}

