package test.akka

import org.scalatest._

import org.scalatest.junit.AssertionsForJUnit
import org.junit.Assert._
import org.junit.Test
import org.junit.Before
import org.junit.BeforeClass

class AkkaActorSpec extends FunSuite with AssertionsForJUnit with BeforeAndAfterAll {
  import cc.acs.commons.util.StringOps._
  import cc.acs.commons.util.FileOps._

  import se.scalablesolutions.akka.actor.Actor
  import se.scalablesolutions.akka.actor.Actor._

  test("local/remote actors are linked") {
    import scala.collection.mutable.HashMap

    import se.scalablesolutions.akka.actor.{SupervisorFactory, Actor, ActorRef, RemoteActor}
    import se.scalablesolutions.akka.remote.{RemoteNode, RemoteClient}
    import se.scalablesolutions.akka.stm.global._
    import se.scalablesolutions.akka.config.ScalaConfig._
    import se.scalablesolutions.akka.config.OneForOneStrategy
    // import se.scalablesolutions.akka.util.Logging
    import Actor._


    sealed trait Message
    case class Login(user: String) extends Message
    case class Logout(user: String) extends Message
    case class Request(who: String, what: String) extends Message
    case class Reply(id: String) extends Message
    case class Shutdown() extends Message

    class Client(val name: String) {
      val server = RemoteClient.actorFor("test:service", "localhost", 9998)
      
      def login = server !! Login(name)
      def logout = server !! Logout(name) 
      def request(message: String) = server !! Request(name, name + ": " + message)
    }

    trait ActionManagement { this: Actor =>
      val sessions: HashMap[String, ActorRef]

      protected def actionManagement: Receive = {
        case msg @ Request(from, _) => sessions(from) !! msg
        // case msg @ SomeForwardableMessage(from) => sessions(from) forward msg todo test this
      }
    }

    class Session(user: String) extends Actor {
      private val loginTime = System.currentTimeMillis

      log.info("New session for user [%s] has been created at [%s]", user, loginTime)

      def receive:Receive = {
        case msg @ Request(who, what) => {
          log.info("Session: %s requested %s".format(who, what))
          self.reply(Reply(user))
        }
      }
    }

    trait SessionManagement { this: Actor =>
      val sessions = new HashMap[String, ActorRef]
    
      protected def sessionManagement: Receive = {      
        case Login(username) => {
          log.info("User [%s] has logged in", username)
          val session = actorOf(new Session(username))
          session.start
          sessions += (username -> session)
        }

        case Logout(username) => {
          log.info("User [%s] has logged out", username)
          val session = sessions(username)
          session.stop
          sessions -= username
        }
      }
    
      protected def shutdownSessions =
        sessions.foreach { case (_, session) => session.stop }
    }

    trait Supervisor extends Actor {
      self.faultHandler = Some(OneForOneStrategy(5, 5000))
      self.trapExit = List(classOf[Exception])
      
      def receive = sessionManagement orElse actionManagement orElse shutdownHook
      
      protected def actionManagement: Receive
      protected def sessionManagement: Receive
      protected def shutdownSessions: Unit
      
      protected def shutdownHook: Receive = {
        case Reply(what) => println("supervisor got reply: " + what)
        case Shutdown() => shutdownSupervisor
      }


      def shutdownSupervisor = {
        log.info("Chat server is shutting down...")
        shutdownSessions
      }
    }

    class MasterService extends Supervisor 
    with ActionManagement 
    with SessionManagement {
      override def init = {
        import se.scalablesolutions.akka.remote.RemoteNode
        RemoteNode.start("localhost",9998)
        RemoteNode.register("test:service", self)
      }
    }
    
    actorOf(new MasterService()).start

    val c1 = new Client("client-1")
    c1.login
    c1.request("some lemonade")
    c1.logout

    RemoteNode.shutdown

  }
}
