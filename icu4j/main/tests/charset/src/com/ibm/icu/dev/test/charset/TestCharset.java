/**
*******************************************************************************
* Copyright (C) 2006-2009, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
*******************************************************************************
*/

package com.ibm.icu.dev.test.charset;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.charset.spi.CharsetProvider;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.MissingResourceException;
import java.util.Set;
import java.util.SortedMap;

import com.ibm.icu.charset.CharsetCallback;
import com.ibm.icu.charset.CharsetEncoderICU;
import com.ibm.icu.charset.CharsetDecoderICU;
import com.ibm.icu.charset.CharsetICU;
import com.ibm.icu.charset.CharsetProviderICU;
import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.text.UTF16;

public class TestCharset extends TestFmwk {
    private String m_encoding = "UTF-16";
    CharsetDecoder m_decoder = null;
    CharsetEncoder m_encoder = null;
    Charset m_charset =null;
    static final String unistr = "abcd\ud800\udc00\u1234\u00a5\u3000\r\n";
    static final byte[] byteStr ={   
            (byte) 0x00,(byte) 'a',
            (byte) 0x00,(byte) 'b',
            (byte) 0x00,(byte) 'c',
            (byte) 0x00,(byte) 'd',
            (byte) 0xd8,(byte) 0x00,
            (byte) 0xdc,(byte) 0x00,
            (byte) 0x12,(byte) 0x34,
            (byte) 0x00,(byte) 0xa5,
            (byte) 0x30,(byte) 0x00,
            (byte) 0x00,(byte) 0x0d,
            (byte) 0x00,(byte) 0x0a };
    static final byte[] expectedByteStr ={
        (byte) 0xfe,(byte) 0xff,
        (byte) 0x00,(byte) 'a',
        (byte) 0x00,(byte) 'b',
        (byte) 0x00,(byte) 'c',
        (byte) 0x00,(byte) 'd',
        (byte) 0xd8,(byte) 0x00,
        (byte) 0xdc,(byte) 0x00,
        (byte) 0x12,(byte) 0x34,
        (byte) 0x00,(byte) 0xa5,
        (byte) 0x30,(byte) 0x00,
        (byte) 0x00,(byte) 0x0d,
        (byte) 0x00,(byte) 0x0a };
    
    protected void init(){
        try{
            if ("UTF-16".equals(m_encoding)) {
                int x = 2;
                x++;
            }
            CharsetProviderICU provider = new CharsetProviderICU();
            //Charset charset = CharsetICU.forName(encoding);
            m_charset = provider.charsetForName(m_encoding);
            m_decoder = (CharsetDecoder) m_charset.newDecoder();
            m_encoder = (CharsetEncoder) m_charset.newEncoder();   
        }catch(MissingResourceException ex){
            warnln("Could not load charset data");
        }
    }
    
    public static void main(String[] args) throws Exception {
        new TestCharset().run(args);
    }
    public void TestUTF16Converter(){
        CharsetProvider icu = new CharsetProviderICU();
        Charset cs1 = icu.charsetForName("UTF-16BE");
        CharsetEncoder e1 = cs1.newEncoder();
        CharsetDecoder d1 = cs1.newDecoder();
        
        Charset cs2 = icu.charsetForName("UTF-16LE");
        CharsetEncoder e2 = cs2.newEncoder();
        CharsetDecoder d2 = cs2.newDecoder();
        
        for(int i=0x0000; i<0x10FFFF; i+=0xFF){
            CharBuffer us = CharBuffer.allocate(0xFF*2);
            ByteBuffer bs1 = ByteBuffer.allocate(0xFF*8);
            ByteBuffer bs2 = ByteBuffer.allocate(0xFF*8);
            for(int j=0;j<0xFF; j++){
                int c = i+j;
              
                if((c>=0xd800&&c<=0xdFFF)||c>0x10FFFF){
                    continue;
                }

                if(c>0xFFFF){
                    char lead = UTF16.getLeadSurrogate(c);
                    char trail = UTF16.getTrailSurrogate(c);
                    if(!UTF16.isLeadSurrogate(lead)){
                        errln("lead is not lead!"+lead+" for cp: \\U"+Integer.toHexString(c));
                        continue;
                    }
                    if(!UTF16.isTrailSurrogate(trail)){
                        errln("trail is not trail!"+trail);
                        continue;
                    }
                    us.put(lead);
                    us.put(trail);
                    bs1.put((byte)(lead>>8));
                    bs1.put((byte)(lead&0xFF));
                    bs1.put((byte)(trail>>8));
                    bs1.put((byte)(trail&0xFF));
                    
                    bs2.put((byte)(lead&0xFF));
                    bs2.put((byte)(lead>>8));
                    bs2.put((byte)(trail&0xFF));
                    bs2.put((byte)(trail>>8));
                }else{

                    if(c<0xFF){
                        bs1.put((byte)0x00);
                        bs1.put((byte)(c));
                        bs2.put((byte)(c));
                        bs2.put((byte)0x00);
                    }else{
                        bs1.put((byte)(c>>8));
                        bs1.put((byte)(c&0xFF));
                        
                        bs2.put((byte)(c&0xFF));
                        bs2.put((byte)(c>>8));
                    }
                    us.put((char)c);
                }
            }
            
            
            us.limit(us.position());
            us.position(0);
            if(us.length()==0){
                continue;
            }
            

            bs1.limit(bs1.position());
            bs1.position(0);
            ByteBuffer newBS = ByteBuffer.allocate(bs1.capacity());
            //newBS.put((byte)0xFE);
            //newBS.put((byte)0xFF);
            newBS.put(bs1);    
            bs1.position(0);
            smBufDecode(d1, "UTF-16", bs1, us);
            smBufEncode(e1, "UTF-16", us, newBS);
            
            bs2.limit(bs2.position());
            bs2.position(0);
            newBS.clear();
            //newBS.put((byte)0xFF);
            //newBS.put((byte)0xFE);
            newBS.put(bs2);     
            bs2.position(0);
            smBufDecode(d2, "UTF16-LE", bs2, us);
            smBufEncode(e2, "UTF-16LE", us, newBS);
            
        }
    }
    public void TestUTF32Converter(){
        CharsetProvider icu = new CharsetProviderICU();
        Charset cs1 = icu.charsetForName("UTF-32BE");
        CharsetEncoder e1 = cs1.newEncoder();
        CharsetDecoder d1 = cs1.newDecoder();
        
        Charset cs2 = icu.charsetForName("UTF-32LE");
        CharsetEncoder e2 = cs2.newEncoder();
        CharsetDecoder d2 = cs2.newDecoder();
        
        for(int i=0x000; i<0x10FFFF; i+=0xFF){
            CharBuffer us = CharBuffer.allocate(0xFF*2);
            ByteBuffer bs1 = ByteBuffer.allocate(0xFF*8);
            ByteBuffer bs2 = ByteBuffer.allocate(0xFF*8);
            for(int j=0;j<0xFF; j++){
                int c = i+j;
              
                if((c>=0xd800&&c<=0xdFFF)||c>0x10FFFF){
                    continue;
                }

                if(c>0xFFFF){
                    char lead = UTF16.getLeadSurrogate(c);
                    char trail = UTF16.getTrailSurrogate(c);

                    us.put(lead);
                    us.put(trail);
                }else{
                    us.put((char)c);
                }
                bs1.put((byte) (c >>> 24));
                bs1.put((byte) (c >>> 16)); 
                bs1.put((byte) (c >>> 8)); 
                bs1.put((byte) (c & 0xFF));       
                                
                bs2.put((byte) (c & 0xFF));  
                bs2.put((byte) (c >>> 8));
                bs2.put((byte) (c >>> 16)); 
                bs2.put((byte) (c >>> 24));
            }
            bs1.limit(bs1.position());
            bs1.position(0);
            bs2.limit(bs2.position());
            bs2.position(0);
            us.limit(us.position());
            us.position(0);
            if(us.length()==0){
                continue;
            }
             

            ByteBuffer newBS = ByteBuffer.allocate(bs1.capacity());
            
            newBS.put((byte)0x00);
            newBS.put((byte)0x00);
            newBS.put((byte)0xFE);
            newBS.put((byte)0xFF);
            
            newBS.put(bs1);
            bs1.position(0);
            smBufDecode(d1, "UTF-32", bs1, us);
            smBufEncode(e1, "UTF-32", us, newBS);
            
            
            newBS.clear();
            
            newBS.put((byte)0xFF);
            newBS.put((byte)0xFE);
            newBS.put((byte)0x00);
            newBS.put((byte)0x00);
            
            newBS.put(bs2);    
            bs2.position(0);
            smBufDecode(d2, "UTF-32LE", bs2, us);
            smBufEncode(e2, "UTF-32LE", us, newBS);

        }
    }
    public void TestASCIIConverter() {
        runTestASCIIBasedConverter("ASCII", 0x80);
    }    
    public void Test88591Converter() {
        runTestASCIIBasedConverter("iso-8859-1", 0x100);
    }
    public void runTestASCIIBasedConverter(String converter, int limit){
        CharsetProvider icu = new CharsetProviderICU();
        Charset icuChar = icu.charsetForName(converter);
        CharsetEncoder encoder = icuChar.newEncoder();
        CharsetDecoder decoder = icuChar.newDecoder();
        CoderResult cr;

        /* test with and without array-backed buffers */ 
        
        byte[] bytes = new byte[0x10000];
        char[] chars = new char[0x10000];
        for (int j = 0; j <= 0xffff; j++) {
            bytes[j] = (byte) j;
            chars[j] = (char) j;
        }

        boolean fail = false;
        boolean arrays = false;
        boolean decoding = false;
        int i;
        
        // 0 thru limit - 1
        ByteBuffer bs = ByteBuffer.wrap(bytes, 0, limit);
        CharBuffer us = CharBuffer.wrap(chars, 0, limit);
        smBufDecode(decoder, converter, bs, us, true);
        smBufDecode(decoder, converter, bs, us, false);
        smBufEncode(encoder, converter, us, bs, true);
        smBufEncode(encoder, converter, us, bs, false);
        for (i = 0; i < limit; i++) {
            bs = ByteBuffer.wrap(bytes, i, 1).slice();
            us = CharBuffer.wrap(chars, i, 1).slice();
            try {
                decoding = true;
                arrays = true;
                smBufDecode(decoder, converter, bs, us, true, false, true);
                
                decoding = true;
                arrays = false;
                smBufDecode(decoder, converter, bs, us, true, false, false);
                
                decoding = false;
                arrays = true;
                smBufEncode(encoder, converter, us, bs, true, false, true);
                
                decoding = false;
                arrays = false;
                smBufEncode(encoder, converter, us, bs, true, false, false);
                
            } catch (Exception ex) {
                errln("Failed to fail to " + (decoding ? "decode" : "encode") + " 0x"
                        + Integer.toHexString(i) + (arrays ? " with arrays" : " without arrays") + " in " + converter);
                return;
            }
        }
        
        // decode limit thru 255
        for (i = limit; i <= 0xff; i++) {
            bs = ByteBuffer.wrap(bytes, i, 1).slice();
            us = CharBuffer.wrap(chars, i, 1).slice();
            try {
                smBufDecode(decoder, converter, bs, us, true, false, true);
                fail = true;
                arrays = true;
                break;
            } catch (Exception ex) {
            }
            try {
                smBufDecode(decoder, converter, bs, us, true, false, false);
                fail = true;
                arrays = false;
                break;
            } catch (Exception ex) {
            }
        }
        if (fail) {
            errln("Failed to fail to decode 0x" + Integer.toHexString(i)
                    + (arrays ? " with arrays" : " without arrays") + " in " + converter);
            return;
        }
        
        // encode limit thru 0xffff, skipping through much of the 1ff to feff range to save
        // time (it would take too much time to test every possible case)
        for (i = limit; i <= 0xffff; i = ((i>=0x1ff && i<0xfeff) ? i+0xfd : i+1)) {
            bs = ByteBuffer.wrap(bytes, i, 1).slice();
            us = CharBuffer.wrap(chars, i, 1).slice();
            try {
                smBufEncode(encoder, converter, us, bs, true, false, true);
                fail = true;
                arrays = true;
                break;
            } catch (Exception ex) {
            }
            try {
                smBufEncode(encoder, converter, us, bs, true, false, false);
                fail = true;
                arrays = false;
                break;
            } catch (Exception ex) {
            }
        }
        if (fail) {
            errln("Failed to fail to encode 0x" + Integer.toHexString(i)
                    + (arrays ? " with arrays" : " without arrays") + " in " + converter);
            return;
        }
        
        // test overflow / underflow edge cases
        outer: for (int n = 1; n <= 3; n++) {
            for (int m = 0; m < n; m++) {
                // expecting underflow
                try {
                    bs = ByteBuffer.wrap(bytes, 'a', m).slice();
                    us = CharBuffer.wrap(chars, 'a', m).slice();
                    smBufDecode(decoder, converter, bs, us, true, false, true);
                    smBufDecode(decoder, converter, bs, us, true, false, false);
                    smBufEncode(encoder, converter, us, bs, true, false, true);
                    smBufEncode(encoder, converter, us, bs, true, false, false);
                    bs = ByteBuffer.wrap(bytes, 'a', m).slice();
                    us = CharBuffer.wrap(chars, 'a', n).slice();
                    smBufDecode(decoder, converter, bs, us, true, false, true, m);
                    smBufDecode(decoder, converter, bs, us, true, false, false, m);
                    bs = ByteBuffer.wrap(bytes, 'a', n).slice();
                    us = CharBuffer.wrap(chars, 'a', m).slice();
                    smBufEncode(encoder, converter, us, bs, true, false, true, m);
                    smBufEncode(encoder, converter, us, bs, true, false, false, m);
                    bs = ByteBuffer.wrap(bytes, 'a', n).slice();
                    us = CharBuffer.wrap(chars, 'a', n).slice();
                    smBufDecode(decoder, converter, bs, us, true, false, true);
                    smBufDecode(decoder, converter, bs, us, true, false, false);
                    smBufEncode(encoder, converter, us, bs, true, false, true);
                    smBufEncode(encoder, converter, us, bs, true, false, false);
                } catch (Exception ex) {
                    fail = true;
                    break outer;
                }
                
                // expecting overflow
                try {
                    bs = ByteBuffer.wrap(bytes, 'a', n).slice();
                    us = CharBuffer.wrap(chars, 'a', m).slice();
                    smBufDecode(decoder, converter, bs, us, true, false, true);
                    fail = true;
                    break;
                } catch (Exception ex) {
                    if (!(ex instanceof BufferOverflowException)) {
                        fail = true;
                        break outer;
                    }
                }
                try {
                    bs = ByteBuffer.wrap(bytes, 'a', n).slice();
                    us = CharBuffer.wrap(chars, 'a', m).slice();
                    smBufDecode(decoder, converter, bs, us, true, false, false);
                    fail = true;
                } catch (Exception ex) {
                    if (!(ex instanceof BufferOverflowException)) {
                        fail = true;
                        break outer;
                    }
                }
                try {
                    bs = ByteBuffer.wrap(bytes, 'a', m).slice();
                    us = CharBuffer.wrap(chars, 'a', n).slice();
                    smBufEncode(encoder, converter, us, bs, true, false, true);
                    fail = true;
                } catch (Exception ex) {
                    if (!(ex instanceof BufferOverflowException)) {
                        fail = true;
                        break outer;
                    }
                }
                try {
                    bs = ByteBuffer.wrap(bytes, 'a', m).slice();
                    us = CharBuffer.wrap(chars, 'a', n).slice();
                    smBufEncode(encoder, converter, us, bs, true, false, false);
                    fail = true;
                } catch (Exception ex) {
                    if (!(ex instanceof BufferOverflowException)) {
                        fail = true;
                        break outer;
                    }
                }
            }
        }
        if (fail) {
            errln("Incorrect result in " + converter + " for underflow / overflow edge cases");
            return;
        }
        
        // test surrogate combinations in encoding
        String lead = "\ud888";
        String trail = "\udc88";
        String norm = "a";
        String ext = "\u0275"; // theta
        String end = "";
        bs = ByteBuffer.wrap(new byte[] { 0 });
        String[] input = new String[] { //
                lead + lead,   // malf(1)
                lead + trail,  // unmap(2)
                lead + norm,   // malf(1)
                lead + ext,    // malf(1)
                lead + end,    // malf(1)
                trail + norm,  // malf(1)
                trail + end,   // malf(1)
                ext   + norm,  // unmap(1)
                ext   + end,   // unmap(1)
        };
        CoderResult[] result = new CoderResult[] {
                CoderResult.malformedForLength(1),
                CoderResult.unmappableForLength(2),
                CoderResult.malformedForLength(1),
                CoderResult.malformedForLength(1),
                CoderResult.malformedForLength(1),
                CoderResult.malformedForLength(1),
                CoderResult.malformedForLength(1),
                CoderResult.unmappableForLength(1),
                CoderResult.unmappableForLength(1),
        };
        
        for (int index = 0; index < input.length; index++) {
            CharBuffer source = CharBuffer.wrap(input[index]);
            cr = encoder.encode(source, bs, true);
            bs.rewind();
            encoder.reset();

            // if cr != results[x]
            if (!((cr.isUnderflow() && result[index].isUnderflow())
                    || (cr.isOverflow() && result[index].isOverflow())
                    || (cr.isMalformed() && result[index].isMalformed())
                    || (cr.isUnmappable() && result[index].isUnmappable()))
                    || (cr.isError() && cr.length() != result[index].length())) {
                errln("Incorrect result in " + converter + " for \"" + input[index] + "\"" + ", expected: " + result[index] + ", received:  " + cr);
                break;
            }

            source = CharBuffer.wrap(input[index].toCharArray());
            cr = encoder.encode(source, bs, true);
            bs.rewind();
            encoder.reset();

            // if cr != results[x]
            if (!((cr.isUnderflow() && result[index].isUnderflow())
                    || (cr.isOverflow() && result[index].isOverflow())
                    || (cr.isMalformed() && result[index].isMalformed())
                    || (cr.isUnmappable() && result[index].isUnmappable()))
                    || (cr.isError() && cr.length() != result[index].length())) {
                errln("Incorrect result in " + converter + " for \"" + input[index] + "\"" + ", expected: " + result[index] + ", received:  " + cr);
                break;
            }
        }
    }
    public void TestUTF8Converter() {
        String converter = "UTF-8";
        CharsetProvider icu = new CharsetProviderICU();
        Charset icuChar = icu.charsetForName(converter);
        CharsetEncoder encoder = icuChar.newEncoder();
        CharsetDecoder decoder = icuChar.newDecoder();
        ByteBuffer bs;
        CharBuffer us;
        CoderResult cr;

        
        int[] size = new int[] { 1<<7, 1<<11, 1<<16 }; // # of 1,2,3 byte combinations
        byte[] bytes = new byte[size[0] + size[1]*2 + size[2]*3];
        char[] chars = new char[size[0] + size[1] + size[2]];
        int i = 0;
        int x, y;
        
        // 0 to 1 << 7 (1 byters)
        for (; i < size[0]; i++) {
            bytes[i] = (byte) i;
            chars[i] = (char) i;
            bs = ByteBuffer.wrap(bytes, i, 1).slice();
            us = CharBuffer.wrap(chars, i, 1).slice();
            try {
                smBufDecode(decoder, converter, bs, us, true, false, true);
                smBufDecode(decoder, converter, bs, us, true, false, false);
                smBufEncode(encoder, converter, us, bs, true, false, true);
                smBufEncode(encoder, converter, us, bs, true, false, false);
            } catch (Exception ex) {
                errln("Incorrect result in " + converter + " for 0x"
                        + Integer.toHexString(i));
                break;
            }
        }

        // 1 << 7 to 1 << 11 (2 byters)
        for (; i < size[1]; i++) {
            x = size[0] + i*2;
            y = size[0] + i;
            bytes[x + 0] = (byte) (0xc0 | ((i >> 6) & 0x1f));
            bytes[x + 1] = (byte) (0x80 | ((i >> 0) & 0x3f));
            chars[y] = (char) i;
            bs = ByteBuffer.wrap(bytes, x, 2).slice();
            us = CharBuffer.wrap(chars, y, 1).slice();
            try {
                smBufDecode(decoder, converter, bs, us, true, false, true);
                smBufDecode(decoder, converter, bs, us, true, false, false);
                smBufEncode(encoder, converter, us, bs, true, false, true);
                smBufEncode(encoder, converter, us, bs, true, false, false);
            } catch (Exception ex) {
                errln("Incorrect result in " + converter + " for 0x"
                        + Integer.toHexString(i));
                break;
            }
        }

        // 1 << 11 to 1 << 16 (3 byters and surrogates)
        for (; i < size[2]; i++) {
            x = size[0] + size[1] * 2 + i * 3;
            y = size[0] + size[1] + i;
            bytes[x + 0] = (byte) (0xe0 | ((i >> 12) & 0x0f));
            bytes[x + 1] = (byte) (0x80 | ((i >> 6) & 0x3f));
            bytes[x + 2] = (byte) (0x80 | ((i >> 0) & 0x3f));
            chars[y] = (char) i;
            if (!UTF16.isSurrogate((char)i)) {
                bs = ByteBuffer.wrap(bytes, x, 3).slice();
                us = CharBuffer.wrap(chars, y, 1).slice();
                try {
                    smBufDecode(decoder, converter, bs, us, true, false, true);
                    smBufDecode(decoder, converter, bs, us, true, false, false);
                    smBufEncode(encoder, converter, us, bs, true, false, true);
                    smBufEncode(encoder, converter, us, bs, true, false, false);
                } catch (Exception ex) {
                    errln("Incorrect result in " + converter + " for 0x"
                            + Integer.toHexString(i));
                    break;
                }
            } else {
                bs = ByteBuffer.wrap(bytes, x, 3).slice();
                us = CharBuffer.wrap(chars, y, 1).slice();
                
                decoder.reset();
                cr = decoder.decode(bs, us, true);
                bs.rewind();
                us.rewind();
                if (!cr.isMalformed() || cr.length() != 3) {
                    errln("Incorrect result in " + converter + " decoder for 0x"
                            + Integer.toHexString(i) + " received " + cr);
                    break;
                }
                encoder.reset();
                cr = encoder.encode(us, bs, true);
                bs.rewind();
                us.rewind();
                if (!cr.isMalformed() || cr.length() != 1) {
                    errln("Incorrect result in " + converter + " encoder for 0x"
                            + Integer.toHexString(i) + " received " + cr);
                    break;
                }
                
                bs = ByteBuffer.wrap(bytes, x, 3).slice();
                us = CharBuffer.wrap(new String(chars, y, 1));
                
                decoder.reset();
                cr = decoder.decode(bs, us, true);
                bs.rewind();
                us.rewind();
                if (!cr.isMalformed() || cr.length() != 3) {
                    errln("Incorrect result in " + converter + " decoder for 0x"
                            + Integer.toHexString(i) + " received " + cr);
                    break;
                }
                encoder.reset();
                cr = encoder.encode(us, bs, true);
                bs.rewind();
                us.rewind();
                if (!cr.isMalformed() || cr.length() != 1) {
                    errln("Incorrect result in " + converter + " encoder for 0x"
                            + Integer.toHexString(i) + " received " + cr);
                    break;
                }
                
                
            }
        }
        if (true)
            return;
    }
    
    public void TestHZ() {
        /* test input */
        char[] in = new char[] {
                0x3000, 0x3001, 0x3002, 0x00B7, 0x02C9, 0x02C7, 0x00A8, 0x3003, 0x3005, 0x2014,
                0xFF5E, 0x2016, 0x2026, 0x007E, 0x997C, 0x70B3, 0x75C5, 0x5E76, 0x73BB, 0x83E0,
                0x64AD, 0x62E8, 0x94B5, 0x000A, 0x6CE2, 0x535A, 0x52C3, 0x640F, 0x94C2, 0x7B94,
                0x4F2F, 0x5E1B, 0x8236, 0x000A, 0x8116, 0x818A, 0x6E24, 0x6CCA, 0x9A73, 0x6355,
                0x535C, 0x54FA, 0x8865, 0x000A, 0x57E0, 0x4E0D, 0x5E03, 0x6B65, 0x7C3F, 0x90E8,
                0x6016, 0x248F, 0x2490, 0x000A, 0x2491, 0x2492, 0x2493, 0x2494, 0x2495, 0x2496,
                0x2497, 0x2498, 0x2499, 0x000A, 0x249A, 0x249B, 0x2474, 0x2475, 0x2476, 0x2477,
                0x2478, 0x2479, 0x247A, 0x000A, 0x247B, 0x247C, 0x247D, 0x247E, 0x247F, 0x2480,
                0x2481, 0x2482, 0x2483, 0x000A, 0x0041, 0x0043, 0x0044, 0x0045, 0x0046, 0x007E,
                0x0048, 0x0049, 0x004A, 0x000A, 0x004B, 0x004C, 0x004D, 0x004E, 0x004F, 0x0050,
                0x0051, 0x0052, 0x0053, 0x000A, 0x0054, 0x0055, 0x0056, 0x0057, 0x0058, 0x0059,
                0x005A, 0x005B, 0x005C, 0x000A
          };
        
        String converter = "HZ";
        CharsetProvider icu = new CharsetProviderICU();
        Charset icuChar = icu.charsetForName(converter);
        CharsetEncoder encoder = icuChar.newEncoder();
        CharsetDecoder decoder = icuChar.newDecoder();
        try {
            CharBuffer start = CharBuffer.wrap(in);
            ByteBuffer bytes = encoder.encode(start);
            CharBuffer finish = decoder.decode(bytes);
            
            if (!equals(start, finish)) {
                errln(converter + " roundtrip test failed: start does not match finish");
                
                char[] finishArray = new char[finish.limit()];
                for (int i=0; i<finishArray.length; i++)
                    finishArray[i] = finish.get(i);
                
                logln("start:  " + hex(in));
                logln("finish: " + hex(finishArray));
            }
        } catch (CharacterCodingException ex) {
            errln(converter + " roundtrip test failed: " + ex.getMessage());
            ex.printStackTrace(System.err);
        }
        
        /* For better code coverage */
        CoderResult result = CoderResult.UNDERFLOW;
        byte byteout[] = {
                (byte)0x7e, (byte)0x7d, (byte)0x41,
                (byte)0x7e, (byte)0x7b, (byte)0x21,
        };
        char charin[] = {
                (char)0x0041, (char)0x0042, (char)0x3000
        };
        ByteBuffer bb = ByteBuffer.wrap(byteout);
        CharBuffer cb = CharBuffer.wrap(charin);
        int testLoopSize = 5;
        int bbLimits[] = { 0, 1, 3, 4, 6};
        int bbPositions[] = { 0, 0, 0, 3, 3 };
        int ccPositions[] = { 0, 0, 0, 2, 2 };
        for (int i = 0; i < testLoopSize; i++) {
            encoder.reset();
            bb.limit(bbLimits[i]);
            bb.position(bbPositions[i]);
            cb.position(ccPositions[i]);
            result = encoder.encode(cb, bb, true);
            
            if (i < 3) {
                if (!result.isOverflow()) {
                    errln("Overflow buffer error should have occurred while encoding HZ (" + i + ")");
                }
            } else {
                if (result.isError()) {
                    errln("Error should not have occurred while encoding HZ.(" + i + ")");
                }
            }
        }
    }

