// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 ***********************************************************************
 * Copyright (C) 2005-2007, International Business Machines Corporation and *
 * others. All Rights Reserved.                                        *
 ***********************************************************************
 *
 */

package com.ibm.icu.dev.tool.charsetdet.sbcs;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;

/**
 * @author emader
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class InputFile implements NGramList.NGramKeyMapper
{

    private File file;
    private FileInputStream fileStream;
    private InputStreamReader inputStream;

    private Charset charset;
    private CharsetDecoder decoder;
    private CharsetEncoder encoder;
    
    private boolean visualOrder;

    private static void exceptionError(Exception e)
    {
        System.err.println("ioError: " + e.toString());
    }

    /**
     * 
     */
    public InputFile(String filename, String encoding, boolean visual)
    {
        file = new File(filename);
        setEncoding(encoding);
        visualOrder = visual;
    }
    
    public boolean open()
    {
        try {
            fileStream = new FileInputStream(file);          
            inputStream = new InputStreamReader(fileStream, "UTF8");
        } catch (Exception e) {
            exceptionError(e);
            return false;
        }
        
        return true;
    }
    
    public void close()
    {
        try {
            inputStream.close();
            fileStream.close();
        } catch (Exception e) {
            // don't really care if this fails...
        }
    }
    
    public String getFilename()
    {
        return file.getName();
    }
    
    public String getParent()
    {
        return file.getParent();
    }
    
    public String getPath()
    {
        return file.getPath();
    }
    
    public int read(char[] buffer)
    {
        int charsRead = -1;
        
        try {
            charsRead = inputStream.read(buffer, 0, buffer.length);
        } catch (Exception e) {
            exceptionError(e);
        }
        
        return charsRead;
    }
    
    public void setEncoding(String encoding)
    {
        charset = Charset.forName(encoding);
        decoder = charset.newDecoder();
        encoder = charset.newEncoder();
        
        encoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
        encoder.onMalformedInput(CodingErrorAction.REPLACE);
        
        decoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
        decoder.onMalformedInput(CodingErrorAction.REPLACE);
    }
    
    public String getEncoding()
    {
        return charset.displayName();
    }
    
    public boolean getVisualOrder()
    {
        return visualOrder;
    }
    
    public Object mapKey(String key)
    {
        byte[] bytes = encode(key.toCharArray());
        int length   = key.length();
        int value    = 0;
        
        for(int b = 0; b < length; b += 1) {
            value <<= 8;
            value += (bytes[b] & 0xFF);
        }

        return value;
    }
    
    public byte[] encode(char[] chars)
    {
        CharBuffer cb = CharBuffer.wrap(chars);
        ByteBuffer bb;
        
        try {
            bb = encoder.encode(cb);
        } catch (CharacterCodingException e) {
            // don't expect to get any exceptions in normal usage...
            return null;
        }

        return bb.array();
    }
    
    public char[] decode(byte[] bytes)
    {
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        CharBuffer cb;
        
        try {
            cb = decoder.decode(bb);
        } catch (CharacterCodingException e) {
            // don't expect to get any exceptions in normal usage...
            return null;
        }
        
        return cb.array();
    }
}
