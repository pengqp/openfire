package com.manybrain.util;


/**
 * Author: Paul Tyma
 * Copyright 2008 ManyBrain, Inc. All Rights Reserved
 * Date: Mar 9, 2008
 *
 * BSD Licensed. Knock yourself out, but don't blame me.
 * Specifically, You may use this software for any purpose, commercial
 * or non-commercial however you do so AT YOUR OWN RISK.
 * No warranty of fitness for any purpose is expressed or implied.
 *
 */

public class ByteCodec {

  public static void encodeBoolean(final boolean v, final byte[] a, final int off) {
    a[off] = (byte)(v ? 1 : 0);
  }

  public static boolean decodeBoolean(final byte[] a, final int off) {
    return (a[off] == 1);
  }

  public static void encodeInt(final int v, final byte[] a, final int off) {
    a[off]   = (byte)((v >>> 24) & 0xFF);
    a[off+1] = (byte)((v >>> 16) & 0xFF);
    a[off+2] = (byte)((v >>> 8) & 0xFF);
    a[off+3] = (byte)(v & 0xFF);
  }

  public static int decodeInt(final byte[] a, final int off) {
    return (a[off] << 24) | (a[off+1] << 16) | (a[off+2] << 8) | (a[off+3]);
  }

  public static void encodeLong(final long v, final byte[] a, final int off) {
    a[off]   = (byte)((v >>> 56) & 0xFF);
    a[off+1] = (byte)((v >>> 48) & 0xFF);
    a[off+2] = (byte)((v >>> 40) & 0xFF);
    a[off+3] = (byte)((v >>> 32) & 0xFF);
    a[off+4] = (byte)((v >>> 24) & 0xFF);
    a[off+5] = (byte)((v >>> 16) & 0xFF);
    a[off+6] = (byte)((v >>> 8) & 0xFF);
    a[off+7] = (byte)(v & 0xFF);
  }

  public static long decodeLong(final byte[] a, final int off) {
    long l = a[off] << 24 | a[off+1] << 16 | a[off+2] << 8 | a[off+3];
    return (l << 32) | (a[off+4] << 24) | (a[off+5] << 16) | (a[off+6] << 8) | (a[off+7]);
  }
}


















