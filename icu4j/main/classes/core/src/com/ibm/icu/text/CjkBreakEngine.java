// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2012-2016, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.text;

import static com.ibm.icu.impl.CharacterIteration.DONE32;
import static com.ibm.icu.impl.CharacterIteration.current32;
import static com.ibm.icu.impl.CharacterIteration.next32;

import java.io.IOException;
import java.text.CharacterIterator;

import com.ibm.icu.impl.Assert;

class CjkBreakEngine extends DictionaryBreakEngine {
    private static final UnicodeSet fHangulWordSet = new UnicodeSet();
    private static final UnicodeSet fHanWordSet = new UnicodeSet();
    private static final UnicodeSet fKatakanaWordSet = new UnicodeSet();
    private static final UnicodeSet fHiraganaWordSet = new UnicodeSet();
    static {
        fHangulWordSet.applyPattern("[\\uac00-\\ud7a3]");
        fHanWordSet.applyPattern("[:Han:]");
        fKatakanaWordSet.applyPattern("[[:Katakana:]\\uff9e\\uff9f]");
        fHiraganaWordSet.applyPattern("[:Hiragana:]");

        // freeze them all
        fHangulWordSet.freeze();
        fHanWordSet.freeze();
        fKatakanaWordSet.freeze();
        fHiraganaWordSet.freeze();
    }

    private DictionaryMatcher fDictionary = null;

