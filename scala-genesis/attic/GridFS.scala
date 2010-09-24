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

import scala.collection.immutable.List

import java.io._
import java.util._

import com.mongodb._

import org.bson._
import org.bson.types._

/**
 *  Implementation of GridFS v1.0
 *  <a href="http://www.mongodb.org/display/DOCS/GridFS+Specification">GridFS 1.0 spec</a>
 * @dochub gridfs
 */
object GridFS {
  val DEFAULT_CHUNKSIZE = 256 * 1024
  val DEFAULT_BUCKET = "fs"
}

class GridFS(db: DB, bucket: String = GridFS.DEFAULT_BUCKET) {
  val filesCollection = db.getCollection(bucket + ".files")
  val chunkCollection = db.getCollection(bucket + ".chunks")

  chunkCollection.ensureIndex(BasicDBObjectBuilder.start().add("files_id", 1).add("n", 1).get());
  filesCollection.setObjectClass(classOf[GridFSDBFile]);

  def idQuery(id: ObjectId): BasicDBObject = new BasicDBObject("_id", id)
  def filesIdQuery(id: ObjectId): BasicDBObject = new BasicDBObject("files_id", id)
  def filenameQuery(fn: String): BasicDBObject = new BasicDBObject("filename", fn)
  val sortByFilename = new BasicDBObject("filename", 1)

  def getFileList(): DBCursor = {
    filesCollection.find().sort(sortByFilename);
  }

  /**
   *   Returns a cursor for this filestore
   *
   * @param query filter to apply
   * @return cursor of file objects
   */
  def getFileList(query: DBObject): DBCursor = {
    filesCollection.find(query).sort(sortByFilename)
  }

  def findOne(id: ObjectId): GridFSDBFile = findOne(idQuery(id))
  def findOne(filename: String): GridFSDBFile = findOne(filenameQuery(filename))
  def findOne(query: DBObject): GridFSDBFile = _fix(filesCollection.findOne(query))

  def find(id: ObjectId): GridFSDBFile = findOne(id)
  def find(filename: String): List[GridFSDBFile] = find(filenameQuery(filename))

  def find(query: DBObject): List[GridFSDBFile] = {
    var files = List[GridFSDBFile]()
    val c = filesCollection.find(query)

    while (c.hasNext()) 
      files :+ _fix(c.next())
    files
  }

  def _fix(f: DBObject): GridFSDBFile = {
    f.asInstanceOf[GridFSDBFile]
  }

  def remove(id: ObjectId) {
    filesCollection.remove(idQuery(id))
    chunkCollection.remove(filesIdQuery(id))
  }

  def remove(filename: String) {
    remove(filenameQuery(filename))
  }


  def remove(query: DBObject) {
    for (f: GridFSDBFile <- find(query))
      f.remove();
  }

  /**
   * after calling this method, you have to call save() on the GridFSInputFile file
   */
  def createFile(data: Array[Byte]): GridFSInputFile = {
    createFile(new ByteArrayInputStream(data));
  }


  /**
   * after calling this method, you have to call save() on the GridFSInputFile file
   */
  def createFile(f: File): GridFSInputFile = {
    createFile(new FileInputStream(f), f.getName());
  }

  /**
   * after calling this method, you have to call save() on the GridFSInputFile file
   */
  def createFile(in: InputStream): GridFSInputFile = {
    createFile(in, null);
  }

  /**
   * after calling this method, you have to call save() on the GridFSInputFile file
   * on that, you can call setFilename, setContentType and control meta data by modifying the 
   *   result of getMetaData
   */
  def createFile(in: InputStream, filename: String): GridFSInputFile = {
    new GridFSInputFile(this, in, filename);
  }
}
