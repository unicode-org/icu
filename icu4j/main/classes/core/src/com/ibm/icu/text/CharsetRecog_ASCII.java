// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.text;

/**
 * Charset recognizer for plain ASCII
 */
class CharsetRecog_ASCII extends com.ibm.icu.text.CharsetRecognizer {

    @Override
    String getName() {
        return "ASCII";
    }

    /* (non-Javadoc)
     * @see com.ibm.icu.text.CharsetRecognizer#match(com.ibm.icu.text.CharsetDetector)
     */
    @Override
    CharsetMatch match(CharsetDetector det) {
        // code similar to determining com.ibm.icu.text.CharsetDetector.fC1Bytes
        boolean highestBitSet = false;
        for (int i = 0x80; i <= 0xFF; i += 1) {
            if (det.fByteStats[i] != 0) {
                highestBitSet = true;
                break;
            }
        }

        if (highestBitSet) {
            // non-ASCII, because (at least) one byte in the stream is >= 128
            return null;
        } else {
            // ASCII, because ALL bytes in the stream are <= 127.
            // However, there could be some unicode (such as Hebrew) which also has this property.
            // Thus, we have confidence of 35.
            return new CharsetMatch(det, this, 35);
        }
    }
}
