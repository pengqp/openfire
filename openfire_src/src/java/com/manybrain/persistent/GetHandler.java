package com.manybrain.persistent;

import com.manybrain.persistent.policy.Flag;
import com.manybrain.persistent.policy.Serializer;
import com.manybrain.util.ByteBuf;
import com.manybrain.util.ByteUtil;
import com.manybrain.util.MultiObjectCache;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Callable;

/**
 * Copyright 2008 ManyBrain, Inc. All Rights Reserved. Us is subject to
 * license Terms.
 *
 * Author: Paul Tyma
 *
 * This file is part of ManyBrain Memcached Java client.
 *
 * The ManyBrain Memcached Java client is free software; you can
 * redistribute it and/or modify
 * it under the terms of the Lesser GNU General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the Lesser GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

public class GetHandler implements Callable {

  public static final byte[] GET = "get".getBytes();
  public static final byte[] GETSPACE = "get ".getBytes();

  final SocketPool pool;
  final ArrayList<char[]> key;

  static final MultiObjectCache<byte[]> byteCache = new MultiObjectCache<byte[]>(10);

  public GetHandler(SocketPool sp, ArrayList<char[]> multiKeys) {
    this.pool = sp;
    key = multiKeys;
  }

  public static final byte[] VALUE = "VALUE ".getBytes();
  public static final byte[] END = "END".getBytes();
  public static final byte[] ERROR = "ERROR".getBytes();
  public static final byte[] SERVER_ERROR = "SERVER_ERROR".getBytes();
  public static final byte[] CLIENT_ERROR = "CLIENT_ERROR".getBytes();

  // VALUE <key> <flags> <bytes> [<cas unique>]\r\n
  // <data block>\r\n

  public Entry[] call() {
    try {
      return multiGet();
    } catch (IOException e) {
      return null;
    }
  }

  public static class Entry {
    public final String key;
    public final Object value;

    public Entry(String k, Object o) {
      key = k;
      value = o;
    }
  }

  private static MultiObjectCache<byte[]> bytes = new MultiObjectCache<byte[]>(10);

  private Entry[] multiGet() throws IOException {
    int numKeys = key.size();

    int totalKeyLen = 0;
    for (int g = 0; g < numKeys; ++g) {
      char[] thiskey = key.get(g);
      totalKeyLen += thiskey.length;
    }

    // construct the command in bytes
    int pos = 3;
    int sendlen = 4 + totalKeyLen + numKeys - 1 + 2;
    byte[] cmd = bytes.get();
    if (cmd == null) {
      cmd = new byte[4096];
    }
    System.arraycopy(GET, 0, cmd, 0, 3);
    for (int h = 0; h < numKeys; ++h) {
      cmd[pos] = ' ';
      char[] thiskey = key.get(h);
      for (int g = 0; g < thiskey.length; ++g) {
        cmd[pos + 1 + g] = (byte) thiskey[g];
      }
      pos = pos + thiskey.length + 1;
    }
    cmd[pos] = Comm.CR;
    cmd[pos + 1] = Comm.LF;

    // getlen + CRLFlen + spaces-between-keys + keys-len
    byte[] data = Comm.getResult(pool, key.get(0), cmd, sendlen, null);

    pos = 0;
    ByteBuf buf = new ByteBuf(data);
    Entry[] map = new Entry[numKeys];

    int ptr = 0;
    do {
      String key = parseKeyString(buf);
      System.out.println("key ="+key);
      if (key == null) break;
      Object result = parseResult(buf);
      map[ptr++] = new Entry(key, result);
    } while (true);
    bytes.release(cmd);
    return map;
  }

  // note key starts at position 6
  private static int getKeyEndPos(ByteBuf data) throws IOException {
    int cmd = data.get();
    // we have the data, parse the elements and deserialize
    if (cmd == 'V') {
      final int keyStart = data.pos() + 5;
      data.jumpToChar(' ', keyStart, data.length());
      return data.pos() - keyStart;
    } else if (cmd == 'E') {
      return -1;
    } else { // errors and such
      return -1;
    }
  }

  private static String parseKeyString(ByteBuf data) throws IOException {
    int cmd = data.get();
    // we have the data, parse the elements and deserialize
    if (cmd == 'V') {
      final int keyStart = data.pos() + 5;
      data.jumpToChar(' ', keyStart, data.length());

      final byte[] dit = data.getArray();
      final char[] newKey = new char[data.pos() - keyStart];
      int len = data.pos() - keyStart;
      for (int g = 0; g < len; ++g) {
        newKey[g] = (char) dit[g + keyStart];
      }

      return new String(newKey);
    } else if (cmd == 'E') {
      return null;
    } else { // errors and such
      return null;
    }
  }

  private static Object parseResult(ByteBuf data) throws IOException {
    byte[] array = data.getArray();
    final int ppos = data.pos();
    int space = data.jumpToChar(' ', data.pos() + 1, data.length()) + 1;
    Flag flag = new Flag(ByteUtil.parseIntTo(array, ppos + 1, space - 1));

    int eol = data.jumpToChar('\r', space, data.length());
    int datalen = ByteUtil.parseIntTo(array, space, eol);
    eol += 2;
    // position to next line
    data.setPos(eol + 2 + datalen);

    // handle compression
    if (flag.isCompressed()) {
      try {
        return  ByteUtil.decompressAndDeserialize(array, eol, datalen, flag);
      } catch (Exception ee) {
        // decompression failed.. we're sorta stuck
        return null;
      }
    }

    return Serializer.deserialize(array, eol, datalen, flag);
  }

  public static Object get(SocketPool pool, final char[] key) throws IOException {
    Object o = null;

    byte[] msg = byteCache.get();
    if (msg == null) {
      msg = new byte[4096];
    }
    System.arraycopy(GETSPACE, 0, msg, 0, 4);

    for (int g = 0; g < key.length; ++g) {
      msg[g + 4] = (byte) key[g];
    }
    msg[key.length + 4] = Comm.CR;
    msg[key.length + 5] = Comm.LF;

    msg = Comm.getResult(pool, key, msg, key.length + 4 + 2, null);
    ByteBuf data = new ByteBuf(msg);

    // key starts at 6
    int keylen = getKeyEndPos(data);
    if (keylen > 0) {
      o = parseResult(data);
    }
    byteCache.release(msg);
    return o;
  }
}
