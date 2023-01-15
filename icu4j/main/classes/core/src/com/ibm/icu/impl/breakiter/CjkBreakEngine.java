// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2012-2016, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl.breakiter;

import static com.ibm.icu.impl.CharacterIteration.DONE32;
import static com.ibm.icu.impl.CharacterIteration.current32;
import static com.ibm.icu.impl.CharacterIteration.next32;
import static com.ibm.icu.impl.CharacterIteration.previous32;

import java.io.IOException;
import java.text.CharacterIterator;
import java.util.HashSet;

import com.ibm.icu.impl.Assert;
import com.ibm.icu.impl.ICUConfig;
import com.ibm.icu.impl.ICUData;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.util.UResourceBundle;
import com.ibm.icu.util.UResourceBundleIterator;

public class CjkBreakEngine extends DictionaryBreakEngine {
    private UnicodeSet fHangulWordSet;
    private UnicodeSet fDigitOrOpenPunctuationOrAlphabetSet;
    private UnicodeSet fClosePunctuationSet;
    private DictionaryMatcher fDictionary = null;
    private HashSet<String> fSkipSet;
    private MlBreakEngine fMlBreakEngine;
    private boolean isCj = false;

    public CjkBreakEngine(boolean korean) throws IOException {
        fHangulWordSet = new UnicodeSet("[\\uac00-\\ud7a3]");
        fHangulWordSet.freeze();
        // Digit, open punctuation and Alphabetic characters.
        fDigitOrOpenPunctuationOrAlphabetSet = new UnicodeSet("[[:Nd:][:Pi:][:Ps:][:Alphabetic:]]");
        fDigitOrOpenPunctuationOrAlphabetSet.freeze();

        fClosePunctuationSet = new UnicodeSet("[[:Pc:][:Pd:][:Pe:][:Pf:][:Po:]]");
        fClosePunctuationSet.freeze();
        fSkipSet = new HashSet<String>();

        fDictionary = DictionaryData.loadDictionaryFor("Hira");
        if (korean) {
            setCharacters(fHangulWordSet);
        } else { //Chinese and Japanese
            isCj = true;
            UnicodeSet cjSet = new UnicodeSet("[[:Han:][:Hiragana:][:Katakana:]\\u30fc\\uff70\\uff9e\\uff9f]");
            setCharacters(cjSet);
            if (Boolean.parseBoolean(
                    ICUConfig.get("com.ibm.icu.impl.breakiter.useMLPhraseBreaking", "false"))) {
                fMlBreakEngine = new MlBreakEngine(fDigitOrOpenPunctuationOrAlphabetSet,
                        fClosePunctuationSet);
            } else {
                initializeJapanesePhraseParamater();
            }
        }
    }

    private void initializeJapanesePhraseParamater() {
        loadJapaneseExtensions();
        loadHiragana();
    }

    private void loadJapaneseExtensions() {
        UResourceBundle rb = UResourceBundle.getBundleInstance(ICUData.ICU_BRKITR_BASE_NAME, "ja");
        final String tag = "extensions";
        UResourceBundle bundle = rb.get(tag);
        UResourceBundleIterator iterator = bundle.getIterator();
        while (iterator.hasNext()) {
            fSkipSet.add(iterator.nextString());
        }
    }

    private void loadHiragana() {
        UnicodeSet hiraganaWordSet = new UnicodeSet("[:Hiragana:]");
        hiraganaWordSet.freeze();
        UnicodeSetIterator iterator = new UnicodeSetIterator(hiraganaWordSet);
        while (iterator.next()) {
            fSkipSet.add(iterator.getString());
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
            DequeI foundBreaks, boolean isPhraseBreaking) {
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
        // Use ML phrase breaking
        if (Boolean.parseBoolean(
                ICUConfig.get("com.ibm.icu.impl.breakiter.useMLPhraseBreaking", "false"))) {
            // PhraseBreaking is supported in ja and ko; MlBreakEngine only supports ja.
            if (isPhraseBreaking && isCj) {
                return fMlBreakEngine.divideUpRange(inText, startPos, endPos, text,
                        numCodePts, charPositions, foundBreaks);
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
        } else if (isPhraseBreaking) {
            t_boundary[numBreaks] = numCodePts;
            numBreaks++;
            int prevIdx = numCodePts;
            int codeUnitIdx = 0, prevCodeUnitIdx = 0, length = 0;
            for (int i = prev[numCodePts]; i > 0; i = prev[i]) {
                codeUnitIdx = prenormstr.offsetByCodePoints(0, i);
                prevCodeUnitIdx = prenormstr.offsetByCodePoints(0, prevIdx);
                length =  prevCodeUnitIdx - codeUnitIdx;
                prevIdx = i;
                String pattern = getPatternFromText(text, s, codeUnitIdx, length);
                // Keep the breakpoint if the pattern is not in the fSkipSet and continuous Katakana
                // characters don't occur.
                text.setIndex(codeUnitIdx);
                if (!fSkipSet.contains(pattern)
                        && (!isKatakana(current32(text)) || !isKatakana(previous32(text)))) {
                    t_boundary[numBreaks] = i;
                    numBreaks++;
                }
            }
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
        int previous = -1;
        for (int i = numBreaks - 1; i >= 0; i--) {
            int pos = charPositions[t_boundary[i]] + startPos;
            // In phrase breaking, there has to be a breakpoint between Cj character and close
            // punctuation.
            // E.g.［携帯電話］正しい選択 -> ［携帯▁電話］▁正しい▁選択 -> breakpoint between ］ and 正
            inText.setIndex(pos);
            if (pos > previous) {
                if (pos != startPos
                        || (isPhraseBreaking && pos > 0
                        && fClosePunctuationSet.contains(previous32(inText)))) {
                    foundBreaks.push(charPositions[t_boundary[i]] + startPos);
                    correctedNumBreaks++;
                }
            }
            previous = pos;
        }

        if (!foundBreaks.isEmpty() && foundBreaks.peek() == endPos) {
            // In phrase breaking, there has to be a breakpoint between Cj character and
            // the number/open punctuation.
            // E.g. る文字「そうだ、京都」->る▁文字▁「そうだ、▁京都」-> breakpoint between 字 and「
            // E.g. 乗車率９０％程度だろうか -> 乗車▁率▁９０％▁程度だろうか -> breakpoint between 率 and ９
            // E.g. しかもロゴがＵｎｉｃｏｄｅ！ -> しかも▁ロゴが▁Ｕｎｉｃｏｄｅ！-> breakpoint between が and Ｕ
            if (isPhraseBreaking) {
                inText.setIndex(endPos);
                int current = current32(inText);
                if (current != DONE32 && !fDigitOrOpenPunctuationOrAlphabetSet.contains(current)) {
                    foundBreaks.pop();
                    correctedNumBreaks--;
                }
            } else {
                foundBreaks.pop();
                correctedNumBreaks--;
            }
        }
        if (!foundBreaks.isEmpty())
            inText.setIndex(foundBreaks.peek());
        return correctedNumBreaks;
    }

    private String getPatternFromText(CharacterIterator text, StringBuffer sb, int start,
            int length) {
        sb.setLength(0);
        if (length > 0) {
            text.setIndex(start);
            sb.append(text.current());
            for (int i = 1; i < length; i++) {
                sb.append(text.next());
            }
        }
        return sb.toString();
    }
}
