package com.manybrain.persistent;

import com.manybrain.persistent.policy.Flag;
import com.manybrain.util.ByteUtil;
import com.manybrain.util.MultiObjectCache;

import java.util.*;
import java.util.concurrent.CompletionService;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;

/**
 * Copyright 2008 ManyBrain, Inc. All Rights Reserved. Us is subject to
 * license Terms.
 * <p/>
 * Author: Paul Tyma
 * <p/>
 * This file is part of ManyBrain Memcached Java client.
 * <p/>
 * The ManyBrain Memcached Java client is free software; you can
 * redistribute it and/or modify
 * it under the terms of the Lesser GNU General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the Lesser GNU
 * General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

public class MemCacheClientGuts {

  public static final int DEFAULT_NUM_CONNECTIONS = 10;

  // number of bytes where we'd rather do a separate native send instead of
  // memcopying this into an complete embedded command
  public static final int MAX_TO_EMBED = 8192;

  private final SocketPool pool;
  private static Executor multiGetExecutor = Executors.newCachedThreadPool();

  private volatile int compressionThreshold = 1024;

  public static final byte CR = (byte) '\r';
  public static final byte LF = (byte) '\n';
  public static final byte[] CRLF = "\r\n".getBytes();
  public static final byte[] DELSPACE = "delete ".getBytes();

  static final MultiObjectCache<byte[]> byteCache = new MultiObjectCache<byte[]>(10);

  protected MemCacheClientGuts(String[] server, int[] port, int[] weights) throws Exception {

    if (port == null) {
      String[] s = server;
      server = new String[s.length];
      port = new int[s.length];
      int ptr = 0;
      int g = 0;
      for (; g < s.length; ++g) {
        int colon = s[g].indexOf(":");
        if (colon < 1) {
          System.out.println("Malformed server:host " + s[g] + " - ignoring");
          continue;
        }
        server[ptr] = s[g].substring(0, colon);
        try {
          port[ptr] = Integer.parseInt(s[g].substring(colon + 1));
        } catch (NumberFormatException nfe) {
          System.out.println("Malformed server:host " + s[g] + " - ignoring");
          continue;
        }
        ptr++;
      }

      if (ptr < g) {
        String[] server1 = new String[ptr];
        int[] port1 = new int[ptr];
        System.arraycopy(server, 0, server1, 0, ptr);
        System.arraycopy(port, 0, port1, 0, ptr);
        server = server1;
        port = port1;
      }
    }

    if (server == null || server.length < 1) {
      throw new Exception("No valid servers given");
    }

    if (server.length != port.length || weights == null || weights.length != server.length) {
      throw new Exception("Number of servers, ports, and weights must be equal");
    }

    pool = new SocketPool(server, port, weights);
  }

  protected static void setMultiGetExecutor(Executor e) {
    multiGetExecutor = e;
  }

  protected void setCompression(int x) {
    compressionThreshold = x;
  }

  public static long hashCode(char a[]) {
    long result = 1;
    for (char element : a)
      result = 31 * result + element;
    return result;
  }

  // single delete
  protected boolean delete(char[] key) {
    byte[] msg = new byte[Math.max(64, key.length + 8)];
    System.arraycopy(DELSPACE, 0, msg, 0, 7);

    for (int g = 0; g < key.length; ++g) {
      msg[g + 7] = (byte) key[g];
    }
    msg[key.length + 7] = CR;
    msg[key.length + 8] = LF;

    return (Comm.getResult(pool, key, msg, key.length + 7 + 2, null))[0] == 'D';
  }

  // single get
  protected Object get(char[] key) {
    try {
      return GetHandler.get(pool, key);
    } catch (Exception ee) {
      return null;
    }
  }

  public Map<String, Object> multiGet(char[][] keys, String origKey) {
    int handleInline = 1;

    HashMap<Server, ArrayList<char[]>> serverKeys =
        new HashMap<Server, ArrayList<char[]>>();

    // partition the keys into arraylists organized by server
    int numCreated = 0;
    for (int g = 0; g < keys.length; ++g) {
      Server server = pool.getServer(keys[g]);
      ArrayList<char[]> keyList = serverKeys.get(server);
      if (keyList == null) {
        keyList = new ArrayList<char[]>(keys.length);
        serverKeys.put(server, keyList);
        numCreated++;
      }
      keyList.add(keys[g]);
    }

    // There is a potential race here that a server might go down at this instant and we have built the
    // the above list with the assumption that doesnt happen. This isn't a big deal as this query will then
    // redirect the query inside the GetHandler (i.e. the single multiget for a server becomes 2).
    CompletionService<GetHandler.Entry[]> cs = null;
    HashMap<String, Object> allResults = new HashMap<String, Object>();

    // construct the key for each server
    // loop through servers
    int numJobs = numCreated;
    Collection<ArrayList<char[]>> keyList = serverKeys.values();
    for (Iterator<ArrayList<char[]>> iterator = keyList.iterator(); iterator.hasNext();)
    {
      ArrayList<char[]> bytes = iterator.next();

      // command is built.. fork it - note we let each particular thread obtain its own socket to let it get its
      // work done and return.
      GetHandler getHandler = new GetHandler(pool, bytes);
      if (numJobs <= handleInline) {
        // do the last (or only) job in this thread
        GetHandler.Entry[] entries = getHandler.call();
        packResults(allResults, entries);
        if (numJobs-- > 1) continue;
        if (cs == null) return allResults;
        break;
      }

      if (cs == null) {
        cs = new ExecutorCompletionService(multiGetExecutor);
      }

      // send n-1 jobs off to their own threads
      cs.submit(getHandler);
      numJobs--;
    }

    if (allResults == null) {
      allResults = new HashMap<String, Object>();
    }
    for (int g = 0; g < numCreated - handleInline; ++g) {
      try {
        packResults(allResults, cs.take().get());
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    return allResults;
  }

  private void packResults(HashMap<String, Object> map, GetHandler.Entry entry[]) {
    if (entry == null) return;
    for (int g = 0; g < entry.length; ++g) {
      if (entry[g] == null) return;
      map.put(entry[g].key, entry[g].value);
    }
  }

  // build command except the value and the final CRLF
  protected final int buildWriteCommand(final byte[] dest, final byte[] cmd,
                                        final char[] key,
                                        final Flag flag, final int timeout) {
    final int cmdlen = cmd.length;

    System.arraycopy(cmd, 0, dest, 0, cmdlen);
    for (int g = 0; g < key.length; ++g) {
      dest[cmdlen + g] = (byte) key[g];
    }
    int pos = cmdlen + key.length;
    dest[pos] = ' ';
    int f = flag.get();
    if (f > 9) {
      dest[pos++] = (byte) ((f / 10) + '0');
      f = f % 10;
    }
    dest[pos + 1] = (byte) (f + '0');
    dest[pos + 2] = ' ';

    pos = ByteUtil.intToBytes(timeout, dest, pos + 3);
    dest[pos] = ' ';
    return pos + 1;
  }

  private byte[] tryToCompress(byte[] serial, Flag flag) {
    if (serial.length > compressionThreshold) {
      try {
        byte[] b = ByteUtil.compress(serial);
        if (b.length < (serial.length * 0.8)) {
          serial = b;
          flag.setCompressed();
        }
      } catch (Exception ee) {
        ee.printStackTrace();
        // if compression somehow fails move along
      }
    }
    return serial;
  }

  protected boolean writeObject(byte[] cmd, char[] key, byte[] val,
                                int timeout, Flag flag) {
    val = tryToCompress(val, flag);
    byte[] dest = byteCache.get();
    if (dest == null || dest.length < val.length + key.length + 50) {
      dest = new byte[val.length + key.length + 50];
    }
    int pos = buildWriteCommand(dest, cmd, key, flag, timeout);

    pos = ByteUtil.intToBytes(val.length, dest, pos);
    dest[pos] = CR;
    dest[pos + 1] = LF;

    if (val.length > MAX_TO_EMBED) {
      return sendSet(key, dest, pos + 2, val);
    }

    System.arraycopy(val, 0, dest, pos + 2, val.length);
    pos = pos + val.length + 2;
    dest[pos] = CR;
    dest[pos + 1] = LF;
    boolean boo = sendSet(key, dest, pos + 2, null);
    byteCache.release(dest);
    return boo;
  }

  // we try to pack everything into a byte array and send it as value
  // but if value is huge, we dont want to cause the memory copy
  protected boolean sendSet(char[] key, byte[] msg, final int len, byte[] val) {
    return (Comm.getResult(pool, key, msg, len, val))[0] == 'S';
  }
}
