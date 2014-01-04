package com.manybrain.persistent;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

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

public class Comm {

  public static final byte CR = (byte) '\r';
  public static final byte LF = (byte) '\n';
  public static final byte[] CRLF = "\r\n".getBytes();

  private static final AtomicLong cumulativeSize = new AtomicLong(1024);
  private static final AtomicLong numResponses = new AtomicLong(1);

  // note msg2 is normally null, but if the payload is huge (e.g. a serialized big hashmap)
  // we send it in a separate send, paying the cost of multiple system calls instead of paying for a memory
  // copy to package it all in one array
  public static byte[] getResult(SocketPool pool, char[] key, byte[] msg, int len, byte[] msg2) {
    do {
      SocketPack socketPack = pool.getSocketPack(key);
      if (socketPack.getSocket() == null) return null;
      try {
        msg = sendCommand(socketPack, msg, len, msg2);
        socketPack.returnSocketPack();
        return msg;
      } catch (Exception ee) {
        ee.printStackTrace();
        socketPack.reportDeath();
      }
    } while (true);
  }
  public static byte[] testreply = "VALUE TheKey 1 23\r\nThe value is right here\r\nEND\r\n".getBytes();

  private static byte[] sendCo3mmand(SocketPack socketPack, byte[] msg, int len, byte[] msg2) throws IOException {
    return testreply;
  }

  private static byte[] sendCommand(SocketPack socketPack, byte[] msg, int len, byte[] msg2) throws IOException {
    // command ready - send
    socketPack.getOutputStream().write(msg, 0, len);
    if (msg2 != null) {
      socketPack.getOutputStream().write(msg2);
      socketPack.getOutputStream().write(CRLF);
    }

    // read the full data
    int totalLen = 0;
    while (true) {
      int lenx = socketPack.getInputStream().read(msg, totalLen, msg.length - totalLen);
      if (lenx <= 0) {
        throw new RuntimeException();
      }
      totalLen += lenx;
      // TODO fix this.... its fragile
      if (totalLen > 0 && msg[totalLen - 1] == LF) break;

      if (totalLen == msg.length) {
        msg = doubleSize(msg);
      }
    }

    if (msg.length > 16) {
//      cumulativeSize.addAndGet(msg.length);
//      numResponses.incrementAndGet();
    }

    return msg;
  }

  public static int averagePayloadSize() {
    long a = cumulativeSize.get()/numResponses.get();  // race not dangerous
//    XXXX hacker's delight to find next highest power of 2'
    return (int)a;
  }

  private static byte[] doubleSize(byte[] msg) {
    System.out.println("expand from " + msg.length);
    byte[] resp2 = new byte[Math.max(msg.length * 2, 2048)];
    System.arraycopy(msg, 0, resp2, 0, msg.length);
    System.out.println("to - "+ resp2.length);
    return resp2;
  }
}
