/*
 *******************************************************************************
 * Copyright (C) 2002-2007, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.perf;

import java.io.*;

import sun.io.*;
//import com.ibm.icu.converters.*;
import com.ibm.icu.charset.*;
import java.nio.charset.*;
import java.nio.*;

/**
 * @author ram
 */
public class ConverterPerformanceTest extends PerfTest {
   public static void main(String[] args) throws Exception {
       new ConverterPerformanceTest().run(args);
   }
   String fileName=null;
   String srcEncoding=null;
   String testEncoderName=null;
   char unicodeBuffer[] = null;
   byte encBuffer[] = null;

   protected void setup(String[] args) {
        try{
            // We only take 3 arguments file name and encoding,
            if (args.length < 6 ) {
                System.err.println("args.length = " + args.length);
                for (int i=0; i<args.length; i++)
                    System.err.println(" : " + args[i]);
                throw new RuntimeException("Please supply file_name <name> src_encoding <enc> test <converter name>");
            }
            for(int i=0; i<args.length; i++){
                if(args[i].equals("file_name")){
                    fileName = args[++i];
                }
                if(args[i].equals("src_encoding")){
                    srcEncoding = args[++i];
                }
                if(args[i].equals("test")){
                    testEncoderName = args[++i];
                }
            }

            FileInputStream in = new FileInputStream(fileName);
            InputStreamReader reader = new InputStreamReader(in,srcEncoding);
            unicodeBuffer = readToEOS(reader);
            //encBuffer = new String(unicodeBuffer).getBytes(testEncoderName);

            // TODO: should use built in nio converters (this is just for setup, not for the actual performance test)
            CharToByteConverter cbConv = CharToByteConverter.getConverter(testEncoderName);
            cbConv.setSubstitutionMode(false);
            encBuffer = cbConv.convertAll(unicodeBuffer);
        }catch(Exception e){
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }

   }

