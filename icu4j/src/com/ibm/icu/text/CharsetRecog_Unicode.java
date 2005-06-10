/*
 *******************************************************************************
 * Copyright (C) 1996-2005, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 */

package com.ibm.icu.text;

/**
 * @author emader
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public abstract class CharsetRecog_Unicode extends CharsetRecognizer {

    /* (non-Javadoc)
     * @see com.ibm.icu.text.CharsetRecognizer#getName()
     */
    abstract String getName();

    /* (non-Javadoc)
     * @see com.ibm.icu.text.CharsetRecognizer#match(com.ibm.icu.text.CharsetDetector)
     */
    abstract int match(CharsetDetector det);
    
    static class CharsetRecog_UTF_16_BE extends CharsetRecog_Unicode
    {
        String getName()
        {
            return "UTF-16BE";
        }
        
        int match(CharsetDetector det)
        {
            byte[] input = det.fRawInput;
            
            if ((input[0] & 0xFF) == 0xFE && (input[1] & 0xFF) == 0xFF) {
                return 100;
            }
            
            // TODO: Do some statastics to check for unsigned UTF-16BE
            return 0;
        }
    }
    
    static class CharsetRecog_UTF_16_LE extends CharsetRecog_Unicode
    {
        String getName()
        {
            return "UTF-16LE";
        }
        
        int match(CharsetDetector det)
        {
            byte[] input = det.fRawInput;
            
            if ((input[0] & 0xFF) == 0xFF && (input[1] & 0xFF) == 0xFE && (input[2] != 0x00 || input[3] != 0x00)) {
                return 100;
            }
            
            // TODO: Do some statastics to check for unsigned UTF-16LE
            return 0;
        }
    }
    
    static class CharsetRecog_UTF_32_BE extends CharsetRecog_Unicode
    {
        String getName()
        {
            return "UTF-32BE";
        }
        
        int match(CharsetDetector det)
        {
            byte[] input   = det.fRawInput;
            int limit      = (det.fRawLength / 4) * 4;
            int numValid   = 0;
            int numInvalid = 0;
            boolean hasBOM = false;
            int confidence = 0;
            
            if (input[0] == 0x00 && input[1] == 0x00 && (input[2] & 0xFF) == 0xFE && (input[3] & 0xFF) == 0xFF) {
                hasBOM = true;
            }
            
            for(int i = 0; i < limit; i += 4) {
                int ch = (input[i + 0] & 0xFF) << 24 | (input[i + 1] & 0xFF << 16) |
                         (input[i + 2] & 0xFF) <<  8 | (input[i + 2] & 0xFF);
                
                if (ch < 0 || ch >= 0x10FFFF) {
                    numInvalid += 1;
                } else {
                    numValid += 1;
                }
            }
            
            
            // Cook up some sort of confidence score, based on presense of a BOM
            //    and the existence of valid and/or invalid multi-byte sequences.
            if (hasBOM && numInvalid==0) {
                confidence = 100;
            } else if (hasBOM && numValid > numInvalid*10) {
                confidence = 80;
            } else if (numValid > 3 && numInvalid == 0) {
                confidence = 100;            
            } else if (numValid > 0 && numInvalid == 0) {
                confidence = 80;
            } else if (numValid > numInvalid*10) {
                // Probably corruput UTF-32BE data.  Valid sequences aren't likely by chance.
                confidence = 25;
            }
            
            return confidence;
        }
    }

    
    static class CharsetRecog_UTF_32_LE extends CharsetRecog_Unicode
    {
        String getName()
        {
            return "UTF-32LE";
        }
        
        int match(CharsetDetector det)
        {
            byte[] input   = det.fRawInput;
            int limit      = (det.fRawLength / 4) * 4;
            int numValid   = 0;
            int numInvalid = 0;
            boolean hasBOM = false;
            int confidence = 0;
            
            if (input[3] == 0x00 && input[2] == 0x00 && (input[1] & 0xFF) == 0xFE && (input[0] & 0xFF) == 0xFF) {
                hasBOM = true;
            }
            
            for(int i = 0; i < limit; i += 4) {
                int ch = (input[i + 3] & 0xFF) << 24 | (input[i + 2] & 0xFF << 16) |
                         (input[i + 1] & 0xFF) <<  8 | (input[i + 0] & 0xFF);
                
                if (ch < 0 || ch > 0x10FFFF) {
                    numInvalid += 1;
                } else {
                    numValid += 1;
                }
            }
            
            
            // Cook up some sort of confidence score, based on presense of a BOM
            //    and the existence of valid and/or invalid multi-byte sequences.
            if (hasBOM && numInvalid==0) {
                confidence = 100;
            } else if (hasBOM && numValid > numInvalid*10) {
                confidence = 80;
            } else if (numValid > 3 && numInvalid == 0) {
                confidence = 100;            
            } else if (numValid > 0 && numInvalid == 0) {
                confidence = 80;
            } else if (numValid > numInvalid*10) {
                // Probably corruput UTF-32BE data.  Valid sequences aren't likely by chance.
                confidence = 25;
            }
            
            return confidence;
        }
    }
}
