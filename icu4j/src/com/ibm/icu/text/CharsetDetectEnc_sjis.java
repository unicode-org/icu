/*
 * Created on Apr 12, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.ibm.icu.text;


/**
 *   Shift-JIS encoding scheme recognizer
 *
 */
class CharsetDetectEnc_sjis extends CharsetDetectEncoding {

     boolean nextChar(iteratedChar retChar, CharsetDetector det) {
         retChar.index = retChar.nextIndex;
         retChar.error = false;
         int firstByte;
         firstByte = retChar.charValue = retChar.nextByte(det);
         if (firstByte < 0) {
             return false;
         }
         
         if (firstByte <= 0x7f || (firstByte>0xa0 && firstByte<=0xdf)) {
             return true;
         }
         
         int secondByte = retChar.nextByte(det);
         if (secondByte < 0)  {
             return false;          
         }
         retChar.charValue = firstByte << 8 + secondByte;
         if (! ((secondByte>=0x40 && secondByte<=0x7f) || (secondByte>=0x80 && secondByte<=0xff))) {
             // Illegal second byte value.
             retChar.error = true;
         }
        return true;
    }

}
