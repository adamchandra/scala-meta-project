package cc.acs.mongofs.gridfs

import cc.acs.util.StringOps._
import scala.collection.JavaConversions._

object MasterWorkerTaskingUI {
  
  def main(args: Array[String]) {
    val opts = argsToMap(args)
    val defaults = Map(
      "master"         -> List(""),
      "worker"         -> List(""),
      "host"       -> List("localhost"),
      "port"       -> List("")
      )

    // new MasterWorkerTaskingUI(opts ++ defaults)()
  }
}

class MasterWorkerTaskingUI(options: Map[String, List[String]]) {
  import cc.acs.util.Hash

  import com.mongodb.Mongo

  def dbname = options("db").head
  def collection = "corpus." + options("collection").head
  def mongodb = new Mongo().getDB(dbname)
  val gridfs: GridFS = new GridFS(mongodb, collection)
  val corpus = new Corpus(dbname)
 
  // def apply() {
  //   List("master"   -> startMaster _,
  //        "worker"   -> startWorker _
  //      ) map { 
  //     case (name, fn) =>
  //       if (options.contains(name)) fn()
  //   }
  // }
  // 
  // import se.scalablesolutions.akka.actor.Actor.actorOf
  // import se.scalablesolutions.akka.actor.RemoteActor
  // import se.scalablesolutions.akka.remote.RemoteNode

  // def startMaster() {
  //   val m = new Master(corpus.pdfFileCollection) 
  //   m.start
  //   m.join
  // 
  //   RemoteNode.start("localhost", 9999)
  //   RemoteNode.register("task-service", actorOf[Master])
  // }
  // 
  // 
  // def startWorker() {
  //   val actor = RemoteClient.actorFor("task-service", "localhost", 9999)
  //   val result = actor !! "Hello"
  //   val worker new Worker(m) {
  //     makeRemote()
  //     override def doWork(taskid: String) = { 
  //       // noop
  //     }
  //   })
  //   for (w <- workers) {
  //     w.start()
  //   }
  // }

}