    public void TestUTF8Surrogates() {
        byte[][] in = new byte[][] {
            { (byte)0x61, },
            { (byte)0xc2, (byte)0x80, },
            { (byte)0xe0, (byte)0xa0, (byte)0x80, },
            { (byte)0xf0, (byte)0x90, (byte)0x80, (byte)0x80, },
            { (byte)0xf4, (byte)0x84, (byte)0x8c, (byte)0xa1, },
            { (byte)0xf0, (byte)0x90, (byte)0x90, (byte)0x81, },
        };

        /* expected test results */
        char[][] results = new char[][] {
            /* number of bytes read, code point */
            { '\u0061', },
            { '\u0080', },
            { '\u0800', },
            { '\ud800', '\udc00', },      //  10000
            { '\udbd0', '\udf21', },      // 104321
            { '\ud801', '\udc01', },      //  10401
        };

        /* error test input */
        byte[][] in2 = new byte[][] {
            { (byte)0x61, },
            { (byte)0xc0, (byte)0x80,                                     /* illegal non-shortest form */
            (byte)0xe0, (byte)0x80, (byte)0x80,                           /* illegal non-shortest form */
            (byte)0xf0, (byte)0x80, (byte)0x80, (byte)0x80,               /* illegal non-shortest form */
            (byte)0xc0, (byte)0xc0,                                       /* illegal trail byte */
            (byte)0xf4, (byte)0x90, (byte)0x80, (byte)0x80,               /* 0x110000 out of range */
            (byte)0xf8, (byte)0x80, (byte)0x80, (byte)0x80, (byte)0x80,   /* too long */
            (byte)0xfe,                                                   /* illegal byte altogether */
            (byte)0x62, },
        };

        /* expected error test results */
        char[][] results2 = new char[][] {
            /* number of bytes read, code point */
            { '\u0062', },
            { '\u0062', },
        };
        
        String converter = "UTF-8";
        CharsetProvider icu = new CharsetProviderICU();
        Charset icuChar = icu.charsetForName(converter);
        CharsetDecoder decoder = icuChar.newDecoder();
        
        int i;
        try {
            for (i = 0; i < in.length; i++) {
                ByteBuffer source = ByteBuffer.wrap(in[i]);
                CharBuffer expected = CharBuffer.wrap(results[i]);
                smBufDecode(decoder, converter, source, expected, true, false,
                        true);
                smBufDecode(decoder, converter, source, expected, true, false,
                        false);
            }
        } catch (Exception ex) {
            errln("Incorrect result in " + converter);
        }
        try {
            for (i = 0; i < in2.length; i++) {
                ByteBuffer source = ByteBuffer.wrap(in2[i]);
                CharBuffer expected = CharBuffer.wrap(results2[i]);
                decoder.onMalformedInput(CodingErrorAction.IGNORE);
                smBufDecode(decoder, converter, source, expected, true, false,
                        true);
                smBufDecode(decoder, converter, source, expected, true, false,
                        false);
            }
        } catch (Exception ex) {
            errln("Incorrect result in " + converter);
        }
    }
    
    public void TestSurrogateBehavior() {
        CharsetProviderICU icu = new CharsetProviderICU();
        
        // get all the converters into an array
        Object[] converters = CharsetProviderICU.getAvailableNames();
        
        String norm = "a";
        String ext = "\u0275"; // theta
        String lead = "\ud835";
        String trail = "\udd04";
        // lead + trail = \U1d504 (fraktur capital A)
        
        String input = 
                        // error    position
                ext     // unmap(1) 1
                + lead  // under    1  
                + lead  // malf(1)  2
                + trail // unmap(2) 4
                + trail // malf(1)  5
                + ext   // unmap(1) 6
                + norm  // unmap(1) 7
        ;
        CoderResult[] results = new CoderResult[] {
                CoderResult.unmappableForLength(1), // or underflow
                CoderResult.UNDERFLOW,
                CoderResult.malformedForLength(1),
                CoderResult.unmappableForLength(2), // or underflow
                CoderResult.malformedForLength(1),
                CoderResult.unmappableForLength(1), // or underflow
                CoderResult.unmappableForLength(1), // or underflow
        };
        int[] positions = new int[] { 1,1,2,4,5,6,7 };
        int n = positions.length;
        
        int badcount = 0;
        int goodcount = 0;
        int[] uhohindices = new int[n];
        int[] badposindices = new int[n];
        int[] malfindices = new int[n];
        int[] unmapindices = new int[n];
        ArrayList pass = new ArrayList();
        ArrayList exempt = new ArrayList();
        
        outer: for (int conv=0; conv<converters.length; conv++) {
            String converter = (String)converters[conv];
            if (converter.equals("x-IMAP-mailbox-name") || converter.equals("UTF-7") || converter.equals("CESU-8") || converter.equals("BOCU-1") ||
                    converter.equals("x-LMBCS-1")) {
                exempt.add(converter);
                continue;
            }
            
            boolean currentlybad = false;
            Charset icuChar = icu.charsetForName(converter);
            CharsetEncoder encoder = icuChar.newEncoder();
            CoderResult cr;
                
            CharBuffer source = CharBuffer.wrap(input);
            ByteBuffer target = ByteBuffer.allocate(30);
            ByteBuffer expected = null;
            try {
                encoder.onUnmappableCharacter(CodingErrorAction.IGNORE);
                encoder.onMalformedInput(CodingErrorAction.IGNORE);
                expected = encoder.encode(CharBuffer.wrap(ext + lead + trail + ext + norm));
                encoder.reset();
            } catch (CharacterCodingException ex) {
                errln("Unexpected CharacterCodingException: " + ex.getMessage());
                return;
            } catch (RuntimeException ex) {
                if (!currentlybad) {currentlybad = true; badcount++; logln(""); }
                errln(converter + " " + ex.getClass().getName() + ": " + ex.getMessage());
                continue outer;
            }
            
            encoder.onUnmappableCharacter(CodingErrorAction.REPORT);
            encoder.onMalformedInput(CodingErrorAction.REPORT);
            for (int i=0; i<n; i++) {
                source.limit(i+1);
                cr = encoder.encode(source, target, i == n - 1);
                if (!(equals(cr, results[i])
                        || (results[i].isUnmappable() && cr.isUnderflow()) // mappability depends on the converter
                    )) {
                    if (!currentlybad) {currentlybad = true; badcount++; logln(""); }
                    if (results[i].isMalformed() && cr.isMalformed()) {
                        malfindices[i]++;
                    } else if (results[i].isUnmappable() && cr.isUnmappable()) {
                        unmapindices[i]++;
                    } else {
                        uhohindices[i]++;
                    }
                    errln("(index=" + i + ") " + converter + " Received: " + cr + " Expected: " + results[i]);
                }
                if (source.position() != positions[i]) {
                    if (!currentlybad) {currentlybad = true; badcount++; logln(""); }
                    badposindices[i]++;
                    errln("(index=" + i + ") " + converter + " Received: " + source.position() + " Expected: " + positions[i]);
                }
                    
            }
            encoder.reset();
            
            //System.out.println("\n" + hex(target.array()));
            //System.out.println(hex(expected.array()) + "\n" + expected.limit());
            if (!(equals(target, expected, expected.limit()) && target.position() == expected.limit())) {
                if (!currentlybad) {currentlybad = true; badcount++; logln(""); }
                errln(converter + " Received: \"" + hex(target.array()) + "\" Expected: \"" + hex(expected.array()) + "\"");
            }
            
            if (!currentlybad) {
                goodcount++;
                pass.add(converter);
            }
        }
        
        logln("\n" + badcount + " / " + (converters.length - exempt.size()) + "   (" + goodcount + " good, " + badcount + " bad)");
        log("index\t"); for (int i=0; i<n; i++) log(i + "\t"); logln("");
        log("unmap\t"); for (int i=0; i<n; i++) log(unmapindices[i] + "\t"); logln("");
        log("malf \t"); for (int i=0; i<n; i++) log(malfindices[i] + "\t"); logln("");
        log("pos  \t"); for (int i=0; i<n; i++) log(badposindices[i] + "\t"); logln("");
        log("uhoh \t"); for (int i=0; i<n; i++) log(uhohindices[i] + "\t"); logln("");
        logln("");
        log("The few that passed: "); for (int i=0; i<pass.size(); i++) log(pass.get(i) + ", "); logln(""); 
        log("The few that are exempt: "); for (int i=0; i<exempt.size(); i++) log(exempt.get(i) + ", "); logln(""); 
    }
    
//    public void TestCharsetCallback() {
//        String currentTest = "initialization";
//        try {
//            Class[] params;
//            
//            // get the classes
//            Class CharsetCallback = Class.forName("com.ibm.icu.charset.CharsetCallback");
//            Class Decoder = Class.forName("com.ibm.icu.charset.CharsetCallback$Decoder");
//            Class Encoder = Class.forName("com.ibm.icu.charset.CharsetCallback$Encoder");
//            
//            // set up encoderCall
//            params = new Class[] {CharsetEncoderICU.class, Object.class, 
//                    CharBuffer.class, ByteBuffer.class, IntBuffer.class, 
//                    char[].class, int.class, int.class, CoderResult.class };
//            Method encoderCall = Encoder.getDeclaredMethod("call", params);
//            
//            // set up decoderCall
//            params = new Class[] {CharsetDecoderICU.class, Object.class, 
//                    ByteBuffer.class, CharBuffer.class, IntBuffer.class,
//                    char[].class, int.class, CoderResult.class};
//            Method decoderCall = Decoder.getDeclaredMethod("call", params);
//            
//            // get relevant fields
//            Object SUB_STOP_ON_ILLEGAL = getFieldValue(CharsetCallback, "SUB_STOP_ON_ILLEGAL", null);
//            
//            // set up a few arguments
//            CharsetProvider provider = new CharsetProviderICU();
//            Charset charset = provider.charsetForName("UTF-8");
//            CharsetEncoderICU encoder = (CharsetEncoderICU)charset.newEncoder();
//            CharsetDecoderICU decoder = (CharsetDecoderICU)charset.newDecoder();
//            CharBuffer chars = CharBuffer.allocate(10);
//            chars.put('o');
//            chars.put('k');
//            ByteBuffer bytes = ByteBuffer.allocate(10);
//            bytes.put((byte)'o');
//            bytes.put((byte)'k');
//            IntBuffer offsets = IntBuffer.allocate(10);
//            offsets.put(0);
//            offsets.put(1);
//            char[] buffer = null;
//            Integer length = new Integer(2);
//            Integer cp = new Integer(0);
//            CoderResult unmap = CoderResult.unmappableForLength(2);
//            CoderResult malf = CoderResult.malformedForLength(2);
//            CoderResult under = CoderResult.UNDERFLOW;
//            
//            // set up error arrays
//            Integer invalidCharLength = new Integer(1);
//            Byte subChar1 = new Byte((byte)0);
//            Byte subChar1_alternate = new Byte((byte)1); // for TO_U_CALLBACK_SUBSTITUTE
//            
//            // set up chars and bytes backups and expected values for certain cases
//            CharBuffer charsBackup = bufferCopy(chars);
//            ByteBuffer bytesBackup = bufferCopy(bytes);
//            IntBuffer offsetsBackup = bufferCopy(offsets);
//            CharBuffer encoderCharsExpected = bufferCopy(chars);
//            ByteBuffer encoderBytesExpected = bufferCopy(bytes);
//            IntBuffer encoderOffsetsExpected = bufferCopy(offsets);
//            CharBuffer decoderCharsExpected1 = bufferCopy(chars);
//            CharBuffer decoderCharsExpected2 = bufferCopy(chars);
//            IntBuffer decoderOffsetsExpected1 = bufferCopy(offsets);
//            IntBuffer decoderOffsetsExpected2 = bufferCopy(offsets);
//            
//            // initialize fields to obtain expected data
//            setFieldValue(CharsetDecoderICU.class, "invalidCharLength", decoder, invalidCharLength);
//            setFieldValue(CharsetICU.class, "subChar1", ((CharsetICU) decoder.charset()), subChar1);
//            
//            // run cbFromUWriteSub
//            Method cbFromUWriteSub = CharsetEncoderICU.class.getDeclaredMethod("cbFromUWriteSub", new Class[] { CharsetEncoderICU.class, CharBuffer.class, ByteBuffer.class, IntBuffer.class});
//            cbFromUWriteSub.setAccessible(true);
//            CoderResult encoderResultExpected = (CoderResult)cbFromUWriteSub.invoke(encoder, new Object[] {encoder, encoderCharsExpected, encoderBytesExpected, encoderOffsetsExpected});
//            
//            // run toUWriteUChars with normal data
//            Method toUWriteUChars = CharsetDecoderICU.class.getDeclaredMethod("toUWriteUChars", new Class[] { CharsetDecoderICU.class, char[].class, int.class, int.class, CharBuffer.class, IntBuffer.class, int.class});
//            toUWriteUChars.setAccessible(true);
//            CoderResult decoderResultExpected1 = (CoderResult)toUWriteUChars.invoke(decoder, new Object[] {decoder, new char[] {0xFFFD}, new Integer(0), new Integer(1), decoderCharsExpected1, decoderOffsetsExpected1, new Integer(bytes.position())});
//            
//            // reset certain fields
//            setFieldValue(CharsetDecoderICU.class, "invalidCharLength", decoder, invalidCharLength);
//            setFieldValue(CharsetICU.class, "subChar1", ((CharsetICU) decoder.charset()), subChar1_alternate);
//            
//            // run toUWriteUChars again
//            CoderResult decoderResultExpected2 = (CoderResult)toUWriteUChars.invoke(decoder, new Object[] {decoder, new char[] {0x1A}, new Integer(0), new Integer(1), decoderCharsExpected2, decoderOffsetsExpected2, new Integer(bytes.position())});
//            
//            // begin creating the tests array
//            ArrayList tests = new ArrayList();
//            
//            // create tests for FROM_U_CALLBACK_SKIP   0
//            tests.add(new Object[] {encoderCall, "FROM_U_CALLBACK_SKIP", new Object[] { encoder, null, chars, bytes, offsets, buffer, length, cp, null }, under, charsBackup, bytesBackup, offsetsBackup, new Object[] { }});
//            tests.add(new Object[] {encoderCall, "FROM_U_CALLBACK_SKIP", new Object[] { encoder, SUB_STOP_ON_ILLEGAL, chars, bytes, offsets, buffer, length, cp, malf }, malf, charsBackup, bytesBackup, offsetsBackup, new Object[] { }});
//            tests.add(new Object[] {encoderCall, "FROM_U_CALLBACK_SKIP", new Object[] { encoder, SUB_STOP_ON_ILLEGAL, chars, bytes, offsets, buffer, length, cp, unmap }, under, charsBackup, bytesBackup, offsetsBackup, new Object[] { }});
//            tests.add(new Object[] {encoderCall, "FROM_U_CALLBACK_SKIP", new Object[] { encoder, SUB_STOP_ON_ILLEGAL + "xx", chars, bytes, offsets, buffer, length, cp, null }, null, charsBackup, bytesBackup, offsetsBackup, new Object[] { }});
//            
//            // create tests for TO_U_CALLBACK_SKIP    4
//            tests.add(new Object[] {decoderCall, "TO_U_CALLBACK_SKIP", new Object[] { decoder, null, bytes, chars, offsets, buffer, length, null }, under, charsBackup, bytesBackup, offsetsBackup, new Object[] { invalidCharLength, subChar1 }});
//            tests.add(new Object[] {decoderCall, "TO_U_CALLBACK_SKIP", new Object[] { decoder, SUB_STOP_ON_ILLEGAL, bytes, chars, offsets, buffer, length, malf }, malf, charsBackup, bytesBackup, offsetsBackup, new Object[] { invalidCharLength, subChar1 }});
//            tests.add(new Object[] {decoderCall, "TO_U_CALLBACK_SKIP", new Object[] { decoder, SUB_STOP_ON_ILLEGAL, bytes, chars, offsets, buffer, length, unmap }, under, charsBackup, bytesBackup, offsetsBackup, new Object[] { invalidCharLength, subChar1 }});
//            tests.add(new Object[] {decoderCall, "TO_U_CALLBACK_SKIP", new Object[] { decoder, SUB_STOP_ON_ILLEGAL + "xx", bytes, chars, offsets, buffer, length, null }, null, charsBackup, bytesBackup, offsetsBackup, new Object[] { invalidCharLength, subChar1 }});
//            
//            // create tests for FROM_U_CALLBACK_STOP   8
//            tests.add(new Object[] {encoderCall, "FROM_U_CALLBACK_STOP", new Object[] { encoder, null, chars, bytes, offsets, buffer, length, cp, null }, null, charsBackup, bytesBackup, offsetsBackup, new Object[] { }});
//            tests.add(new Object[] {encoderCall, "FROM_U_CALLBACK_STOP", new Object[] { encoder, SUB_STOP_ON_ILLEGAL, chars, bytes, offsets, buffer, length, cp, malf }, malf, charsBackup, bytesBackup, offsetsBackup, new Object[] { }});
//            tests.add(new Object[] {encoderCall, "FROM_U_CALLBACK_STOP", new Object[] { encoder, SUB_STOP_ON_ILLEGAL, chars, bytes, offsets, buffer, length, cp, unmap }, unmap, charsBackup, bytesBackup, offsetsBackup, new Object[] { }});
//            tests.add(new Object[] {encoderCall, "FROM_U_CALLBACK_STOP", new Object[] { encoder, SUB_STOP_ON_ILLEGAL + "xx", chars, bytes, offsets, buffer, length, cp, null }, null, charsBackup, bytesBackup, offsetsBackup, new Object[] { }});
//            
//            // create tests for TO_U_CALLBACK_STOP   12
//            tests.add(new Object[] {decoderCall, "TO_U_CALLBACK_STOP", new Object[] { decoder, null, bytes, chars, offsets, buffer, length, null }, null, charsBackup, bytesBackup, offsetsBackup, new Object[] { invalidCharLength, subChar1 }});
//            tests.add(new Object[] {decoderCall, "TO_U_CALLBACK_STOP", new Object[] { decoder, SUB_STOP_ON_ILLEGAL, bytes, chars, offsets, buffer, length, malf }, malf, charsBackup, bytesBackup, offsetsBackup, new Object[] { invalidCharLength, subChar1 }});
//            tests.add(new Object[] {decoderCall, "TO_U_CALLBACK_STOP", new Object[] { decoder, SUB_STOP_ON_ILLEGAL, bytes, chars, offsets, buffer, length, unmap }, unmap, charsBackup, bytesBackup, offsetsBackup, new Object[] { invalidCharLength, subChar1 }});
//            tests.add(new Object[] {decoderCall, "TO_U_CALLBACK_STOP", new Object[] { decoder, SUB_STOP_ON_ILLEGAL + "xx", bytes, chars, offsets, buffer, length, null }, null, charsBackup, bytesBackup, offsetsBackup, new Object[] { invalidCharLength, subChar1 }});
//            
//            // create tests for FROM_U_CALLBACK_SUBSTITUTE  16
//            tests.add(new Object[] {encoderCall, "FROM_U_CALLBACK_SUBSTITUTE", new Object[] { encoder, null, chars, bytes, offsets, buffer, length, cp, null }, encoderResultExpected, encoderCharsExpected, encoderBytesExpected, encoderOffsetsExpected, new Object[] { }});
//            tests.add(new Object[] {encoderCall, "FROM_U_CALLBACK_SUBSTITUTE", new Object[] { encoder, SUB_STOP_ON_ILLEGAL, chars, bytes, offsets, buffer, length, cp, malf }, malf, charsBackup, bytesBackup, offsetsBackup, new Object[] { }});
//            tests.add(new Object[] {encoderCall, "FROM_U_CALLBACK_SUBSTITUTE", new Object[] { encoder, SUB_STOP_ON_ILLEGAL, chars, bytes, offsets, buffer, length, cp, unmap }, encoderResultExpected, encoderCharsExpected, encoderBytesExpected, encoderOffsetsExpected, new Object[] { }});
//            tests.add(new Object[] {encoderCall, "FROM_U_CALLBACK_SUBSTITUTE", new Object[] { encoder, SUB_STOP_ON_ILLEGAL + "xx", chars, bytes, offsets, buffer, length, cp, null }, null, charsBackup, bytesBackup, offsetsBackup, new Object[] { }});
//            
//            // create tests for TO_U_CALLBACK_SUBSTITUTE   20
//            tests.add(new Object[] {decoderCall, "TO_U_CALLBACK_SUBSTITUTE", new Object[] { decoder, null, bytes, chars, offsets, buffer, length, null }, decoderResultExpected1, decoderCharsExpected1, bytesBackup, decoderOffsetsExpected1, new Object[] { invalidCharLength, subChar1 }});
//            tests.add(new Object[] {decoderCall, "TO_U_CALLBACK_SUBSTITUTE", new Object[] { decoder, null, bytes, chars, offsets, buffer, length, null }, decoderResultExpected2, decoderCharsExpected2, bytesBackup, decoderOffsetsExpected2, new Object[] { invalidCharLength, subChar1_alternate }});
//            
//            Iterator iter = tests.iterator();
//            for (int i=0; iter.hasNext(); i++) {
//                // get the data out of the map
//                Object[] next = (Object[])iter.next();
//                
//                Method method = (Method)next[0];
//                String fieldName = (String)next[1];
//                Object field = getFieldValue(CharsetCallback, fieldName, null);
//                Object[] args = (Object[])next[2];
//                CoderResult expected = (CoderResult)next[3];
//                CharBuffer charsExpected = (CharBuffer)next[4];
//                ByteBuffer bytesExpected = (ByteBuffer)next[5];
//                IntBuffer offsetsExpected = (IntBuffer)next[6];
//                
//                // set up error arrays and certain fields
//                Object[] values = (Object[])next[7];
//                if (method == decoderCall) {
//                    decoder.reset();
//                    setFieldValue(CharsetDecoderICU.class, "invalidCharLength", decoder, values[0]);
//                    setFieldValue(CharsetICU.class, "subChar1", ((CharsetICU) decoder.charset()), values[1]);
//                } else if (method == encoderCall) {
//                    encoder.reset();
//                }
//                
//                try {
//                    // invoke the method
//                    CoderResult actual = (CoderResult)method.invoke(field, args);
//                    
//                    // if expected != actual
//                    if (!coderResultsEqual(expected, actual)) {
//                        // case #i refers to the index in the arraylist tests
//                        errln(fieldName + " failed to return the correct result for case #" + i + ".");
//                    }
//                    // if the expected buffers != actual buffers
//                    else if (!(buffersEqual(chars, charsExpected) && 
//                            buffersEqual(bytes, bytesExpected) &&
//                            buffersEqual(offsets, offsetsExpected))) {
//                        // case #i refers to the index in the arraylist tests
//                        errln(fieldName + " did not perform the correct operation on the buffers for case #" + i + ".");
//                    }
//                } catch (InvocationTargetException ex)  {
//                    // case #i refers to the index in the arraylist tests
//                    errln(fieldName + " threw an exception for case #" + i + ": " + ex.getCause());
//                    //ex.getCause().printStackTrace();
//                }
//                
//                // reset the buffers
//                System.arraycopy(bytesBackup.array(), 0, bytes.array(), 0, 10);
//                System.arraycopy(charsBackup.array(), 0, chars.array(), 0, 10);
//                System.arraycopy(offsetsBackup.array(), 0, offsets.array(), 0, 10);
//                bytes.position(bytesBackup.position());
//                chars.position(charsBackup.position());
//                offsets.position(offsetsBackup.position());
//            }
//            
//        } catch (Exception ex) {
//            errln("TestCharsetCallback skipped due to " + ex.toString());
//            ex.printStackTrace();
//        }
//    }
//    
//    private Object getFieldValue(Class c, String name, Object instance) throws Exception {
//        Field field = c.getDeclaredField(name);
//        field.setAccessible(true);
//        return field.get(instance);
//    }
//    private void setFieldValue(Class c, String name, Object instance, Object value) throws Exception {
//        Field field = c.getDeclaredField(name);
//        field.setAccessible(true);
//        if (value instanceof Boolean)
//            field.setBoolean(instance, ((Boolean)value).booleanValue());
//        else if (value instanceof Byte)
//            field.setByte(instance, ((Byte)value).byteValue());
//        else if (value instanceof Character)
//            field.setChar(instance, ((Character)value).charValue());
//        else if (value instanceof Double)
//            field.setDouble(instance, ((Double)value).doubleValue());
//        else if (value instanceof Float)
//            field.setFloat(instance, ((Float)value).floatValue());
//        else if (value instanceof Integer)
//            field.setInt(instance, ((Integer)value).intValue());
//        else if (value instanceof Long)
//            field.setLong(instance, ((Long)value).longValue());
//        else if (value instanceof Short)
//            field.setShort(instance, ((Short)value).shortValue());
//        else
//            field.set(instance, value);
//    }
//    private boolean coderResultsEqual(CoderResult a, CoderResult b) {
//        if (a == null && b == null)
//            return true;
//        if (a == null || b == null)
//            return false;
//        if ((a.isUnderflow() && b.isUnderflow()) || (a.isOverflow() && b.isOverflow()))
//            return true;
//        if (a.length() != b.length())
//            return false;
//        if ((a.isMalformed() && b.isMalformed()) || (a.isUnmappable() && b.isUnmappable()))
//            return true;
//        return false;
//    }
//    private boolean buffersEqual(ByteBuffer a, ByteBuffer b) {
//        if (a.position() != b.position())
//            return false;
//        int limit = a.position();
//        for (int i=0; i<limit; i++)
//            if (a.get(i) != b.get(i))
//                return false;
//        return true;
//    }
//    private boolean buffersEqual(CharBuffer a, CharBuffer b) {
//        if (a.position() != b.position())
//            return false;
//        int limit = a.position();
//        for (int i=0; i<limit; i++)
//            if (a.get(i) != b.get(i))
//                return false;
//        return true;
//    }
//    private boolean buffersEqual(IntBuffer a, IntBuffer b) {
//        if (a.position() != b.position())
//            return false;
//        int limit = a.position();
//        for (int i=0; i<limit; i++)
//            if (a.get(i) != b.get(i))
//                return false;
//        return true;
//    }
//    private ByteBuffer bufferCopy(ByteBuffer src) {
//        ByteBuffer dest = ByteBuffer.allocate(src.limit());
//        System.arraycopy(src.array(), 0, dest.array(), 0, src.limit());
//        dest.position(src.position());
//        return dest;
//    }
//    private CharBuffer bufferCopy(CharBuffer src) {
//        CharBuffer dest = CharBuffer.allocate(src.limit());
//        System.arraycopy(src.array(), 0, dest.array(), 0, src.limit());
//        dest.position(src.position());
//        return dest;
//    }
//    private IntBuffer bufferCopy(IntBuffer src) {
//        IntBuffer dest = IntBuffer.allocate(src.limit());
//        System.arraycopy(src.array(), 0, dest.array(), 0, src.limit());
//        dest.position(src.position());
//        return dest;
//    }
    

    public void TestAPISemantics(/*String encoding*/) 
                throws Exception {
        int rc;
        ByteBuffer byes = ByteBuffer.wrap(byteStr);
        CharBuffer uniVal = CharBuffer.wrap(unistr);
        ByteBuffer expected = ByteBuffer.wrap(expectedByteStr);
        
        rc = 0;
        if(m_decoder==null){
            warnln("Could not load decoder.");
            return;
        }
        m_decoder.reset();
        /* Convert the whole buffer to Unicode */
        try {
            CharBuffer chars = CharBuffer.allocate(unistr.length());
            CoderResult result = m_decoder.decode(byes, chars, false);

            if (result.isError()) {
                errln("ToChars encountered Error");
                rc = 1;
            }
            if (result.isOverflow()) {
                errln("ToChars encountered overflow exception");
                rc = 1;
            }
            if (!equals(chars, unistr)) {
                errln("ToChars does not match");
                printchars(chars);
                errln("Expected : ");
                printchars(unistr);
                rc = 2;
            }

        } catch (Exception e) {
            errln("ToChars - exception in buffer");
            rc = 5;
        }

        /* Convert single bytes to Unicode */
        try {
            CharBuffer chars = CharBuffer.allocate(unistr.length());
            ByteBuffer b = ByteBuffer.wrap(byteStr);
            m_decoder.reset();
            CoderResult result=null;
            for (int i = 1; i <= byteStr.length; i++) {
                b.limit(i);
                result = m_decoder.decode(b, chars, false);
                if(result.isOverflow()){
                    errln("ToChars single threw an overflow exception");
                }
                if (result.isError()) {
                    errln("ToChars single the result is an error "+result.toString());
                } 
            }
            if (unistr.length() != (chars.limit())) {
                errln("ToChars single len does not match");
                rc = 3;
            }
            if (!equals(chars, unistr)) {
                errln("ToChars single does not match");
                printchars(chars);
                rc = 4;
            }
        } catch (Exception e) {
            errln("ToChars - exception in single");
            //e.printStackTrace();
            rc = 6;
        }

        /* Convert the buffer one at a time to Unicode */
        try {
            CharBuffer chars = CharBuffer.allocate(unistr.length());
            m_decoder.reset();
            byes.rewind();
            for (int i = 1; i <= byteStr.length; i++) {
                byes.limit(i);
                CoderResult result = m_decoder.decode(byes, chars, false);
                if (result.isError()) {
                    errln("Error while decoding: "+result.toString());
                }
                if(result.isOverflow()){
                    errln("ToChars Simple threw an overflow exception");
                }
            }
            if (chars.limit() != unistr.length()) {
                errln("ToChars Simple buffer len does not match");
                rc = 7;
            }
            if (!equals(chars, unistr)) {
                errln("ToChars Simple buffer does not match");
                printchars(chars);
                err(" Expected : ");
                printchars(unistr);
                rc = 8;
            }
        } catch (Exception e) {
            errln("ToChars - exception in single buffer");
            //e.printStackTrace(System.err);
            rc = 9;
        }
        if (rc != 0) {
            errln("Test Simple ToChars for encoding : FAILED");
        }

        rc = 0;
        /* Convert the whole buffer from unicode */
        try {
            ByteBuffer bytes = ByteBuffer.allocate(expectedByteStr.length);
            m_encoder.reset();
            CoderResult result = m_encoder.encode(uniVal, bytes, false);
            if (result.isError()) {
                errln("FromChars reported error: " + result.toString());
                rc = 1;
            }
            if(result.isOverflow()){
                errln("FromChars threw an overflow exception");
            }
            bytes.position(0);
            if (!bytes.equals(expected)) {
                errln("FromChars does not match");
                printbytes(bytes);
                printbytes(expected);
                rc = 2;
            }
        } catch (Exception e) {
            errln("FromChars - exception in buffer");
            //e.printStackTrace(System.err);
            rc = 5;
        }

        /* Convert the buffer one char at a time to unicode */
        try {
            ByteBuffer bytes = ByteBuffer.allocate(expectedByteStr.length);
            CharBuffer c = CharBuffer.wrap(unistr);
            m_encoder.reset();
            CoderResult result= null;
            for (int i = 1; i <= unistr.length(); i++) {
                c.limit(i);
                result = m_encoder.encode(c, bytes, false);
                if(result.isOverflow()){
                    errln("FromChars single threw an overflow exception");
                }
                if(result.isError()){
                    errln("FromChars single threw an error: "+ result.toString());
                }
            }
            if (expectedByteStr.length != bytes.limit()) {
                errln("FromChars single len does not match");
                rc = 3;
            }

            bytes.position(0);
            if (!bytes.equals(expected)) {
                errln("FromChars single does not match");
                printbytes(bytes);
                printbytes(expected);
                rc = 4;
            }

        } catch (Exception e) {
            errln("FromChars - exception in single");
            //e.printStackTrace(System.err);
            rc = 6;
        }

        /* Convert one char at a time to unicode */
        try {
            ByteBuffer bytes = ByteBuffer.allocate(expectedByteStr.length);
            m_encoder.reset();
            char[] temp = unistr.toCharArray();
            CoderResult result=null;
            for (int i = 0; i <= temp.length; i++) {
                uniVal.limit(i);
                result = m_encoder.encode(uniVal, bytes, false);
                if(result.isOverflow()){
                    errln("FromChars simple threw an overflow exception");
                }
                if(result.isError()){
                    errln("FromChars simple threw an error: "+ result.toString());
                }
            }
            if (bytes.limit() != expectedByteStr.length) {
                errln("FromChars Simple len does not match");
                rc = 7;
            }
            if (!bytes.equals(byes)) {
                errln("FromChars Simple does not match");
                printbytes(bytes);
                printbytes(byes);
                rc = 8;
            }
        } catch (Exception e) {
            errln("FromChars - exception in single buffer");
            //e.printStackTrace(System.err);
            rc = 9;
        }
        if (rc != 0) {
            errln("Test Simple FromChars " + m_encoding + " --FAILED");
        }
    }

