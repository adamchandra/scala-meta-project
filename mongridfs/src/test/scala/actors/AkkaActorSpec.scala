package test.akka

import org.scalatest._

import org.scalatest.junit.AssertionsForJUnit
import org.junit.Assert._
import org.junit.Test
import org.junit.Before
import org.junit.BeforeClass

// todo rename this or break it up into other specs
class AkkaActorSpec extends FunSuite with AssertionsForJUnit with BeforeAndAfterAll {
  import cc.acs.commons.util.StringOps._
  import cc.acs.commons.util.FileOps._

  import se.scalablesolutions.akka.actor.Actor
  import se.scalablesolutions.akka.actor.Actor._
  // import se.scalablesolutions.akka.actor.RemoteActor
  // import se.scalablesolutions.akka.remote.RemoteClient

  test("simple actors work") {
    class MyActor extends Actor {
      def receive = {
        case "test" => log.info("received test")
        case _ => log.info("received unknown message")
      }
    }
  }

  test("actors join a cluster, get broadcast messages") {
    import se.scalablesolutions.akka.remote.Cluster
    case object Ping

    actor {
      case Ping => println("pinged")
      case _ => log.info("received unknown message")
    }

    Cluster.relayMessage(classOf[Actor], Ping)
  }
  
}
