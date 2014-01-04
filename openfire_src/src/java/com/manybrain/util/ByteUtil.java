package com.manybrain.util;

import com.manybrain.persistent.policy.Flag;
import com.manybrain.persistent.policy.Serializer;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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

public class ByteUtil {

  private static final NumberFormatException nfe = new NumberFormatException("not a num");
//  private static final SingleObjectCache<byte[]> byteCache = new SingleObjectCache<byte[]>();
  private static final MultiObjectCache<byte[]> byteCache = new MultiObjectCache<byte[]>(10);

  public static long parseLong(byte[] b, int off, int len) throws NumberFormatException {
    int neg = 1;
    if (b[off] == '-') {
      neg = -1;
      off++;
    }
    long val = 0;
    for (int g = off; g < off + len; ++g) {
      int i = b[g] - '0';
      if (i < 0 || i > 9) throw nfe;
      val = (val * 10) + i;
    }
    return val * neg;
  }

  public static int parseInt(final byte[] b, int off, final int len) throws NumberFormatException {
    int neg = 1;
    if (b[off] == '-') {
      neg = -1;
      off++;
    }
    int val = 0;
    for (int g = off; g < off + len; ++g) {
      int i = b[g] - '0';
      if (i < 0 || i > 9) throw nfe;
      val = (val * 10) + i;
    }
    return val;
  }

  public static int parseIntTo(final byte[] b, final int off, final int end) throws NumberFormatException {
    int val = 0;
    for (int g = off; g < end; ++g) {
      int i = b[g] - '0';
      if (i < 0 || i > 9) throw nfe;
      val = (val * 10) + i;
    }
    return val;
  }

  public static byte[] intToBytes(int i) {
    boolean neg = false;
    int digs = 1;
    if (i < 0) {
      digs++;
      neg = true;
      i = -i;
    }

    int base10 = 10;
    while (true) {
      if (base10 > i) break;
      base10 *= 10;
      digs++;
    }

    byte[] bytes = new byte[digs];

    for (int g = digs - 1; g >= 0; --g) {
      bytes[g] = (byte) ((i % 10) + '0');
      i = i / 10;
    }

    if (neg) bytes[0] = '-';
    return bytes;
  }

  public static int intToBytes(int i, byte[] bytes, int off) {

    int digs = 1;
    int base10 = 10;
    while (i > base10) {
      base10 *= 10;
      digs++;
    }

    off = off + digs;
    digs = off;

    do {
      bytes[--off] = (byte) ((i % 10) + '0');
      i = i / 10;
    } while (i != 0);
    return digs;
  }

  public static final byte[] compress(byte[] str) throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ZipOutputStream zout = new ZipOutputStream(out);
    zout.putNextEntry(new ZipEntry("0"));
    zout.write(str);
    zout.closeEntry();
    byte[] compressed = out.toByteArray();
    zout.close();
    return compressed;
  }

  public static final byte[] decompress(byte[] compressed) throws Exception {
    return decompress(compressed, 0, compressed.length);
  }

  public static final Object decompressAndDeserialize(byte[] compressed, int off,
                                                      int len, Flag flag) throws Exception {
//    ByteArrayOutputStream out = new ByteArrayOutputStream();
// TODO - check this out - does it memcpy?
    ByteArrayInputStream in = new ByteArrayInputStream(compressed, off, len);

    ZipInputStream zin = new ZipInputStream(in);
    ZipEntry entry = zin.getNextEntry(); // must call this to kickstart zipp

    byte[] buffer = byteCache.get();
    if (buffer == null) {
      buffer = new byte[compressed.length * 2];
    }
    int offset = 0;
    // read the full data
    int totalLen = 0;

    while (true) {
      int maxlen = buffer.length - offset;
      int lenx = zin.read(buffer, offset, maxlen);
      if (lenx < 0) break;
      offset += lenx;
      if (offset < buffer.length) continue;
      byte[] bb = new byte[buffer.length * 2];
      System.arraycopy(buffer, 0, bb, 0, buffer.length);
      byte[] t = buffer;
      buffer = bb;
    }

    zin.close();
    Object o = Serializer.deserialize(buffer, 0, offset, flag);
    byteCache.release(buffer);
    return o;
  }


   public static final byte[] decompress(byte[] compressed, int off, int len) throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ByteArrayInputStream in = new ByteArrayInputStream(compressed, off, len);
    ZipInputStream zin = new ZipInputStream(in);
    ZipEntry entry = zin.getNextEntry();
    byte[] buffer = new byte[1024];
    int offset = -1;
    while ((offset = zin.read(buffer)) != -1) {
      out.write(buffer, 0, offset);
    }
    byte[] bb = out.toByteArray();
    out.close();
    zin.close();
    return bb;
  }


}
