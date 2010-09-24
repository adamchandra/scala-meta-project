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

import com.mongodb._
import com.mongodb.util._

import org.bson._

object GridFSFile { 
  import scala.collection.immutable.Set
  import cc.acs.util.StringOps._
  val VALID_FIELDS = Set[String]("_id filename contentType length chunkSize uploadDate aliases md5".wsv: _*)
}


case class GridFSFile(fs:GridFS) extends DBObject {
  var id: Object = new Object()
  var filename: String = null
  var contentType: String = null
  var length :Long = 0
  var chunkSize: Long = 0
  var uploadDate: java.util.Date = null
  var aliases :List[String] = List()
  var metadata :DBObject = new BasicDBObject() 
  var md5 :String = ""

  def save() { fs.filesCollection.save(this) }

  def validate() {
    // DBObject res = _fs._db.command( new BasicDBObject( "filemd5" , _id ) );
    // String m = res.get( "md5" ).toString();
    // if ( m.equals( _md5 ) )
    //   return;
    // 
    // throw new MongoException( "md5 differ.  mine [" + _md5 + "] theirs [" + m + "]" );
  }

  def numChunks(): Int = {
    var d = length
    d = d / chunkSize
    Math.ceil(d).toInt
  }

  def getAliases():List[String] = metadata.get( "aliases" ).asInstanceOf[List[String]]

  override def put(key:String, v:Object): Object = {
    var prev = get(key)
    key match {
      case "id"          => id = v
      case "filename"    => filename = v.asInstanceOf[String]
      case "contentType" => contentType = v.asInstanceOf[String]
      case "length"      => length = v.asInstanceOf[Long]
      case "chunkSize"   => chunkSize = v.asInstanceOf[Long]
      case "uploadDate"  => uploadDate = v.asInstanceOf[java.util.Date]
      case "md5"         => md5 = v.asInstanceOf[String]
      case _             => metadata.put(key,v)
    }
    prev
  }

  override def get(key:String): Object = key match {
    case "id"          => id
    case "filename"    => filename
    case "contentType" => contentType
    case "length"      => length.asInstanceOf[AnyRef]
    case "chunkSize"   => chunkSize.asInstanceOf[AnyRef]
    case "uploadDate"  => uploadDate
    case "md5"         => md5
    case _             => metadata.get( key )
  }

  override def putAll(o:BSONObject):Unit = throw new UnsupportedOperationException();
  override def putAll(m: java.util.Map[_,_]):Unit = throw new UnsupportedOperationException()
  override def toMap(): java.util.Map[String, Any] = throw new UnsupportedOperationException()
  override def removeField(key: String): Object = throw new UnsupportedOperationException()

  override def containsKey(s:String): Boolean = containsField(s)
  override def containsField(s:String): Boolean = keySet().contains(s)

  import scala.collection.JavaConversions._
  override def keySet():java.util.Set[String] =  GridFSFile.VALID_FIELDS ++ metadata.keySet

  def isPartialObject():Boolean = false
  def markAsPartialObject = throw new RuntimeException( "can't load partial GridFSFile file" )

  override def toString():String = JSON.serialize(this)
}
