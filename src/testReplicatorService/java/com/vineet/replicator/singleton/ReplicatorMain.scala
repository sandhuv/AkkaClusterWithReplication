package testReplicatorService.java.com.vineet.replicator.singleton

import akka.actor._
import akka.contrib.pattern.{ClusterClient, ClusterSingletonManager}
import akka.japi.Util._
import com.typesafe.config.ConfigFactory
import testReplicatorService.java.com.vineet.replicator.singleton.ReplicatorSingleton.Registry

import scala.concurrent.duration._

/**
 * Created by Vineet Sandhu on 09/05/15.
 */

object Backend_Main_1 {

  def main(args: Array[String]): Unit = {
    startSingletonservice(2418, "backend")
  }

  def workTimeout = 10.seconds

  def startSingletonservice(port: Int, role: String): Unit = {
    val conf = ConfigFactory.parseString(s"akka.cluster.roles=[$role]").
      withFallback(ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port)).
      withFallback(ConfigFactory.load())
    val system = ActorSystem("ClusterSystem", conf)

    val registry = system.actorOf(Registry.props, "registry")

    system.actorOf(ClusterSingletonManager.props(
      Props(classOf[ReplicatorSingleton], workTimeout, registry), "active", PoisonPill, Some(role)), "master")
  }
}



object Backend_Main_2 {

  def main(args: Array[String]): Unit = {
    startSingletonservice(2419, "backend")
  }

  def workTimeout = 10.seconds

  def startSingletonservice(port: Int, role: String): Unit = {
    val conf = ConfigFactory.parseString(s"akka.cluster.roles=[$role]").
      withFallback(ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port)).
      withFallback(ConfigFactory.load())
    val system = ActorSystem("ClusterSystem", conf)

    val registry = system.actorOf(Registry.props, "registry")

    system.actorOf(ClusterSingletonManager.props(
      Props(classOf[ReplicatorSingleton], workTimeout, registry), "active", PoisonPill, Some(role)), "master")
  }
}



object ClusterClientWorkerMain {

  def main(args: Array[String]): Unit = {
    startWorker(5001, "exec")
  }

  def workTimeout = 5.seconds

  def startWorker(port: Int, role: String): Unit = {
    val conf = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port).
      withFallback(ConfigFactory.load("exec"))

    val system = ActorSystem("WorkerSystem", conf)

    val initialContacts = immutableSeq(conf.getStringList("contact-points")).map {
      case AddressFromURIString(addr) ⇒ system.actorSelection(RootActorPath(addr) / "user" / "receptionist")
    }.toSet

    val clusterClient = system.actorOf(ClusterClient.props(initialContacts), "clusterClient")
    system.actorOf(ClusterClientExecutor.props(clusterClient), "worker")
  }
}