    public CjkBreakEngine(boolean korean) throws IOException {
        fDictionary = DictionaryData.loadDictionaryFor("Hira");
        if (korean) {
            setCharacters(fHangulWordSet);
        } else { //Chinese and Japanese
            UnicodeSet cjSet = new UnicodeSet();
            cjSet.addAll(fHanWordSet);
            cjSet.addAll(fKatakanaWordSet);
            cjSet.addAll(fHiraganaWordSet);
            cjSet.add(0xFF70); // HALFWIDTH KATAKANA-HIRAGANA PROLONGED SOUND MARK
            cjSet.add(0x30FC); // KATAKANA-HIRAGANA PROLONGED SOUND MARK
            setCharacters(cjSet);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CjkBreakEngine) {
            CjkBreakEngine other = (CjkBreakEngine)obj;
            return this.fSet.equals(other.fSet);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    private static final int kMaxKatakanaLength = 8;
    private static final int kMaxKatakanaGroupLength = 20;
    private static final int maxSnlp = 255;
    private static final int kint32max = Integer.MAX_VALUE;
    private static int getKatakanaCost(int wordlength) {
        int katakanaCost[] =  new int[] { 8192, 984, 408, 240, 204, 252, 300, 372, 480 };
        return (wordlength > kMaxKatakanaLength) ? 8192 : katakanaCost[wordlength];
    }

    private static boolean isKatakana(int value) {
        return (value >= 0x30A1 && value <= 0x30FE && value != 0x30FB) ||
                (value >= 0xFF66 && value <= 0xFF9F);
    }

    @Override
    public int divideUpDictionaryRange(CharacterIterator inText, int startPos, int endPos,
            DequeI foundBreaks) {
        if (startPos >= endPos) {
            return 0;
        }

        inText.setIndex(startPos);

        int inputLength = endPos - startPos;
        int[] charPositions = new int[inputLength + 1];
        StringBuffer s = new StringBuffer("");
        inText.setIndex(startPos);
        while (inText.getIndex() < endPos) {
            s.append(inText.current());
            inText.next();
        }
        String prenormstr = s.toString();
        boolean isNormalized = Normalizer.quickCheck(prenormstr, Normalizer.NFKC) == Normalizer.YES ||
                               Normalizer.isNormalized(prenormstr, Normalizer.NFKC, 0);
        CharacterIterator text;
        int numCodePts = 0;
        if (isNormalized) {
            text = new java.text.StringCharacterIterator(prenormstr);
            int index = 0;
            charPositions[0] = 0;
            while (index < prenormstr.length()) {
                int codepoint = prenormstr.codePointAt(index);
                index += Character.charCount(codepoint);
                numCodePts++;
                charPositions[numCodePts] = index;
            }
        } else {
            String normStr = Normalizer.normalize(prenormstr, Normalizer.NFKC);
            text = new java.text.StringCharacterIterator(normStr);
            charPositions = new int[normStr.length() + 1];
            Normalizer normalizer = new Normalizer(prenormstr, Normalizer.NFKC, 0);
            int index = 0;
            charPositions[0] = 0;
            while (index < normalizer.endIndex()) {
                normalizer.next();
                numCodePts++;
                index = normalizer.getIndex();
                charPositions[numCodePts] = index;
            }
        }

        // From here on out, do the algorithm. Note that our indices
        // refer to indices within the normalized string.
        int[] bestSnlp = new int[numCodePts + 1];
        bestSnlp[0] = 0;
        for (int i = 1; i <= numCodePts; i++) {
            bestSnlp[i] = kint32max;
        }

        int[] prev = new int[numCodePts + 1];
        for (int i = 0; i <= numCodePts; i++) {
            prev[i] = -1;
        }

        final int maxWordSize = 20;
        int values[] = new int[numCodePts];
        int lengths[] = new int[numCodePts];
        // dynamic programming to find the best segmentation

        // In outer loop, i  is the code point index,
        //                ix is the corresponding code unit index.
        //    They differ when the string contains supplementary characters.
        int ix = 0;
        text.setIndex(ix);
        boolean is_prev_katakana = false;
        for (int i = 0; i < numCodePts; i++, text.setIndex(ix), next32(text)) {
            ix = text.getIndex();
            if (bestSnlp[i] == kint32max) {
                continue;
            }

            int maxSearchLength = (i + maxWordSize < numCodePts) ? maxWordSize : (numCodePts - i);
            int[] count_ = new int[1];
            fDictionary.matches(text, maxSearchLength, lengths, count_, maxSearchLength, values);
            int count = count_[0];

            // if there are no single character matches found in the dictionary
            // starting with this character, treat character as a 1-character word
            // with the highest value possible (i.e. the least likely to occur).
            // Exclude Korean characters from this treatment, as they should be
            // left together by default.
            text.setIndex(ix);  // fDictionary.matches() advances the text position; undo that.
            if ((count == 0 || lengths[0] != 1) && current32(text) != DONE32 && !fHangulWordSet.contains(current32(text))) {
                values[count] = maxSnlp;
                lengths[count] = 1;
                count++;
            }

            for (int j = 0; j < count; j++) {
                int newSnlp = bestSnlp[i] + values[j];
                if (newSnlp < bestSnlp[lengths[j] + i]) {
                    bestSnlp[lengths[j] + i] = newSnlp;
                    prev[lengths[j] + i] = i;
                }
            }

            // In Japanese, single-character Katakana words are pretty rare.
            // So we apply the following heuristic to Katakana: any continuous
            // run of Katakana characters is considered a candidate word with
            // a default cost specified in the katakanaCost table according
            // to its length.
            boolean is_katakana = isKatakana(current32(text));
            if (!is_prev_katakana && is_katakana) {
                int j = i + 1;
                next32(text);
                while (j < numCodePts && (j - i) < kMaxKatakanaGroupLength && isKatakana(current32(text))) {
                    next32(text);
                    ++j;
                }

                if ((j - i) < kMaxKatakanaGroupLength) {
                    int newSnlp = bestSnlp[i] + getKatakanaCost(j - i);
                    if (newSnlp < bestSnlp[j]) {
                        bestSnlp[j] = newSnlp;
                        prev[j] = i;
                    }
                }
            }
            is_prev_katakana = is_katakana;
        }

        int t_boundary[] = new int[numCodePts + 1];
        int numBreaks = 0;
        if (bestSnlp[numCodePts] == kint32max) {
            t_boundary[numBreaks] = numCodePts;
            numBreaks++;
        } else {
            for (int i = numCodePts; i > 0; i = prev[i]) {
                t_boundary[numBreaks] = i;
                numBreaks++;
            }
            Assert.assrt(prev[t_boundary[numBreaks - 1]] == 0);
        }

        if (foundBreaks.size() == 0 || foundBreaks.peek() < startPos) {
            t_boundary[numBreaks++] = 0;
        }

        int correctedNumBreaks = 0;
        for (int i = numBreaks - 1; i >= 0; i--) {
            int pos = charPositions[t_boundary[i]] + startPos;
            if (!(foundBreaks.contains(pos) || pos == startPos)) {
                foundBreaks.push(charPositions[t_boundary[i]] + startPos);
                correctedNumBreaks++;
            }
        }

        if (!foundBreaks.isEmpty() && foundBreaks.peek() == endPos) {
            foundBreaks.pop();
            correctedNumBreaks--;
        }
        if (!foundBreaks.isEmpty())
            inText.setIndex(foundBreaks.peek());
        return correctedNumBreaks;
    }
}
