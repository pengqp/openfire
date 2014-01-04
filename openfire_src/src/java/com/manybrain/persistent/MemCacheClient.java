package com.manybrain.persistent;                   

import com.manybrain.persistent.policy.Flag;
import com.manybrain.persistent.policy.Serializer;
import com.manybrain.util.ByteCodec;
import com.manybrain.util.IntVector;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


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
 *
 * TODO: flush, cas, compress right into array?
 */

/*
This class is the entry point for use, create one of these with your list
of servers (and weights) and go to town. Typical use:

String servers = new String[] { "server1.com:3993", "server2.com: 8833" };
int weights = new int[] { 3, 6 };
MemCacheClient mcc = new MemCacheClient(servers, weights };

The server list array and the weight array must have the same number of elements.
 */

public class MemCacheClient {

  private final MemCacheClientGuts mccg;

  private static final byte[] SET = "set ".getBytes();
  private static final byte[] ADD = "add ".getBytes();
  private static final byte[] APPEND = "append ".getBytes();
  private static final byte[] PREPEND = "prepend ".getBytes();
  private static final byte[] REPLACE = "replace ".getBytes();

  private static final byte[] CAS = "cas ".getBytes();
  private static final byte[] DELETE = "delete ".getBytes();

  private volatile int defaultTimeout = 9999;

  public MemCacheClient(String[] server, int[] port, int[] weights) throws Exception {
    mccg = new MemCacheClientGuts(server, port, weights);
  }

  public MemCacheClient(String[] s, int[] weights) throws Exception {
    this(s, null, weights);
  }

  public void setMultiGetGlobalExecutor(Executor ex) {
    mccg.setMultiGetExecutor(ex);
  }

  public void setDefaultTimeout(int x) {
    defaultTimeout = x;
  }                             

  public void setNumberMultiGetThreads(int x) {
    Executor executor = Executors.newFixedThreadPool(x);
    mccg.setMultiGetExecutor(executor);
  }

  public void setCompression(boolean f) {
    mccg.setCompression(f ? 1024 : Integer.MAX_VALUE);
  }

  public void setCompressionThreshold(int x) {
    mccg.setCompression(x);
  }

  static Class string = String.class;
  static Field f;
  static Field f1;

  static {
    try {
      f = string.getDeclaredField("value");
      f.setAccessible(true);
      f1 = string.getDeclaredField("count");
      f1.setAccessible(true);
    } catch (Exception ee) {
    }
  }

  public static char[] getChars(String k) {
    try {
      return (char[]) f.get(k);
    } catch (Exception ee) {
      ee.printStackTrace();
    }
    return null;
  }

  public static final String getString(byte[] bytes, int off, int len) {

    final char[] newKey = new char[len - off];
    for (int g = 0; g < newKey.length; ++g) {
      newKey[g] = (char) bytes[g + off];
    }
    return new String(newKey);
  }

  // single get
  public Object get(String key) {
    return mccg.get(getChars(key));
  }

  public Map<String, Object> multiGet(String key) {
    // determine if this is a space delimited list for multiget
    IntVector starts = new IntVector(8);

    final char[] keyData = getChars(key);
    // find all spaces
    int tot = 1;
    for (int g = 0; g < keyData.length; ++g) {
      if (keyData[g] != ' ') continue;
      starts.add(g);
      tot++;
    }

    // if this isn't really a multi-get, fallback to a regular get
    if (tot == 1) {
      return packSingleGet(key);
    }

    starts.add(keyData.length);

    int start = 0;
    char[][] keys = new char[tot][];
    for (int g = 0; g < tot; ++g) {
      int space = starts.get(g);
      char[] keyx = new char[space - start];
      System.arraycopy(keyData, start, keyx, 0, space - start);
      start = space + 1;
      keys[g] = keyx;
    }

    return mccg.multiGet(keys, key);
  }

