/*
 * Created on Apr 12, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.ibm.icu.text;


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
    int         match(iteratedChar iter, CharsetDetector det) {
        int   singleByteCharCount = 0;
        int   doubleByteCharCount = 0;
        int   badCharCount        = 0;
        int   totalCharCount      = 0;
        
        
        for (iter.reset(); nextChar(iter, det);) {
            totalCharCount++;
            if (iter.error) {
                badCharCount++; 
            } else {
                
                if (iter.charValue <= 0xff) {
                    singleByteCharCount++;
                } else {
                    doubleByteCharCount++;
                }
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
     
     // "Character" iterator class & interface
     //    Recognizers for specific mbcs encodings make their "characters" available
     //    by subclassing this iterator class.   The returned characters are not converted
     //    to Unicode - they are still values that are specific to the encoding - but
     //    multi-byte sequences are combined to form single int values.
     //
     abstract static class iteratedChar {
         int             charValue = 0;             // The char value is a value from the encoding.
                                                    //   It's meaning is not well defined, other than
                                                    //   different encodings
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
             return 0;
         }
         
         String getName() {
             return "SHIFT_JIS";
         }
         
     }
     
     
     /**
      *   EUC charset recognizer.   
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
     }
     
     
}
