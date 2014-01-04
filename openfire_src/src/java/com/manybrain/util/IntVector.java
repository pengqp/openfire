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

public class IntVector {

  int[] b;
  int count;

  public IntVector(int x) {
    b = new int[x];
  }

  public IntVector() {
    this(16);
  }

  public void add(int x) {
    if (count == b.length) {
      expand(count * 2);
    }

    b[count++] = x;
  }

  private void expand(int x) {
    int[] newb = new int[x];
    System.arraycopy(b, 0, newb, 0, count);
    b = newb;
  }

  public void append(IntVector bv) {
    if (bv.size() + count > b.length) {
      expand((bv.size() + count) * 2);
    }

    System.arraycopy(bv.getArray(), 0, b, count, bv.size());
    count += bv.size();
  }

  public int[] getArray() {
    return b;
  }

  public int get(int x) {
    return b[x];
  }

  public int inc(int loc) {
    return ++b[loc];
  }

  public void trim() {
    if (count == b.length) return;

    int[] newb = new int[count];
    System.arraycopy(b, 0, newb, 0, count);
    b = newb;
  }

  public int size() {
    return count;
  }

  public String toString() {
    return new String(b, 0, count);
  }

  public void unAdd() {
    count--;
  }
}
