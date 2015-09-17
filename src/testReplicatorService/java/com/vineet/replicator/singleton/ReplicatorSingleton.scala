package testReplicatorService.java.com.vineet.replicator.singleton

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.cluster.ClusterEvent.LeaderChanged
import akka.cluster.{Cluster, ClusterEvent}
import akka.contrib.datareplication.Replicator._
import akka.contrib.datareplication.{DataReplication, ORSet}
import akka.contrib.pattern.{ClusterReceptionistExtension, DistributedPubSubExtension}
import testReplicatorService.java.com.vineet.replicator.singleton.ReplicatorSingleton.Registry.Protocol.helloReplicator
import testReplicatorService.java.com.vineet.replicator.singleton.proto.hello

import scala.concurrent.duration.FiniteDuration

/**
 * Created by Vineet Sandhu on 07/05/15.
 */

object ReplicatorSingleton{

  class Registry extends Actor with ActorLogging{
    import Registry.Protocol._
    import Registry._

    val key = "collectors"
    implicit val cluster = Cluster(context.system)
    val replicator = DataReplication(context.system).replicator
    var collectors = Set.empty[MessageCollector]
    var leader = false

    //wrappedBecome(initialReceive)

    override def preStart(): Unit = {
      replicator ! Subscribe(key, self)
      cluster.subscribe(self, ClusterEvent.InitialStateAsEvents, classOf[ClusterEvent.LeaderChanged])
    }

    override def postStop = {
      cluster.unsubscribe(self)
    }

    override def receive : Receive = {

      case helloReplicator(collector) =>
        println("message in replicator  : " +collector)
        replicator ! Update(key, ORSet(), WriteLocal)(_ + collector)

      case Changed(key, data: ORSet[MessageCollector] @unchecked) =>
        val allCollectors = data.elements
        collectors = allCollectors
        collectors.foreach(println)

      case LeaderChanged(node) =>  leader = node.exists(_ == cluster.selfAddress)

      case _: UpdateResponse => // ok
      case x => println("registry message " +x)

    }

  }

  object Registry {

    def activeCollectorsForProfile(collectors: Set[MessageCollector]) =
      collectors.map(_.message).toList

    def mkActiveCollectorsEvent(collectors: Set[MessageCollector]) = {
      val profileCollectors = activeCollectorsForProfile(collectors)
    }
    def props = Props[Registry]
    case class MessageCollector(message : Set[String])
    object Protocol {
      case class helloReplicator(msg: Set[String])
    }

  }

}

class ReplicatorSingleton(workTimeout: FiniteDuration, registry : ActorRef) extends Actor with ActorLogging{

  val mediator = DistributedPubSubExtension(context.system).mediator
  ClusterReceptionistExtension(context.system).registerService(self)

  println("***ACTIVE*** & ++++Standby++++ singletonActor ")

  override def receive: Receive = {
    case hello(msg) => {
      println("message in Singleton Actor : " + msg)
      registry ! helloReplicator(msg)
    }
    case x =>println("anything else :  " +x)
  }
}
object proto {
  case class hello(msg : Set[String])
}