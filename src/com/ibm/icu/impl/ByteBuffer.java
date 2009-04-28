//##header
//#if defined(FOUNDATION10) || defined(J2SE13) || defined(ECLIPSE_FRAGMENT)
//##/*
//## * *****************************************************************************
//## * Copyright (C) 2006-2009, International Business Machines
//## * Corporation and others. All Rights Reserved.
//## * *****************************************************************************
//## */
//##// dlf13 internal 1.3 compatibility only
//##
//##package com.ibm.icu.impl;
//##
//##/**
//## * @internal
//## */
//##public final class ByteBuffer {
//##    private byte[] data;
//##
//##    private int pos;
//##
//##    private int limit;
//##
//##    private ByteBuffer() {
//##    }
//##
//##    public byte[] array() {
//##        byte[] result = new byte[limit];
//##        for (int i = 0; i < limit; ++i) {
//##            result[i] = data[i];
//##        }
//##        return result;
//##    }
//##
//##    public static ByteBuffer wrap(byte[] data) {
//##        if (data == null)
//##            throw new NullPointerException();
//##        ByteBuffer result = new ByteBuffer();
//##        result.data = data;
//##        result.pos = 0;
//##        result.limit = data.length;
//##        return result;
//##    }
//##
//##    public int limit() {
//##        return limit;
//##    }
//##
//##    public int position() {
//##        return pos;
//##    }
//##
//##    public int remaining() {
//##        return limit - pos;
//##    }
//##
//##    public byte get() {
//##        if (pos < limit)
//##            return data[pos++];
//##        throw new IndexOutOfBoundsException();
//##    }
//##
//##    public void get(byte[] dst, int offset, int length) {
//##        if (offset < 0 || offset + length > dst.length || pos + length > limit) {
//##            throw new IndexOutOfBoundsException();
//##        }
//##        for (int i = 0; i < length; ++i) {
//##            dst[offset++] = data[pos++];
//##        }
//##    }
//##
//##    public void put(byte b) {
//##        if (pos < limit) {
//##            data[pos++] = b;
//##        } else {
//##            throw new IndexOutOfBoundsException();
//##        }
//##    }
//##
//##    public void put(byte[] src, int offset, int length) {
//##        if (offset < 0 || offset + length > src.length || pos + length > limit) {
//##            throw new IndexOutOfBoundsException();
//##        }
//##        for (int i = offset; i < offset + length; i++) {
//##            put(src[i]);
//##        }
//##    }
//##
//##    public void put(byte[] src) {
//##        put(src, 0, src.length);
//##    }
//##
//##    public static final ByteBuffer allocate(int size){
//##        ByteBuffer ret = new ByteBuffer();
//##        ret.data = new byte[size];
//##        ret.pos = 0;
//##        ret.limit = size;
//##        return ret;
//##    }
//##}
//#endif
