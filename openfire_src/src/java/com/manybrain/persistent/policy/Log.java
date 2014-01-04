package com.manybrain.persistent.policy;

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
 * <p/>
 */
public class Log {

  private static volatile Log instance = new Log();

  public static void setLog(Log l) {
    instance = l;
  }

  public static void warning(String x) {
    instance.logWarning(x);
  }

  public static void info(String x) {
    instance.logInfo(x);
  }

  public void logInfo(String x) {
    System.out.println(x);
  }

  public void logWarning(String x) {
    System.out.println(x);
  }

}