  public Map<String, Object> multiGet(String[] key) {
    if (key.length == 1) {
      return packSingleGet(key[0]);
    }
    char[][] keys = new char[key.length][];
    for (int g = 0; g < key.length; ++g) {
      keys[g] = getChars(key[g]);
    }

    return mccg.multiGet(keys, null);
  }

  private Map<String, Object> packSingleGet(String key) {
    Object result = get(key);
    if (result == null) return null;
    Map<String, Object> tempMap = new HashMap<String, Object>();
    tempMap.put(key, result);
    return tempMap;
  }

  public boolean replace(String key, Object value) throws IOException {
    return replace(key, defaultTimeout, value);
  }

  public boolean replace(String key, int timeout, Object value) throws IOException {
    return mccg.writeObject(REPLACE, getChars(key), Serializer.serialize(value),
        timeout, new Flag());
  }

  public boolean replace(String key, byte[] value) {
    return replace(key, defaultTimeout, value);
  }

  public boolean replace(String key, int timeout, byte[] value) {
    return mccg.writeObject(REPLACE, getChars(key), value, timeout, new Flag());
  }

  public boolean replace(String key, String value) {
    return replace(key, defaultTimeout, value);
  }

  public boolean replace(String key, int timeout, String value) {
    Flag flag = new Flag();
    flag.setString();
    return mccg.writeObject(REPLACE, getChars(key), value.getBytes(),
        timeout, flag);
  }

  public boolean set(String key, Object value) throws IOException {
    return set(key, defaultTimeout, value);
  }

  public boolean set(String key, int timeout, Object value) throws IOException {
    return mccg.writeObject(SET, getChars(key), Serializer.serialize(value),
        timeout, new Flag());
  }

  public boolean set(String key, byte[] value) {
    return set(key, defaultTimeout, value);
  }

  public boolean set(String key, int timeout, byte[] value) {
    return mccg.writeObject(SET, getChars(key), value, timeout, new Flag());
  }

  public boolean set(String key, String value) {
    return set(key, defaultTimeout, value);
  }

  public boolean set(String key, int timeout, String value) {
    Flag flag = new Flag();
    flag.setString();
    return mccg.writeObject(SET, getChars(key), value.getBytes(), timeout, flag);
  }

  public boolean append(String key, Object value) throws IOException {
    return append(key, defaultTimeout, value);
  }

  public boolean append(String key, int timeout, Object value) throws IOException {
    return mccg.writeObject(APPEND, getChars(key), Serializer.serialize(value),
        timeout, new Flag());
  }

  public boolean append(String key, byte[] value) {
    return append(key, defaultTimeout, value);
  }

  public boolean append(String key, int timeout, byte[] value) {
    return mccg.writeObject(APPEND, getChars(key), value, timeout, new Flag());
  }

  public boolean append(String key, String value) {
    return append(key, defaultTimeout, value);
  }

  public boolean append(String key, int timeout, String value) {
    Flag flag = new Flag();
    flag.setString();
    return mccg.writeObject(APPEND, getChars(key), value.getBytes(),
        timeout, flag);
  }

  public boolean prepend(String key, Object value) throws IOException {
    return prepend(key, defaultTimeout, value);
  }

  public boolean prepend(String key, int timeout, Object value) throws IOException {
    return mccg.writeObject(PREPEND, getChars(key), Serializer.serialize(value),
        timeout, new Flag());
  }

  public boolean prepend(String key, byte[] value) {
    return prepend(key, defaultTimeout, value);
  }

  public boolean prepend(String key, int timeout, byte[] value) {
    return mccg.writeObject(PREPEND, getChars(key), value, timeout, new Flag());
  }

  public boolean prepend(String key, String value) {
    return prepend(key, defaultTimeout, value);
  }

  public boolean prepend(String key, int timeout, String value) {
    Flag flag = new Flag();
    flag.setString();
    return mccg.writeObject(PREPEND, getChars(key), value.getBytes(), timeout, flag);
  }

  public boolean setInt(String key, int x) {
    return setInt(getChars(key), x);
  }

