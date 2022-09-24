// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.text;

/**
 * Charset recognizer for plain ASCII
 */
class CharsetRecog_ASCII extends CharsetRecognizer {

    @Override
    String getName() {
        return "ASCII";
    }

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
            // However, there could be some encoding (such as Hebrew or ISO-2022) which also has this property.
            // Thus, we have a confidence lower than 100.
            // We could execute the charset detectors of the other languages;
            // if they don't have a hit, we can increase our confidence.
            // However, this would lead to dependencies to outer CharsetRecognizers which is not a well-designed architecture.
            return new CharsetMatch(det, this, 80);
        }
    }
}
