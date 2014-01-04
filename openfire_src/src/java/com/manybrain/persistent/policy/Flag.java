package com.manybrain.persistent.policy;

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

public class Flag {

  private static int STRING = 1;
  private static int INT = 2;
  private static int LONG = 4;
  private static int COMPRESSED = 8;

  private int flag;

  public Flag(int x) {
    flag = x;
  }

  public Flag() {
  }

  public int get() {
    return flag;
  }

  public void setCompressed() {
    flag |= COMPRESSED;
  }

  public void clearCompressed() {
    flag &= ~COMPRESSED;
  }

  public boolean isCompressed() {
    return (flag & COMPRESSED) > 0;
  }

  public void setString() {
    flag |= STRING;
  }

  public void clearString() {
    flag &= ~STRING;
  }

  public boolean isString() {
    return (flag & STRING) > 0;
  }

  public void setInt() {
    flag |= INT;
  }

  public void clearInt() {
    flag &= ~INT;
  }

  public boolean isInt() {
    return (flag & INT) > 0;
  }

  public void setLong() {
    flag |= LONG;
  }

  public void clearLong() {
    flag &= ~LONG;
  }

  public boolean isLong() {
    return (flag & LONG) > 0;
  }
}
