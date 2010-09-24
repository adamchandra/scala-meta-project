package cc.acs.mongofs.gridfs

import cc.acs.util.StringOps._

import scala.collection.JavaConversions._

object GridFSUI {
  
  def main(args: Array[String]) {
    val opts = argsToMap(args)
    val defaults = Map(
      "db"         -> List("rexa"),
      // "collection" -> List(),
      "host"       -> List("localhost"),
      "port"       -> List("")
      )

    // todo ensure unique index on filename
    // in java:  _chunkCollection.ensureIndex( BasicDBObjectBuilder.start().add( "files_id" , 1 ).add( "n" , 1 ).get() );

    new GridFSUI(opts ++ defaults)()
  }
}

class GridFSUI(options: Map[String, List[String]]) {
  import cc.acs.util.Hash

  import com.mongodb.Mongo

  def dbname = options("db").head
  def collection = "corpus." + options("collection").head
  def mongodb = new Mongo().getDB(dbname)
  val gridfs: GridFS = new GridFS(mongodb, collection)
  val corpus = new Corpus(dbname)
 
  def apply() {
    List("list"   -> listFiles _,
         "get"    -> get _,
         "put"    -> put _,
         "drop"   -> drop _,
         "update" -> update _,
         "md5"    -> md5 _,
         "sha"    -> sha1 _
       ) map { 
      case (name, fn) =>
        if (options.contains(name)) fn()
    }
  }

  def listFiles() {
    val ffs = gridfs.getFileList()
    while (ffs.hasNext) {
      val f = ffs.next
      println("%-60s %-10d".format(f.get("filename"), f.get("length")))
    }
  }

  def md5sum(is: java.io.InputStream): String = {
    import cc.acs.util.{ Hash => Digest }
    Digest.toHex(Digest("md5", is))
  }

  def sha1sum(is: java.io.InputStream): String = {
    import cc.acs.util.{ Hash => Digest }
    Digest.toHex(Digest("sha1", is))
  }

  def md5() {
    val filename = options("md5").head
    val f = gridfs.findOne(filename)
    val md5 = md5sum(f.getInputStream)
    println("%40s %-10s".format(md5, f.get("filename")))
  }

  def sha1() {
    val filename = options("sha").head
    val f = gridfs.findOne(filename)
    val md5 = sha1sum(f.getInputStream)
    println("%40s %-10s".format(md5, f.get("filename")))
  }

  import cc.acs.util.FileOps._
  def sha1(f: java.io.File): String = sha1sum(fistream(f))

  def maybe[T](t: T): Option[T] = if (t != null) Some(t) else None

  def drop:Unit = drop(corpus.pdfFiles)
  import com.mongodb.DBObject

  def drop(gridfs: GridFS) {
    val ffs:Iterable[DBObject] = gridfs.getFileList()
    for (f <- ffs) {
      gridfs.remove(f)
    }
  }

  def put:Unit = for (arg <- options("put")) put(corpus.pdfFiles, arg)

  def put(coll: GridFS, fname:String) {
    try {
      val fsha = sha1(file(fname))
      // todo: after ensureIndex specifies unique sha id, make this try/catch w/o a findOne
      maybe(gridfs.findOne(fsha)) match {
        case Some(f) => {
          println("duplicate file sha: " + fsha)
        }
        case None => {
          println("putting " + fname + "; sha: " + fsha)
          val gf = gridfs.createFile(fistream(fname), fsha)
          gf.save()
          // todo validate saved file
        }
      }
    } catch {
      case e: Exception => println("file not found: " + fname)
    }
  }

  def get() { 
    for (arg <- options("get")) {
      gridfs findOne arg match {
        case null => println( "can't find file: " + arg )
        case f    => f.writeTo(f.getFilename)
      }
    }
  }

  def update() {
    for (arg <- options("update")) {
      
    }
  }
}
