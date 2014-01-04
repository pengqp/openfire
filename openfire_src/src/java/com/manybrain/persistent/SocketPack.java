package com.manybrain.persistent;

import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

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

class SocketPack {
  final Socket socket;
  final Server server;
  final InputStream inputStream;
  final OutputStream outputStream;
  final BufferedOutputStream bos;

  public SocketPack(Socket socket, Server server) {
    this.socket = socket;
    this.server = server;

    InputStream is = null;
    OutputStream os = null;
    BufferedOutputStream bbos = null;

    try {
      is = socket.getInputStream();
      os = socket.getOutputStream();
      bbos = new BufferedOutputStream(os);
    } catch (Exception e) {
      // we assume you sent us a viable socket
      // if not, do nothing here, when someone tries to use it
      // it will expire itself
    }
    inputStream = is;
    outputStream = os;
    bos = bbos;
  }

  public final OutputStream getOutputStream() {
    return outputStream;
  }

  public final BufferedOutputStream getBufferedOutputStream() {
    return bos;
  }

  public final InputStream getInputStream() {
    return inputStream;
  }

  public Socket getSocket() {
    return socket;
  }

  public Server getServer() {
    return server;
  }

  public void close() {
    try { socket.close(); } catch (Exception ee) {}
  }

  public void returnSocketPack() {
    server.returnTheSocket(this);
  }

  public void reportDeath() {
    server.socketDied();
  }
}

