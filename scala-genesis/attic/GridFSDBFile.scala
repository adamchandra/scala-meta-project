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

import com.mongodb.util.SimplePool
import com.mongodb.BasicDBObjectBuilder
import com.mongodb.BasicDBObject
import com.mongodb.MongoException

import java.io._
import java.util._

class GridFSDBFile(fs: GridFS) extends GridFSFile(fs) {
  def getInputStream(): InputStream = new MyInputStream(numChunks())

  def writeTo(filename: String): Long = writeTo(new File(filename))

  def writeTo(f: File): Long = writeTo(new FileOutputStream(f))

  def writeTo(out: OutputStream): Long = {
    val nc = numChunks();
    for (i <- 0 to nc)
      out.write(getChunk(i));
    length;
  }

  def getChunk(i: Int): Array[Byte] = {
    val chunk = fs.chunkCollection.findOne(BasicDBObjectBuilder.start("files_id", id)
      .add("n", i).get());

    if (chunk == null)
      throw new MongoException("can't find a chunk!  file id: " + id + " chunk: " + i);

    return chunk.get("data").asInstanceOf[Array[Byte]]
  }

  class MyInputStream(numChunks: Int) extends InputStream {
    var nextChunk: Int = 0;
    var offset: Int = 0
    var data: Array[Byte] = null

    override def available(): Int = {
      if (data == null)
        return 0;
      return data.length - offset;
    }

    override def close() {}

    override def mark(readlimit: Int) = throw new RuntimeException("mark not supported");
    override def reset() = throw new RuntimeException("mark not supported");
    override def markSupported(): Boolean = false

    override def read(): Int = {
      val b = new Array[Byte](1)
      val res = read(b);
      if (res < 0)
        return -1;
      b(0) & 0xFF;
    }

    override def read(b: Array[Byte]): Int = read(b, 0, b.length)

    override def read(b: Array[Byte], off: Int, len: Int): Int = {
      if (data == null || offset >= data.length) {

        if (nextChunk >= numChunks)
          return -1;

        data = getChunk(nextChunk);
        offset = 0;
        nextChunk += 1
      }

      val r = Math.min(len, data.length - offset);
      System.arraycopy(data, offset, b, off, r);
      offset += r;
      return r;
    }
  }

  def remove() {
    fs.filesCollection.remove(new BasicDBObject("id", id));
    fs.chunkCollection.remove(new BasicDBObject("filesid", id));
  }
}

