/*
 * Created on Apr 12, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.ibm.icu.text;

import java.util.Arrays;

/**
 * CharsetRecognizer implemenation for Asian  - double or multi-byte - charsets.
 *                   Match is determined mostly by the input data adhering to the
 *                   encoding scheme for the charset, and, optionally,
 *                   frequency-of-occurence of characters.
 * <p/>
 *                   Instances of this class are singletons, one per encoding
 *                   being recognized.  They are created in the main
 *                   CharsetDetector class and kept in the global list of available
 *                   encodings to be checked.  The specific encoding being recognized
 *                   is determined by subclass.
 *                   
 */
abstract class CharsetRecog_mbcs extends CharsetRecognizer {

    
     
    /**
     * Get the IANA name of this charset.
     * @return the charset name.
     */
    abstract String      getName() ;
    
    
    /**
     * Test the match of this charset with the input text data
     *      which is obtained via the CharsetDetector object.
     * 
     * @param det  The CharsetDetector, which contains the input text
     *             to be checked for being in this charset.
     * @return     Two values packed into one int  (Damn java, anyhow)
     *             <br/>
     *             bits 0-7:  the match confidence, ranging from 0-100
     *             <br/>
     *             bits 8-15: The match reason, an enum-like value.
     */
    int         match(CharsetDetector det, int [] commonChars) {
        int   singleByteCharCount = 0;
        int   doubleByteCharCount = 0;
        int   commonCharCount     = 0;
        int   badCharCount        = 0;
        int   totalCharCount      = 0;
        iteratedChar   iter       = new iteratedChar();
        
        
        for (iter.reset(); nextChar(iter, det);) {
            totalCharCount++;
            if (iter.error) {
                badCharCount++; 
            } else {
                
                if (iter.charValue <= 0xff) {
                    singleByteCharCount++;
                } else {
                    doubleByteCharCount++;
                    if (commonChars != null) {
                        if (Arrays.binarySearch(commonChars, iter.charValue) >= 0){
                            commonCharCount++;
                        }
                    }
                }
            }
            if (badCharCount >= 2 && badCharCount*5 >= doubleByteCharCount) {
                // Bail out early if the byte data is not matching the encoding scheme.
                return 0;
            }
        }
        
        int confidence = 40 + doubleByteCharCount - 10*badCharCount;
        if (confidence < 0) {
            confidence = 0;
        }
        if (confidence > 100) {
            confidence = 100;
        }
        
        return confidence;
    }
     
     // "Character"  iterated character class.
     //    Recognizers for specific mbcs encodings make their "characters" available
     //    by providing a nextChar() function that fills in an instance of iteratedChar
     //    with the next char from the input.
     //    The returned characters are not converted to Unicode, but remain as the raw
     //    bytes (concatenated into an int) from the codepage data.
     //
     static class iteratedChar {
         int             charValue = 0;             // 1-4 bytes from the raw input data
         int             index     = 0;
         int             nextIndex = 0;
         boolean         error     = false;
         boolean         done      = false;
         
         void reset() {
             charValue = 0;
             index     = -1;
             nextIndex = 0;
             error     = false;
             done      = false;
         }
         
         int nextByte(CharsetDetector det) {
             if (nextIndex >= det.fInputLen) {
                 done = true;
                 return -1;
             }
             int byteValue = (int)det.fInputBytes[nextIndex++] & 0x00ff;
             return byteValue;
         }       
     }
     
     /**
      * Get the next character (however many bytes it is) from the input data
      *    Subclasses for specific charset encodings must implement this function
      *    to get characters according to the rules of their encoding scheme.
      * 
      *  This function is not a method of class iteratedChar only because
      *   that would require a lot of extra derived classes, which is awkward.
      * @param it  The iteratedChar "struct" into which the returned char is placed.
      * @param det The charset detector, which is needed to get at the input byte data
      *            being iterated over.
      * @return    True if a character was returned, false at end of input.
      */
     abstract boolean nextChar(iteratedChar it, CharsetDetector det);
     


     
     
     /**
      *   Shift-JIS charset recognizer.   
      *
      */
     static class CharsetRecog_sjis extends CharsetRecog_mbcs {
         
         boolean nextChar(iteratedChar it, CharsetDetector det) {
             it.index = it.nextIndex;
             it.error = false;
             int firstByte;
             firstByte = it.charValue = it.nextByte(det);
             if (firstByte < 0) {
                 return false;
             }
             
             if (firstByte <= 0x7f || (firstByte>0xa0 && firstByte<=0xdf)) {
                 return true;
             }
             
             int secondByte = it.nextByte(det);
             if (secondByte < 0)  {
                 return false;          
             }
             it.charValue = firstByte << 8 + secondByte;
             if (! ((secondByte>=0x40 && secondByte<=0x7f) || (secondByte>=0x80 && secondByte<=0xff))) {
                 // Illegal second byte value.
                 it.error = true;
             }
             return true;
         }
         
