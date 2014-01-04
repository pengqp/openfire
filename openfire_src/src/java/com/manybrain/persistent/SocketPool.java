package com.manybrain.persistent;

import java.util.*;

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

public class SocketPool {

  private final ArrayList<Server> servers = new ArrayList<Server>();
  private static final int findArraySize = 2048;
  private volatile Server[] find = new Server[findArraySize];
  private final Timer timer = new Timer();

  public SocketPool(String[] s, int[] ports, int[] w) {
    for (int g = 0; g < s.length; ++g) {
      Server thisServer = new Server(s[g], ports[g], w[g]);
      servers.add(thisServer);
      if (!thisServer.populateSockets()) {
        thisServer.markFailed();
      }
    }

    constructFindArray();

    timer.schedule(new Repopulator(), 5000, 10000);
  }

  public synchronized void addServer(Server s, int weight) {
    if (!s.populateSockets()) {
      s.markFailed();
      return;
    }

    servers.add(s);
    constructFindArray();
  }

  public synchronized void deleteServer(Server s) {
    servers.remove(s);
    constructFindArray();
  }

  public synchronized void constructFindArray() {
    Server[] newfind = new Server[findArraySize];

    int alive = 0;

    for (int g = 0; g < servers.size(); ++g) {
      Server s = servers.get(g);
      if (s.isDead()) continue;
      alive++;
      int w = s.getWeight();
      for (int h = 0; h < w; ++h) {
        int hash = getHash(s.getHost().toCharArray());
        hash = ((hash >>> s.getSalt()) >>> h) & (findArraySize - 1);
        if (find[hash] != null) {
          System.out.println("collision");
          s.incSalt();
          constructFindArray();
          return;
        }
        newfind[hash] = s;
        System.out.println("hash at " + hash);
      }
    }

    if (alive == 0) return;

    // find first nonempty spot
    int start = 0;
    while (newfind[start] == null) start++;

    int loc = start;
    // stretch out the values so all lookups are a single array lookup
    Server s1 = null;
    do {
      if (newfind[loc] == null) {
        newfind[loc] = s1;
      } else if (newfind[loc] != s1) {
        s1 = newfind[loc];
      }
      loc = loc - 1;
      if (loc < 0) loc = findArraySize - 1;
    } while (loc != start);

    find = newfind;

    int count[] = new int[servers.size()];
    System.out.println(" -------------------");
    for (int g = 0; g < find.length; ++g) {
      for (int h = 0; h < servers.size(); ++h) {
        if (servers.get(h) == find[g]) {
          System.out.print(" " + h);
          count[h]++;
          break;
        }
      }
    }
    System.out.println("");
    for (int g=0;g<count.length;++g) {
      System.out.println(g+" : "+count[g]);
    }

  }

  public static int getHash(char[] key) {
    return Arrays.hashCode(key) & 0x7FFFFFFF;
  }

  public SocketPack getSocketPack(char[] key) {
    return getSocketPackServers(getHash(key));
  }

  public int getNumberOfServers() {
    return servers.size();
  }

  public Server getServer(char[] key) {
     return getServer(getHash(key));
   }

  private SocketPack getSocketPackServers(long key) {
    Server[] findArray = find;
    int loc = (int) (key % findArraySize);
    int start = loc;
    Server server = null;
    SocketPack socket = null;

    server = findArray[loc];
    if (server == null) {
      throw new RuntimeException("No servers active");
    }
    if (!server.isDead()) {
      socket = server.getAvailableSocket();
      // if we have a valid socket, return it
      if (socket != null) {
        return socket;
      }
      server.incFailure();
    }

    System.out.println("failure");
    // if not, lets zoom around the array looking for one
    HashSet<Server> tried = new HashSet<Server>();
    tried.add(server);

    do {
      while (findArray[loc] == server) {
        if (++loc == start) return null;
      }

      server = findArray[loc];

      if (!tried.contains(server)) {
        tried.add(server);
        if (!server.isDead()) {
          socket = server.getAvailableSocket();
          if (socket != null) return socket;
          server.incFailure();
        }
      }
    } while (true);
  }

  public Server getServer(long key) {
    Server[] findArray = find;
    int loc = (int) (key % findArraySize);
    int start = loc;

    Server server = findArray[loc];
    if (server == null) {
      throw new RuntimeException("No servers active");
    }
    if (!server.isDead()) {
      return server;
    }

    // if not, lets zoom around the array looking for one
    HashSet<Server> tried = new HashSet<Server>();
    tried.add(server);

    do {
      while (findArray[loc] == server) {
        if (++loc == start) {
          throw new RuntimeException("No servers active");
        }
      }

      server = findArray[loc];

      if (!tried.contains(server)) {
        tried.add(server);
        if (!server.isDead()) {
          return server;
        }
      }
    } while (true);
  }

  class Repopulator extends TimerTask {
    public void run() {

      synchronized (SocketPool.this) {
        for (int g = 0; g < servers.size(); ++g) {
          servers.get(g).populateSockets();
        }
      }
    }
  }
}
