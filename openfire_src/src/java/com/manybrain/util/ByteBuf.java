package com.manybrain.util;

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

public class ByteBuf {

  public static final byte CR = (byte) '\n';

  byte[] buf;
  int pos;

  public ByteBuf(int z) {
    buf = new byte[z];
  }

  public ByteBuf(byte[] z) {
    buf = z;
  }

  public final void add(byte[] b) {
    while (pos + b.length >= buf.length) {
      expand();
    }

    System.arraycopy(b, 0, buf, pos, b.length);
    pos += b.length;
  }

  public final void newLine() {
    if (pos + 1 >= buf.length) {
      expand();
    }
    buf[pos++] = CR;
  }

  public int length() {
    return buf.length;
  }

  private final void expand() {
    byte[] buf1 = new byte[buf.length * 2];
    System.arraycopy(buf, 0, buf1, 0, pos);
    buf = buf1;
  }

  public final byte[] getArray() {
    return buf;
  }

  public final int pos() {
    return pos;
  }

  public final void setPos(int x) {
    pos = x;
  }

  public final void clear() {
    buf = new byte[1];
  }

  public final void reset() {
    pos = 0;
  }

  public final byte get() {
    return buf[pos++];
  }

  public final byte get(int i) {
    return buf[i];
  }

  public final byte getUpper(int i) {
    return (byte) (buf[i] & 0xDF);
  }

  public final void skip(int i) {
    pos += i;
  }

  public final int findEOL() {
    int g = 0;
    for (g = pos; g < buf.length; ++g) {
      if ((buf[g] == '\n') || (buf[g] == '\r')) break;
    }
    return g;
  }

  public final void skipPastEol() {
    int g = findEOL();
    skipPastSingleEol(g);
  }

  public final void skipPastSingleEol(int start) {
    if (buf[start] == '\r') start++;
    if (buf[start] == '\n') start++;
    pos = start;
  }

  public final void skipWhiteSpace() {
    while (buf[pos] == ' ' || buf[pos] == '\t') pos++;
  }

  public final void skipWhiteSpace(int start) {
    pos = start;
    skipWhiteSpace();
  }

  public final int findChar(char c, int stop) {
    for (int g = pos; g < stop; ++g) {
      if (buf[g] == c) return g;
    }
    return -1;
  }

  public final int jumpToChar(char c, int start, int stop) {
    for (int g = start; g < stop; ++g) {
      if (buf[g] == c) {
        return pos = g;
      }
    }
    return -1;
  }

  // match one part of the buf with another
  public final boolean shortbrokeMatch(int off, int len) {
    for (int g = off; g < len; ++g) {
      // convert to upper
      int bufg = (buf[g] >= 'a') ? buf[g] & 0xDF : buf[g];
      int gpos = g + pos;
      int bufgpos = (buf[gpos] >= 'a' ? buf[gpos] & 0xDF : buf[gpos]);
      if (bufgpos != bufg) return false;
//      if ((buf[g] & 0xDF) != (buf[g + pos] & 0xDF)) return false;
    }
    return true;
  }

  public final boolean shortMatch(int off, int len) {
    for (int g = 0; g < len; ++g) {
      int goff = g + off;
      int bufoff = (buf[goff] >= 'a' && buf[goff] <= 'z') ? buf[goff] & 0xDF : buf[goff];
      goff = g + pos;
      int bufpos = (buf[goff] >= 'a' && buf[goff] <= 'z') ? buf[goff] & 0xDF : buf[goff];
      if (bufoff != bufpos) return false;
    }
    return true;
  }


  // match buf with some other byte array
  public final boolean shortMatch(byte match[]) {
    int len = match.length;
    for (int g = 0; g < len; ++g) {
      // convert to upper
      int goff = g + pos;
//      int bufg = (buf[goff] >= 'a' && buf[goff] <= 'z') ? buf[goff] & 0xDF : buf[goff];
      int bufg = buf[goff];
      if (bufg >= 'a' && bufg < 'z') bufg &= 0xDF;
      if (match[g] != bufg) {
        return false;
      }
    }
    return true;
  }

  public final boolean skipMatch(byte match[]) {
    if (shortMatch(match)) {
      skip(match.length);
      return true;
    }
    return false;
  }

  public final boolean skipMatch(int off, int len) {
    if (shortMatch(off, len)) {
      skip(len);
      return true;
    }
    return false;
  }
}
