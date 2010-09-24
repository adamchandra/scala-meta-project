// GridFSInputFile.java

/**
 *      Copyright (C) 2008 10gen Inc.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package cc.acs.mongofs.gridfs


import java.io._;
import java.security._;
import java.util._;

import org.bson.types._;

import com.mongodb.DBObject
import com.mongodb.BasicDBObject
import com.mongodb.BasicDBObjectBuilder
import com.mongodb.MongoException
import com.mongodb.util.SimplePool
import com.mongodb.util.Util

class GridFSInputFile(fs: GridFS, in:InputStream, filename:String) extends GridFSFile(fs) {
  // override val id = new ObjectId();
  // val chunkSize = GridFS.DEFAULT_CHUNKSIZE;
  // val uploadDate = new Date();
  // val _in: InputStream
  var saved = false


  def getMetaData(): DBObject = {
    if ( metadata == null )
      metadata = new BasicDBObject()
    metadata
  }

  def setContentType(ct:String) {
    contentType = ct;
  }


  override def save() {
    save(GridFS.DEFAULT_CHUNKSIZE);
  }
  
  def save(chunkSize:Int) {
    if ( !saved ){
      try {
        saveChunks(chunkSize);
      }
      catch {
        case ioe:IOException => throw new MongoException( "couldn't save chunks" , ioe );
      }
    }
    super.save()
  }

  
  def saveChunks(chunkSize:Int): Int = {
    if ( saved )
      throw new RuntimeException( "already saved!" );
    
    if ( chunkSize > 3.5 * 1000 * 1000 )
      throw new RuntimeException( "chunkSize must be less than 3.5MiB!" );
    
    val b = new Array[Byte](1)

    var total = 0
    var cn = 0
    
    val md:MessageDigest = md5Pool.get();
    md.reset();
    val dinstr:DigestInputStream  = new DigestInputStream(in , md)
    
    var start = 0
    do {
      start = 0
      while ( start < b.length ) try {
        val r = dinstr.read( b , start , b.length - start );
        if ( r == 0 )
          throw new RuntimeException( "i'm doing something wrong" );
        if ( r < 0 )
          throw new java.lang.RuntimeException()
        start += r;
      }
      catch {case _ => /* noop */ }
      
      total += start;
      
      var mine = b;
      
      if ( start != b.length ){
        val mine = new Array[Byte](start)
        System.arraycopy(b, 0, mine, 0, start)
      }

      cn += 1

      var chunk = BasicDBObjectBuilder.start()
      .add( "files_id" , id )
      .add( "n" , cn+1 )
      .add( "data" , mine )
      .get()
      
      fs.chunkCollection.save( chunk );
      
    } while  ( start < b.length )

    
    md5 = Util.toHex(md.digest())
    md5Pool.done( md )
    
    length = total
    saved = true;
    return cn;
  }
  
  var md5Pool: SimplePool[MessageDigest] = new SimplePool[MessageDigest]("md5" , 10 , -1 , false , false) {
    override def createNew(): MessageDigest = {
      try MessageDigest.getInstance("MD5")
      catch{case _: java.security.NoSuchAlgorithmException => null}
    }
  }
}
