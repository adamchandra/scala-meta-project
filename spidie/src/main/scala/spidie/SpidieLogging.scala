package cc.refectorie.spidie
import java.io.FileOutputStream

object SpidieLogger {
  val logger = new Logger("cc.refectorie.spidie", new FileOutputStream("/tmp/spidie.log"), Logger.WARN)
}

trait SpidieLogging extends GlobalLogging {
  override def logger = SpidieLogger.logger
}