    void printchars(CharBuffer buf) {
        int i;
        char[] chars = new char[buf.limit()];
        //save the current position
        int pos = buf.position();
        buf.position(0);
        buf.get(chars);
        //reset to old position
        buf.position(pos);
        for (i = 0; i < chars.length; i++) {
            err(hex(chars[i]) + " ");
        }
        errln("");
    }
    void printchars(String str) {
        char[] chars = str.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            err(hex(chars[i]) + " ");
        }
        errln("");
    }
    void printbytes(ByteBuffer buf) {
        int i;
        byte[] bytes = new byte[buf.limit()];
        //save the current position
        int pos = buf.position();
        buf.position(0);
        buf.get(bytes);
        //reset to old position
        buf.position(pos);
        for (i = 0; i < bytes.length; i++) {
            System.out.print(hex(bytes[i]) + " ");
        }
        errln("");
    }

    public boolean equals(CoderResult a, CoderResult b) {
        return (a.isUnderflow() && b.isUnderflow())
                || (a.isOverflow() && b.isOverflow())
                || (a.isMalformed() && b.isMalformed() && a.length() == b.length())
                || (a.isUnmappable() && b.isUnmappable() && a.length() == b.length());
    }
    public boolean equals(CharBuffer buf, String str) {
        return equals(buf, str.toCharArray());
    }
    public boolean equals(CharBuffer buf, CharBuffer str) {
        if (buf.limit() != str.limit())
            return false;
        int limit = buf.limit();
        for (int i = 0; i < limit; i++)
            if (buf.get(i) != str.get(i))
                return false;
        return true;
    }
    public boolean equals(CharBuffer buf, CharBuffer str, int limit) {
        if (limit > buf.limit() || limit > str.limit())
            return false;
        for (int i = 0; i < limit; i++)
            if (buf.get(i) != str.get(i))
                return false;
        return true;
    }
    public boolean equals(CharBuffer buf, char[] compareTo) {
        char[] chars = new char[buf.limit()];
        //save the current position
        int pos = buf.position();
        buf.position(0);
        buf.get(chars);
        //reset to old position
        buf.position(pos);
        return equals(chars, compareTo);
    }

    public boolean equals(char[] chars, char[] compareTo) {
        if (chars.length != compareTo.length) {
            errln(
                "Length does not match chars: "
                    + chars.length
                    + " compareTo: "
                    + compareTo.length);
            return false;
        } else {
            boolean result = true;
            for (int i = 0; i < chars.length; i++) {
                if (chars[i] != compareTo[i]) {    
                    logln(
                        "Got: "
                            + hex(chars[i])
                            + " Expected: "
                            + hex(compareTo[i])
                            + " At: "
                            + i);
                    result = false;
                }
            }
            return result;
        }
    }

    public boolean equals(ByteBuffer buf, byte[] compareTo) {
        byte[] chars = new byte[buf.limit()];
        //save the current position
        int pos = buf.position();
        buf.position(0);
        buf.get(chars);
        //reset to old position
        buf.position(pos);
        return equals(chars, compareTo);
    }
    public boolean equals(ByteBuffer buf, ByteBuffer compareTo) {
        if (buf.limit() != compareTo.limit())
            return false;
        int limit = buf.limit();
        for (int i = 0; i < limit; i++)
            if (buf.get(i) != compareTo.get(i))
                return false;
        return true;
    }
    public boolean equals(ByteBuffer buf, ByteBuffer compareTo, int limit) {
        if (limit > buf.limit() || limit > compareTo.limit())
            return false;
        for (int i = 0; i < limit; i++)
            if (buf.get(i) != compareTo.get(i))
                return false;
        return true;
    }
    public boolean equals(byte[] chars, byte[] compareTo) {
        if (false/*chars.length != compareTo.length*/) {
            errln(
                "Length does not match chars: "
                    + chars.length
                    + " compareTo: "
                    + compareTo.length);
            return false;
        } else {
            boolean result = true;
            for (int i = 0; i < chars.length; i++) {
                if (chars[i] != compareTo[i]) {
                    logln(
                        "Got: "
                            + hex(chars[i])
                            + " Expected: "
                            + hex(compareTo[i])
                            + " At: "
                            + i);
                    result = false;
                }
            }
            return result;
        }
    }

//  TODO
  /*
    public void TestCallback(String encoding) throws Exception {
        
        byte[] gbSource =
            {
                (byte) 0x81,
                (byte) 0x36,
                (byte) 0xDE,
                (byte) 0x36,
                (byte) 0x81,
                (byte) 0x36,
                (byte) 0xDE,
                (byte) 0x37,
                (byte) 0x81,
                (byte) 0x36,
                (byte) 0xDE,
                (byte) 0x38,
                (byte) 0xe3,
                (byte) 0x32,
                (byte) 0x9a,
                (byte) 0x36 };

        char[] subChars = { 'P', 'I' };

        decoder.reset();

        decoder.replaceWith(new String(subChars));
        ByteBuffer mySource = ByteBuffer.wrap(gbSource);
        CharBuffer myTarget = CharBuffer.allocate(5);

        decoder.decode(mySource, myTarget, true);
        char[] expectedResult =
            { '\u22A6', '\u22A7', '\u22A8', '\u0050', '\u0049', };

        if (!equals(myTarget, new String(expectedResult))) {
            errln("Test callback GB18030 to Unicode : FAILED");
        }
        
    }
*/
    public void TestCanConvert(/*String encoding*/)throws Exception {
        char[] mySource = { 
            '\ud800', '\udc00',/*surrogate pair */
            '\u22A6','\u22A7','\u22A8','\u22A9','\u22AA',
            '\u22AB','\u22AC','\u22AD','\u22AE','\u22AF',
            '\u22B0','\u22B1','\u22B2','\u22B3','\u22B4',
            '\ud800','\udc00',/*surrogate pair */
            '\u22B5','\u22B6','\u22B7','\u22B8','\u22B9',
            '\u22BA','\u22BB','\u22BC','\u22BD','\u22BE' 
            };
        if(m_encoder==null){
            warnln("Could not load encoder.");
            return;
        }
        m_encoder.reset();
        if (!m_encoder.canEncode(new String(mySource))) {
            errln("Test canConvert() " + m_encoding + " failed. "+m_encoder);
        }

    }
    public void TestAvailableCharsets() {
        SortedMap map = Charset.availableCharsets();
        Set keySet = map.keySet();
        Iterator iter = keySet.iterator();
        while(iter.hasNext()){
            logln("Charset name: "+iter.next().toString());
        }
        Object[] charsets = CharsetProviderICU.getAvailableNames();
        int mapSize = map.size();
        if(mapSize < charsets.length){
            errln("Charset.availableCharsets() returned a number less than the number returned by icu. ICU: " + charsets.length
                    + " JDK: " + mapSize);
        }
        logln("Total Number of chasets = " + map.size());
    }
    /* ticket 5580 */
    public void TestJavaCanonicalNameOnAvailableCharsets() {
        CharsetProviderICU provider = new CharsetProviderICU();
        Iterator allCharsets = provider.charsets();
        String errorMessage = null;
        
        while (allCharsets.hasNext()) {
            Charset _chset = (Charset)allCharsets.next();
            Charset chset = Charset.forName(_chset.name());
            
            if (!chset.name().equals(_chset.name())) {
                if (errorMessage == null) {
                    errorMessage = new String("Error: Charset.forName( " + _chset.name() + " ) returned " + chset + " instead of " + _chset);
                } else {
                    errorMessage = errorMessage + "\nError: Charset.forName( " + _chset.name() + " ) returned " + chset + " instead of " + _chset;
                }
            }
        }
        
        if (errorMessage != null) {
            errln(errorMessage);
        }
    }
    
    public void TestWindows936(){
        CharsetProviderICU icu = new CharsetProviderICU();
        Charset cs = icu.charsetForName("windows-936-2000");
        String canonicalName = cs.name();
        if(!canonicalName.equals("GBK")){
            errln("Did not get the expected canonical name. Got: "+canonicalName); //get the canonical name
        }
    }
    
    public void TestICUAvailableCharsets() {
        CharsetProviderICU icu = new CharsetProviderICU();
        Object[] charsets = CharsetProviderICU.getAvailableNames();
        for(int i=0;i<charsets.length;i++){
            Charset cs = icu.charsetForName((String)charsets[i]);
            try{
                CharsetEncoder encoder = cs.newEncoder();
                if(encoder!=null){
                    logln("Creation of encoder succeeded. "+cs.toString());
                }
            }catch(Exception ex){
                errln("Could not instantiate encoder for "+charsets[i]+". Error: "+ex.toString());
            }
            try{
                CharsetDecoder decoder = cs.newDecoder();
                if(decoder!=null){
                    logln("Creation of decoder succeeded. "+cs.toString());
                }
            }catch(Exception ex){
                errln("Could not instantiate decoder for "+charsets[i]+". Error: "+ex.toString());
            }
        }
    }
    /* jitterbug 4312 */
    public void TestUnsupportedCharset(){
        CharsetProvider icu = new CharsetProviderICU();
        Charset icuChar = icu.charsetForName("impossible");
        if(icuChar != null){
            errln("ICU does not conform to the spec");
        }
    }


    public void TestEncoderCreation(){
        try{
            Charset cs = Charset.forName("GB_2312-80");
            CharsetEncoder enc = cs.newEncoder();
            if(enc!=null && (enc instanceof CharsetEncoderICU) ){
                logln("Successfully created the encoder: "+ enc);
            }else{
                errln("Error creating charset encoder.");
            }
        }catch(Exception e){
            warnln("Error creating charset encoder."+ e.toString());
           // e.printStackTrace();
        }
        try{
            Charset cs = Charset.forName("x-ibm-971_P100-1995");
            CharsetEncoder enc = cs.newEncoder();
            if(enc!=null && (enc instanceof CharsetEncoderICU) ){
                logln("Successfully created the encoder: "+ enc);
            }else{
                errln("Error creating charset encoder.");
            }
        }catch(Exception e){
            warnln("Error creating charset encoder."+ e.toString());
        }
    }
    public void TestSubBytes(){
        try{
            //create utf-8 decoder
            CharsetDecoder decoder = new CharsetProviderICU().charsetForName("utf-8").newDecoder();
    
            //create a valid byte array, which can be decoded to " buffer"
            byte[] unibytes = new byte[] { 0x0020, 0x0062, 0x0075, 0x0066, 0x0066, 0x0065, 0x0072 };
    
            ByteBuffer buffer = ByteBuffer.allocate(20);
    
            //add a evil byte to make the byte buffer be malformed input
            buffer.put((byte)0xd8);
    
            //put the valid byte array
            buffer.put(unibytes);
    
            //reset postion
            buffer.flip();  
            
            decoder.onMalformedInput(CodingErrorAction.REPLACE);
            CharBuffer out = decoder.decode(buffer);
            String expected = "\ufffd buffer";
            if(!expected.equals(new String(out.array()))){
                errln("Did not get the expected result for substitution chars. Got: "+
                       new String(out.array()) + "("+ hex(out.array())+")");
            }
            logln("Output: "+  new String(out.array()) + "("+ hex(out.array())+")");
        }catch (CharacterCodingException ex){
            errln("Unexpected exception: "+ex.toString());
        }
    }
    /*
    public void TestImplFlushFailure(){
   
       try{
           CharBuffer in = CharBuffer.wrap("\u3005\u3006\u3007\u30FC\u2015\u2010\uFF0F");
           CharsetEncoder encoder = new CharsetProviderICU().charsetForName("iso-2022-jp").newEncoder();
           ByteBuffer out = ByteBuffer.allocate(30);
           encoder.encode(in, out, true);
           encoder.flush(out);
           if(out.position()!= 20){
               errln("Did not get the expected position from flush");
           }
           
       }catch (Exception ex){
           errln("Could not create encoder for  iso-2022-jp exception: "+ex.toString());
       } 
    }
   */
    public void TestISO88591() {
       
        Charset cs = new CharsetProviderICU().charsetForName("iso-8859-1");
        if(cs!=null){
            CharsetEncoder encoder = cs.newEncoder();
            if(encoder!=null){
                encoder.canEncode("\uc2a3");
            }else{
                errln("Could not create encoder for iso-8859-1");
            }
        }else{
            errln("Could not create Charset for iso-8859-1");
        }
        
    }
    public void TestUTF8Encode() {
        CharsetEncoder encoderICU = new CharsetProviderICU().charsetForName("utf-8").newEncoder();
        ByteBuffer out = ByteBuffer.allocate(30);
        CoderResult result = encoderICU.encode(CharBuffer.wrap("\ud800"), out, true);
        
        if (result.isMalformed()) {
            logln("\\ud800 is malformed for ICU4JNI utf-8 encoder");
        } else if (result.isUnderflow()) {
            errln("\\ud800 is OK for ICU4JNI utf-8 encoder");
        }

        CharsetEncoder encoderJDK = Charset.forName("utf-8").newEncoder();
        result = encoderJDK.encode(CharBuffer.wrap("\ud800"), ByteBuffer
                .allocate(10), true);
        if (result.isUnderflow()) {
            errln("\\ud800 is OK for JDK utf-8 encoder");
        } else if (result.isMalformed()) {
            logln("\\ud800 is malformed for JDK utf-8 encoder");
        }
    }