  public boolean setInt(char[] key, int x) {
    Flag flag = new Flag();
    flag.setInt();

    byte[] dest = new byte[key.length + 24];
    int pos = mccg.buildWriteCommand(dest, SET, key, flag, 60);
    dest[pos] = ' ';
    dest[pos + 1] = '4';
    dest[pos + 2] = MemCacheClientGuts.CR;
    dest[pos + 3] = MemCacheClientGuts.LF;

    ByteCodec.encodeInt(x, dest, pos + 4);
    dest[pos + 8] = MemCacheClientGuts.CR;
    dest[pos + 9] = MemCacheClientGuts.LF;

    return mccg.sendSet(key, dest, pos + 10, null);
  }

  public boolean setLong(String key, long x) {
    return setLong(SET, getChars(key), x);
  }

  public boolean setLong(byte[] cmd, char[] key, long x) {
    Flag flag = new Flag();
    flag.setLong();

    byte[] dest = new byte[key.length + 24];
    int pos = mccg.buildWriteCommand(dest, cmd, key, flag, 60);
    dest[pos] = dest[pos + 2] = ' ';
    dest[pos + 1] = '8';
    dest[pos + 2] = MemCacheClientGuts.CR;
    dest[pos + 3] = MemCacheClientGuts.LF;

    ByteCodec.encodeLong(x, dest, pos + 4);
    dest[pos + 12] = MemCacheClientGuts.CR;
    dest[pos + 13] = MemCacheClientGuts.LF;

    return mccg.sendSet(key, dest, pos + 14, null);
  }

  public static void main(String[] args) throws Exception {
//    getTest();
    multiGetTest();
    System.exit(1);
  }


  public static void getTest() throws Exception {
    String[] a = new String[]{"192.168.1.106"};
    int b[] = new int[]{2};
    MemCacheClient mcc = new MemCacheClient(a, b);

    mcc.set("TheKey", "The value is right here dude");

    System.out.println(mcc.get("TheKey"));
  }

  public static void multiGetTest() throws Exception {
    String[] a = new String[]{"127.0.0.1:11211", "192.168.1.108:11211"};
    int b[] = new int[]{2, 3};
    MemCacheClient mcc = new MemCacheClient(a, b);

    HashMap<Long, String> map = new HashMap<Long, String>();
       for (int g=0;g<500;++g) {
         long a2 = (long)(Math.random()*2038408);
         String xx4 = "oiwjfowjfowej"+a2;
         map.put(a2, xx4);
       }

    System.out.println("sets");

    mcc.set("TheKey", "The value is right here dude");
    mcc.set("TheOtherKey", "The other value is right here dude");
    mcc.set("TheOtherKey1", "The other1 value is right here dude");
    mcc.set("TheOtherKey2", "The other2 value is right here dude");
    mcc.set("TheOtherKey3", "The other3 value is right here dude");

    mcc.set("TheBigObject", map);

    System.out.println("gets");

    System.out.println("SHOULD BE: The value..." + mcc.get("TheKey"));
    System.out.println("SHOULD BE: null  = " +mcc.get("TheKeyNotFound"));

    System.out.println("SHOULD BE: The value..." + mcc.multiGet("TheKey"));

    System.out.println("multi");
    Map<String, Object> vals = mcc.multiGet("TheKey Notfindme TheOtherKey TheOtherKey1 TheOtherKey2 TheOtherKey3");
    Set set = vals.keySet();
    for (Iterator iterator = set.iterator(); iterator.hasNext();) {
      Object o = iterator.next();
      String x = (String) o;
      System.out.println(o + "           ----  " + vals.get(o));
    }

    Object probablyMap = mcc.get("TheBigObject");
    map = (HashMap)probablyMap;
    Set kk = map.keySet();
    for (Iterator iterator = kk.iterator(); iterator.hasNext();) {
      Object o = iterator.next();
      System.out.print(o +"    " + map.get(o)+ "      || ");
    }
    System.out.println("");
  }
}








