package cc.refectorie.spidie

import com.mongodb._
import org.bson.types.ObjectId
import com.osinka.mongodb._
import com.osinka.mongodb.shape._
import scala.xml._
import scala.util.matching.Regex
import scala.collection.mutable.{HashMap,ArrayBuffer}
import scala.io.Source
import java.io.File
import java.net.{URLConnection, URL, HttpURLConnection}
import java.util.Date
import scala.actors.Actor

class Spider(val host:String) extends Actor with SpidieLogging {
  info("Creating Spider for "+host)
  val delay = Exclusion.crawlDelay(host) * 1000 //if (host.contains("cs.umass.edu")) 0 else if (host.contains("umass.edu")) 100 else 10000 // milliseconds
  def act() = loop {
    while(true) receive {
      case url:URL => {
        if ((!Spidie.exiting) && Spidie.addPage(url)) {
          Thread.sleep(delay)
        }
      }
      case pendingBatchSize:Int => {
        for (address <- (Address where {(Address.code is ResponseCode.todo) and (Address.host is host)} take pendingBatchSize in Spidie.addresses)) { 
          this ! address.url
        }
      }
      case x => error("Spider got unknown message "+x)
    }
  }
}