         int match(CharsetDetector det) {
             return match(det, null);
         }
         
         String getName() {
             return "SHIFT_JIS";
         }
         
     }
     
     
     /**
      *   EUC charset recognizers.  One abstract class that provides the common function
      *             for getting the next character according to the EUC encoding scheme,
      *             and nested derived classes for EUC_KR, EUC_JP, EUC_CN.   
      *
      */
     abstract static class CharsetRecog_euc extends CharsetRecog_mbcs {
         
         /*
          *  (non-Javadoc)
          *  Get the next character value for EUC based encodings.
          *  Character "value" is simply the raw bytes that make up the character
          *     packed into an int.
          */
         boolean nextChar(iteratedChar it, CharsetDetector det) {
             it.index = it.nextIndex;
             it.error = false;
             int firstByte  = 0;
             int secondByte = 0;
             int thirdByte  = 0;
             int fourthByte = 0;
             
             buildChar: {
                 firstByte = it.charValue = it.nextByte(det);                 
                 if (firstByte < 0) {
                     // Ran off the end of the input data
                     it.done = true;
                     break buildChar;
                 }
                 if (firstByte <= 0x8d) {
                     // single byte char
                     break buildChar;
                 }
                 
                 secondByte = it.nextByte(det);
                 it.charValue = (it.charValue << 8) | secondByte;
                 
                 if (firstByte >= 0xA1 && firstByte <= 0xfe) {
                     // Two byte Char
                     if (secondByte < 0xa1) {
                         it.error = true;
                     }
                     break buildChar;
                 }
                 if (firstByte == 0x8e) {
                     // Code Set 2.
                     //   In EUC-JP, total char size is 2 bytes, only one byte of actual char value.
                     //   In EUC-TW, total char size is 4 bytes, three bytes contribute to char value.
                     // We don't know which we've got.
                     // Treat it like EUC-JP.  If the data really was EUC-TW, the following two
                     //   bytes will look like a well formed 2 byte char.  
                     if (secondByte < 0xa1) {
                         it.error = true;
                     }
                     break buildChar;                     
                 }
                 
                 if (firstByte == 0x8f) {
                     // Code set 3.
                     // Three byte total char size, two bytes of actual char value.
                     thirdByte    = it.nextByte(det);
                     it.charValue = (it.charValue << 8) | thirdByte;
                     if (thirdByte < 0xa1) {
                         it.error = true;
                     }
                 }
              }
             
             return (it.done == false);
         }
         
         /**
          * The charset recognize for EUC-JP.  A singleton instance of this class
          *    is created and kept by the public CharsetDetector class
          */
         static class CharsetRecog_euc_jp extends CharsetRecog_euc {
             static int [] commonChars = 
                 // TODO:  This set of data comes from the character frequency-
                 //        of-occurence analysis tool.  The data needs to be moved
                 //        into a resource and loaded from there.
                    {0xa4ce, 0xa4c7, 0xa4a4, 0xa4b9, 0xa4b7, 0xa4cb, 0xa1a2, 0xa4c6, 0xa4c8, 
                     0xa4de, 0xa4cf, 0xa1bc, 0xa1a3, 0xa4eb, 0xa4f2, 0xa4ca, 0xa4ac, 0xa4bf, 0xa4ec, 
                     0xa4a6, 0xa4b3, 0xa4ab, 0xa4e2, 0xa5f3, 0xa5c8, 0xa5b9, 0xa5af, 0xa5a4, 0xa4ea};
             
             String getName() {
                 return "EUC_JP";
             }
             
             int match(CharsetDetector det) {
                 return match(det, commonChars);
             }
         }

     
         