/*    private void printCB(CharBuffer buf){
        buf.rewind();
        while(buf.hasRemaining()){
            System.out.println(hex(buf.get()));
        }
        buf.rewind();
    }
*/
    public void TestUTF8() throws CharacterCodingException{
           try{
               CharsetEncoder encoderICU = new CharsetProviderICU().charsetForName("utf-8").newEncoder();
               encoderICU.encode(CharBuffer.wrap("\ud800"));
               errln("\\ud800 is OK for ICU4JNI utf-8 encoder");
           }catch (Exception e) {
               logln("\\ud800 is malformed for JDK utf-8 encoder");
              //e.printStackTrace();
           }
           
           CharsetEncoder encoderJDK = Charset.forName("utf-8").newEncoder();
           try {
               encoderJDK.encode(CharBuffer.wrap("\ud800"));
               errln("\\ud800 is OK for JDK utf-8 encoder");
           } catch (Exception e) {
               logln("\\ud800 is malformed for JDK utf-8 encoder");
               //e.printStackTrace();
           }         
    }
    
    public void TestUTF16Bom(){

        Charset cs = (new CharsetProviderICU()).charsetForName("UTF-16");
        char[] in = new char[] { 0x1122, 0x2211, 0x3344, 0x4433,
                                0x5566, 0x6655, 0x7788, 0x8877, 0x9900 };
        CharBuffer inBuf = CharBuffer.allocate(in.length);
        inBuf.put(in);
        CharsetEncoder encoder = cs.newEncoder();
        ByteBuffer outBuf = ByteBuffer.allocate(in.length*2+2);
        inBuf.rewind();
        encoder.encode(inBuf, outBuf, true);
        outBuf.rewind();
        if(outBuf.get(0)!= (byte)0xFE && outBuf.get(1)!= (byte)0xFF){
            errln("The UTF16 encoder did not appended bom. Length returned: " + outBuf.remaining());
        }
        while(outBuf.hasRemaining()){
            logln("0x"+hex(outBuf.get()));
        }
        CharsetDecoder decoder = cs.newDecoder();
        outBuf.rewind();
        CharBuffer rt = CharBuffer.allocate(in.length);
        CoderResult cr = decoder.decode(outBuf, rt, true);
        if(cr.isError()){
            errln("Decoding with BOM failed. Error: "+ cr.toString());
        }
        equals(rt, in);
        {
            rt.clear();
            outBuf.rewind();
            Charset utf16 = Charset.forName("UTF-16");
            CharsetDecoder dc = utf16.newDecoder();
            cr = dc.decode(outBuf, rt, true);
            equals(rt, in);
        }
    }
     
    private void smBufDecode(CharsetDecoder decoder, String encoding, ByteBuffer source, CharBuffer target,
            boolean throwException, boolean flush) throws BufferOverflowException, Exception {
        smBufDecode(decoder, encoding, source, target, throwException, flush, true);
    }

    private void smBufDecode(CharsetDecoder decoder, String encoding, ByteBuffer source, CharBuffer target,
            boolean throwException, boolean flush, boolean backedByArray) throws BufferOverflowException, Exception {
        smBufDecode(decoder, encoding, source, target, throwException, flush, backedByArray, -1);
    }

    private void smBufDecode(CharsetDecoder decoder, String encoding, ByteBuffer source, CharBuffer target,
            boolean throwException, boolean flush, boolean backedByArray, int targetLimit)
            throws BufferOverflowException, Exception {
        ByteBuffer mySource;
        CharBuffer myTarget;
        if (backedByArray) {
            mySource = ByteBuffer.allocate(source.capacity());
            myTarget = CharBuffer.allocate(target.capacity());
        } else {
            // this does not guarantee by any means that mySource and myTarget
            // are not backed by arrays
            mySource = ByteBuffer.allocateDirect(source.capacity());
            myTarget = ByteBuffer.allocateDirect(target.capacity() * 2).asCharBuffer();
        }
        mySource.position(source.position());
        for (int i = source.position(); i < source.limit(); i++)
            mySource.put(i, source.get(i));

        {
            decoder.reset();
            myTarget.limit(target.limit());
            mySource.limit(source.limit());
            mySource.position(source.position());
            CoderResult result = CoderResult.UNDERFLOW;
            result = decoder.decode(mySource, myTarget, true);
            if (flush) {
                result = decoder.flush(myTarget);
            }
            if (result.isError()) {
                if (throwException) {
                    throw new Exception();
                }
                errln("Test complete buffers while decoding failed. " + result.toString());
                return;
            }
            if (result.isOverflow()) {
                if (throwException) {
                    throw new BufferOverflowException();
                }
                errln("Test complete buffers while decoding threw overflow exception");
                return;
            }
            myTarget.limit(myTarget.position());
            myTarget.position(0);
            target.position(0);
            if (result.isUnderflow() && !equals(myTarget, target, targetLimit)) {
                errln(" Test complete buffers while decoding  " + encoding + " TO Unicode--failed");
            }
        }
        if (isQuick()) {
            return;
        }
        {
            decoder.reset();
            myTarget.limit(target.position());
            mySource.limit(source.position());
            mySource.position(source.position());
            myTarget.clear();
            myTarget.position(0);

            int inputLen = mySource.remaining();

            CoderResult result = CoderResult.UNDERFLOW;
            for (int i = 1; i <= inputLen; i++) {
                mySource.limit(i);
                if (i == inputLen) {
                    result = decoder.decode(mySource, myTarget, true);
                } else {
                    result = decoder.decode(mySource, myTarget, false);
                }
                if (result.isError()) {
                    errln("Test small input buffers while decoding failed. " + result.toString());
                    break;
                }
                if (result.isOverflow()) {
                    if (throwException) {
                        throw new BufferOverflowException();
                    }
                    errln("Test small input buffers while decoding threw overflow exception");
                    break;
                }

            }
            if (result.isUnderflow() && !equals(myTarget, target, targetLimit)) {
                errln("Test small input buffers while decoding " + encoding + " TO Unicode--failed");
            }
        }
        {
            decoder.reset();
            myTarget.limit(0);
            mySource.limit(0);
            mySource.position(source.position());
            myTarget.clear();
            while (true) {
                CoderResult result = decoder.decode(mySource, myTarget, false);
                if (result.isUnderflow()) {
                    if (mySource.limit() < source.limit())
                        mySource.limit(mySource.limit() + 1);
                } else if (result.isOverflow()) {
                    if (myTarget.limit() < target.limit())
                        myTarget.limit(myTarget.limit() + 1);
                    else
                        break;
                } else /*if (result.isError())*/ {
                    errln("Test small output buffers while decoding " + result.toString());
                }
                if (mySource.position() == mySource.limit()) {
                    result = decoder.decode(mySource, myTarget, true);
                    if (result.isError()) {
                        errln("Test small output buffers while decoding " + result.toString());
                    }
                    result = decoder.flush(myTarget);
                    if (result.isError()) {
                        errln("Test small output buffers while decoding " + result.toString());
                    }
                    break;
                }
            }

            if (!equals(myTarget, target, targetLimit)) {
                errln("Test small output buffers " + encoding + " TO Unicode failed");
            }
        }
    }

    private void smBufEncode(CharsetEncoder encoder, String encoding, CharBuffer source, ByteBuffer target,
            boolean throwException, boolean flush) throws Exception, BufferOverflowException {
        smBufEncode(encoder, encoding, source, target, throwException, flush, true);
    }

    private void smBufEncode(CharsetEncoder encoder, String encoding, CharBuffer source, ByteBuffer target,
            boolean throwException, boolean flush, boolean backedByArray) throws Exception, BufferOverflowException {
        smBufEncode(encoder, encoding, source, target, throwException, flush, true, -1);
    }

    private void smBufEncode(CharsetEncoder encoder, String encoding, CharBuffer source, ByteBuffer target,
            boolean throwException, boolean flush, boolean backedByArray, int targetLimit) throws Exception,
            BufferOverflowException {
        logln("Running smBufEncode for " + encoding + " with class " + encoder);

        CharBuffer mySource;
        ByteBuffer myTarget;
        if (backedByArray) {
            mySource = CharBuffer.allocate(source.capacity());
            myTarget = ByteBuffer.allocate(target.capacity());
        } else {
            mySource = ByteBuffer.allocateDirect(source.capacity() * 2).asCharBuffer();
            myTarget = ByteBuffer.allocateDirect(target.capacity());
        }
        mySource.position(source.position());
        for (int i = source.position(); i < source.limit(); i++)
            mySource.put(i, source.get(i));

        myTarget.clear();
        {
            logln("Running tests on small input buffers for " + encoding);
            encoder.reset();
            myTarget.limit(target.limit());
            mySource.limit(source.limit());
            mySource.position(source.position());
            CoderResult result = null;

            result = encoder.encode(mySource, myTarget, true);
            if (flush) {
                result = encoder.flush(myTarget);
            }

            if (result.isError()) {
                if (throwException) {
                    throw new Exception();
                }
                errln("Test complete while encoding failed. " + result.toString());
            }
            if (result.isOverflow()) {
                if (throwException) {
                    throw new BufferOverflowException();
                }
                errln("Test complete while encoding threw overflow exception");
            }
            if (!equals(myTarget, target, targetLimit)) {
                errln("Test complete buffers while encoding for " + encoding + " failed");

            } else {
                logln("Tests complete buffers for " + encoding + " passed");
            }
        }
        if (isQuick()) {
            return;
        }
        {
            logln("Running tests on small input buffers for " + encoding);
            encoder.reset();
            myTarget.clear();
            myTarget.limit(target.limit());
            mySource.limit(source.limit());
            mySource.position(source.position());
            int inputLen = mySource.limit();
            CoderResult result = null;
            for (int i = 1; i <= inputLen; i++) {
                mySource.limit(i);
                result = encoder.encode(mySource, myTarget, false);
                if (result.isError()) {
                    errln("Test small input buffers while encoding failed. " + result.toString());
                }
                if (result.isOverflow()) {
                    if (throwException) {
                        throw new BufferOverflowException();
                    }
                    errln("Test small input buffers while encoding threw overflow exception");
                }
            }
            if (!equals(myTarget, target, targetLimit)) {
                errln("Test small input buffers " + encoding + " From Unicode failed");
            } else {
                logln("Tests on small input buffers for " + encoding + " passed");
            }
        }
        {
            logln("Running tests on small output buffers for " + encoding);
            encoder.reset();
            myTarget.clear();
            myTarget.limit(target.limit());
            mySource.limit(source.limit());
            mySource.position(source.position());
            mySource.position(0);
            myTarget.position(0);

            logln("myTarget.limit: " + myTarget.limit() + " myTarget.capcity: " + myTarget.capacity());

            while (true) {
                int pos = myTarget.position();

                CoderResult result = encoder.encode(mySource, myTarget, false);
                logln("myTarget.Position: " + pos + " myTarget.limit: " + myTarget.limit());
                logln("mySource.position: " + mySource.position() + " mySource.limit: " + mySource.limit());

                if (result.isError()) {
                    errln("Test small output buffers while encoding " + result.toString());
                }
                if (mySource.position() == mySource.limit()) {
                    result = encoder.encode(mySource, myTarget, true);
                    if (result.isError()) {
                        errln("Test small output buffers while encoding " + result.toString());
                    }

                    myTarget.limit(myTarget.capacity());
                    result = encoder.flush(myTarget);
                    if (result.isError()) {
                        errln("Test small output buffers while encoding " + result.toString());
                    }
                    break;
                }
            }
            if (!equals(myTarget, target, targetLimit)) {
                errln("Test small output buffers " + encoding + " From Unicode failed.");
            }
            logln("Tests on small output buffers for " + encoding + " passed");
        }
    }

    public void convertAllTest(ByteBuffer bSource, CharBuffer uSource) throws Exception {
        {
            try {
                m_decoder.reset();
                ByteBuffer mySource = bSource.duplicate();
                CharBuffer myTarget = m_decoder.decode(mySource);
                if (!equals(myTarget, uSource)) {
                    errln(
                        "--Test convertAll() "
                            + m_encoding
                            + " to Unicode  --FAILED");
                }
            } catch (Exception e) {
                //e.printStackTrace();
                errln(e.getMessage());
            }
        }
        {
            try {
                m_encoder.reset();
                CharBuffer mySource = CharBuffer.wrap(uSource);
                ByteBuffer myTarget = m_encoder.encode(mySource);
                if (!equals(myTarget, bSource)) {
                    errln(
                        "--Test convertAll() "
                            + m_encoding
                            + " to Unicode  --FAILED");
                }
            } catch (Exception e) {
                //e.printStackTrace();
                errln("encoder.encode() failed "+ e.getMessage()+" "+e.toString());
            }
        }

    }
    //TODO
    /*
    public void TestString(ByteBuffer bSource, CharBuffer uSource) throws Exception {
        try {
            {
                String source = uSource.toString();
                byte[] target = source.getBytes(m_encoding);
                if (!equals(target, bSource.array())) {
                    errln("encode using string API failed");
                }
            }
            {

                String target = new String(bSource.array(), m_encoding);
                if (!equals(uSource, target.toCharArray())) {
                    errln("decode using string API failed");
                }
            }
        } catch (Exception e) {
            //e.printStackTrace();
            errln(e.getMessage());
        }
    }

    /*private void fromUnicodeTest() throws Exception {
        
        logln("Loaded Charset: " + charset.getClass().toString());
        logln("Loaded CharsetEncoder: " + encoder.getClass().toString());
        logln("Loaded CharsetDecoder: " + decoder.getClass().toString());
        
        ByteBuffer myTarget = ByteBuffer.allocate(gbSource.length);
        logln("Created ByteBuffer of length: " + uSource.length);
        CharBuffer mySource = CharBuffer.wrap(uSource);
        logln("Wrapped ByteBuffer with CharBuffer  ");
        encoder.reset();
        logln("Test Unicode to " + encoding );
        encoder.encode(mySource, myTarget, true);
        if (!equals(myTarget, gbSource)) {
            errln("--Test Unicode to " + encoding + ": FAILED");
        } 
        logln("Test Unicode to " + encoding +" passed");
    }

    public void TestToUnicode( ) throws Exception {
        
        logln("Loaded Charset: " + charset.getClass().toString());
        logln("Loaded CharsetEncoder: " + encoder.getClass().toString());
        logln("Loaded CharsetDecoder: " + decoder.getClass().toString());
        
        CharBuffer myTarget = CharBuffer.allocate(uSource.length);
        ByteBuffer mySource = ByteBuffer.wrap(getByteArray(gbSource));
        decoder.reset();
        CoderResult result = decoder.decode(mySource, myTarget, true);
        if (result.isError()) {
            errln("Test ToUnicode -- FAILED");
        }
        if (!equals(myTarget, uSource)) {
            errln("--Test " + encoding + " to Unicode :FAILED");
        }
    }

    public static byte[] getByteArray(char[] source) {
        byte[] target = new byte[source.length];
        int i = source.length;
        for (; --i >= 0;) {
            target[i] = (byte) source[i];
        }
        return target;
    }
    /*
    private void smBufCharset(Charset charset) {
        try {
            ByteBuffer bTarget = charset.encode(CharBuffer.wrap(uSource));
            CharBuffer uTarget =
                charset.decode(ByteBuffer.wrap(getByteArray(gbSource)));

            if (!equals(uTarget, uSource)) {
                errln("Test " + charset.toString() + " to Unicode :FAILED");
            }
            if (!equals(bTarget, gbSource)) {
                errln("Test " + charset.toString() + " from Unicode :FAILED");
            }
        } catch (Exception ex) {
            errln("Encountered exception in smBufCharset");
        }
    }
    
    public void TestMultithreaded() throws Exception {
        final Charset cs = Charset.forName(encoding);
        if (cs == charset) {
            errln("The objects are equal");
        }
        smBufCharset(cs);
        try {
            final Thread t1 = new Thread() {
                public void run() {
                    // commented out since the mehtods on
                    // Charset API are supposed to be thread
                    // safe ... to test it we dont sync
            
                    // synchronized(charset){
                   while (!interrupted()) {
                        try {
                            smBufCharset(cs);
                        } catch (UnsupportedCharsetException ueEx) {
                            errln(ueEx.toString());
                        }
                    }

                    // }
                }
            };
            final Thread t2 = new Thread() {
                public void run() {
                        // synchronized(charset){
                    while (!interrupted()) {
                        try {
                            smBufCharset(cs);
                        } catch (UnsupportedCharsetException ueEx) {
                            errln(ueEx.toString());
                        }
                    }

                    //}
                }
            };
            t1.start();
            t2.start();
            int i = 0;
            for (;;) {
                if (i > 1000000000) {
                    try {
                        t1.interrupt();
                    } catch (Exception e) {
                    }
                    try {
                        t2.interrupt();
                    } catch (Exception e) {
                    }
                    break;
                }
                i++;
            }
        } catch (Exception e) {
            throw e;
        }
    }

    public void TestSynchronizedMultithreaded() throws Exception {
        // Methods on CharsetDecoder and CharsetEncoder classes
        // are inherently unsafe if accessed by multiple concurrent
        // thread so we synchronize them
        final Charset charset = Charset.forName(encoding);
        final CharsetDecoder decoder = charset.newDecoder();
        final CharsetEncoder encoder = charset.newEncoder();
        try {
            final Thread t1 = new Thread() {
                public void run() {
                    while (!interrupted()) {
                        try {
                            synchronized (encoder) {
                                smBufEncode(encoder, encoding);
                            }
                            synchronized (decoder) {
                                smBufDecode(decoder, encoding);
                            }
                        } catch (UnsupportedCharsetException ueEx) {
                            errln(ueEx.toString());
                        }
                    }

                }
            };
            final Thread t2 = new Thread() {
                public void run() {
                    while (!interrupted()) {
                        try {
                            synchronized (encoder) {
                                smBufEncode(encoder, encoding);
                            }
                            synchronized (decoder) {
                                smBufDecode(decoder, encoding);
                            }
                        } catch (UnsupportedCharsetException ueEx) {
                            errln(ueEx.toString());
                        }
                    }
                }
            };
            t1.start();
            t2.start();
            int i = 0;
            for (;;) {
                if (i > 1000000000) {
                    try {
                        t1.interrupt();
                    } catch (Exception e) {
                    }
                    try {
                        t2.interrupt();
                    } catch (Exception e) {
                    }
                    break;
                }
                i++;
            }
        } catch (Exception e) {
            throw e;
        }
    }
    */
    
    public void TestMBCS(){      
        {
            // Encoder: from Unicode conversion
            CharsetEncoder encoderICU = new CharsetProviderICU().charsetForName("ibm-971").newEncoder();
            ByteBuffer out = ByteBuffer.allocate(6);
            encoderICU.onUnmappableCharacter(CodingErrorAction.REPLACE);
            CoderResult result = encoderICU.encode(CharBuffer.wrap("\u0131\u0061\u00a1"), out, true);
            if(!result.isError()){
                byte[] expected = {(byte)0xA9, (byte)0xA5, (byte)0xAF, (byte)0xFE, (byte)0xA2, (byte)0xAE};
                if(!equals(expected, out.array())){
                    errln("Did not get the expected result for substitution bytes. Got: "+
                           hex(out.array()));
                }
                logln("Output: "+  hex(out.array()));
            }else{
                errln("Encode operation failed for encoder: "+encoderICU.toString());
            }
        }
        {
            // Decoder: to Unicode conversion
            CharsetDecoder decoderICU = new CharsetProviderICU().charsetForName("ibm-971").newDecoder();
            CharBuffer out = CharBuffer.allocate(3);
            decoderICU.onMalformedInput(CodingErrorAction.REPLACE);
            CoderResult result = decoderICU.decode(ByteBuffer.wrap(new byte[] { (byte)0xA2, (byte)0xAE, (byte)0x12, (byte)0x34, (byte)0xEF, (byte)0xDC }), out, true);
            if(!result.isError()){
                char[] expected = {'\u00a1', '\ufffd', '\u6676'};
                if(!equals(expected, out.array())){
                    errln("Did not get the expected result for substitution chars. Got: "+
                           hex(out.array()));
                }
                logln("Output: "+  hex(out.array()));
            }else{
                errln("Decode operation failed for encoder: "+decoderICU.toString());
            }
        }
    }
    
    public void TestJB4897(){
        CharsetProviderICU provider = new CharsetProviderICU();
        Charset charset = provider.charsetForName("x-abracadabra");  
        if(charset!=null && charset.canEncode()== true){
            errln("provider.charsetForName() does not validate the charset names" );
        }
    }

    public void TestJB5027() {
        CharsetProviderICU provider= new CharsetProviderICU();

        Charset fake = provider.charsetForName("doesNotExist");
        if(fake != null){
            errln("\"doesNotExist\" returned " + fake);
        }
        Charset xfake = provider.charsetForName("x-doesNotExist");
        if(xfake!=null){
            errln("\"x-doesNotExist\" returned " + xfake);
        }
    }
    //test to make sure that number of aliases and canonical names are in the charsets that are in
    public void TestAllNames() {
        
        CharsetProviderICU provider= new CharsetProviderICU();
        Object[] available = CharsetProviderICU.getAvailableNames();
        for(int i=0; i<available.length;i++){
            try{
                String canon  = CharsetProviderICU.getICUCanonicalName((String)available[i]);

                // ',' is not allowed by Java's charset name checker
                if(canon.indexOf(',')>=0){
                    continue;
                }
                Charset cs = provider.charsetForName((String)available[i]);
              
                Object[] javaAliases =  cs.aliases().toArray();
                //seach for ICU canonical name in javaAliases
                boolean inAliasList = false;
                for(int j=0; j<javaAliases.length; j++){
                    String java = (String) javaAliases[j];
                    if(java.equals(canon)){
                        logln("javaAlias: " + java + " canon: " + canon);
                        inAliasList = true;
                    }
                }
                if(inAliasList == false){
                    errln("Could not find ICU canonical name: "+canon+ " for java canonical name: "+ available[i]+ " "+ i);
                }
            }catch(UnsupportedCharsetException ex){
                errln("could no load charset "+ available[i]+" "+ex.getMessage());
                continue;
            }
        }
    }
    public void TestDecoderImplFlush() {
        CharsetProviderICU provider = new CharsetProviderICU();
        Charset ics = provider.charsetForName("UTF-16");
        Charset jcs = Charset.forName("UTF-16"); // Java's UTF-16 charset
        execDecoder(jcs);
        execDecoder(ics);
    }
    public void TestEncoderImplFlush() {
        CharsetProviderICU provider = new CharsetProviderICU();
        Charset ics = provider.charsetForName("UTF-16");
        Charset jcs = Charset.forName("UTF-16"); // Java's UTF-16 charset
        execEncoder(jcs);
        execEncoder(ics);
    }
    private void execDecoder(Charset cs){
        CharsetDecoder decoder = cs.newDecoder();
        decoder.onMalformedInput(CodingErrorAction.REPORT);
        decoder.onUnmappableCharacter(CodingErrorAction.REPORT);
        CharBuffer out = CharBuffer.allocate(10);
        CoderResult result = decoder.decode(ByteBuffer.wrap(new byte[] { -1,
                -2, 32, 0, 98 }), out, false);
        result = decoder.decode(ByteBuffer.wrap(new byte[] { 98 }), out, true);

        logln(cs.getClass().toString()+ ":" +result.toString());
        try {
            result = decoder.flush(out);
            logln(cs.getClass().toString()+ ":" +result.toString());
        } catch (Exception e) {
            errln(e.getMessage()+" "+cs.getClass().toString());
        }
    }
    private void execEncoder(Charset cs){
        CharsetEncoder encoder = cs.newEncoder();
        encoder.onMalformedInput(CodingErrorAction.REPORT);
        encoder.onUnmappableCharacter(CodingErrorAction.REPORT);
        ByteBuffer out = ByteBuffer.allocate(10);
        CoderResult result = encoder.encode(CharBuffer.wrap(new char[] { '\uFFFF',
                '\u2345', 32, 98 }), out, false);
        logln(cs.getClass().toString()+ ":" +result.toString());
        result = encoder.encode(CharBuffer.wrap(new char[] { 98 }), out, true);

        logln(cs.getClass().toString()+ ":" +result.toString());
        try {
            result = encoder.flush(out);
            logln(cs.getClass().toString()+ ":" +result.toString());
        } catch (Exception e) {
            errln(e.getMessage()+" "+cs.getClass().toString());
        }
    }
    public void TestDecodeMalformed() {
        CharsetProviderICU provider = new CharsetProviderICU();
        Charset ics = provider.charsetForName("UTF-16BE");
        //Use SUN's charset
        Charset jcs = Charset.forName("UTF-16");
        CoderResult ir = execMalformed(ics);
        CoderResult jr = execMalformed(jcs);
        if(ir!=jr){
            errln("ICU's decoder did not return the same result as Sun. ICU: "+ir.toString()+" Sun: "+jr.toString());
        }
    }
    private CoderResult execMalformed(Charset cs){
        CharsetDecoder decoder = cs.newDecoder();
        decoder.onMalformedInput(CodingErrorAction.IGNORE);
        decoder.onUnmappableCharacter(CodingErrorAction.REPORT);
        ByteBuffer in = ByteBuffer.wrap(new byte[] { 0x00, 0x41, 0x00, 0x42, 0x01 });
        CharBuffer out = CharBuffer.allocate(3);
        return decoder.decode(in, out, true);
    }
    
    public void TestJavaUTF16Decoder(){
        CharsetProviderICU provider = new CharsetProviderICU();
        Charset ics = provider.charsetForName("UTF-16BE");
        //Use SUN's charset
        Charset jcs = Charset.forName("UTF-16");
        Exception ie = execConvertAll(ics);
        Exception je = execConvertAll(jcs);
        if(ie!=je){
            errln("ICU's decoder did not return the same result as Sun. ICU: "+ie.toString()+" Sun: "+je.toString());
        }
    }
    private Exception execConvertAll(Charset cs){
        ByteBuffer in = ByteBuffer.allocate(400);
        int i=0;
        while(in.position()!=in.capacity()){
            in.put((byte)0xD8);
            in.put((byte)i);
            in.put((byte)0xDC);
            in.put((byte)i);
            i++;
        }
        in.limit(in.position());
        in.position(0);
        CharsetDecoder decoder = cs.newDecoder();
        decoder.onMalformedInput(CodingErrorAction.IGNORE);
        decoder.onUnmappableCharacter(CodingErrorAction.REPORT);
        try{
            CharBuffer out = decoder.decode(in);
            if(out!=null){
                logln(cs.toString()+" encoing succeeded as expected!");
            }
        }catch ( Exception ex){
            errln("Did not get expected exception for encoding: "+cs.toString());
            return ex;
        }
        return null;
    }
    public void TestUTF32BOM(){

        Charset cs = (new CharsetProviderICU()).charsetForName("UTF-32");
        char[] in = new char[] { 0xd800, 0xdc00, 
                                 0xd801, 0xdc01,
                                 0xdbff, 0xdfff, 
                                 0xd900, 0xdd00, 
                                 0x0000, 0x0041,
                                 0x0000, 0x0042,
                                 0x0000, 0x0043};
        
        CharBuffer inBuf = CharBuffer.allocate(in.length);
        inBuf.put(in);
        CharsetEncoder encoder = cs.newEncoder();
        ByteBuffer outBuf = ByteBuffer.allocate(in.length*4+4);
        inBuf.rewind();
        encoder.encode(inBuf, outBuf, true);
        outBuf.rewind();
        if(outBuf.get(0)!= (byte)0x00 && outBuf.get(1)!= (byte)0x00 && 
                outBuf.get(2)!= (byte)0xFF && outBuf.get(3)!= (byte)0xFE){
            errln("The UTF32 encoder did not appended bom. Length returned: " + outBuf.remaining());
        }
        while(outBuf.hasRemaining()){
            logln("0x"+hex(outBuf.get()));
        }
        CharsetDecoder decoder = cs.newDecoder();
        outBuf.limit(outBuf.position());
        outBuf.rewind();
        CharBuffer rt = CharBuffer.allocate(in.length);
        CoderResult cr = decoder.decode(outBuf, rt, true);
        if(cr.isError()){
            errln("Decoding with BOM failed. Error: "+ cr.toString());
        }
        equals(rt, in);
        try{
            rt.clear();
            outBuf.rewind();
            Charset utf16 = Charset.forName("UTF-32");
            CharsetDecoder dc = utf16.newDecoder();
            cr = dc.decode(outBuf, rt, true);
            equals(rt, in);
        }catch(UnsupportedCharsetException ex){
            // swallow the expection.
        }
    }
    
    /*
     *  Michael Ow
     *  Modified 070424
     */
    /*The following two methods provides the option of exceptions when Decoding 
     * and Encoding if needed for testing purposes.
     */
    private void smBufDecode(CharsetDecoder decoder, String encoding, ByteBuffer source, CharBuffer target) {
        smBufDecode(decoder, encoding, source, target, true);
    }
    private void smBufDecode(CharsetDecoder decoder, String encoding, ByteBuffer source, CharBuffer target, boolean backedByArray) {
        try {
            smBufDecode(decoder, encoding, source, target, false, false, backedByArray);
        }    
        catch (Exception ex) {           
            System.out.println("!exception!");
        }
    }
    private void smBufEncode(CharsetEncoder encoder, String encoding, CharBuffer source, ByteBuffer target)  {
        smBufEncode(encoder, encoding, source, target, true);
    }
    private void smBufEncode(CharsetEncoder encoder, String encoding, CharBuffer source, ByteBuffer target, boolean backedByArray)  {
        try {
            smBufEncode(encoder, encoding, source, target, false, false); 
        }
        catch (Exception ex) {
            System.out.println("!exception!");
        }
    }
    //Test CharsetICUProvider
    public void TestNullCanonicalName() {
        String enc = null;
        String canonicalName = CharsetProviderICU.getICUCanonicalName(enc);
        
        if (canonicalName != null) {
            errln("getICUCanonicalName return a non-null string for given null string");
        }
    }
    public void TestGetAllNames() {
        String[] names = null;
        
        names = CharsetProviderICU.getAllNames();
        
        if (names == null) {
            errln("getAllNames returned a null string.");
        }
    }
    //Test CharsetICU
    public void TestCharsetContains() {
        boolean test;
        
        CharsetProvider provider = new CharsetProviderICU();     
        Charset cs1 = provider.charsetForName("UTF-32");
        Charset cs2 = null;
        
        test = cs1.contains(cs2);
        
        if (test != false) {
            errln("Charset.contains returned true for a null charset.");
        }
        
        cs2 = CharsetICU.forNameICU("UTF-32");
        
        test = cs1.contains(cs2);
        
        if (test != true) {
            errln("Charset.contains returned false for an identical charset.");
        }
        
        cs2 = provider.charsetForName("UTF-8");
        
        test = cs1.contains(cs2);
        
        if (test != false) {
            errln("Charset.contains returned true for a different charset.");
        }
    }
    public void TestCharsetICUNullCharsetName() {
        String charsetName = null;
        
        try {
            CharsetICU.forNameICU(charsetName);
            errln("CharsetICU.forName should have thown an exception after getting a null charsetName.");
        }
        catch(Exception ex) {          
        }
    }
    
    //Test CharsetASCII
    public void TestCharsetASCIIOverFlow() {
        int byteBufferLimit;
        int charBufferLimit;
        
        CharsetProvider provider = new CharsetProviderICU();
        Charset cs = provider.charsetForName("ASCII");        
        CharsetEncoder encoder = cs.newEncoder();
        CharsetDecoder decoder = cs.newDecoder();
        
        CharBuffer charBuffer = CharBuffer.allocate(0x90);
        ByteBuffer byteBuffer = ByteBuffer.allocate(0x90);
        
        CharBuffer charBufferTest = CharBuffer.allocate(0xb0);
        ByteBuffer byteBufferTest = ByteBuffer.allocate(0xb0);
        
        for(int j=0;j<=0x7f; j++){
           charBuffer.put((char)j);
           byteBuffer.put((byte)j);
        }
        
        byteBuffer.limit(byteBufferLimit = byteBuffer.position());
        byteBuffer.position(0);
        charBuffer.limit(charBufferLimit = charBuffer.position());
        charBuffer.position(0);
        
        //test for overflow
        byteBufferTest.limit(byteBufferLimit - 5);
        byteBufferTest.position(0);
        charBufferTest.limit(charBufferLimit - 5);
        charBufferTest.position(0);
        try {
            smBufDecode(decoder, "ASCII", byteBuffer, charBufferTest, true, false);
            errln("Overflow exception while decoding ASCII should have been thrown.");
        }
        catch(Exception ex) {
        }
        try {
            smBufEncode(encoder, "ASCII", charBuffer, byteBufferTest, true, false);
            errln("Overflow exception while encoding ASCII should have been thrown.");
        }
        catch (Exception ex) {
        }
        
        // For better code coverage
        /* For better code coverage */
        byte byteout[] = {
                (byte)0x01
        };
        char charin[] = {
                (char)0x0001, (char)0x0002
        };
        ByteBuffer bb = ByteBuffer.wrap(byteout);
        CharBuffer cb = CharBuffer.wrap(charin);
        CharBuffer cb2 = CharBuffer.wrap(cb.subSequence(0, 2));
        encoder.reset();
        if (!(encoder.encode(cb2, bb, true)).isOverflow()) {
            errln("Overflow error while encoding ASCII should have occurred.");
        }
    }
    //Test CharsetUTF7
    public void TestCharsetUTF7() {
        CoderResult result = CoderResult.UNDERFLOW;
        CharsetProvider provider = new CharsetProviderICU();
        Charset cs = provider.charsetForName("UTF-7");        
        CharsetEncoder encoder = cs.newEncoder();
        CharsetDecoder decoder = cs.newDecoder();
        
        CharBuffer us = CharBuffer.allocate(0x100);
        ByteBuffer bs = ByteBuffer.allocate(0x100);
        
        /* Unicode :  A<not equal to Alpha Lamda>. */
        /* UTF7: AImIDkQ. */
        us.put((char)0x41); us.put((char)0x2262); us.put((char)0x391); us.put((char)0x39B); us.put((char)0x2e);
        bs.put((byte)0x41); bs.put((byte)0x2b); bs.put((byte)0x49); bs.put((byte)0x6d); 
        bs.put((byte)0x49); bs.put((byte)0x44); bs.put((byte)0x6b); bs.put((byte)0x51); 
        bs.put((byte)0x4f); bs.put((byte)0x62); bs.put((byte)0x2e);
        
        bs.limit(bs.position());
        bs.position(0);
        us.limit(us.position());
        us.position(0);

        smBufDecode(decoder, "UTF-7", bs, us);
        smBufEncode(encoder, "UTF-7", us, bs);
        
        /* ticket 6151 */
        CharBuffer smallus = CharBuffer.allocate(1);
        ByteBuffer bigbs = ByteBuffer.allocate(3);
        bigbs.put((byte)0x41); bigbs.put((byte)0x41); bigbs.put((byte)0x41);
        bigbs.position(0);
        try {
            smBufDecode(decoder, "UTF-7-DE-Overflow", bigbs, smallus, true, false);
            errln("Buffer Overflow exception should have been thrown while decoding UTF-7.");
        } catch (Exception ex) {
        }
        
        //The rest of the code in this method is to provide better code coverage
        CharBuffer ccus = CharBuffer.allocate(0x10);
        ByteBuffer ccbs = ByteBuffer.allocate(0x10);
        
        //start of charset decoder code coverage code
        //test for accurate illegal and control character checking
        ccbs.put((byte)0x0D); ccbs.put((byte)0x05);
        ccus.put((char)0x0000);
        
        ccbs.limit(ccbs.position());
        ccbs.position(0);
        ccus.limit(ccus.position());
        ccus.position(0);

        try {
            smBufDecode(decoder, "UTF-7-CC-DE-1", ccbs, ccus, true, false);
            errln("Exception while decoding UTF-7 code coverage test should have been thrown.");
        }
        catch (Exception ex) {
        }
        
        ccbs.clear();
        ccus.clear();
        
        //test for illegal base64 character
        ccbs.put((byte)0x2b); ccbs.put((byte)0xff);
        ccus.put((char)0x0000);
        
        ccbs.limit(ccbs.position());
        ccbs.position(0);
        ccus.limit(ccus.position());
        ccus.position(0);
        
        try {
            smBufDecode(decoder, "UTF-7-CC-DE-2", ccbs, ccus, true, false);
            errln("Exception while decoding UTF-7 code coverage test should have been thrown.");
        }
        catch (Exception ex) {
        }
        
        ccbs.clear();
        ccus.clear();
        
        //test for illegal order of the base64 character sequence
        ccbs.put((byte)0x2b); ccbs.put((byte)0x2d); ccbs.put((byte)0x2b); ccbs.put((byte)0x49); ccbs.put((byte)0x2d);
        ccus.put((char)0x0000); ccus.put((char)0x0000);
        
        ccbs.limit(ccbs.position());
        ccbs.position(0);
        ccus.limit(ccus.position());
        ccus.position(0);
        
        try {
            smBufDecode(decoder, "UTF-7-CC-DE-3", ccbs, ccus, true, false);
            errln("Exception while decoding UTF-7 code coverage test should have been thrown.");
        }
        catch (Exception ex) {
        }
        
        ccbs.clear();
        ccus.clear();
        
        //test for illegal order of the base64 character sequence 
        ccbs.put((byte)0x2b); ccbs.put((byte)0x0a); ccbs.put((byte)0x09);
        ccus.put((char)0x0000);
        
        ccbs.limit(ccbs.position());
        ccbs.position(0);
        ccus.limit(ccus.position());
        ccus.position(0);
        
        try {
            smBufDecode(decoder, "UTF-7-CC-DE-4", ccbs, ccus, true, false);
            errln("Exception while decoding UTF-7 code coverage test should have been thrown.");
        }
        catch (Exception ex) {
        }
        
        ccbs.clear();
        ccus.clear();
        
        //test for illegal order of the base64 character sequence
        ccbs.put((byte)0x2b); ccbs.put((byte)0x49); ccbs.put((byte)0x0a);
        ccus.put((char)0x0000);
        
        ccbs.limit(ccbs.position());
        ccbs.position(0);
        ccus.limit(ccus.position());
        ccus.position(0);
        
        try {
            smBufDecode(decoder, "UTF-7-CC-DE-5", ccbs, ccus, true, false);
            errln("Exception while decoding UTF-7 code coverage test should have been thrown.");
        }
        catch (Exception ex) {
        }
        
        ccbs.clear();
        ccus.clear();
        
        //test for illegal order of the base64 character sequence
        ccbs.put((byte)0x2b); ccbs.put((byte)0x00);
        ccus.put((char)0x0000);
        
        ccbs.limit(ccbs.position());
        ccbs.position(0);
        ccus.limit(ccus.position());
        ccus.position(0);
        
        try {
            smBufDecode(decoder, "UTF-7-CC-DE-6", ccbs, ccus, true, false);
            errln("Exception while decoding UTF-7 code coverage test should have been thrown.");
        }
        catch (Exception ex) {
        }
        
        ccbs.clear();
        ccus.clear();
        
        //test for overflow buffer error
        ccbs.put((byte)0x2b); ccbs.put((byte)0x49);
        
        ccbs.limit(ccbs.position());
        ccbs.position(0);
        ccus.limit(0);
        ccus.position(0);
        
        try {
            smBufDecode(decoder, "UTF-7-CC-DE-7", ccbs, ccus, true, false);
            errln("Exception while decoding UTF-7 code coverage test should have been thrown.");
        }
        catch (Exception ex) {
        }
        
        ccbs.clear();
        ccus.clear();
        
        //test for overflow buffer error
        ccbs.put((byte)0x0c); ccbs.put((byte)0x0c);
        
        ccbs.limit(ccbs.position());
        ccbs.position(0);
        ccus.limit(0);
        ccus.position(0);
        
        try {
            smBufDecode(decoder, "UTF-7-CC-DE-8", ccbs, ccus, true, false);
            errln("Exception while decoding UTF-7 code coverage test should have been thrown.");
        }
        catch (Exception ex) {
        }
        //end of charset decoder code coverage code
        
        //start of charset encoder code coverage code
        ccbs.clear();
        ccus.clear();
        //test for overflow buffer error
        ccus.put((char)0x002b);
        ccbs.put((byte)0x2b); 
        
        ccbs.limit(ccbs.position());
        ccbs.position(0);
        ccus.limit(ccus.position());
        ccus.position(0);
        
        try {
            smBufEncode(encoder, "UTF-7-CC-EN-1", ccus, ccbs, true, false);
            errln("Exception while encoding UTF-7 code coverage test should have been thrown.");
        }
        catch (Exception ex) {
        }
        
        ccbs.clear();
        ccus.clear();
        
        //test for overflow buffer error
        ccus.put((char)0x002b); ccus.put((char)0x2262);
        ccbs.put((byte)0x2b); ccbs.put((byte)0x2d); ccbs.put((byte)0x00); ccbs.put((byte)0x00);
        
        ccbs.limit(ccbs.position());
        ccbs.position(0);
        ccus.limit(ccus.position());
        ccus.position(0);
        
        try {
            smBufEncode(encoder, "UTF-7-CC-EN-2", ccus, ccbs, true, false);
            errln("Exception while encoding UTF-7 code coverage test should have been thrown.");
        }
        catch (Exception ex) {
        } 
        
        ccbs.clear();
        ccus.clear();
        
        //test for overflow buffer error
        ccus.put((char)0x2262); ccus.put((char)0x0049);
        ccbs.put((byte)0x00); ccbs.put((byte)0x00); ccbs.put((byte)0x00); ccbs.put((byte)0x00); ccbs.put((byte)0x00);
        ccbs.limit(ccbs.position());
        ccbs.position(0);
        ccus.limit(ccus.position());
        ccus.position(0);
        
        try {
            smBufEncode(encoder, "UTF-7-CC-EN-3", ccus, ccbs, true, false);
            errln("Exception while encoding UTF-7 code coverage test should have been thrown.");
        }
        catch (Exception ex) {
        }  
        
        ccbs.clear();
        ccus.clear();
        
        //test for overflow buffer error
        ccus.put((char)0x2262); ccus.put((char)0x0395);
        ccbs.put((byte)0x00); ccbs.put((byte)0x00); ccbs.put((byte)0x00); ccbs.put((byte)0x00);
        ccbs.limit(ccbs.position());
        ccbs.position(0);
        ccus.limit(ccus.position());
        ccus.position(0);
        
        try {
            smBufEncode(encoder, "UTF-7-CC-EN-4", ccus, ccbs, true, false);
            errln("Exception while encoding UTF-7 code coverage test should have been thrown.");
        }
        catch (Exception ex) {
        }  
        
        ccbs.clear();
        ccus.clear();
        
        //test for overflow buffer error
        ccus.put((char)0x2262); ccus.put((char)0x0395);
        ccbs.put((byte)0x00); ccbs.put((byte)0x00); ccbs.put((byte)0x00); ccbs.put((byte)0x00); ccbs.put((byte)0x00);
        ccbs.limit(ccbs.position());
        ccbs.position(0);
        ccus.limit(ccus.position());
        ccus.position(0);
        
        try {
            smBufEncode(encoder, "UTF-7-CC-EN-5", ccus, ccbs, true, false);
            errln("Exception while encoding UTF-7 code coverage test should have been thrown.");
        }
        catch (Exception ex) {
        }  
        
        ccbs.clear();
        ccus.clear();
        
        //test for overflow buffer error
        ccus.put((char)0x2262); ccus.put((char)0x0395); ccus.put((char)0x0391);
        ccbs.put((byte)0x00); ccbs.put((byte)0x00); ccbs.put((byte)0x00); ccbs.put((byte)0x00); ccbs.put((byte)0x00); ccbs.put((byte)0x00); ccbs.put((byte)0x00);
        ccbs.limit(ccbs.position());
        ccbs.position(0);
        ccus.limit(ccus.position());
        ccus.position(0);
        
        try {
            smBufEncode(encoder, "UTF-7-CC-EN-6", ccus, ccbs, true, false);
            errln("Exception while encoding UTF-7 code coverage test should have been thrown.");
        }
        catch (Exception ex) {
        }  
        
        ccbs.clear();
        ccus.clear();
        
        //test for overflow buffer error
        ccus.put((char)0x2262); ccus.put((char)0x0395); ccus.put((char)0x0391);
        ccbs.put((byte)0x00); ccbs.put((byte)0x00); ccbs.put((byte)0x00); ccbs.put((byte)0x00); 
        ccbs.put((byte)0x00); ccbs.put((byte)0x00); ccbs.put((byte)0x00); ccbs.put((byte)0x00);
        ccbs.limit(ccbs.position());
        ccbs.position(0);
        ccus.limit(ccus.position());
        ccus.position(0);
        
        try {
            smBufEncode(encoder, "UTF-7-CC-EN-7", ccus, ccbs, true, false);
            errln("Exception while encoding UTF-7 code coverage test should have been thrown.");
        }
        catch (Exception ex) {
        }  
        
        ccbs.clear();
        ccus.clear();
        
        //test for overflow buffer error
        ccus.put((char)0x0049); ccus.put((char)0x0048);
        ccbs.put((byte)0x00); 
        ccbs.limit(ccbs.position());
        ccbs.position(0);
        ccus.limit(ccus.position());
        ccus.position(0);
        
        try {
            smBufEncode(encoder, "UTF-7-CC-EN-8", ccus, ccbs, true, false);
            errln("Exception while encoding UTF-7 code coverage test should have been thrown.");
        }
        catch (Exception ex) {
        } 
        
        ccbs.clear();
        ccus.clear();
        
        //test for overflow buffer error
        ccus.put((char)0x2262);
        ccbs.put((byte)0x00);
        ccbs.limit(ccbs.position());
        ccbs.position(0);
        ccus.limit(ccus.position());
        ccus.position(0);
        
        try {
            smBufEncode(encoder, "UTF-7-CC-EN-9", ccus, ccbs, true, false);
            errln("Exception while encoding UTF-7 code coverage test should have been thrown.");
        }
        catch (Exception ex) {
        } 
        
        ccbs.clear();
        ccus.clear();
        
        //test for overflow buffer error
        ccus.put((char)0x2262); ccus.put((char)0x0049);
        ccbs.put((byte)0x00); ccbs.put((byte)0x00); ccbs.put((byte)0x00); ccbs.put((byte)0x00);
        ccbs.limit(ccbs.position());
        ccbs.position(0);
        ccus.limit(ccus.position());
        ccus.position(0);
        
        try {
            smBufEncode(encoder, "UTF-7-CC-EN-10", ccus, ccbs, true, false);
            errln("Exception while encoding UTF-7 code coverage test should have been thrown.");
        }
        catch (Exception ex) {
        }  
        
        ccbs.clear();
        ccus.clear();
        
        //test for overflow buffer error
        ccus.put((char)0x2262);
        ccbs.put((byte)0x2b); ccbs.put((byte)0x49); ccbs.put((byte)0x6d); ccbs.put((byte)0x49);
        
        ccbs.limit(ccbs.position());
        ccbs.position(0);
        ccus.limit(ccus.position());
        ccus.position(0);
        try {
            smBufEncode(encoder, "UTF-7-CC-EN-11", ccus, ccbs, false, true);
        } catch (Exception ex) {
            errln("Exception while encoding UTF-7 code coverage test should not have been thrown.");
        }
        
        ccbs.clear();
        ccus.clear();
        
        //test for overflow buffer error
        encoder.reset();
        ccus.put((char)0x3980); ccus.put((char)0x2715);
        ccbs.put((byte)0x2b); ccbs.put((byte)0x4f); ccbs.put((byte)0x59);
        
        ccbs.limit(ccbs.position());
        ccbs.position(0);
        ccus.limit(ccus.position());
        ccus.position(0);
        
        result = encoder.encode(ccus, ccbs, true);
        result = encoder.flush(ccbs);
        if (!result.isOverflow()) {
            errln("Overflow buffer while encoding UTF-7 should have occurred.");
        }
        //end of charset encoder code coverage code
    }
    //Test Charset ISCII
    public void TestCharsetISCII() {
        CharsetProvider provider = new CharsetProviderICU();
        Charset cs = provider.charsetForName("ISCII,version=0");        
        CharsetEncoder encoder = cs.newEncoder();
        CharsetDecoder decoder = cs.newDecoder();
        
        CharBuffer us = CharBuffer.allocate(0x100);
        ByteBuffer bs = ByteBuffer.allocate(0x100);
        ByteBuffer bsr = ByteBuffer.allocate(0x100);
        
        //test full range of Devanagari
        us.put((char)0x0901); us.put((char)0x0902); us.put((char)0x0903); us.put((char)0x0905); us.put((char)0x0906); us.put((char)0x0907);
        us.put((char)0x0908); us.put((char)0x0909); us.put((char)0x090A); us.put((char)0x090B); us.put((char)0x090E); us.put((char)0x090F);
        us.put((char)0x0910); us.put((char)0x090D); us.put((char)0x0912); us.put((char)0x0913); us.put((char)0x0914); us.put((char)0x0911);
        us.put((char)0x0915); us.put((char)0x0916); us.put((char)0x0917); us.put((char)0x0918); us.put((char)0x0919); us.put((char)0x091A);
        us.put((char)0x091B); us.put((char)0x091C); us.put((char)0x091D); us.put((char)0x091E); us.put((char)0x091F); us.put((char)0x0920);
        us.put((char)0x0921); us.put((char)0x0922); us.put((char)0x0923); us.put((char)0x0924); us.put((char)0x0925); us.put((char)0x0926); 
        us.put((char)0x0927); us.put((char)0x0928); us.put((char)0x0929); us.put((char)0x092A); us.put((char)0x092B); us.put((char)0x092C); 
        us.put((char)0x092D); us.put((char)0x092E); us.put((char)0x092F); us.put((char)0x095F); us.put((char)0x0930); us.put((char)0x0931); 
        us.put((char)0x0932); us.put((char)0x0933); us.put((char)0x0934); us.put((char)0x0935); us.put((char)0x0936); us.put((char)0x0937); 
        us.put((char)0x0938); us.put((char)0x0939); us.put((char)0x200D); us.put((char)0x093E); us.put((char)0x093F); us.put((char)0x0940); 
        us.put((char)0x0941); us.put((char)0x0942); us.put((char)0x0943); us.put((char)0x0946); us.put((char)0x0947); us.put((char)0x0948); 
        us.put((char)0x0945); us.put((char)0x094A); us.put((char)0x094B); us.put((char)0x094C); us.put((char)0x0949); us.put((char)0x094D); 
        us.put((char)0x093D); us.put((char)0x0966); us.put((char)0x0967); us.put((char)0x0968); us.put((char)0x0969); us.put((char)0x096A); 
        us.put((char)0x096B); us.put((char)0x096C); us.put((char)0x096D); us.put((char)0x096E); us.put((char)0x096F); 
        
        bs.put((byte)0xEF); bs.put((byte)0x42);
        bs.put((byte)0xA1); bs.put((byte)0xA2); bs.put((byte)0xA3); bs.put((byte)0xA4); bs.put((byte)0xA5); bs.put((byte)0xA6);
        bs.put((byte)0xA7); bs.put((byte)0xA8); bs.put((byte)0xA9); bs.put((byte)0xAA); bs.put((byte)0xAB); bs.put((byte)0xAC); 
        bs.put((byte)0xAD); bs.put((byte)0xAE); bs.put((byte)0xAF); bs.put((byte)0xB0); bs.put((byte)0xB1); bs.put((byte)0xB2); 
        bs.put((byte)0xB3); bs.put((byte)0xB4); bs.put((byte)0xB5); bs.put((byte)0xB6); bs.put((byte)0xB7); bs.put((byte)0xB8); 
        bs.put((byte)0xB9); bs.put((byte)0xBA); bs.put((byte)0xBB); bs.put((byte)0xBC); bs.put((byte)0xBD); bs.put((byte)0xBE); 
        bs.put((byte)0xBF); bs.put((byte)0xC0); bs.put((byte)0xC1); bs.put((byte)0xC2); bs.put((byte)0xC3); bs.put((byte)0xC4); 
        bs.put((byte)0xC5); bs.put((byte)0xC6); bs.put((byte)0xC7); bs.put((byte)0xC8); bs.put((byte)0xC9); bs.put((byte)0xCA); 
        bs.put((byte)0xCB); bs.put((byte)0xCC); bs.put((byte)0xCD); bs.put((byte)0xCE); bs.put((byte)0xCF); bs.put((byte)0xD0); 
        bs.put((byte)0xD1); bs.put((byte)0xD2); bs.put((byte)0xD3); bs.put((byte)0xD4); bs.put((byte)0xD5); bs.put((byte)0xD6); 
        bs.put((byte)0xD7); bs.put((byte)0xD8); bs.put((byte)0xD9); bs.put((byte)0xDA); bs.put((byte)0xDB); bs.put((byte)0xDC); 
        bs.put((byte)0xDD); bs.put((byte)0xDE); bs.put((byte)0xDF); bs.put((byte)0xE0); bs.put((byte)0xE1); bs.put((byte)0xE2); 
        bs.put((byte)0xE3); bs.put((byte)0xE4); bs.put((byte)0xE5); bs.put((byte)0xE6); bs.put((byte)0xE7); bs.put((byte)0xE8); 
        bs.put((byte)0xEA); bs.put((byte)0xE9); bs.put((byte)0xF1); bs.put((byte)0xF2); bs.put((byte)0xF3); bs.put((byte)0xF4); 
        bs.put((byte)0xF5); bs.put((byte)0xF6); bs.put((byte)0xF7); bs.put((byte)0xF8); bs.put((byte)0xF9); bs.put((byte)0xFA); 
        
        bsr.put((byte)0xA1); bsr.put((byte)0xA2); bsr.put((byte)0xA3); bsr.put((byte)0xA4); bsr.put((byte)0xA5); bsr.put((byte)0xA6);
        bsr.put((byte)0xA7); bsr.put((byte)0xA8); bsr.put((byte)0xA9); bsr.put((byte)0xAA); bsr.put((byte)0xAB); bsr.put((byte)0xAC); 
        bsr.put((byte)0xAD); bsr.put((byte)0xAE); bsr.put((byte)0xAF); bsr.put((byte)0xB0); bsr.put((byte)0xB1); bsr.put((byte)0xB2); 
        bsr.put((byte)0xB3); bsr.put((byte)0xB4); bsr.put((byte)0xB5); bsr.put((byte)0xB6); bsr.put((byte)0xB7); bsr.put((byte)0xB8); 
        bsr.put((byte)0xB9); bsr.put((byte)0xBA); bsr.put((byte)0xBB); bsr.put((byte)0xBC); bsr.put((byte)0xBD); bsr.put((byte)0xBE); 
        bsr.put((byte)0xBF); bsr.put((byte)0xC0); bsr.put((byte)0xC1); bsr.put((byte)0xC2); bsr.put((byte)0xC3); bsr.put((byte)0xC4); 
        bsr.put((byte)0xC5); bsr.put((byte)0xC6); bsr.put((byte)0xC7); bsr.put((byte)0xC8); bsr.put((byte)0xC9); bsr.put((byte)0xCA); 
        bsr.put((byte)0xCB); bsr.put((byte)0xCC); bsr.put((byte)0xCD); bsr.put((byte)0xCE); bsr.put((byte)0xCF); bsr.put((byte)0xD0); 
        bsr.put((byte)0xD1); bsr.put((byte)0xD2); bsr.put((byte)0xD3); bsr.put((byte)0xD4); bsr.put((byte)0xD5); bsr.put((byte)0xD6); 
        bsr.put((byte)0xD7); bsr.put((byte)0xD8); bsr.put((byte)0xD9); bsr.put((byte)0xDA); bsr.put((byte)0xDB); bsr.put((byte)0xDC); 
        bsr.put((byte)0xDD); bsr.put((byte)0xDE); bsr.put((byte)0xDF); bsr.put((byte)0xE0); bsr.put((byte)0xE1); bsr.put((byte)0xE2); 
        bsr.put((byte)0xE3); bsr.put((byte)0xE4); bsr.put((byte)0xE5); bsr.put((byte)0xE6); bsr.put((byte)0xE7); bsr.put((byte)0xE8); 
        bsr.put((byte)0xEA); bsr.put((byte)0xE9); bsr.put((byte)0xF1); bsr.put((byte)0xF2); bsr.put((byte)0xF3); bsr.put((byte)0xF4); 
        bsr.put((byte)0xF5); bsr.put((byte)0xF6); bsr.put((byte)0xF7); bsr.put((byte)0xF8); bsr.put((byte)0xF9); bsr.put((byte)0xFA); 
        
        //test Soft Halant
        us.put((char)0x0915); us.put((char)0x094d); us.put((char)0x200D);
        bs.put((byte)0xB3); bs.put((byte)0xE8); bs.put((byte)0xE9);
        bsr.put((byte)0xB3); bsr.put((byte)0xE8); bsr.put((byte)0xE9);
        
        //test explicit halant
        us.put((char)0x0915); us.put((char)0x094D); us.put((char)0x200C);
        bs.put((byte)0xB3); bs.put((byte)0xE8); bs.put((byte)0xE8);
        bsr.put((byte)0xB3); bsr.put((byte)0xE8); bsr.put((byte)0xE8);
        
        //test double danda
        us.put((char)0x0965); 
        bs.put((byte)0xEA); bs.put((byte)0xEA); 
        bsr.put((byte)0xEA); bsr.put((byte)0xEA); 
        
        //test ASCII
        us.put((char)0x1B); us.put((char)0x24); us.put((char)0x29); us.put((char)0x47); us.put((char)0x0E); us.put((char)0x23);
        us.put((char)0x21); us.put((char)0x23); us.put((char)0x22); us.put((char)0x23); us.put((char)0x23); us.put((char)0x23);
        us.put((char)0x24); us.put((char)0x23); us.put((char)0x25); us.put((char)0x23); us.put((char)0x26); us.put((char)0x23);
        us.put((char)0x27); us.put((char)0x23); us.put((char)0x28); us.put((char)0x23); us.put((char)0x29); us.put((char)0x23);
        us.put((char)0x2A); us.put((char)0x23); us.put((char)0x2B); us.put((char)0x0F); us.put((char)0x2F); us.put((char)0x2A);
        
        bs.put((byte)0x1B); bs.put((byte)0x24); bs.put((byte)0x29); bs.put((byte)0x47); bs.put((byte)0x0E); bs.put((byte)0x23);
        bs.put((byte)0x21); bs.put((byte)0x23); bs.put((byte)0x22); bs.put((byte)0x23); bs.put((byte)0x23); bs.put((byte)0x23);
        bs.put((byte)0x24); bs.put((byte)0x23); bs.put((byte)0x25); bs.put((byte)0x23); bs.put((byte)0x26); bs.put((byte)0x23);
        bs.put((byte)0x27); bs.put((byte)0x23); bs.put((byte)0x28); bs.put((byte)0x23); bs.put((byte)0x29); bs.put((byte)0x23);
        bs.put((byte)0x2A); bs.put((byte)0x23); bs.put((byte)0x2B); bs.put((byte)0x0F); bs.put((byte)0x2F); bs.put((byte)0x2A);
        
        bsr.put((byte)0x1B); bsr.put((byte)0x24); bsr.put((byte)0x29); bsr.put((byte)0x47); bsr.put((byte)0x0E); bsr.put((byte)0x23);
        bsr.put((byte)0x21); bsr.put((byte)0x23); bsr.put((byte)0x22); bsr.put((byte)0x23); bsr.put((byte)0x23); bsr.put((byte)0x23);
        bsr.put((byte)0x24); bsr.put((byte)0x23); bsr.put((byte)0x25); bsr.put((byte)0x23); bsr.put((byte)0x26); bsr.put((byte)0x23);
        bsr.put((byte)0x27); bsr.put((byte)0x23); bsr.put((byte)0x28); bsr.put((byte)0x23); bsr.put((byte)0x29); bsr.put((byte)0x23);
        bsr.put((byte)0x2A); bsr.put((byte)0x23); bsr.put((byte)0x2B); bsr.put((byte)0x0F); bsr.put((byte)0x2F); bsr.put((byte)0x2A);
        
        //test from Lotus
        //Some of the Lotus ISCII code points have been changed or commented out.
        us.put((char)0x0061); us.put((char)0x0915); us.put((char)0x000D); us.put((char)0x000A); us.put((char)0x0996); us.put((char)0x0043);
        us.put((char)0x0930); us.put((char)0x094D); us.put((char)0x200D); us.put((char)0x0901); us.put((char)0x000D); us.put((char)0x000A);
        us.put((char)0x0905); us.put((char)0x0985); us.put((char)0x0043); us.put((char)0x0915); us.put((char)0x0921); us.put((char)0x002B);
        us.put((char)0x095F); 
        bs.put((byte)0x61); bs.put((byte)0xB3);
        bs.put((byte)0x0D); bs.put((byte)0x0A); 
        bs.put((byte)0xEF); bs.put((byte)0x42); 
        bs.put((byte)0xEF); bs.put((byte)0x43); bs.put((byte)0xB4); bs.put((byte)0x43);
        bs.put((byte)0xEF); bs.put((byte)0x42); bs.put((byte)0xCF); bs.put((byte)0xE8); bs.put((byte)0xE9); bs.put((byte)0xA1); bs.put((byte)0x0D); bs.put((byte)0x0A); bs.put((byte)0xEF); bs.put((byte)0x42);
        bs.put((byte)0xA4); bs.put((byte)0xEF); bs.put((byte)0x43); bs.put((byte)0xA4); bs.put((byte)0x43); bs.put((byte)0xEF);
        bs.put((byte)0x42); bs.put((byte)0xB3); bs.put((byte)0xBF); bs.put((byte)0x2B);
        bs.put((byte)0xCE);
        bsr.put((byte)0x61); bsr.put((byte)0xEF); bsr.put((byte)0x42); bsr.put((byte)0xEF); bsr.put((byte)0x30); bsr.put((byte)0xB3);
        bsr.put((byte)0x0D); bsr.put((byte)0x0A); bsr.put((byte)0xEF); bsr.put((byte)0x43); bsr.put((byte)0xB4); bsr.put((byte)0x43);
        bsr.put((byte)0xEF); bsr.put((byte)0x42); bsr.put((byte)0xCF); bsr.put((byte)0xE8); bsr.put((byte)0xD9); bsr.put((byte)0xEF);
        bsr.put((byte)0x42); bsr.put((byte)0xA1); bsr.put((byte)0x0D); bsr.put((byte)0x0A); bsr.put((byte)0xEF); bsr.put((byte)0x42);
        bsr.put((byte)0xA4); bsr.put((byte)0xEF); bsr.put((byte)0x43); bsr.put((byte)0xA4); bsr.put((byte)0x43); bsr.put((byte)0xEF);
        bsr.put((byte)0x42); bsr.put((byte)0xB3); bsr.put((byte)0xBF); bsr.put((byte)0x2B); bsr.put((byte)0xEF); bsr.put((byte)0x42);
        bsr.put((byte)0xCE);
        //end of test from Lotus
        
        //tamil range
        us.put((char)0x0B86); us.put((char)0x0B87); us.put((char)0x0B88);
        bs.put((byte)0xEF); bs.put((byte)0x44); bs.put((byte)0xA5); bs.put((byte)0xA6); bs.put((byte)0xA7);
        bsr.put((byte)0xEF); bsr.put((byte)0x44); bsr.put((byte)0xA5); bsr.put((byte)0xA6); bsr.put((byte)0xA7);
        
        //telugu range
        us.put((char)0x0C05); us.put((char)0x0C02); us.put((char)0x0C03); us.put((char)0x0C31);
        bs.put((byte)0xEF); bs.put((byte)0x45); bs.put((byte)0xA4); bs.put((byte)0xA2); bs.put((byte)0xA3); bs.put((byte)0xD0);
        bsr.put((byte)0xEF); bsr.put((byte)0x45); bsr.put((byte)0xA4); bsr.put((byte)0xA2); bsr.put((byte)0xA3); bsr.put((byte)0xD0);
        
        //kannada range
        us.put((char)0x0C85); us.put((char)0x0C82); us.put((char)0x0C83);
        bs.put((byte)0xEF); bs.put((byte)0x48); bs.put((byte)0xA4); bs.put((byte)0xA2); bs.put((byte)0xA3);
        bsr.put((byte)0xEF); bsr.put((byte)0x48); bsr.put((byte)0xA4); bsr.put((byte)0xA2); bsr.put((byte)0xA3);  
        
        //test Abbr sign and Anudatta
        us.put((char)0x0970); us.put((char)0x0952); us.put((char)0x0960); us.put((char)0x0944); us.put((char)0x090C); us.put((char)0x0962);
        us.put((char)0x0961); us.put((char)0x0963); us.put((char)0x0950); us.put((char)0x093D); us.put((char)0x0958); us.put((char)0x0959);
        us.put((char)0x095A); us.put((char)0x095B); us.put((char)0x095C); us.put((char)0x095D); us.put((char)0x095E); us.put((char)0x0020);
        us.put((char)0x094D); us.put((char)0x0930); us.put((char)0x0000); us.put((char)0x00A0); 
        bs.put((byte)0xEF); bs.put((byte)0x42); bs.put((byte)0xF0); bs.put((byte)0xBF); bs.put((byte)0xF0); bs.put((byte)0xB8);
        bs.put((byte)0xAA); bs.put((byte)0xE9); bs.put((byte)0xDF); bs.put((byte)0xE9); bs.put((byte)0xA6); bs.put((byte)0xE9);
        bs.put((byte)0xDB); bs.put((byte)0xE9); bs.put((byte)0xA7); bs.put((byte)0xE9); bs.put((byte)0xDC); bs.put((byte)0xE9);
        bs.put((byte)0xA1); bs.put((byte)0xE9); bs.put((byte)0xEA); bs.put((byte)0xE9); bs.put((byte)0xB3); bs.put((byte)0xE9);
        bs.put((byte)0xB4); bs.put((byte)0xE9); bs.put((byte)0xB5); bs.put((byte)0xE9); bs.put((byte)0xBA); bs.put((byte)0xE9);
        bs.put((byte)0xBF); bs.put((byte)0xE9); bs.put((byte)0xC0); bs.put((byte)0xE9); bs.put((byte)0xC9); bs.put((byte)0xE9);
        bs.put((byte)0x20); bs.put((byte)0xE8); bs.put((byte)0xCF); bs.put((byte)0x00); bs.put((byte)0xA0); 
        //bs.put((byte)0xEF); bs.put((byte)0x30); 
        bsr.put((byte)0xEF); bsr.put((byte)0x42); bsr.put((byte)0xF0); bsr.put((byte)0xBF); bsr.put((byte)0xF0); bsr.put((byte)0xB8);
        bsr.put((byte)0xAA); bsr.put((byte)0xE9); bsr.put((byte)0xDF); bsr.put((byte)0xE9); bsr.put((byte)0xA6); bsr.put((byte)0xE9);
        bsr.put((byte)0xDB); bsr.put((byte)0xE9); bsr.put((byte)0xA7); bsr.put((byte)0xE9); bsr.put((byte)0xDC); bsr.put((byte)0xE9);
        bsr.put((byte)0xA1); bsr.put((byte)0xE9); bsr.put((byte)0xEA); bsr.put((byte)0xE9); bsr.put((byte)0xB3); bsr.put((byte)0xE9);
        bsr.put((byte)0xB4); bsr.put((byte)0xE9); bsr.put((byte)0xB5); bsr.put((byte)0xE9); bsr.put((byte)0xBA); bsr.put((byte)0xE9);
        bsr.put((byte)0xBF); bsr.put((byte)0xE9); bsr.put((byte)0xC0); bsr.put((byte)0xE9); bsr.put((byte)0xC9); bsr.put((byte)0xE9);
        bsr.put((byte)0xD9); bsr.put((byte)0xE8); bsr.put((byte)0xCF); bsr.put((byte)0x00); bsr.put((byte)0xA0);  
        
        bs.limit(bs.position());
        bs.position(0);
        us.limit(us.position());
        us.position(0);
        bsr.limit(bsr.position());
        bsr.position(0);
        
        //round trip test
        try {
            smBufDecode(decoder, "ISCII-part1", bsr, us, false, true);
            smBufEncode(encoder, "ISCII-part2", us, bs); 
            smBufDecode(decoder, "ISCII-part3", bs, us, false, true);
        } catch (Exception ex) {
            errln("ISCII round trip test failed.");
        }
        
        //Test new characters in the ISCII charset
        encoder = provider.charsetForName("ISCII,version=0").newEncoder();
        decoder = provider.charsetForName("ISCII,version=0").newDecoder();
        char u_pts[] = {
                /* DEV */ (char)0x0904,
                /* PNJ */ (char)0x0A01, (char)0x0A03, (char)0x0A33, (char)0x0A70
            };
        byte b_pts[] = {
                                (byte)0xef, (byte)0x42,
                /* DEV */ (byte)0xa4, (byte)0xe0,
                /* PNJ */ (byte)0xef, (byte)0x4b, (byte)0xa1, (byte)0xa3, (byte)0xd2, (byte)0xf0, (byte)0xbf
            };
        us = CharBuffer.allocate(u_pts.length);
        bs = ByteBuffer.allocate(b_pts.length);
        us.put(u_pts);
        bs.put(b_pts);
        
        bs.limit(bs.position());
        bs.position(0);
        us.limit(us.position());
        us.position(0);
        
        try {
            smBufDecode(decoder, "ISCII-update", bs, us, true, true);         
            bs.position(0);
            us.position(0);
            smBufEncode(encoder, "ISCII-update", us, bs, true, true);
        } catch (Exception ex) {
            errln("Error occurred while encoding/decoding ISCII with the new characters.");
        }
        
        //The rest of the code in this method is to provide better code coverage
        CharBuffer ccus = CharBuffer.allocate(0x10);
        ByteBuffer ccbs = ByteBuffer.allocate(0x10);
        
        //start of charset decoder code coverage code
        //test overflow buffer
        ccbs.put((byte)0x49);
        
        ccbs.limit(ccbs.position());
        ccbs.position(0);
        ccus.limit(0);
        ccus.position(0);
        
        try {
            smBufDecode(decoder, "ISCII-CC-DE-1", ccbs, ccus, true, false);
            errln("Exception while decoding ISCII should have been thrown.");
        }
        catch (Exception ex) {
        }
        
        ccbs.clear();
        ccus.clear();
        
        //test atr overflow buffer
        ccbs.put((byte)0xEF); ccbs.put((byte)0x40); ccbs.put((byte)0xEF); ccbs.put((byte)0x20);
        ccus.put((char)0x00);
        
        ccbs.limit(ccbs.position());
        ccbs.position(0);
        ccus.limit(ccus.position());
        ccus.position(0);
        
        try {
            smBufDecode(decoder, "ISCII-CC-DE-2", ccbs, ccus, true, false);
            errln("Exception while decoding ISCII should have been thrown.");
        }
        catch (Exception ex) {
        }
        
        //end of charset decoder code coverage code
        
        ccbs.clear();
        ccus.clear();
      
        //start of charset encoder code coverage code
        //test ascii overflow buffer
        ccus.put((char)0x41);
        
        ccus.limit(ccus.position());
        ccus.position(0);
        ccbs.limit(0);
        ccbs.position(0);
           
        try {
            smBufEncode(encoder, "ISCII-CC-EN-1", ccus, ccbs, true, false);
            errln("Exception while encoding ISCII should have been thrown.");
        }
        catch (Exception ex) {
        }
        
        ccbs.clear();
        ccus.clear();
        
        //test ascii overflow buffer
        ccus.put((char)0x0A); ccus.put((char)0x0043);
        ccbs.put((byte)0x00); ccbs.put((byte)0x00);
        
        ccus.limit(ccus.position());
        ccus.position(0);
        ccbs.limit(ccbs.position());
        ccbs.position(0);
           
        try {
            smBufEncode(encoder, "ISCII-CC-EN-2", ccus, ccbs, true, false);
            errln("Exception while encoding ISCII should have been thrown.");
        }
        catch (Exception ex) {
        }
        
        ccbs.clear();
        ccus.clear();
        
        //test surrogate malform
        ccus.put((char)0x06E3);
        ccbs.put((byte)0x00);
        
        ccus.limit(ccus.position());
        ccus.position(0);
        ccbs.limit(ccbs.position());
        ccbs.position(0);
           
        try {
            smBufEncode(encoder, "ISCII-CC-EN-3", ccus, ccbs, true, false);
            errln("Exception while encoding ISCII should have been thrown.");
        }
        catch (Exception ex) {
        }
        
        ccbs.clear();
        ccus.clear();
        
        //test surrogate malform
        ccus.put((char)0xD801); ccus.put((char)0xDD01);
        ccbs.put((byte)0x00);
        
        ccus.limit(ccus.position());
        ccus.position(0);
        ccbs.limit(ccbs.position());
        ccbs.position(0);
           
        try {
            smBufEncode(encoder, "ISCII-CC-EN-4", ccus, ccbs, true, false);
            errln("Exception while encoding ISCII should have been thrown.");
        }
        catch (Exception ex) {
        }
        
        ccbs.clear();
        ccus.clear();
        
        //test trail surrogate malform
        ccus.put((char)0xDD01); 
        ccbs.put((byte)0x00);
        
        ccus.limit(ccus.position());
        ccus.position(0);
        ccbs.limit(ccbs.position());
        ccbs.position(0);
           
        try {
            smBufEncode(encoder, "ISCII-CC-EN-5", ccus, ccbs, true, false);
            errln("Exception while encoding ISCII should have been thrown.");
        }
        catch (Exception ex) {
        }
        
        ccbs.clear();
        ccus.clear();
        
        //test lead surrogates malform
        ccus.put((char)0xD801); ccus.put((char)0xD802); 
        ccbs.put((byte)0x00);
        
        ccus.limit(ccus.position());
        ccus.position(0);
        ccbs.limit(ccbs.position());
        ccbs.position(0);
           
        try {
            smBufEncode(encoder, "ISCII-CC-EN-6", ccus, ccbs, true, false);
            errln("Exception while encoding ISCII should have been thrown.");
        }
        catch (Exception ex) {
        }
        
        ccus.clear();
        ccbs.clear();
        
        //test overflow buffer
        ccus.put((char)0x0901); 
        ccbs.put((byte)0x00);
        
        ccus.limit(ccus.position());
        ccus.position(0);
        ccbs.limit(ccbs.position());
        ccbs.position(0);
           
        cs = provider.charsetForName("ISCII,version=0");
        encoder = cs.newEncoder();
        
        try {
            smBufEncode(encoder, "ISCII-CC-EN-7", ccus, ccbs, true, false);
            errln("Exception while encoding ISCII should have been thrown.");
        }
        catch (Exception ex) {
        }
        //end of charset encoder code coverage code
    }
    
    //Test for the IMAP Charset
    public void TestCharsetIMAP() {
        CharsetProvider provider = new CharsetProviderICU();
        Charset cs = provider.charsetForName("IMAP-mailbox-name");        
        CharsetEncoder encoder = cs.newEncoder();
        CharsetDecoder decoder = cs.newDecoder();
        
        CharBuffer us = CharBuffer.allocate(0x20);
        ByteBuffer bs = ByteBuffer.allocate(0x20);
        
        us.put((char)0x00A3); us.put((char)0x2020); us.put((char)0x41);
        
        bs.put((byte)0x26); bs.put((byte)0x41); bs.put((byte)0x4B); bs.put((byte)0x4D); bs.put((byte)0x67); bs.put((byte)0x49);
        bs.put((byte)0x41); bs.put((byte)0x2D); bs.put((byte)0x41);
        
        
        bs.limit(bs.position());
        bs.position(0);
        us.limit(us.position());
        us.position(0);

        smBufDecode(decoder, "IMAP", bs, us);
        smBufEncode(encoder, "IMAP", us, bs);
        
        //the rest of the code in this method is for better code coverage
        us.clear();
        bs.clear();
        
        //start of charset encoder code coverage
        //test buffer overflow
        us.put((char)0x0026); us.put((char)0x17A9); 
        bs.put((byte)0x00); bs.put((byte)0x00); bs.put((byte)0x00); bs.put((byte)0x00);
        
        bs.limit(bs.position());
        bs.position(0);
        us.limit(us.position());
        us.position(0);
        
        try {
            smBufEncode(encoder, "IMAP-EN-1", us, bs, true, false);
            errln("Exception while encoding IMAP (1) should have been thrown.");
        } catch(Exception ex) {
        }
        
        us.clear();
        bs.clear();
        
        //test buffer overflow
        us.put((char)0x17A9); us.put((char)0x0941);
        bs.put((byte)0x00); bs.put((byte)0x00); bs.put((byte)0x00); bs.put((byte)0x00); bs.put((byte)0x00);
        
        bs.limit(bs.position());
        bs.position(0);
        us.limit(us.position());
        us.position(0);
        
        try {
            smBufEncode(encoder, "IMAP-EN-2", us, bs, true, false);
            errln("Exception while encoding IMAP (2) should have been thrown.");
        } catch(Exception ex) {
        }
        
        us.clear();
        bs.clear();
        
        //test buffer overflow
        us.put((char)0x17A9); us.put((char)0x0941);
        bs.put((byte)0x00); bs.put((byte)0x00); bs.put((byte)0x00); bs.put((byte)0x00);   
        
        bs.limit(bs.position());
        bs.position(0);
        us.limit(us.position());
        us.position(0);
        
        try {
            smBufEncode(encoder, "IMAP-EN-3", us, bs, true, false);
            errln("Exception while encoding IMAP (3) should have been thrown.");
        } catch(Exception ex) {
        }
        
        us.clear();
        bs.clear();
        
        //test buffer overflow
        us.put((char)0x17A9); us.put((char)0x0941); us.put((char)0x0955);
        bs.put((byte)0x00); bs.put((byte)0x00); bs.put((byte)0x00); bs.put((byte)0x00); bs.put((byte)0x00); bs.put((byte)0x00);      
        bs.put((byte)0x00);
        
        bs.limit(bs.position());
        bs.position(0);
        us.limit(us.position());
        us.position(0);
        
        try {
            smBufEncode(encoder, "IMAP-EN-4", us, bs, true, false);
            errln("Exception while encoding IMAP (4) should have been thrown.");
        } catch(Exception ex) {
        }
        
        us.clear();
        bs.clear();
        
        //test buffer overflow
        us.put((char)0x17A9); us.put((char)0x0941); us.put((char)0x0955);
        bs.put((byte)0x00); bs.put((byte)0x00); bs.put((byte)0x00); bs.put((byte)0x00); bs.put((byte)0x00); bs.put((byte)0x00);  
        bs.put((byte)0x00); bs.put((byte)0x00); 
        
        bs.limit(bs.position());
        bs.position(0);
        us.limit(us.position());
        us.position(0);
        
        try {
            smBufEncode(encoder, "IMAP-EN-5", us, bs, true, false);
            errln("Exception while encoding IMAP (5) should have been thrown.");
        } catch(Exception ex) {
        }
        
        us.clear();
        bs.clear();
        
        //test buffer overflow
        us.put((char)0x17A9); us.put((char)0x0941); us.put((char)0x0955); us.put((char)0x0970);
        bs.put((byte)0x00); bs.put((byte)0x00); bs.put((byte)0x00); bs.put((byte)0x00); bs.put((byte)0x00); bs.put((byte)0x00);  
        bs.put((byte)0x00); bs.put((byte)0x00); bs.put((byte)0x00);
        
        bs.limit(bs.position());
        bs.position(0);
        us.limit(us.position());
        us.position(0);
        
        try {
            smBufEncode(encoder, "IMAP-EN-6", us, bs, true, false);
            errln("Exception while encoding IMAP (6) should have been thrown.");
        } catch(Exception ex) {
        }
        
        us.clear();
        bs.clear();
        
        //test buffer overflow
        us.put((char)0x17A9); us.put((char)0x0941);
        bs.put((byte)0x00); bs.put((byte)0x00); bs.put((byte)0x00); bs.put((byte)0x00);  bs.put((byte)0x00); bs.put((byte)0x00);
        bs.put((byte)0x00); 
        
        bs.limit(bs.position());
        bs.position(0);
        us.limit(us.position());
        us.position(0);
        
        try {
            smBufEncode(encoder, "IMAP-EN-7", us, bs, true, true);
            errln("Exception while encoding IMAP (7) should have been thrown.");
        } catch(Exception ex) {
        }
        
        us.clear();
        bs.clear();
        
        //test flushing
        us.put((char)0x17A9); us.put((char)0x0941); 
        bs.put((byte)0x26); bs.put((byte)0x46); bs.put((byte)0x36); bs.put((byte)0x6b);  bs.put((byte)0x4a); bs.put((byte)0x51);
        bs.put((byte)0x51); bs.put((byte)0x2d);
        
        bs.limit(bs.position());
        bs.position(0);
        us.limit(us.position());
        us.position(0);
        
        try {
            smBufEncode(encoder, "IMAP-EN-8", us, bs, true, true);
        } catch(Exception ex) {
            errln("Exception while encoding IMAP (8) should not have been thrown.");
        }
        
        us = CharBuffer.allocate(0x08);
        bs = ByteBuffer.allocate(0x08);
        
        //test flushing buffer overflow
        us.put((char)0x0061);
        bs.put((byte)0x61); bs.put((byte)0x00);
        
        bs.limit(bs.position());
        bs.position(0);
        us.limit(us.position());
        us.position(0);
        
        try {
            smBufEncode(encoder, "IMAP-EN-9", us, bs, true, true);
        } catch(Exception ex) {
            errln("Exception while encoding IMAP (9) should not have been thrown.");
        }
        //end of charset encoder code coverage
        
        us = CharBuffer.allocate(0x10);
        bs = ByteBuffer.allocate(0x10);
        
        //start of charset decoder code coverage
        //test malform case 2
        us.put((char)0x0000); us.put((char)0x0000); 
        bs.put((byte)0x26); bs.put((byte)0x41); bs.put((byte)0x43); bs.put((byte)0x41);  
        
        bs.limit(bs.position());
        bs.position(0);
        us.limit(us.position());
        us.position(0);
        
        try {
            smBufDecode(decoder, "IMAP-DE-1", bs, us, true, false);
            errln("Exception while decoding IMAP (1) should have been thrown.");
        } catch(Exception ex) {
        }
        
        us.clear();
        bs.clear();
        
        //test malform case 5
        us.put((char)0x0000); us.put((char)0x0000); us.put((char)0x0000);
        bs.put((byte)0x26); bs.put((byte)0x41); bs.put((byte)0x41); bs.put((byte)0x41); 
        bs.put((byte)0x41); bs.put((byte)0x49); bs.put((byte)0x41);  
        
        bs.limit(bs.position());
        bs.position(0);
        us.limit(us.position());
        us.position(0);
        
        try {
            smBufDecode(decoder, "IMAP-DE-2", bs, us, true, false);
            errln("Exception while decoding IMAP (2) should have been thrown.");
        } catch(Exception ex) {
        }
        
        us.clear();
        bs.clear();
        
        //test malform case 7
        us.put((char)0x0000); us.put((char)0x0000); us.put((char)0x0000); us.put((char)0x0000);
        bs.put((byte)0x26); bs.put((byte)0x41); bs.put((byte)0x41); bs.put((byte)0x41); 
        bs.put((byte)0x41); bs.put((byte)0x41); bs.put((byte)0x41); bs.put((byte)0x42); 
        bs.put((byte)0x41);  
        
        bs.limit(bs.position());
        bs.position(0);
        us.limit(us.position());
        us.position(0);
        
        try {
            smBufDecode(decoder, "IMAP-DE-3", bs, us, true, false);
            errln("Exception while decoding IMAP (3) should have been thrown.");
        } catch(Exception ex) {
        }
        //end of charset decoder coder coverage  
    }
    
    //Test for charset UTF32LE to provide better code coverage
    public void TestCharsetUTF32LE() {
        CoderResult result = CoderResult.UNDERFLOW;
        CharsetProvider provider = new CharsetProviderICU();
        Charset cs = provider.charsetForName("UTF-32LE");        
        CharsetEncoder encoder = cs.newEncoder();
        //CharsetDecoder decoder = cs.newDecoder();
        
        CharBuffer us = CharBuffer.allocate(0x10);
        ByteBuffer bs = ByteBuffer.allocate(0x10);
        
        
        //test malform surrogate
        us.put((char)0xD901);
        bs.put((byte)0x00);
        
        bs.limit(bs.position());
        bs.position(0);
        us.limit(us.position());
        us.position(0);
        
        try {
            smBufEncode(encoder, "UTF32LE-EN-1", us, bs, true, false);
            errln("Exception while encoding UTF32LE (1) should have been thrown.");
        } catch (Exception ex) {
        }
        
        bs.clear();
        us.clear();
        
        //test malform surrogate
        us.put((char)0xD901); us.put((char)0xD902);
        bs.put((byte)0x00);
        
        bs.limit(bs.position());
        bs.position(0);
        us.limit(us.position());
        us.position(0);
        
        result = encoder.encode(us, bs, true);
        
        if (!result.isError() && !result.isOverflow()) {
            errln("Error while encoding UTF32LE (2) should have occurred.");
        }
        
        bs.clear();
        us.clear();
        
        //test overflow trail surrogate
        us.put((char)0xDD01); us.put((char)0xDD0E); us.put((char)0xDD0E);
        bs.put((byte)0x00); bs.put((byte)0x00); bs.put((byte)0x00); bs.put((byte)0x00);
        bs.put((byte)0x00); bs.put((byte)0x00); bs.put((byte)0x00); 
        
        bs.limit(bs.position());
        bs.position(0);
        us.limit(us.position());
        us.position(0);
        
        result = encoder.encode(us, bs, true);
        
        if (!result.isError() && !result.isOverflow()) {
            errln("Error while encoding UTF32LE (3) should have occurred.");
        }
        
        bs.clear();
        us.clear();
        
        //test malform lead surrogate
        us.put((char)0xD90D); us.put((char)0xD90E);
        bs.put((byte)0x00); 
        
        bs.limit(bs.position());
        bs.position(0);
        us.limit(us.position());
        us.position(0);
        
        try {
            smBufEncode(encoder, "UTF32LE-EN-4", us, bs, true, false);
            errln("Exception while encoding UTF32LE (4) should have been thrown.");
        } catch (Exception ex) {
        }
        
        bs.clear();
        us.clear();
        
        //test overflow buffer
        us.put((char)0x0061);
        bs.put((byte)0x00); 
        
        bs.limit(bs.position());
        bs.position(0);
        us.limit(us.position());
        us.position(0);
        
        try {
            smBufEncode(encoder, "UTF32LE-EN-5", us, bs, true, false);
            errln("Exception while encoding UTF32LE (5) should have been thrown.");
        } catch (Exception ex) {
        }
        
        bs.clear();
        us.clear();
        
        //test malform trail surrogate
        us.put((char)0xDD01);
        bs.put((byte)0x00); 
        
        bs.limit(bs.position());
        bs.position(0);
        us.limit(us.position());
        us.position(0);
        
        try {
            smBufEncode(encoder, "UTF32LE-EN-6", us, bs, true, false);
            errln("Exception while encoding UTF32LE (6) should have been thrown.");
        } catch (Exception ex) {
        }
    }

    //Test for charset UTF16LE to provide better code coverage
    public void TestCharsetUTF16LE() {
        CoderResult result = CoderResult.UNDERFLOW;
        CharsetProvider provider = new CharsetProviderICU();
        Charset cs = provider.charsetForName("UTF-16LE");        
        CharsetEncoder encoder = cs.newEncoder();
        //CharsetDecoder decoder = cs.newDecoder();
        
        // Test for malform and change fromUChar32 for next call
        char u_pts1[] = {
                (char)0xD805, 
                (char)0xDC01, (char)0xDC02, (char)0xDC03,
                (char)0xD901, (char)0xD902
                };
        byte b_pts1[] = {
                (byte)0x00, 
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00
                };
        
        CharBuffer us = CharBuffer.allocate(u_pts1.length);
        ByteBuffer bs = ByteBuffer.allocate(b_pts1.length);
        
        us.put(u_pts1);
        bs.put(b_pts1);
        
        us.limit(1);
        us.position(0);
        bs.limit(1);
        bs.position(0);
       
        result = encoder.encode(us, bs, true);
        
        if (!result.isMalformed()) {
            // LE should not output BOM, so this should be malformed 
            errln("Malformed while encoding UTF-16LE (1) should have occured.");
        }
        
        // Test for malform surrogate from previous buffer
        us.limit(4);
        us.position(1);
        bs.limit(7);
        bs.position(1);
        
        result = encoder.encode(us, bs, true);
        
        if (!result.isMalformed()) {
            errln("Error while encoding UTF-16LE (2) should have occured.");
        }       
        
        // Test for malform trail surrogate
        encoder.reset();
        
        us.limit(1);
        us.position(0);
        bs.limit(1);
        bs.position(0);
       
        result = encoder.encode(us, bs, true);    
        
        us.limit(6);
        us.position(4);
        bs.limit(4);
        bs.position(1);
        
        result = encoder.encode(us, bs, true);
        
        if (!result.isMalformed()) {
            errln("Error while encoding UTF-16LE (3) should have occured.");
        }          
    }
    
    //provide better code coverage for the generic charset UTF32
    public void TestCharsetUTF32() {
        CoderResult result = CoderResult.UNDERFLOW;
        CharsetProvider provider = new CharsetProviderICU();
        Charset cs = provider.charsetForName("UTF-32");        
        CharsetDecoder decoder = cs.newDecoder();
        CharsetEncoder encoder = cs.newEncoder();
        
        //start of decoding code coverage
        char us_array[] = {
                0x0000, 0x0000, 0x0000, 0x0000,
            };
        
        byte bs_array1[] = {
                (byte)0x00, (byte)0x00, (byte)0xFE, (byte)0xFF,
                (byte)0x00, (byte)0x00, (byte)0x04, (byte)0x43,
                (byte)0xFF, (byte)0xFE, (byte)0x00, (byte)0x00,
                (byte)0x43, (byte)0x04, (byte)0x00, (byte)0x00,
            };
        
        byte bs_array2[] = {
                (byte)0xFF, (byte)0xFE, (byte)0x00, (byte)0x00,
                (byte)0x43, (byte)0x04, (byte)0x00, (byte)0x00,
            };
        
        CharBuffer us = CharBuffer.allocate(us_array.length);
        ByteBuffer bs = ByteBuffer.allocate(bs_array1.length);
        
        us.put(us_array);
        bs.put(bs_array1);
        
        us.limit(us.position());
        us.position(0);
        bs.limit(bs.position());
        bs.position(0);
            
        try {
            smBufDecode(decoder, "UTF32-DE-1", bs, us, true, false);
            errln("Malform exception while decoding UTF32 charset (1) should have been thrown.");
        } catch (Exception ex) {
        }
        
        decoder = cs.newDecoder();
        
        bs = ByteBuffer.allocate(bs_array2.length);
        bs.put(bs_array2);
        
        us.limit(4);
        us.position(0);
        bs.limit(bs.position());
        bs.position(0);
            
        try {
            smBufDecode(decoder, "UTF32-DE-2", bs, us, true, false);
        } catch (Exception ex) {
            // should recognize little endian BOM
            errln("Exception while decoding UTF32 charset (2) should not have been thrown.");
        }
        
        //Test malform exception
        bs.clear();
        us.clear();
        
        bs.put((byte)0x00); bs.put((byte)0xFE); bs.put((byte)0xFF); bs.put((byte)0x00); bs.put((byte)0x00);
        us.put((char)0x0000);
        
        us.limit(us.position());
        us.position(0);
        bs.limit(bs.position());
        bs.position(0);
        
        try {
            smBufDecode(decoder, "UTF32-DE-3", bs, us, true, false);
            errln("Malform exception while decoding UTF32 charset (3) should have been thrown.");
        } catch (Exception ex) {
        }
        
        //Test BOM testing
        bs.clear();
        us.clear();
        
        bs.put((byte)0x00); bs.put((byte)0x00); bs.put((byte)0xFF); bs.put((byte)0xFE); 
        us.put((char)0x0000);
        
        us.limit(us.position());
        us.position(0);
        bs.limit(bs.position());
        bs.position(0);
        
        try {
            smBufDecode(decoder, "UTF32-DE-4", bs, us, true, false);
        } catch (Exception ex) {
            // should recognize big endian BOM
            errln("Exception while decoding UTF32 charset (4) should not have been thrown.");
        }
        //end of decoding code coverage
        
        //start of encoding code coverage
        us = CharBuffer.allocate(0x10);
        bs = ByteBuffer.allocate(0x10);
        
        //test wite BOM overflow error
        us.put((char)0xDC01);
        bs.put((byte)0x00); bs.put((byte)0x00); bs.put((byte)0x00);
        
        us.limit(us.position());
        us.position(0);
        bs.limit(bs.position());
        bs.position(0);
        
        result = encoder.encode(us, bs, true);
        // must try to output BOM first for UTF-32 (not UTF-32BE or UTF-32LE)
        if (!result.isOverflow()) {
            errln("Buffer overflow error while encoding UTF32 charset (1) should have occurred."); 
        }
        
        us.clear();
        bs.clear();
        
        //test malform surrogate and store value in fromChar32
        us.put((char)0xD801); us.put((char)0xD802);
        bs.put((byte)0x00); bs.put((byte)0x00); bs.put((byte)0x00); bs.put((byte)0x00); bs.put((byte)0x00); bs.put((byte)0x00);
        
        us.limit(us.position());
        us.position(0);
        bs.limit(bs.position());
        bs.position(0);
        
        result = encoder.encode(us, bs, true);
        if (!result.isMalformed()) {
            errln("Malformed error while encoding UTF32 charset (2) should have occurred.");
        }    
        
        us.clear();
        bs.clear();
        
        //test malform surrogate
        us.put((char)0x0000); us.put((char)0xD902);
        
        us.limit(us.position());
        us.position(0);
        bs.limit(bs.position());
        bs.position(0);
        
        result = encoder.encode(us, bs, true);
        if (!result.isOverflow()) {
            errln("Overflow error while encoding UTF32 charset (3) should have occurred.");
        } 
        
        us.clear();
        bs.clear();
        
        //test malform surrogate
        encoder.reset();
        us.put((char)0xD801);
        bs.put((byte)0x00); bs.put((byte)0x00); bs.put((byte)0x00); bs.put((byte)0x00); bs.put((byte)0x00);
   
        us.limit(us.position());
        us.position(0);
        bs.limit(bs.position());
        bs.position(0);
        
        result = encoder.encode(us, bs, true);
        if (!result.isMalformed()) {
            errln("Malform error while encoding UTF32 charset (4) should have occurred.");
        } 
        
        us.clear();
        bs.clear();
        
        //test overflow surrogate
        us.put((char)0x0000); us.put((char)0xDDE1); us.put((char)0xD915); us.put((char)0xDDF2);
        bs.put((byte)0x00); bs.put((byte)0x00); bs.put((byte)0x00); bs.put((byte)0x00); bs.put((byte)0x00); bs.put((byte)0x00); 
   
        us.limit(us.position());
        us.position(0);
        bs.limit(bs.position());
        bs.position(0);
        
        result = encoder.encode(us, bs, true);
        if (!result.isOverflow()) {
            errln("Overflow error while encoding UTF32 charset (5) should have occurred.");
        } 
        
        us.clear();
        bs.clear();
        
        //test malform surrogate
        encoder.reset();
        us.put((char)0xDDE1);
        bs.put((byte)0x00); bs.put((byte)0x00); bs.put((byte)0x00); bs.put((byte)0x00); bs.put((byte)0x00);
   
        us.limit(us.position());
        us.position(0);
        bs.limit(bs.position());
        bs.position(0);
        
        result = encoder.encode(us, bs, true);
        if (!result.isMalformed()) {
            errln("Malform error while encoding UTF32 charset (6) should have occurred.");
        } 
        //end of encoding code coverage
    }
    
    //this method provides better code coverage decoding UTF32 LE/BE
    public void TestDecodeUTF32LEBE() {
        CoderResult result = CoderResult.UNDERFLOW;
        CharsetProvider provider = new CharsetProviderICU();       
        CharsetDecoder decoder;
        CharBuffer us = CharBuffer.allocate(0x10);
        ByteBuffer bs = ByteBuffer.allocate(0x10);
        
        //decode UTF32LE
        decoder = provider.charsetForName("UTF-32LE").newDecoder();
        //test overflow buffer
        bs.put((byte)0x41); bs.put((byte)0xFF); bs.put((byte)0x01); bs.put((byte)0x00);
        us.put((char)0x0000);
        
        us.limit(us.position());
        us.position(0);
        bs.limit(bs.position());
        bs.position(0);
        
        try {
            smBufDecode(decoder, "UTF-32LE", bs, us, true, false);
            errln("Overflow exception while decoding UTF32LE (1) should have been thrown.");
        } catch (Exception ex) {
        }
        // test overflow buffer handling in CharsetDecoderICU
        bs.position(0);
        us.position(0);
        decoder.reset();
        result = decoder.decode(bs, us, true);
        if (result.isOverflow()) {
            result = decoder.decode(bs, us, true);
            if (!result.isOverflow()) {
                errln("Overflow buffer error while decoding UTF32LE should have occurred.");
            }
        } else {
            errln("Overflow buffer error while decoding UTF32LE should have occurred.");
        }
        
        us.clear();
        bs.clear();
        //test malform buffer
        bs.put((byte)0x02); bs.put((byte)0xD9); bs.put((byte)0x00); bs.put((byte)0x00);
        us.put((char)0x0000);
        
        us.limit(us.position());
        us.position(0);
        bs.limit(bs.position());
        bs.position(0);
        
        try {
            smBufDecode(decoder, "UTF-32LE", bs, us, true, false);
            errln("Malform exception while decoding UTF32LE (2) should have been thrown.");
        } catch (Exception ex) {
        }
        
        us.clear();
        bs.clear();
        //test malform buffer
        bs.put((byte)0xFF); bs.put((byte)0xFE); bs.put((byte)0x00); bs.put((byte)0x00);
        bs.put((byte)0xFF); bs.put((byte)0xDF); bs.put((byte)0x10); 
        us.put((char)0x0000);
        
        us.limit(us.position());
        us.position(0);
        bs.limit(bs.position());
        bs.position(0);
        
        try {
            // must flush in order to exhibit malformed behavior
            smBufDecode(decoder, "UTF-32LE", bs, us, true, true);
            errln("Malform exception while decoding UTF32LE (3) should have been thrown.");
        } catch (Exception ex) {
        }
        
        us.clear();
        bs.clear();
        //test malform buffer
        bs.put((byte)0xFF); bs.put((byte)0xFE); bs.put((byte)0x00); bs.put((byte)0x00);
        bs.put((byte)0x02); bs.put((byte)0xD9); bs.put((byte)0x00); bs.put((byte)0x00);
        us.put((char)0x0000);
        
        us.limit(us.position());
        us.position(0);
        bs.limit(bs.position());
        bs.position(0);
        
        try {
            smBufDecode(decoder, "UTF-32LE", bs, us, true, false);
            errln("Malform exception while decoding UTF32LE (4) should have been thrown.");
        } catch (Exception ex) {
        }
        
        us.clear();
        bs.clear();
        //test overflow buffer
        bs.put((byte)0xFF); bs.put((byte)0xFE); bs.put((byte)0x00); bs.put((byte)0x00);
        bs.put((byte)0xDD); bs.put((byte)0xFF); bs.put((byte)0x10); bs.put((byte)0x00);
        us.put((char)0x0000);
        
        us.limit(us.position());
        us.position(0);
        bs.limit(bs.position());
        bs.position(0);
        
        try {
            smBufDecode(decoder, "UTF-32LE", bs, us, true, false);
            errln("Overflow exception while decoding UTF32LE (5) should have been thrown.");
        } catch (Exception ex) {
        }
        //end of decode UTF32LE
        
        bs.clear();
        us.clear();
        
        //decode UTF32BE
        decoder = provider.charsetForName("UTF-32BE").newDecoder();
        //test overflow buffer
        bs.put((byte)0x00); bs.put((byte)0x01); bs.put((byte)0xFF); bs.put((byte)0x41);
        us.put((char)0x0000);
        
        us.limit(us.position());
        us.position(0);
        bs.limit(bs.position());
        bs.position(0);
        
        try {
            smBufDecode(decoder, "UTF-32BE", bs, us, true, false);
            errln("Overflow exception while decoding UTF32BE (1) should have been thrown.");
        } catch (Exception ex) {
        }
        
        bs.clear();
        us.clear();
        //test malform buffer
        bs.put((byte)0x00); bs.put((byte)0x00); bs.put((byte)0xD9); bs.put((byte)0x02);
        us.put((char)0x0000);
        
        us.limit(us.position());
        us.position(0);
        bs.limit(bs.position());
        bs.position(0);
        
        try {
            smBufDecode(decoder, "UTF-32BE", bs, us, true, false);
            errln("Malform exception while decoding UTF32BE (2) should have been thrown.");
        } catch (Exception ex) {
        }
        
        bs.clear();
        us.clear();
        //test malform buffer
        bs.put((byte)0x00); bs.put((byte)0x00); bs.put((byte)0xFE); bs.put((byte)0xFF);
        bs.put((byte)0x10); bs.put((byte)0xFF); bs.put((byte)0xDF);
        us.put((char)0x0000);
        
        us.limit(us.position());
        us.position(0);
        bs.limit(bs.position());
        bs.position(0);
        
        try {
            // must flush to exhibit malformed behavior
            smBufDecode(decoder, "UTF-32BE", bs, us, true, true);
            errln("Malform exception while decoding UTF32BE (3) should have been thrown.");
        } catch (Exception ex) {
        }
        
        bs.clear();
        us.clear();
        //test overflow buffer
        bs.put((byte)0x00); bs.put((byte)0x00); bs.put((byte)0xFE); bs.put((byte)0xFF);
        bs.put((byte)0x00); bs.put((byte)0x10); bs.put((byte)0xFF); bs.put((byte)0xDD);
        us.put((char)0x0000);
        
        us.limit(us.position());
        us.position(0);
        bs.limit(bs.position());
        bs.position(0);
        
        try {
            smBufDecode(decoder, "UTF-32BE", bs, us, true, false);
            errln("Overflow exception while decoding UTF32BE (4) should have been thrown.");
        } catch (Exception ex) {
        }
        
        bs.clear();
        us.clear();
        //test malform buffer
        bs.put((byte)0x00); bs.put((byte)0x00); bs.put((byte)0xFE); 
        us.put((char)0x0000);
        
        us.limit(us.position());
        us.position(0);
        bs.limit(bs.position());
        bs.position(0);
        
        try {
            // must flush to exhibit malformed behavior
            smBufDecode(decoder, "UTF-32BE", bs, us, true, true);
            errln("Malform exception while decoding UTF32BE (5) should have been thrown.");
        } catch (Exception ex) {
        }
        //end of decode UTF32BE
    }
    
    //provide better code coverage for UTF8
    public void TestCharsetUTF8() {
        CoderResult result = CoderResult.UNDERFLOW;
        CharsetProvider provider = new CharsetProviderICU();       
        CharsetDecoder decoder = provider.charsetForName("UTF-8").newDecoder();
        CharsetEncoder encoder = provider.charsetForName("UTF-8").newEncoder();
        
        CharBuffer us = CharBuffer.allocate(0x10);
        ByteBuffer bs = ByteBuffer.allocate(0x10);
        ByteBuffer bs2;
        CharBuffer us2;
        int limit_us;
        int limit_bs;
        
        //encode and decode using read only buffer
        encoder.reset();
        decoder.reset();
        us.put((char)0x0041); us.put((char)0x0081); us.put((char)0xEF65); us.put((char)0xD902);
        bs.put((byte)0x41); bs.put((byte)0xc2); bs.put((byte)0x81); bs.put((byte)0xee); bs.put((byte)0xbd); bs.put((byte)0xa5);
        bs.put((byte)0x00); 
        limit_us = us.position();
        limit_bs = bs.position();
        
        us.limit(limit_us);
        us.position(0);
        bs.limit(limit_bs);
        bs.position(0);
        bs2 = bs.asReadOnlyBuffer();
        us2 = us.asReadOnlyBuffer();
        
        result = decoder.decode(bs2, us, true);
        if (!result.isUnderflow() || !equals(us, us2)) {
            errln("Error while decoding UTF-8 (1) should not have occured.");
        }
        
        us2.limit(limit_us);
        us2.position(0);
        bs.limit(limit_bs);
        bs.position(0);
        
        result = encoder.encode(us2, bs, true); 
        if (!result.isUnderflow() || !equals(bs, bs2)) {
            errln("Error while encoding UTF-8 (1) should not have occured.");
        }  
        
        us.clear();
        bs.clear();
        
        //test overflow buffer while encoding
        //readonly buffer
        encoder.reset();
        us.put((char)0x0081); us.put((char)0xEF65); 
        bs.put((byte)0x00); bs.put((byte)0x00); bs.put((byte)0x00);
        limit_us = us.position();
        us2 = us.asReadOnlyBuffer();
        us2.limit(limit_us);
        us2.position(0);
        bs.limit(1);
        bs.position(0);
        result = encoder.encode(us2, bs, true);
        if (!result.isOverflow()) {
            errln("Overflow Error should have occured while encoding UTF-8 (2).");
        }
        
        encoder.reset();
        
        us2.limit(limit_us);
        us2.position(1);
        bs.limit(1);
        bs.position(0);
        result = encoder.encode(us2, bs, true);
        if (!result.isOverflow()) {
            errln("Overflow Error should have occured while encoding UTF-8 (3).");
        }
        
        encoder.reset();
        
        us2.limit(limit_us);
        us2.position(1);
        bs.limit(2);
        bs.position(0);
        result = encoder.encode(us2, bs, true);
        if (!result.isOverflow()) {
            errln("Overflow Error should have occured while encoding UTF-8 (4).");
        }
        
        encoder.reset();
        
        us2.limit(limit_us);
        us2.position(0);
        bs.limit(2);
        bs.position(0);
        result = encoder.encode(us2, bs, true);
        if (!result.isOverflow()) {
            errln("Overflow Error should have occured while encoding UTF-8 (5).");
        }   
        
        //not readonly buffer
        encoder.reset();
        
        us.limit(limit_us);
        us.position(0);
        bs.limit(1);
        bs.position(0);
        result = encoder.encode(us, bs, true);
        if (!result.isOverflow()) {
            errln("Overflow Error should have occured while encoding UTF-8 (6).");
        }
        
        encoder.reset();
        
        us.limit(limit_us);
        us.position(0);
        bs.limit(3);
        bs.position(0);
        result = encoder.encode(us, bs, true);
        if (!result.isOverflow()) {
            errln("Overflow Error should have occured while encoding UTF-8 (7).");
        }
        
        encoder.reset();
        
        us.limit(limit_us);
        us.position(1);
        bs.limit(2);
        bs.position(0);
        result = encoder.encode(us, bs, true);
        if (!result.isOverflow()) {
            errln("Overflow Error should have occured while encoding UTF-8 (8).");
        }   
        
        encoder.reset();
        
        us.limit(limit_us + 1);
        us.position(1);
        bs.limit(3);
        bs.position(0);
        result = encoder.encode(us, bs, true);
        if (!result.isOverflow()) {
            errln("Overflow Error should have occured while encoding UTF-8 (9).");
        }  
        
        us.clear();
        bs.clear();
        
        //test encoding 4 byte characters
        encoder.reset();
        us.put((char)0xD902); us.put((char)0xDD02); us.put((char)0x0041);
        bs.put((byte)0x00); bs.put((byte)0x00); bs.put((byte)0x00); bs.put((byte)0x00);
        limit_us = us.position();
        us2 = us.asReadOnlyBuffer();
        us2.limit(limit_us);
        us2.position(0);
        bs.limit(1);
        bs.position(0);
        result = encoder.encode(us2, bs, true);
        if (!result.isOverflow()) {
            errln("Overflow Error should have occured while encoding UTF-8 (10).");
        }
        
        encoder.reset();
        
        us2.limit(limit_us);
        us2.position(0);
        bs.limit(2);
        bs.position(0);
        result = encoder.encode(us2, bs, true);
        if (!result.isOverflow()) {
            errln("Overflow Error should have occured while encoding UTF-8 (11).");
        }
        
        encoder.reset();
        
        us2.limit(limit_us);
        us2.position(0);
        bs.limit(3);
        bs.position(0);
        result = encoder.encode(us2, bs, true);
        if (!result.isOverflow()) {
            errln("Overflow Error should have occured while encoding UTF-8 (12).");
        }
        
        encoder.reset();
        
        us2.limit(limit_us);
        us2.position(0);
        bs.limit(4);
        bs.position(0);
        result = encoder.encode(us2, bs, true);
        if (!result.isOverflow()) {
            errln("Overflow Error should have occured while encoding UTF-8 (13).");
        }
        
        us.clear();
        bs.clear();
        
        //decoding code coverage
        //test malform error
        decoder.reset();
        bs.put((byte)0xC0); bs.put((byte)0xC0);
        us.put((char)0x0000);
        bs2 = bs.asReadOnlyBuffer();
        
        us.limit(1);
        us.position(0);
        bs2.limit(1);
        bs2.position(0);
        
        result = decoder.decode(bs2, us, true);
        result = decoder.flush(us);
        if (!result.isMalformed()) {
            errln("Malform error should have occurred while decoding UTF-8 (1).");
        }    
        
        us.limit(1);
        us.position(0);
        bs2.limit(1);
        bs2.position(0);
        
        decoder.reset();
        
        result = decoder.decode(bs2, us, true);
        us.limit(1);
        us.position(0);
        bs2.limit(2);
        bs2.position(0);
        result = decoder.decode(bs2, us, true);
        if (!result.isMalformed()) {
            errln("Malform error should have occurred while decoding UTF-8 (2).");
        }  
        
        us.clear();
        bs.clear();
        
        //test overflow buffer
        bs.put((byte)0x01); bs.put((byte)0x41);
        us.put((char)0x0000);
        bs2 = bs.asReadOnlyBuffer();
        us.limit(1);
        us.position(0);
        bs2.limit(2);
        bs2.position(0);
        
        result = decoder.decode(bs2, us, true);
        if (!result.isOverflow()) {
            errln("Overflow error should have occurred while decoding UTF-8 (3).");
        }
        
        us.clear();
        bs.clear();
        
        //test malform string
        decoder.reset();
        bs.put((byte)0xF5); bs.put((byte)0xB4); bs.put((byte)0x8A); bs.put((byte)0x8C);
        us.put((char)0x0000);
        bs2 = bs.asReadOnlyBuffer();
        us.limit(1);
        us.position(0);
        bs2.limit(4);
        bs2.position(0);
        
        result = decoder.decode(bs2, us, true);
        if (!result.isMalformed()) {
            errln("Malform error should have occurred while decoding UTF-8 (4).");
        }
        
        bs.clear();
        
        //test overflow
        decoder.reset();
        bs.put((byte)0xF3); bs.put((byte)0xB4); bs.put((byte)0x8A); bs.put((byte)0x8C);
        bs2 = bs.asReadOnlyBuffer();
        us.limit(1);
        us.position(0);
        bs2.limit(4);
        bs2.position(0);
        
        result = decoder.decode(bs2, us, true);
        if (!result.isOverflow()) {
            errln("Overflow error should have occurred while decoding UTF-8 (5).");
        }
        
        //test overflow
        decoder.reset();
        us.limit(2);
        us.position(0);
        bs2.limit(5);
        bs2.position(0);
        
        result = decoder.decode(bs2, us, true);
        if (!result.isOverflow()) {
            errln("Overflow error should have occurred while decoding UTF-8 (5).");
        }
        
        //test overflow
        decoder.reset();
        us.limit(1);
        us.position(0);
        bs.limit(5);
        bs.position(0);
        
        result = decoder.decode(bs, us, true);
        if (!result.isOverflow()) {
            errln("Overflow error should have occurred while decoding UTF-8 (6).");
        }
      
        bs.clear();
        
        //test overflow
        decoder.reset();
        bs.put((byte)0x41); bs.put((byte)0x42);
        us.limit(1);
        us.position(0);
        bs.limit(2);
        bs.position(0);
        
        result = decoder.decode(bs, us, true);
        if (!result.isOverflow()) {
            errln("Overflow error should have occurred while decoding UTF-8 (7).");
        }
        
    }
    
    //provide better code coverage for Charset UTF16
    public void TestCharsetUTF16() {
        CoderResult result = CoderResult.UNDERFLOW;
        CharsetProvider provider = new CharsetProviderICU();       
        CharsetDecoder decoder = provider.charsetForName("UTF-16").newDecoder();
        CharsetEncoder encoder = provider.charsetForName("UTF-16").newEncoder();
        
        CharBuffer us = CharBuffer.allocate(0x10);
        ByteBuffer bs = ByteBuffer.allocate(0x10);
        
        //test flush buffer and malform string
        bs.put((byte)0xFF); 
        us.put((char)0x0000);
        
        us.limit(us.position());
        us.position(0);
        bs.limit(bs.position());
        bs.position(0);
        
        result = decoder.decode(bs, us, true);
        result = decoder.flush(us);
        if (!result.isMalformed()) {
            errln("Malform error while decoding UTF-16 should have occurred.");
        }
        
        us.clear();
        bs.clear();
        
        us.put((char)0xD902); us.put((char)0xDD01); us.put((char)0x0041);
        
        us.limit(1);
        us.position(0);
        bs.limit(4);
        bs.position(0);
        
        result = encoder.encode(us, bs, true);
        us.limit(3);
        us.position(0);
        bs.limit(3);
        bs.position(0);
        result = encoder.encode(us, bs, true);
        if (!result.isOverflow()) {
            errln("Overflow buffer while encoding UTF-16 should have occurred.");
        }   
        
        us.clear();
        bs.clear();
        
        //test overflow buffer
        decoder.reset();
        decoder = provider.charsetForName("UTF-16BE").newDecoder();
        
        bs.put((byte)0xFF); bs.put((byte)0xFE); bs.put((byte)0x41);
        
        us.limit(0);
        us.position(0);
        bs.limit(3);
        bs.position(0);
        
        result = decoder.decode(bs, us, true);
        if (!result.isOverflow()) {
            errln("Overflow buffer while decoding UTF-16 should have occurred.");
        }        
    }
    
    //provide better code coverage for Charset ISO-2022-KR
    public void TestCharsetISO2022KR() {
        CoderResult result = CoderResult.UNDERFLOW;
        CharsetProvider provider = new CharsetProviderICU();       
        CharsetDecoder decoder = provider.charsetForName("ISO-2022-KR").newDecoder();
        
        byte bytearray[] = {
                (byte)0x1b, (byte)0x24, (byte)0x29, (byte)0x43, (byte)0x41, (byte)0x42,
        };
        char chararray[] = {
                (char)0x0041
        };
        ByteBuffer bb = ByteBuffer.wrap(bytearray);
        CharBuffer cb = CharBuffer.wrap(chararray);
        
        result = decoder.decode(bb, cb, true);
        
        if (!result.isOverflow()) {
            errln("Overflow buffer while decoding ISO-2022-KR should have occurred.");
        }
    }
    
    //provide better code coverage for Charset ISO-2022-JP
    public void TestCharsetISO2022JP() {
        CoderResult result = CoderResult.UNDERFLOW;
        CharsetProvider provider = new CharsetProviderICU();       
        CharsetDecoder decoder = provider.charsetForName("ISO-2022-JP-2").newDecoder();
        
        byte bytearray[] = {
                (byte)0x1b, (byte)0x24, (byte)0x28, (byte)0x44, (byte)0x0A, (byte)0x41,
        };
        char chararray[] = {
                (char)0x000A
        };
        ByteBuffer bb = ByteBuffer.wrap(bytearray);
        CharBuffer cb = CharBuffer.wrap(chararray);
        
        result = decoder.decode(bb, cb, true);
        
        if (!result.isOverflow()) {
            errln("Overflow buffer while decoding ISO-2022-KR should have occurred.");
        }
    }
    
    //provide better code coverage for Charset ASCII
    public void TestCharsetASCII() {
        CoderResult result = CoderResult.UNDERFLOW;
        CharsetProvider provider = new CharsetProviderICU();       
        CharsetDecoder decoder = provider.charsetForName("US-ASCII").newDecoder();
        
        byte bytearray[] = {
                (byte)0x41
        };
        char chararray[] = {
                (char)0x0041
        };
        
        ByteBuffer bb = ByteBuffer.wrap(bytearray);
        CharBuffer cb = CharBuffer.wrap(chararray);
        
        result = decoder.decode(bb, cb, true);
        result = decoder.flush(cb);
        
        if (result.isError()) {
            errln("Error occurred while decoding US-ASCII.");
        }
    }
    
    // provide better code coverage for Charset Callbacks
    /* Different aspects of callbacks are being tested including using different context available */
    public void TestCharsetCallbacks() {
        CoderResult result = CoderResult.UNDERFLOW;
        CharsetProvider provider = new CharsetProviderICU();       
        CharsetEncoder encoder = provider.charsetForName("iso-2022-jp").newEncoder();
        CharsetDecoder decoder = provider.charsetForName("iso-2022-jp").newDecoder();
        
        String context3[] = {
                "i",
                "J"
        };
        
        // Testing encoder escape callback
        String context1[] = {
                "J",
                "C",
                "D",
                null
        };
        char chararray[] = {
                (char)0xd122
        };
        ByteBuffer bb = ByteBuffer.allocate(20);
        CharBuffer cb = CharBuffer.wrap(chararray);
        
        ((CharsetEncoderICU)encoder).setFromUCallback(CoderResult.OVERFLOW, CharsetCallback.FROM_U_CALLBACK_ESCAPE, null);  // This callback is not valid.
        for (int i = 0; i < context1.length; i++) {
            encoder.reset();
            cb.position(0);
            bb.position(0);
            ((CharsetEncoderICU)encoder).setFromUCallback(CoderResult.unmappableForLength(1), CharsetCallback.FROM_U_CALLBACK_ESCAPE, context1[i]); // This callback is valid.
            
            result = encoder.encode(cb, bb, true);
            if (result.isError()) {
                errln("Error occurred while testing of callbacks for ISO-2022-JP encoder.");
            }
        }
        
        // Testing encoder skip callback
        for (int i = 0; i < context3.length; i++) {
            encoder.reset();
            cb.position(0);
            bb.position(0);
            ((CharsetEncoderICU)encoder).setFromUCallback(CoderResult.unmappableForLength(1), CharsetCallback.FROM_U_CALLBACK_SKIP, context3[i]); 
            
            result = encoder.encode(cb, bb, true);
            if (result.isError() && i == 0) {
                errln("Error occurred while testing of callbacks for ISO-2022-JP encoder.");
            }
        }
        
        // Testing encoder sub callback
        for (int i = 0; i < context3.length; i++) {
            encoder.reset();
            cb.position(0);
            bb.position(0);
            ((CharsetEncoderICU)encoder).setFromUCallback(CoderResult.unmappableForLength(1), CharsetCallback.FROM_U_CALLBACK_SUBSTITUTE, context3[i]); 
            
            result = encoder.encode(cb, bb, true);
            if (result.isError() && i == 0) {
                errln("Error occurred while testing of callbacks for ISO-2022-JP encoder.");
            }
        }
        
        // Testing decoder escape callback
        String context2[] = {
                "X",
                "C",
                "D",
                null
        };
        byte bytearray[] = {
                (byte)0x1b, (byte)0x2e, (byte)0x43
        };
        bb = ByteBuffer.wrap(bytearray);
        cb = CharBuffer.allocate(20);
        
        ((CharsetDecoderICU)decoder).setToUCallback(CoderResult.OVERFLOW, CharsetCallback.TO_U_CALLBACK_ESCAPE, null);  // This callback is not valid.
        for (int i = 0; i < context2.length; i++) {
            decoder.reset();
            cb.position(0);
            bb.position(0);
            ((CharsetDecoderICU)decoder).setToUCallback(CoderResult.malformedForLength(1), CharsetCallback.TO_U_CALLBACK_ESCAPE, context2[i]); // This callback is valid.
            
            result = decoder.decode(bb, cb, true);
            if (result.isError()) {
                errln("Error occurred while testing of callbacks for ISO-2022-JP decoder.");
            }
        }
        
        // Testing decoder skip callback
        for (int i = 0; i < context3.length; i++) {
            decoder.reset();
            cb.position(0);
            bb.position(0);
            ((CharsetDecoderICU)decoder).setToUCallback(CoderResult.malformedForLength(1), CharsetCallback.TO_U_CALLBACK_SKIP, context3[i]);
            result = decoder.decode(bb, cb, true);
            if (!result.isError()) {
                errln("Error occurred while testing of callbacks for ISO-2022-JP decoder should have occurred.");
            }
        }
    }
    
    // Testing invalid input exceptions
    public void TestInvalidInput() {
        CharsetProvider provider = new CharsetProviderICU();
        Charset charset = provider.charsetForName("iso-2022-jp");
        CharsetEncoder encoder = charset.newEncoder();
        CharsetDecoder decoder = charset.newDecoder();
        
        try {
            encoder.encode(CharBuffer.allocate(10), null, true);
            errln("Illegal argument exception should have been thrown due to null target.");
        } catch (Exception ex) {
        }
        
        try {
            decoder.decode(ByteBuffer.allocate(10), null, true);
            errln("Illegal argument exception should have been thrown due to null target.");
        } catch (Exception ex) {
        }
    }
    
    // Test java canonical names
    public void TestGetICUJavaCanonicalNames() {
        // Ambiguous charset name.
        String javaCName = CharsetProviderICU.getJavaCanonicalName("windows-1250");
        String icuCName = CharsetProviderICU.getICUCanonicalName("Windows-1250");
        if (javaCName == null || icuCName == null) {
            errln("Unable to get Java or ICU canonical name from ambiguous alias");
        }
        
    }
    
    // Port over from ICU4C for test conversion tables (mbcs version 5.x)
    // Provide better code coverage in CharsetMBCS, CharsetDecoderICU, and CharsetEncoderICU.
    public void TestCharsetTestData() {
        CoderResult result = CoderResult.UNDERFLOW;
        String charsetName = "test4";
        CharsetProvider provider = new CharsetProviderICU();
        Charset charset = ((CharsetProviderICU)provider).charsetForName(charsetName, "com/ibm/icu/dev/data/testdata",
                            this.getClass().getClassLoader());
        CharsetEncoder encoder = charset.newEncoder();
        CharsetDecoder decoder = charset.newDecoder();
        
        byte bytearray[] = {
                0x01, 0x02, 0x03, 0x0a,
                0x01, 0x02, 0x03, 0x0b,
                0x01, 0x02, 0x03, 0x0d,
        };
        
        // set the callback for overflow errors
        ((CharsetDecoderICU)decoder).setToUCallback(CoderResult.OVERFLOW, CharsetCallback.TO_U_CALLBACK_STOP, null);
        
        ByteBuffer bb = ByteBuffer.wrap(bytearray);
        CharBuffer cb = CharBuffer.allocate(10);
        
        bb.limit(4);
        cb.limit(1); // Overflow should occur and is expected
        result = decoder.decode(bb, cb, false);
        if (result.isError()) {
            errln("Error occurred while decoding: " + charsetName + " with error: " + result);
        }
        
        bb.limit(8);
        result = decoder.decode(bb, cb, false);
        if (result.isError()) {
            errln("Error occurred while decoding: " + charsetName + " with error: " + result);
        }
        
        bb.limit(12);
        result = decoder.decode(bb, cb, true);
        if (result.isError()) {
            errln("Error occurred while decoding: " + charsetName + " with error: " + result);
        }
        
        char chararray[] = {
                0xDBC4,0xDE34,0xD900,0xDC05,/* \U00101234\U00050005 */
                0xD940,     /* first half of \U00060006 or \U00060007 */
                0xDC07/* second half of \U00060007 */
        };
        
        cb = CharBuffer.wrap(chararray);
        bb = ByteBuffer.allocate(10);
        
        bb.limit(2);
        cb.limit(4);
        result = encoder.encode(cb, bb, false);
        if (result.isError()) {
            errln("Error occurred while encoding: " + charsetName + " with error: " + result);
        }
        cb.limit(5);
        result = encoder.encode(cb, bb, false);
        if (result.isError()) {
            errln("Error occurred while encoding: " + charsetName + " with error: " + result);
        }
        cb.limit(6);
        result = encoder.encode(cb, bb, true);
        if (!result.isError()) {
            errln("Error should have occurred while encoding: " + charsetName);
        }
    }
    
    /* Round trip test of SCSU converter*/
    public void TestSCSUConverter(){
        byte allFeaturesSCSU[]={
            0x41,(byte) 0xdf, 0x12,(byte) 0x81, 0x03, 0x5f, 0x10, (byte)0xdf, 0x1b, 0x03,
            (byte)0xdf, 0x1c,(byte) 0x88,(byte) 0x80, 0x0b, (byte)0xbf,(byte) 0xff,(byte) 0xff, 0x0d, 0x0a,
            0x41, 0x10, (byte)0xdf, 0x12, (byte)0x81, 0x03, 0x5f, 0x10, (byte)0xdf, 0x13,
            (byte)0xdf, 0x14,(byte) 0x80, 0x15, (byte)0xff 
        }; 

        char allFeaturesUTF16[]={
            0x0041, 0x00df, 0x0401, 0x015f, 0x00df, 0x01df, 0xf000, 0xdbff,
            0xdfff, 0x000d, 0x000a, 0x0041, 0x00df, 0x0401, 0x015f, 0x00df,
            0x01df, 0xf000, 0xdbff, 0xdfff
        };

        
        char germanUTF16[]={
            0x00d6, 0x006c, 0x0020, 0x0066, 0x006c, 0x0069, 0x0065, 0x00df, 0x0074
        };

        byte germanSCSU[]={
            (byte)0xd6, 0x6c, 0x20, 0x66, 0x6c, 0x69, 0x65,(byte) 0xdf, 0x74
        };

       char russianUTF16[]={
            0x041c, 0x043e, 0x0441, 0x043a, 0x0432, 0x0430
        };

       byte russianSCSU[]={
            0x12, (byte)0x9c,(byte)0xbe,(byte) 0xc1, (byte)0xba, (byte)0xb2, (byte)0xb0
        };

       char japaneseUTF16[]={
            0x3000, 0x266a, 0x30ea, 0x30f3, 0x30b4, 0x53ef, 0x611b,
            0x3044, 0x3084, 0x53ef, 0x611b, 0x3044, 0x3084, 0x30ea, 0x30f3,
            0x30b4, 0x3002, 0x534a, 0x4e16, 0x7d00, 0x3082, 0x524d, 0x306b,
            0x6d41, 0x884c, 0x3057, 0x305f, 0x300c, 0x30ea, 0x30f3, 0x30b4,
            0x306e, 0x6b4c, 0x300d, 0x304c, 0x3074, 0x3063, 0x305f, 0x308a,
            0x3059, 0x308b, 0x304b, 0x3082, 0x3057, 0x308c, 0x306a, 0x3044,
            0x3002, 0x7c73, 0x30a2, 0x30c3, 0x30d7, 0x30eb, 0x30b3, 0x30f3,
            0x30d4, 0x30e5, 0x30fc, 0x30bf, 0x793e, 0x306e, 0x30d1, 0x30bd,
            0x30b3, 0x30f3, 0x300c, 0x30de, 0x30c3, 0x30af, 0xff08, 0x30de,
            0x30c3, 0x30ad, 0x30f3, 0x30c8, 0x30c3, 0x30b7, 0x30e5, 0xff09,
            0x300d, 0x3092, 0x3001, 0x3053, 0x3088, 0x306a, 0x304f, 0x611b,
            0x3059, 0x308b, 0x4eba, 0x305f, 0x3061, 0x306e, 0x3053, 0x3068,
            0x3060, 0x3002, 0x300c, 0x30a2, 0x30c3, 0x30d7, 0x30eb, 0x4fe1,
            0x8005, 0x300d, 0x306a, 0x3093, 0x3066, 0x8a00, 0x3044, 0x65b9,
            0x307e, 0x3067, 0x3042, 0x308b, 0x3002
        };

        // SCSUEncoder produces a slightly longer result (179B vs. 178B) because of one different choice:
         //it uses an SQn once where a longer look-ahead could have shown that SCn is more efficient 
        byte japaneseSCSU[]={
            0x08, 0x00, 0x1b, 0x4c,(byte) 0xea, 0x16, (byte)0xca, (byte)0xd3,(byte) 0x94, 0x0f, 0x53, (byte)0xef, 0x61, 0x1b, (byte)0xe5,(byte) 0x84,
            (byte)0xc4, 0x0f, (byte)0x53,(byte) 0xef, 0x61, 0x1b, (byte)0xe5, (byte)0x84, (byte)0xc4, 0x16, (byte)0xca, (byte)0xd3, (byte)0x94, 0x08, 0x02, 0x0f,
            0x53, 0x4a, 0x4e, 0x16, 0x7d, 0x00, 0x30, (byte)0x82, 0x52, 0x4d, 0x30, 0x6b, 0x6d, 0x41,(byte) 0x88, 0x4c,
            (byte) 0xe5,(byte) 0x97, (byte)0x9f, 0x08, 0x0c, 0x16,(byte) 0xca,(byte) 0xd3, (byte)0x94, 0x15, (byte)0xae, 0x0e, 0x6b, 0x4c, 0x08, 0x0d,
            (byte) 0x8c, (byte)0xb4, (byte)0xa3,(byte) 0x9f,(byte) 0xca, (byte)0x99, (byte)0xcb,(byte) 0x8b, (byte)0xc2,(byte) 0x97,(byte) 0xcc,(byte) 0xaa,(byte) 0x84, 0x08, 0x02, 0x0e,
            0x7c, 0x73, (byte)0xe2, 0x16, (byte)0xa3,(byte) 0xb7, (byte)0xcb, (byte)0x93, (byte)0xd3,(byte) 0xb4,(byte) 0xc5, (byte)0xdc, (byte)0x9f, 0x0e, 0x79, 0x3e,
            0x06, (byte)0xae, (byte)0xb1, (byte)0x9d,(byte) 0x93, (byte)0xd3, 0x08, 0x0c, (byte)0xbe,(byte) 0xa3, (byte)0x8f, 0x08,(byte) 0x88,(byte) 0xbe,(byte) 0xa3,(byte) 0x8d,
            (byte)0xd3,(byte) 0xa8, (byte)0xa3, (byte)0x97,(byte) 0xc5, 0x17,(byte) 0x89, 0x08, 0x0d, 0x15,(byte) 0xd2, 0x08, 0x01, (byte)0x93, (byte)0xc8,(byte) 0xaa,
            (byte)0x8f, 0x0e, 0x61, 0x1b, (byte)0x99,(byte) 0xcb, 0x0e, 0x4e, (byte)0xba, (byte)0x9f, (byte)0xa1,(byte) 0xae,(byte) 0x93, (byte)0xa8,(byte) 0xa0, 0x08,
            0x02, 0x08, 0x0c, (byte)0xe2, 0x16, (byte)0xa3, (byte)0xb7, (byte)0xcb, 0x0f, 0x4f,(byte) 0xe1,(byte) 0x80, 0x05,(byte) 0xec, 0x60, (byte)0x8d,
            (byte)0xea, 0x06,(byte) 0xd3,(byte) 0xe6, 0x0f,(byte) 0x8a, 0x00, 0x30, 0x44, 0x65,(byte) 0xb9, (byte)0xe4, (byte)0xfe,(byte) 0xe7,(byte) 0xc2, 0x06,
            (byte)0xcb, (byte)0x82
        };
        
        CharsetProviderICU cs = new CharsetProviderICU();
        CharsetICU charset = (CharsetICU)cs.charsetForName("scsu");
        CharsetDecoder decode = charset.newDecoder();
        CharsetEncoder encode = charset.newEncoder();
        
        //String[] codePoints = {"allFeatures", "german","russian","japanese"};
        byte[][] fromUnicode={allFeaturesSCSU,germanSCSU,russianSCSU,japaneseSCSU};
        char[][] toUnicode = {allFeaturesUTF16, germanUTF16,russianUTF16,japaneseUTF16};
        
        for(int i=0;i<4;i++){
            ByteBuffer decoderBuffer = ByteBuffer.wrap(fromUnicode[i]);
            CharBuffer encoderBuffer = CharBuffer.wrap(toUnicode[i]);
                           
            try{
                // Decoding 
                CharBuffer decoderResult = decode.decode(decoderBuffer);
                encoderBuffer.position(0);
                if(!decoderResult.equals(encoderBuffer)){
                    errln("Error occured while decoding "+ charset.name());
                }
                // Encoding 
                ByteBuffer encoderResult = encode.encode(encoderBuffer);
                // RoundTrip Test
                ByteBuffer roundTrip = encoderResult;
                CharBuffer roundTripResult = decode.decode(roundTrip);
                encoderBuffer.position(0);
                if(!roundTripResult.equals(encoderBuffer)){
                    errln("Error occured while encoding "+ charset.name());
                }
                // Test overflow for code coverage reasons
                if (i == 0) {
                    ByteBuffer test = encoderResult;
                    test.position(0);
                    CharBuffer smallBuffer = CharBuffer.allocate(11);
                    decode.reset();
                    CoderResult status = decode.decode(test, smallBuffer, true);
                    if (status != CoderResult.OVERFLOW) {
                        errln("Overflow buffer error should have been thrown.");
                    }
                }
            }catch(Exception e){
                errln("Exception while converting SCSU thrown: " + e);
            }
        }
        
        /* Provide better code coverage */
        /* testing illegal codepoints */
        CoderResult illegalResult = CoderResult.UNDERFLOW;
        CharBuffer illegalDecoderTrgt = CharBuffer.allocate(10);
        
        byte[] illegalDecoderSrc1 = { (byte)0x41, (byte)0xdf, (byte)0x0c };
        decode.reset();
        illegalResult = decode.decode(ByteBuffer.wrap(illegalDecoderSrc1), illegalDecoderTrgt, true);
        if (illegalResult == CoderResult.OVERFLOW || illegalResult == CoderResult.UNDERFLOW) {
            errln("Malformed error should have been returned for decoder " + charset.name());
        }
        /* code coverage test from nucnvtst.c in ICU4C */
        CoderResult ccResult = CoderResult.UNDERFLOW;
        int CCBufSize = 120 * 10;
        ByteBuffer trgt = ByteBuffer.allocate(CCBufSize);
        CharBuffer test = CharBuffer.allocate(CCBufSize);
        String [] ccSrc = {
            "\ud800\udc00", /* smallest surrogate*/
            "\ud8ff\udcff",
            "\udBff\udFff", /* largest surrogate pair*/
            "\ud834\udc00",
            //"\U0010FFFF",
            "Hello \u9292 \u9192 World!",
            "Hell\u0429o \u9292 \u9192 W\u00e4rld!",
            "Hell\u0429o \u9292 \u9292W\u00e4rld!",

            "\u0648\u06c8", /* catch missing reset*/
            "\u0648\u06c8",

            "\u4444\uE001", /* lowest quotable*/
            "\u4444\uf2FF", /* highest quotable*/
            "\u4444\uf188\u4444",
            "\u4444\uf188\uf288",
            "\u4444\uf188abc\u0429\uf288",
            "\u9292\u2222",
            "Hell\u0429\u04230o \u9292 \u9292W\u00e4\u0192rld!",
            "Hell\u0429o \u9292 \u9292W\u00e4rld!",
            "Hello World!123456",
            "Hello W\u0081\u011f\u0082!", /* Latin 1 run*/

            "abc\u0301\u0302",  /* uses SQn for u301 u302*/
            "abc\u4411d",      /* uses SQU*/
            "abc\u4411\u4412d",/* uses SCU*/
            "abc\u0401\u0402\u047f\u00a5\u0405", /* uses SQn for ua5*/
            "\u9191\u9191\u3041\u9191\u3041\u3041\u3000", /* SJIS like data*/
            "\u9292\u2222",
            "\u9191\u9191\u3041\u9191\u3041\u3041\u3000",
            "\u9999\u3051\u300c\u9999\u9999\u3060\u9999\u3065\u3065\u3065\u300c",
            "\u3000\u266a\u30ea\u30f3\u30b4\u53ef\u611b\u3044\u3084\u53ef\u611b\u3044\u3084\u30ea\u30f3\u30b4\u3002",

            "", /* empty input*/
            "\u0000", /* smallest BMP character*/
            "\uFFFF", /* largest BMP character*/

            /* regression tests*/
            "\u6441\ub413\ua733\uf8fe\ueedb\u587f\u195f\u4899\uf23d\u49fd\u0aac\u5792\ufc22\ufc3c\ufc46\u00aa",
            /*"\u00df\u01df\uf000\udbff\udfff\u000d\n\u0041\u00df\u0401\u015f\u00df\u01df\uf000\udbff\udfff",*/
            "\u30f9\u8321\u05e5\u181c\ud72b\u2019\u99c9\u2f2f\uc10c\u82e1\u2c4d\u1ebc\u6013\u66dc\ubbde\u94a5\u4726\u74af\u3083\u55b9\u000c",
            "\u0041\u00df\u0401\u015f",
            "\u9066\u2123abc",
            //"\ud266\u43d7\ue386\uc9c0\u4a6b\u9222\u901f\u7410\ua63f\u539b\u9596\u482e\u9d47\ucfe4\u7b71\uc280\uf26a\u982f\u862a\u4edd\uf513\ufda6\u869d\u2ee0\ua216\u3ff6\u3c70\u89c0\u9576\ud5ec\ubfda\u6cca\u5bb3\ubcea\u554c\u914e\ufa4a\uede3\u2990\ud2f5\u2729\u5141\u0f26\uccd8\u5413\ud196\ubbe2\u51b9\u9b48\u0dc8\u2195\u21a2\u21e9\u00e4\u9d92\u0bc0\u06c5",
            "\uf95b\u2458\u2468\u0e20\uf51b\ue36e\ubfc1\u0080\u02dd\uf1b5\u0cf3\u6059\u7489",
        };
        for (int i = 0; i < ccSrc.length; i++) {
            CharBuffer ubuf = CharBuffer.wrap(ccSrc[i]);
            encode.reset();
            decode.reset();
            trgt.clear();
            test.clear();
            ccResult = encode.encode(ubuf, trgt, true);
            if (ccResult.isError()) {
                errln("Error while encoding " + charset.name() + " in test for code coverage[" + i + "].");
            } else {
                trgt.limit(trgt.position());
                trgt.position(0);
                ccResult = decode.decode(trgt, test, true);
                if (ccResult.isError()) {
                    errln("Error while decoding " + charset.name() + " in test for code coverage[" + i + "].");
                } else {
                    ubuf.position(0);
                    test.limit(test.position());
                    test.position(0);
                    if (!equals(test, ubuf)) {
                        errln("Roundtrip failed for " + charset.name() + " in test for code coverage[" + i + "].");
                    }
                }
            }
        }
        
        /* Monkey test */
        {
            char[] monkeyIn = {
                    0x00A8, 0x3003, 0x3005, 0x2015, 0xFF5E, 0x2016, 0x2026, 0x2018, 0x000D, 0x000A,
                    0x2019, 0x201C, 0x201D, 0x3014, 0x3015, 0x3008, 0x3009, 0x300A, 0x000D, 0x000A,
                    0x300B, 0x300C, 0x300D, 0x300E, 0x300F, 0x3016, 0x3017, 0x3010, 0x000D, 0x000A,
                    0x3011, 0x00B1, 0x00D7, 0x00F7, 0x2236, 0x2227, 0x7FC1, 0x8956, 0x000D, 0x000A,
                    0x9D2C, 0x9D0E, 0x9EC4, 0x5CA1, 0x6C96, 0x837B, 0x5104, 0x5C4B, 0x000D, 0x000A,
                    0x61B6, 0x81C6, 0x6876, 0x7261, 0x4E59, 0x4FFA, 0x5378, 0x57F7, 0x000D, 0x000A,
                    0x57F4, 0x57F9, 0x57FA, 0x57FC, 0x5800, 0x5802, 0x5805, 0x5806, 0x000D, 0x000A,
                    0x580A, 0x581E, 0x6BB5, 0x6BB7, 0x6BBA, 0x6BBC, 0x9CE2, 0x977C, 0x000D, 0x000A,
                    0x6BBF, 0x6BC1, 0x6BC5, 0x6BC6, 0x6BCB, 0x6BCD, 0x6BCF, 0x6BD2, 0x000D, 0x000A,
                    0x6BD3, 0x6BD4, 0x6BD6, 0x6BD7, 0x6BD8, 0x6BDB, 0x6BEB, 0x6BEC, 0x000D, 0x000A,
                    0x6C05, 0x6C08, 0x6C0F, 0x6C11, 0x6C13, 0x6C23, 0x6C34, 0x0041, 0x000D, 0x000A,
                    0x0043, 0x0044, 0x0045, 0x0046, 0x0047, 0x0048, 0x0049, 0x004A, 0x000D, 0x000A,
                    0x004B, 0x004C, 0x004D, 0x004E, 0x004F, 0x0050, 0x0051, 0x0052, 0x000D, 0x000A,
                    0x0053, 0x0054, 0x0055, 0x0056, 0x0057, 0x0058, 0x0059, 0x005A, 0x000D, 0x000A,
                    0x005B, 0x9792, 0x9CCC, 0x9CCD, 0x9CCE, 0x9CCF, 0x9CD0, 0x9CD3, 0x000D, 0x000A,
                    0x9CD4, 0x9CD5, 0x9CD7, 0x9CD8, 0x9CD9, 0x9CDC, 0x9CDD, 0x9CDF, 0x000D, 0x000A,
                    0x9785, 0x9791, 0x00BD, 0x0390, 0x0385, 0x0386, 0x0388, 0x0389, 0x000D, 0x000A,
                    0x038E, 0x038F, 0x0390, 0x0391, 0x0392, 0x0393, 0x0394, 0x0395, 0x000D, 0x000A,
                    0x0396, 0x0397, 0x0398, 0x0399, 0x039A, 0x038A, 0x038C, 0x039C, 0x000D, 0x000A,
                    /* test non-BMP code points */
                    0xD869, 0xDE99, 0xD869, 0xDE9C, 0xD869, 0xDE9D, 0xD869, 0xDE9E, 0xD869, 0xDE9F,
                    0xD869, 0xDEA0, 0xD869, 0xDEA5, 0xD869, 0xDEA6, 0xD869, 0xDEA7, 0xD869, 0xDEA8,
                    0xD869, 0xDEAB, 0xD869, 0xDEAC, 0xD869, 0xDEAD, 0xD869, 0xDEAE, 0xD869, 0xDEAF,
                    0xD869, 0xDEB0, 0xD869, 0xDEB1, 0xD869, 0xDEB3, 0xD869, 0xDEB5, 0xD869, 0xDEB6,
                    0xD869, 0xDEB7, 0xD869, 0xDEB8, 0xD869, 0xDEB9, 0xD869, 0xDEBA, 0xD869, 0xDEBB,
                    0xD869, 0xDEBC, 0xD869, 0xDEBD, 0xD869, 0xDEBE, 0xD869, 0xDEBF, 0xD869, 0xDEC0,
                    0xD869, 0xDEC1, 0xD869, 0xDEC2, 0xD869, 0xDEC3, 0xD869, 0xDEC4, 0xD869, 0xDEC8,
                    0xD869, 0xDECA, 0xD869, 0xDECB, 0xD869, 0xDECD, 0xD869, 0xDECE, 0xD869, 0xDECF,
                    0xD869, 0xDED0, 0xD869, 0xDED1, 0xD869, 0xDED2, 0xD869, 0xDED3, 0xD869, 0xDED4,
                    0xD869, 0xDED5, 0xD800, 0xDC00, 0xD800, 0xDC00, 0xD800, 0xDC00, 0xDBFF, 0xDFFF,
                    0xDBFF, 0xDFFF, 0xDBFF, 0xDFFF,


                    0x4DB3, 0x4DB4, 0x4DB5, 0x4E00, 0x4E00, 0x4E01, 0x4E02, 0x4E03, 0x000D, 0x000A,
                    0x0392, 0x0393, 0x0394, 0x0395, 0x0396, 0x0397, 0x33E0, 0x33E6, 0x000D, 0x000A,
                    0x4E05, 0x4E07, 0x4E04, 0x4E08, 0x4E08, 0x4E09, 0x4E0A, 0x4E0B, 0x000D, 0x000A,
                    0x4E0C, 0x0021, 0x0022, 0x0023, 0x0024, 0xFF40, 0xFF41, 0xFF42, 0x000D, 0x000A,
                    0xFF43, 0xFF44, 0xFF45, 0xFF46, 0xFF47, 0xFF48, 0xFF49, 0xFF4A, 0x000D, 0x000A,
            };
            encode.reset();
            decode.reset();
            CharBuffer monkeyCB = CharBuffer.wrap(monkeyIn);
            try {
                ByteBuffer monkeyBB = encode.encode(monkeyCB); 
                /* CharBuffer monkeyEndResult =*/ decode.decode(monkeyBB);
                
            } catch (Exception ex) {
                errln("Exception thrown while encoding/decoding monkey test in SCSU: " + ex);
            }
        }
        // Test malformed
        {
            char[] malformedSequence = {
                    0xD899, 0xDC7F, 0xDC88, 0xDC88, 0xD888, 0xDDF9
            };
            encode.reset();
            CharBuffer malformedSrc = CharBuffer.wrap(malformedSequence);
            
            try {
                encode.encode(malformedSrc);
                errln("Malformed error should have thrown an exception.");
            } catch (Exception ex) {
            }
        }
        // Test overflow buffer
        {
            ByteBuffer overflowTest = ByteBuffer.wrap(allFeaturesSCSU);
            int sizes[] = { 8, 2, 11 };
            for (int i = 0; i < sizes.length; i++) {
                try {
                    decode.reset();
                    overflowTest.position(0);
                    smBufDecode(decode, "SCSU overflow test", overflowTest, CharBuffer.allocate(sizes[i]), true, false);
                    errln("Buffer overflow exception should have been thrown.");
                } catch (BufferOverflowException ex) {
                } catch (Exception ex) {
                    errln("Buffer overflow exception should have been thrown.");
                }
            }
            
        }
    } 
    
    /* Test for BOCU1 converter*/
    public void TestBOCU1Converter(){
        char expected[]={
                  0xFEFF, 0x0061, 0x0062, 0x0020, // 0 
                  0x0063, 0x0061, 0x000D, 0x000A,

                  0x0020, 0x0000, 0x00DF, 0x00E6, // 8 
                  0x0930, 0x0020, 0x0918, 0x0909,

                  0x3086, 0x304D, 0x0020, 0x3053, // 16 
                  0x4000, 0x4E00, 0x7777, 0x0020, 

                  0x9FA5, 0x4E00, 0xAC00, 0xBCDE, // 24 
                  0x0020, 0xD7A3, 0xDC00, 0xD800,

                  0xD800, 0xDC00, 0xD845, 0xDDDD, // 32 
                  0xDBBB, 0xDDEE, 0x0020, 0xDBFF,

                  0xDFFF, 0x0001, 0x0E40, 0x0020, // 40 
                  0x0009  
        };
        
        byte sampleText[]={ // from cintltst/bocu1tst.c/TestBOCU1 text 1 
            (byte) 0xFB,
            (byte) 0xEE,
            0x28, // from source offset 0
            0x24, 0x1E, 0x52, (byte) 0xB2, 0x20,
            (byte) 0xB3,
            (byte) 0xB1,
            0x0D,
            0x0A,

            0x20, // from 8
            0x00, (byte) 0xD0, 0x6C, (byte) 0xB6, (byte) 0xD8, (byte) 0xA5,
            0x20, 0x68,
            0x59,

            (byte) 0xF9,
            0x28, // from 16
            0x6D, 0x20, 0x73, (byte) 0xE0, 0x2D, (byte) 0xDE, 0x43,
            (byte) 0xD0, 0x33, 0x20,

            (byte) 0xFA,
            (byte) 0x83, // from 24
            0x25, 0x01, (byte) 0xFB, 0x16, (byte) 0x87, 0x4B, 0x16, 0x20,
            (byte) 0xE6, (byte) 0xBD, (byte) 0xEB, 0x5B, 0x4B, (byte) 0xCC,

            (byte) 0xF9,
            (byte) 0xA2, // from 32
            (byte) 0xFC, 0x10, 0x3E, (byte) 0xFE, 0x16, 0x3A, (byte) 0x8C,
            0x20, (byte) 0xFC, 0x03, (byte) 0xAC,

            0x01, /// from 41 
            (byte) 0xDE, (byte) 0x83, 0x20, 0x09 
        };
        
        CharsetProviderICU cs = new CharsetProviderICU();
        CharsetICU charset = (CharsetICU)cs.charsetForName("BOCU-1");
        CharsetDecoder decode = charset.newDecoder();
        CharsetEncoder encode = charset.newEncoder();
       
        ByteBuffer decoderBuffer = ByteBuffer.wrap(sampleText);
        CharBuffer encoderBuffer = CharBuffer.wrap(expected);
        try{
            // Decoding 
            CharBuffer decoderResult = decode.decode(decoderBuffer);
            
            encoderBuffer.position(0);
            if(!decoderResult.equals(encoderBuffer)){
                errln("Error occured while decoding "+ charset.name());
            }
            // Encoding 
            ByteBuffer encoderResult = encode.encode(encoderBuffer);
            // RoundTrip Test
            ByteBuffer roundTrip = encoderResult;
            CharBuffer roundTripResult = decode.decode(roundTrip);
            
            encoderBuffer.position(0);
            if(!roundTripResult.equals(encoderBuffer)){
                errln("Error occured while encoding "+ charset.name());
            }
        }catch(Exception e){
            errln("Exception while converting BOCU-1 thrown: " + e);
        }
    }
    
    /* Test that ICU4C and ICU4J get the same ICU canonical name when given the same alias. */
    public void TestICUCanonicalNameConsistency() {
        String[] alias = {
                "KSC_5601"
        };
        String[] expected = {
                "windows-949-2000"
        };

        for (int i = 0; i < alias.length; i++) {
            String name = CharsetProviderICU.getICUCanonicalName(alias[i]);
            if (!name.equals(expected[i])) {
                errln("The ICU canonical name in ICU4J does not match that in ICU4C. Result: " + name + "Expected: " + expected[i]);
            }
        }
    }
    
    /* Increase code coverage for CharsetICU and CharsetProviderICU*/
    public void TestCharsetICUCodeCoverage() {
        CharsetProviderICU provider = new CharsetProviderICU();

        if (provider.charsetForName("UTF16", null) != null) {
            errln("charsetForName should have returned a null");
        }

        if (CharsetProviderICU.getJavaCanonicalName(null) != null) {
            errln("getJavaCanonicalName should have returned a null when null is given to it.");
        }

        try {
            Charset testCharset = CharsetICU.forNameICU("bogus");
            errln("UnsupportedCharsetException should be thrown for charset \"bogus\" - but got charset " + testCharset.name());
        } catch (UnsupportedCharsetException ex) {
            logln("UnsupportedCharsetException was thrown for CharsetICU.forNameICU(\"bogus\")");
        }

        Charset charset = provider.charsetForName("UTF16");

        try {
            ((CharsetICU)charset).getUnicodeSet(null, 0);
        } catch (IllegalArgumentException ex) {
            return;
        }
        errln("IllegalArgumentException should have been thrown.");
    }
    
    public void TestCharsetLMBCS() {
        String []lmbcsNames = {
                "LMBCS-1",
                "LMBCS-2",
                "LMBCS-3",
                "LMBCS-4",
                "LMBCS-5",
                "LMBCS-6",
                "LMBCS-8",
                "LMBCS-11",
                "LMBCS-16",
                "LMBCS-17",
                "LMBCS-18",
                "LMBCS-19"
        };
        
        char[] src = {
                0x0192, 0x0041, 0x0061, 0x00D0, 0x00F6, 0x0100, 0x0174, 0x02E4, 0x03F5, 0x03FB,
                0x05D3, 0x05D4, 0x05EA, 0x0684, 0x0685, 0x1801, 0x11B3, 0x11E8, 0x1F9A, 0x2EB4,
                0x3157, 0x3336, 0x3304, 0xD881, 0xDC88
        };
        CharBuffer cbInput = CharBuffer.wrap(src);
        
        CharsetProviderICU provider = new CharsetProviderICU();
        
        for (int i = 0; i < lmbcsNames.length; i++) {
            Charset charset = provider.charsetForName(lmbcsNames[i]);
            if (charset == null) {
                errln("Unable to create LMBCS charset: " + lmbcsNames[i]);
                return;
            }
            CharsetEncoder encoder = charset.newEncoder();
            CharsetDecoder decoder = charset.newDecoder();
            
            try {
                cbInput.position(0);
                ByteBuffer bbTmp = encoder.encode(cbInput);
                CharBuffer cbOutput = decoder.decode(bbTmp);
                
                if (!equals(cbInput, cbOutput)) {
                    errln("Roundtrip test failed for charset: " + lmbcsNames[i]);
                }
            } catch (Exception ex) {
                if (i >= 8) {
                    /* Expected exceptions */
                    continue;
                }
                errln("Exception thrown: " + ex + " while using charset: " + lmbcsNames[i]);
            }
            
        }
        
        // Test malformed
        CoderResult malformedResult = CoderResult.UNDERFLOW;
        byte[] malformedBytes = {
                (byte)0x61, (byte)0x01, (byte)0x29, (byte)0x81, (byte)0xa0, (byte)0x0f
        };
        ByteBuffer malformedSrc = ByteBuffer.wrap(malformedBytes);
        CharBuffer malformedTrgt = CharBuffer.allocate(10);
        int[] malformedLimits = {
                2, 6
        };
        CharsetDecoder malformedDecoderTest = provider.charsetForName("LMBCS-1").newDecoder();
        for (int n = 0; n < malformedLimits.length; n++) {
            malformedDecoderTest.reset();
            
            malformedSrc.position(0);
            malformedSrc.limit(malformedLimits[n]);
            
            malformedTrgt.clear();
            
            malformedResult = malformedDecoderTest.decode(malformedSrc,malformedTrgt, true);
            if (!malformedResult.isMalformed()) {
                errln("Malformed error should have resulted.");
            }
        }
    }
    
    /*
     * This is a port of ICU4C TestAmbiguousConverter in cintltst.
     * Since there is no concept of ambiguous converters in ICU4J
     * this test is merely for code coverage reasons.
     */
    public void TestAmbiguousConverter() {
        byte [] inBytes = {
                0x61, 0x5b, 0x5c
        };
        ByteBuffer src = ByteBuffer.wrap(inBytes);
        CharBuffer trgt = CharBuffer.allocate(20);
        
        CoderResult result = CoderResult.UNDERFLOW;
        CharsetProviderICU provider = new CharsetProviderICU();
        String[] names = CharsetProviderICU.getAllNames();
        
        for (int i = 0; i < names.length; i++) {
            Charset charset = provider.charsetForName(names[i]);
            if (charset == null) {
                /* We don't care about any failures because not all converters are available. */
                continue;
            }
            CharsetDecoder decoder = charset.newDecoder();
            
            src.position(0);
            trgt.clear();
            
            result = decoder.decode(src, trgt, true);
            if (result.isError()) {
                /* We don't care about any failures. */
                continue;
            }
        }
    }
}
