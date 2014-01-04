package com.manybrain.persistent.policy;

import com.manybrain.util.ByteUtil;

import java.io.*;
import java.nio.charset.Charset;

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

public class Serializer {

  private static volatile Serializer instance = new Serializer();

  // TODO - uncommenting these lines - with NO other changes loses 1% performance?
  Charset USASCII = Charset.forName("US-ASCII");

  static Charset charset = null;

  public static void setSerializer(Serializer ss) {
    instance = ss;
  }

  public static byte[] serialize(Object value) throws IOException {
    return instance.serializeObject(value);
  }

  public static Object deserialize(byte[] d, int o, int l, Flag f) throws IOException {
    return instance.deserializeBytes(d, o, l, f);
  }

  public static void setCharset(String x) {
    charset = Charset.forName(x);
  }
  // =======================================================================
  // override the methods below in a subclass to implement a different serialization
  // scheme (and then call setSerializer above before instantiating/using a client

  public byte[] serializeObject(Object value) throws IOException {
    ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);
    (new ObjectOutputStream(bos)).writeObject(value);
    byte[] val = bos.toByteArray();
    return val;
  }

  public Object deserializeBytes(byte[] data, int off, int datalen, Flag flag) throws IOException {
    if (flag.isString()) {
      if (charset == null || charset == USASCII) {
        final char[] newKey = new char[datalen];
        for (int g = 0; g < newKey.length; ++g) {
          newKey[g] = (char) data[g + off];
        }
        return new String(newKey);
      }

      return new String(data, off, datalen, charset);
    } else if (flag.isInt()) {
      return ByteUtil.parseInt(data, off, datalen);
    } else if (!flag.isLong()) {
      return deserializeObject(data, off, datalen);
    } else {                                       
      return ByteUtil.parseLong(data, off, datalen);
    }
  }

  private Object deserializeObject(byte[] data, int off, int datalen) throws IOException {
    try {
      ByteArrayInputStream bis = new ByteArrayInputStream(data, off, datalen);
      return (new ObjectInputStream(bis)).readObject();
    } catch (ClassNotFoundException cnfe) {
      cnfe.printStackTrace();
    }
    return null;
  }
}
