/*
*******************************************************************************
* Copyright (C) 2005, International Business Machines Corporation and         *
* others. All Rights Reserved.                                                *
*******************************************************************************
*/
package com.ibm.icu.text;

/**
 * @author andy
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
abstract class CharsetDetectEncoding {
    
    static class iteratedChar {
        int             charValue = 0;
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
     
    
    abstract boolean nextChar(iteratedChar retChar, CharsetDetector det);
    
  }
