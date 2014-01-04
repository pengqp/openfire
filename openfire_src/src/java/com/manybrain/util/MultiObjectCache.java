package com.manybrain.util;

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

public class MultiObjectCache<T> {

  private final T[] cache;
  private final int max;
  private int ptr;

  // tried ConcurrentLinkedQueue here.. was surprisingly slow - maybe only on Intel?
  public MultiObjectCache(int max) {
    this.max = max;
    cache = (T[]) new Object[max];
  }

  public synchronized T get() {
    if (ptr == 0) return null;
    return cache[--ptr];
  }

  public synchronized void release(T t) {
    if (ptr >= max) {
      return;
    }
    cache[ptr++] = t;
  }
}
