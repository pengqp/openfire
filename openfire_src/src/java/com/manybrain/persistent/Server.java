package com.manybrain.persistent;

import com.manybrain.persistent.policy.Log;

import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;
import java.io.IOException;

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

class Server {

  public static final int deadThreshold = 5;

  final String host;
  SocketPack packs[];
  final int weight;
  final int port;
  int existing;
  int ptr;
  AtomicInteger salt = new AtomicInteger();
  AtomicInteger failures = new AtomicInteger();

  public Server(String host, int port, int weight) {
    this.host = host;
    this.weight = weight;
    this.port = port;
    packs = new SocketPack[weight];
  }

  public int getWeight() {
    return weight;
  }

  public boolean isDead() {
    return failures.get() > deadThreshold;
  }

  public int getSalt() {
    return salt.get();
  }

  public void incSalt() {
    salt.incrementAndGet();
  }

  public String getHost() {
    return host;
  }

  public void incFailure() {
    failures.incrementAndGet();
  }

  public void markFailed() {
    failures.set(deadThreshold + 1);
  }

  public synchronized void returnTheSocket(SocketPack s) {
    if (ptr == existing) notifyAll();
    packs[--ptr] = s;
  }

  public synchronized void socketDied() {
    existing--;
    ptr--;
    System.out.println("died");
    notifyAll(); // let sleepers go away if they want to
  }

  public synchronized SocketPack getAvailableSocket() {
    while (ptr >= existing) {
      if (existing == 0) return null;
      try {
        wait();
      } catch (Exception e) {
      }
    }
    return packs[ptr++];
  }

  public synchronized boolean populateSockets() {
    try {
      int connect = 0;
      while (existing < weight) {
        SocketPack s = new SocketPack(new Socket(host, port), this);
        packs[existing++] = s;
        connect++;
      }
      if (connect > 0)
        Log.info("CONNECT: " + connect + " successful connections to " + this.getHost());
    } catch (IOException ee) {
      // failure
      Log.warning("Server " + this.getHost() + " failed to initialize. Will retry.");
      // if we did get any successful ones, back them out
      for (int g = 0; g < existing; ++g) {
        if (packs[g] != null)
          packs[g].close();
      }
      return false;
    } finally {
      notifyAll();
    }
    failures.set(0);
    return true;
  }
}