   PerfTest.Function TestFromUnicodeStream() {
        return new PerfTest.Function() {
            public void call() {
                try{
                    ByteArrayOutputStream out = new ByteArrayOutputStream(unicodeBuffer.length * 10);
                    OutputStreamWriter writer = new OutputStreamWriter(out, testEncoderName);
                    writer.write(unicodeBuffer, 0, unicodeBuffer.length);
                    writer.flush();
                }catch(Exception e){
                    e.printStackTrace();
                    throw new RuntimeException(e.getMessage());
                }
            }
            public long getOperationsPerIteration() {
                return unicodeBuffer.length;
            }
        };
    }
    PerfTest.Function TestToUnicodeStream() {
        return new PerfTest.Function() {
            char[] dst = new char[encBuffer.length];
            public void call() {
                try{
                    ByteArrayInputStream is = new ByteArrayInputStream(encBuffer, 0, encBuffer.length);
                    InputStreamReader reader = new InputStreamReader(is, testEncoderName);
                    reader.read(dst, 0, dst.length);
                    reader.close();
                }catch(Exception e){
                    e.printStackTrace();
                    throw new RuntimeException(e.getMessage());
                }
            }
            public long getOperationsPerIteration() {
                return encBuffer.length;
            }
        };
    }
/*
    PerfTest.Function TestByteToCharConverter() { // decoder  charset.forname().newencoder().decode
        try{
            return new PerfTest.Function() {
                char[] dst = new char[encBuffer.length];
                int numOut =0;
                ByteToCharConverter conv = ByteToCharConverter.getConverter(testEncoderName);
                int num =0;
                public void call() {
                    try{
                        numOut= conv.convert(encBuffer, 0, encBuffer.length, dst, 0,dst.length);
                        conv.reset();
                    }catch(Exception e){
                        e.printStackTrace();
                        throw new RuntimeException(e.getMessage());
                    }
                }
                public long getOperationsPerIteration() {
                    return encBuffer.length;
                }
            };
        }catch(Exception e){
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    PerfTest.Function TestCharToByteConverter() { // encoder charset.forname().newencoder().encode
        try{
            return new PerfTest.Function() {
                byte[] dst = new byte[encBuffer.length];
                int numOut =0;
                CharToByteConverter conv = CharToByteConverter.getConverter(testEncoderName);
                int num =0;
                public void call() {
                    try{
                        numOut= conv.convert(unicodeBuffer, 0,unicodeBuffer.length,dst,0, dst.length);
                        conv.reset();
                    }catch(Exception e){
                        e.printStackTrace();
                        throw new RuntimeException(e.getMessage());
                    }
                }
                public long getOperationsPerIteration() {
                    return unicodeBuffer.length;
                }
            };
        }catch(Exception e){
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    PerfTest.Function TestByteToCharConverterICU() { // decoder  charsetprovidericu.getdecoder
        try{
            return new PerfTest.Function() {
                char[] dst = new char[encBuffer.length];
                int numOut =0;
                ByteToCharConverter conv = ByteToCharConverterICU.createConverter(testEncoderName);
                int num =0;
                public void call() {
                    try{
                        numOut= conv.convert(encBuffer, 0, encBuffer.length, dst, 0,dst.length);
                        conv.reset();
                    }catch(Exception e){
                        e.printStackTrace();
                        throw new RuntimeException(e.getMessage());
                    }
                }
                public long getOperationsPerIteration() {
                    return encBuffer.length;
                }
            };
        }catch(Exception e){
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    PerfTest.Function TestCharToByteConverterICU() {
        try{
            return new PerfTest.Function() {
                byte[] dst = new byte[encBuffer.length*2];
                int numOut =0;
                CharToByteConverter conv = CharToByteConverterICU.createConverter(testEncoderName);
                int num =0;
                public void call() {
                    try{
                        numOut= conv.convert(unicodeBuffer, 0,unicodeBuffer.length,dst,0, dst.length);
                        conv.reset();
                    }catch(Exception e){
                        e.printStackTrace();
                        throw new RuntimeException(e.getMessage());
                    }
                }
                public long getOperationsPerIteration() {
                    return unicodeBuffer.length;
                }
            };
        }catch(Exception e){
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }
*/
    PerfTest.Function TestCharsetDecoder() {
        try{
            return new PerfTest.Function() {
                CharBuffer outBuf = CharBuffer.allocate(unicodeBuffer.length);
                Charset myCharset = Charset.forName(testEncoderName);
                ByteBuffer srcBuf = ByteBuffer.wrap(encBuffer,0,encBuffer.length);
                CharsetDecoder decoder = myCharset.newDecoder();

                public void call() {
                    try{
                        decoder.decode(srcBuf,outBuf,false);
                        decoder.reset();
                        srcBuf.rewind();
                        outBuf.rewind();
                    }catch(Exception e){
                        e.printStackTrace();
                        throw new RuntimeException(e.getMessage());
                    }
                }
                public long getOperationsPerIteration() {
                    return encBuffer.length;
                }
            };
        }catch(Exception e){
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    PerfTest.Function TestCharsetEncoder() {
        try{
            return new PerfTest.Function() {
                ByteBuffer outBuf = ByteBuffer.allocate(encBuffer.length);
                Charset myCharset = Charset.forName(testEncoderName);
                CharBuffer srcBuf = CharBuffer.wrap(unicodeBuffer,0,unicodeBuffer.length);
                CharsetEncoder encoder = myCharset.newEncoder();

                public void call() {
                    try{
                        encoder.encode(srcBuf,outBuf,false);
                        encoder.reset();
                        srcBuf.rewind();
                        outBuf.rewind();
                    }catch(Exception e){
                        e.printStackTrace();
                        throw new RuntimeException(e.getMessage());
                    }
                }
                public long getOperationsPerIteration() {
                    return unicodeBuffer.length;
                }
            };
        }catch(Exception e){
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    PerfTest.Function TestCharsetDecoderICU() {
        try{
            return new PerfTest.Function() {
                CharBuffer outBuf = CharBuffer.allocate(unicodeBuffer.length);
                Charset myCharset = new CharsetProviderICU().charsetForName(testEncoderName);
                ByteBuffer srcBuf = ByteBuffer.wrap(encBuffer,0,encBuffer.length);
                CharsetDecoder decoder = myCharset.newDecoder();

                public void call() {
                    try{
                        decoder.decode(srcBuf,outBuf,false);
                        decoder.reset();
                        srcBuf.rewind();
                        outBuf.rewind();
                    }catch(Exception e){
                        e.printStackTrace();
                        throw new RuntimeException(e.getMessage());
                    }
                }
                public long getOperationsPerIteration() {
                    return encBuffer.length;
                }
            };
        }catch(Exception e){
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    PerfTest.Function TestCharsetEncoderICU() {
        try{
            return new PerfTest.Function() {
                ByteBuffer outBuf = ByteBuffer.allocate(encBuffer.length);
                Charset myCharset = new CharsetProviderICU().charsetForName(testEncoderName);
                CharBuffer srcBuf = CharBuffer.wrap(unicodeBuffer,0,unicodeBuffer.length);
                CharsetEncoder encoder = myCharset.newEncoder();

                public void call() {
                    try{
                        encoder.encode(srcBuf,outBuf,false);
                        encoder.reset();
                        srcBuf.rewind();
                        outBuf.rewind();
                    }catch(Exception e){
                        e.printStackTrace();
                        throw new RuntimeException(e.getMessage());
                    }
                }
                public long getOperationsPerIteration() {
                    return unicodeBuffer.length;
                }
            };
        }catch(Exception e){
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }
}