         /**
          * The charset recognize for EUC-KR.  A singleton instance of this class
          *    is created and kept by the public CharsetDetector class
          */
         static class CharsetRecog_euc_kr extends CharsetRecog_euc {
             static int [] commonChars = 
                 // TODO:  This set of data comes from the character frequency-
                 //        of-occurence analysis tool.  The data needs to be moved
                 //        into a resource and loaded from there.
                    {0xc0cc, 0xbbe7, 0xc0c7, 0xb1e2, 0xb4eb, 0xbdba, 0xc1f6, 0xbab8, 0xc1a4, 
                     0xbdc3, 0xc7d1, 0xb4d9, 0xbfa1, 0xb4c2, 0xb0a1, 0xc0da, 0xc7cf, 0xbcad, 0xb8ae, 
                     0xc0bb, 0xb0ed, 0xb7ce, 0xc1a6, 0xc0ce, 0xc8b8, 0xbff8, 0xb1b9, 0xbace, 0xb5b5, 
                     0xc0fc, 0xbec6, 0xbfa9, 0xc0cf, 0xb0f8, 0xb5bf, 0xb1b8, 0xbfac, 0xc0fb, 0xbaf1, 
                     0xb1b3, 0xc0a7, 0xc7d8, 0xc7d0, 0xb0fa, 0xc8ad, 0xbcd2, 0xbcf6, 0xbbf3, 0xc0ba, 
                     0xc0b0, 0xbeee, 0xc1d6, 0xb9ae, 0xc0e5, 0xbfeb, 0xb8a6, 0xbcba, 0xc6ae, 0xc0db, 
                     0xb0e8, 0xc0d6};
             
             String getName() {
                 return "EUC_KR";
             }
             
             int match(CharsetDetector det) {
                 return match(det, commonChars);
             }
         }
         
         
         /**
          * The charset recognize for EUC-CN.  A singleton instance of this class
          *    is created and kept by the public CharsetDetector class
          */
         static class CharsetRecog_euc_cn extends CharsetRecog_euc {
             static int [] commonChars = 
                 // TODO:  This set of data comes from the character frequency-
                 //        of-occurence analysis tool.  The data needs to be moved
                 //        into a resource and loaded from there.
                    {0xb5c4, 0xd6d0, 0xa1a4, 0xa1a1, 0xa3ac, 0xcce5, 0xcec4, 0xd1a7, 0xcdf8, 
                     0xb9fa, 0xcbce, 0xc8cb, 0xd3c3, 0xa1a3, 0xd2bb, 0xa3ba, 0xb4f3, 0xbbe1, 0xd0c2, 
                     0xa1a2, 0xd4da, 0xb1a8, 0xb0a9, 0xb7a2, 0xc9cf, 0xd3d0, 0xc9fa, 0xc2db, 0xb1b1, 
                     0xbcfe, 0xc8d5, 0xcab1, 0xbfc9, 0xc7f8, 0xbdcc, 0xbea9, 0xb2bb, 0xb7d6, 0xd2d4, 
                     0xc4ea, 0xd2b3, 0xcfc2, 0xbacd, 0xd7d6, 0xbde1, 0xd0c5, 0xd3fd, 0xc3f1, 0xb8df, 
                     0xd1d0, 0xbfbc, 0xcac7, 0xbcd2, 0xb3c9, 0xd7d4, 0xceaa, 0xc8eb, 0xd0c4, 0xbfc6, 
                     0xd7a8, 0xbfaa, 0xcfa2, 0xbbaf, 0xb8e6, 0xcfdf, 0xd7ca, 0xb6af, 0xb7a8, 0xcaf8, 
                     0xd2bd, 0xd0d0, 0xa1b0, 0xcad0, 0xa1b1, 0xb1be, 0xb7bd, 0xb2e9, 0xcad4, 0xced2, 
                     0xb6e0, 0xb1ed, 0xd5be, 0xc4da, 0xd7f7, 0xd2aa, 0xb8f6, 0xbbaa, 0xc9e7, 0xbead, 
                     0xd5df, 0xc3e6, 0xbbfa, 0xbebf, 0xd2a9, 0xb5bd, 0xb3f6, 0xc0ed, 0xb5e3, 0xcab9, 
                     0xbcd3, 0xc6da, 0xb0b8, 0xd7d3, 0xbac3, 0xb9d8, 0xcec5, 0xc3fb, 0xd5b9, 0xb2bf, 
                     0xb9ab, 0xc1cb, 0xd6ce, 0xb9a4, 0xccec, 0xb9e3, 0xb5d8, 0xd4c2, 0xc7eb, 0xbcbc, 
                     0xb0e6, 0xb5c0, 0xc4dc, 0xd4ba, 0xd3eb, 0xb6a8, 0xb5e7, 0xcef1, 0xcce2, 0xcff2, 
                     0xbaf3, 0xd3d1, 0xc1ac, 0xcae4, 0xcfb5, 0xcae9, 0xd7a2, 0xbdab, 0xd6f7, 0xc8ab, 
                     0xc2eb, 0xbdf0, 0xb6d4, 0xccd8, 0xcee5, 0xceca, 0xc0b4, 0xd2b5, 0xcabe};
             
             String getName() {
                 return "EUC_CN";
             }
             
             int match(CharsetDetector det) {
                 return match(det, commonChars);
             }
         }
     }
     
     
}
