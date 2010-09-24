package cc.acs.mongofs.gridfs

import cc.acs.mongofs.gridfs._
import org.bson.types.ObjectId
import com.osinka.mongodb.DBObjectCollection
import com.mongodb.DBObject

import cc.acs.util.Hash

import scala.collection.JavaConversions._
import com.mongodb.Mongo

import se.scalablesolutions.akka.actor.Actor
import se.scalablesolutions.akka.actor.Actor.actorOf
// import se.scalablesolutions.akka.actor.RemoteActor
// import se.scalablesolutions.akka.remote.RemoteClient

case class WorkerOnline(who: Actor)
case class WorkerReady(who: Actor)
case class WorkOrder(taskname: String)
case class WorkComplete(who: Actor, name: String)
case object Stop
case object Start


abstract class Master extends Actor {
  var lock = new scala.concurrent.Lock()
  lock.acquire
  def join = lock.acquire

  def nextTaskName():Option[String]

  var workerCount = 0

  def receive  = {
    case WorkerOnline(worker) => {
      workerCount += 1
      log("worker count = " + workerCount)
    }
    case WorkerReady(worker) => {
      log("worker ready signal from " + worker)
      val actor = actorOf(worker)
      val task = nextTaskName
      task match {
        case Some(task) => actor ! WorkOrder(task)
        case None => {
          actor ! Stop
          workerCount -= 1
          if (workerCount == 0) {
            log("exiting")
            lock.release
            exit()
          }
        }
      }
    }
    case WorkComplete(worker, task) => {
      log("master got work complete msg for " + task)
    }
    case Stop => exit
  }


  override def toString = "master"

  def log(s: String) {
    println(this.toString + ": " + s)
  }
}

object Worker {
  private val nextId = new java.util.concurrent.atomic.AtomicLong(0)
  def hostname = java.net.InetAddress.getLocalHost().getHostName()
  def nextWorkerId: String = {
    nextId.set(nextId.get + 1)
    hostname + ":" + nextId.get
  }
}


// abstract class Worker(host:String, port: Int, service: String) extends RemoteActor(host, port) {
abstract class Worker extends Actor {
  def doWork(taskid: String)

  val workerId = Worker.nextWorkerId

  // val master = RemoteClient.actorFor(service, host, port)

  // master ! WorkerOnline(this)
  // master ! WorkerReady(this)
  log("started")


  def receive = {
    case WorkOrder(name) => {
      
      log("working on " + name)
      // master ! WorkComplete(this, name)
      // master ! WorkerReady(this)
    }
    case Stop => {
      log("exiting")
      exit()
    }
  }

  def log(s: String) {
    println(this.toString + ": " + s)
  }

  override def toString = "worker:" + workerId
}
