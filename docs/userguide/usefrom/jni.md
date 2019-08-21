# Java Native Interface (JNI)

## Overview

ICU4JNI is a subproject of ICU for Java™ (ICU4J). ICU4JNI provides full
conformance with Unicode 3.1.1, enhanced functionality, increased performance,
better cross language, and increased cross platform stability of results.
ICU4JNI also provides greater flexibility, customization, and access to certain
ICU4C native services from Java using the Java Native Interface (JNI).
Currently, the following services are accessible through JNI:

1.  Character Conversion

2.  Collation

3.  Normalization

## Character Conversion

Character conversion is the conversion of bytes in one charset specification to
another. One of the problems in character conversion is that the mappings vary
and are imprecise across various platforms. For example, the results of a
conversion for a Shift-JIS byte stream to Unicode on an IBM® platform will not
match the conversion on a Sun® Solaris platform. This service is useful in a
situation where an application is multi-language and cannot afford differences
in conversion output. It can also be used when an application requires a higher
level of customization and flexibility of character conversion. The requirement
for realizing performance gains is that the buffers passed to the converters
should be large enough to offset the JNI overhead.

Conversion service can be accessed through the following APIs:

CharToByteConverterICU and ByteToCharConverterICU classes in the com.ibm.icu4jni
converters package. These classes inherit from the CharToByteConverter and the
ByteToCharConverter classes in the com.sun.converters package. This interface is
limited in its functionality since the public conversion APIs like String,
InputStream, and OutputStream cannot access ICU's converters unless the
converters are integrated into the Java Virtual Machine (JVM). However, this
requires access to JVM's source code ( please refer to the Readme for more
information). If operations on byte arrays and char arrays can be afforded by
the application (instead of relying on the Java API's conversion routines), then
ICU's classes provide methods to instantiate converter objects and to perform
the conversion. The following example shows this conversion:

    try{
        CharToByteConverter cbConv =
        CharToByteConverterICU.createConverter("gb-18030");
        char\[\] source = { '\\u9001','\\u3005','\\u6458'} ;
        byte\[\] result = new byte\[source.length \* cbConv.getMaxBytesPerChar()\];
        cbConv.convert(source, 0, source.length,result,0,result.length);
    }catch(Exception e){
        ... //do something interesting
    }

The Charset, CharsetEncoderICU, CharsetDecoderICU, and CharsetProviderICU
classes in the com.ibm.icu4jni.charset package. In Java 1.4, a new public API
for character conversions will be added to provide a method for third party
implementers to plug in their converters and enable the other public APIs to use
them as well. ICU4JNI's classes are based on this new character conversion API.
The following example uses ICU4JNI's classes:

    try{
        Charset cs = Charset.forName("gb-18030");
        char\[\] source = { '\\u9001','\\u3005','\\u6458'} ;
        CharBuffer cb = CharBuffer.wrap(source);
        ByteBuffer result = cs.encode(cb)
    }catch(Exception e){
        ... //do something interesting
    }
    ByteBuffer bb = ByteBuffer.allocate(cs.newEncoder().maxBytesPerChar()));
    try{
        Charset cs = Charset.forName("gb-18030");
        CharsetEncoder encoder = cs.newEncoder();
        char\[\] source = { '\\u9001','\\u3005','\\u6458'} ;
        CharBuffer cb = CharBuffer.wrap(source);
        ByteBuffer bb = ByteBuffer.allocate(cs.newEncoder().maxBytesPerChar()));
        for (i=0; i<=temp.length; i++) {
            cb.limit(i);
            CoderResult result = encoder.encode(cb,bb,false);
        }
    }catch(Exception e){
        ... //do something interesting
    }

For more information on character conversion, see the ICU
[Conversion](../conversion/index.md) chapter.

## Collation

[Collation ](../collation/index.md) service provided by ICU is fully Unicode
Collation Algorithm (UCA) and ISO 14651 compliant. The following lists some of
the advantages of the ICU collation service over Java:

The following demonstrates how to create a collator:

    try{
        Collator coll = Collator.createInstance(Locale("en", "US"));
    }catch(ParseException e){
        ... //do something interesting
    }

The following demonstrates how to compare strings:

    try{
        Collator coll = Collator.createInstance(Locale("th", "TH"));
        String jp1 = new String("\\u0e01");
        String jp2 = new String("\\u0e01\\u0e01");
        if(coll.compare(jp1,jp2)==Collator.RESULT_LESS){
            ...//compare succeeded do something
        }else{
            ...//failed do something
        }
    }catch(ParseException e){
        ... //do something interesting
    }

## Normalization

Normalization converts text into a unique, equivalent form. Systems can
normalize Unicode-encoded text into one particular sequence, such as normalizing
composite character sequences into pre-composed characters. The semantics and
use are similar to ICU4J Normalization service, except for character iteration
functionality.

The following demonstrates how to use a normalizer:

    try{
        String source = "\\u00e0ardvark";
        String decomposed = "a\\u0300ardvark";
        String composed = "\\u00e0ardvark";
        If(Normalizer.normalize(source,Normalizer.UNORM_NFC).equals(composed){
            ...// do something interesting
        }
        if(Normalizer.normalize(source,Normalizer.UNORM_NFD).equals(decomposed){
            ...// do something interesting
        }
    }catch(ParseException e){
        ... //do something interesting
    }
