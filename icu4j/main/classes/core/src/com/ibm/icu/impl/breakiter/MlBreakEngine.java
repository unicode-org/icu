// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

package com.ibm.icu.impl.breakiter;

import static com.ibm.icu.impl.CharacterIteration.DONE32;
import static com.ibm.icu.impl.CharacterIteration.current32;
import static com.ibm.icu.impl.CharacterIteration.next32;
import static com.ibm.icu.impl.CharacterIteration.previous32;

import com.ibm.icu.impl.Assert;
import com.ibm.icu.impl.ICUData;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.UResourceBundle;
import com.ibm.icu.util.UResourceBundleIterator;

import java.lang.System;
import java.text.CharacterIterator;
import java.util.ArrayList;
import java.util.HashMap;

public class MlBreakEngine {

    private static final int INVALID = '|';
    private static final String INVALID_STRING = "|";
    private static final int MAX_FEATURE = 26;
    private UnicodeSet fDigitOrOpenPunctuationOrAlphabetSet;
    private UnicodeSet fClosePunctuationSet;
    private HashMap<String, Integer> fModel;

    private int fNegativeSum;

    static class Element {
        private int character;
        private String ublock;

        /**
         * Default constructor.
         */
        public Element() {
            character = 0;
            ublock = null;
        }

        /**
         * Set the character and its unicode block.
         *
         * @param ch  A unicode character.
         * @param str The unicode block of the character.
         */
        public void setCharAndUblock(int ch, String str) {
            Assert.assrt(str.length() <= 3);
            this.character = ch;
            ublock = str;
        }

        /**
         * Get the unicode character.
         *
         * @return The unicode character.
         */
        public int getCharacter() {
            return character;
        }

        /**
         * Get the unicode character's unicode block.
         *
         * @return The unicode block.
         */
        public String getUblock() {
            return ublock;
        }
    }

    private static boolean isValid(Element element) {
        String ublock = element.getUblock();
        return ublock.length() != 1 || (int) ublock.charAt(0) != INVALID;
    }

    /**
     * Constructor for Chinese and Japanese phrase breaking.
     *
     * @param digitOrOpenPunctuationOrAlphabetSet An unicode set with the digit and open punctuation
     *                                            and alphabet.
     * @param closePunctuationSet                 An unicode set with the close punctuation.
     */
    public MlBreakEngine(UnicodeSet digitOrOpenPunctuationOrAlphabetSet,
            UnicodeSet closePunctuationSet) {
        fDigitOrOpenPunctuationOrAlphabetSet = digitOrOpenPunctuationOrAlphabetSet;
        fClosePunctuationSet = closePunctuationSet;
        fModel = new HashMap<String, Integer>();
        fNegativeSum = 0;
        loadMLModel();
    }

    /**
     * Divide up a range of characters handled by this break engine.
     *
     * @param inText        A input text.
     * @param startPos      The start index of the input text.
     * @param endPos        The end index of the input text.
     * @param inString      A input string normalized from inText from startPos to endPos
     * @param numCodePts    The number of code points of inString
     * @param charPositions A map that transforms inString's code point index to code unit index.
     * @param foundBreaks   A list to store the breakpoint.
     * @return The number of breakpoints
     */
    public int divideUpRange(CharacterIterator inText, int startPos, int endPos,
            CharacterIterator inString, int numCodePts, int[] charPositions,
            DictionaryBreakEngine.DequeI foundBreaks) {
        if (startPos >= endPos) {
            return 0;
        }
        ArrayList<Integer> boundary = new ArrayList<Integer>(numCodePts);
        int ch;
        String ublock;
        // The ML model groups six char to evaluate if the 4th char is a breakpoint.
        // Like a sliding window, the elementList removes the first char and appends the new char
        // from inString in each iteration so that its size always remains at six.
        Element elementList[] = new Element[6];
        initElementList(inString, elementList, numCodePts);

        // Add a break for the start.
        boundary.add(0, 0);
        for (int i = 1; i < numCodePts; i++) {
            evaluateBreakpoint(elementList, i, boundary);
            if (i + 1 > numCodePts) {
                break;
            }
            shiftLeftOne(elementList);

            ch = (i + 3) < numCodePts ? next32(inString) : INVALID;
            ublock = (ch != INVALID) ? getUnicodeBlock(ch) : INVALID_STRING;
            elementList[5].setCharAndUblock(ch, ublock);
        }

        // Add a break for the end if there is not one there already.
        if (boundary.get(boundary.size() - 1) != numCodePts) {
            boundary.add(numCodePts);
        }

        int correctedNumBreaks = 0;
        int previous = -1;
        int numBreaks = boundary.size();
        for (int i = 0; i < numBreaks; i++) {
            int pos = charPositions[boundary.get(i)] + startPos;
            // In phrase breaking, there has to be a breakpoint between Cj character and close
            // punctuation.
            // E.g.［携帯電話］正しい選択 -> ［携帯▁電話］▁正しい▁選択 -> breakpoint between ］ and 正
            inText.setIndex(pos);
            if (pos > previous) {
                if (pos != startPos
                        || (pos > 0
                        && fClosePunctuationSet.contains(previous32(inText)))) {
                    foundBreaks.push(pos);
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
            inText.setIndex(endPos);
            int current = current32(inText);
            if (current != DONE32 && !fDigitOrOpenPunctuationOrAlphabetSet.contains(current)) {
                foundBreaks.pop();
                correctedNumBreaks--;
            }

        }
        if (!foundBreaks.isEmpty()) {
            inText.setIndex(foundBreaks.peek());
        }
        return correctedNumBreaks;
    }

    private void shiftLeftOne(Element[] elementList) {
        int length = elementList.length;
        for (int i = 1; i < length; i++) {
            elementList[i - 1].character = elementList[i].character;
            elementList[i - 1].ublock = elementList[i].ublock;
        }
    }

    /**
     * Evaluate whether the index is a potential breakpoint.
     *
     * @param elementList A list including six elements for the breakpoint evaluation.
     * @param index       The breakpoint index to be evaluated.
     * @param boundary    An list including the index of the breakpoint.
     */
    private void evaluateBreakpoint(Element[] elementList, int index, ArrayList<Integer> boundary) {
        String[] featureList = new String[MAX_FEATURE];
        final int w1 = elementList[0].getCharacter();
        final int w2 = elementList[1].getCharacter();
        final int w3 = elementList[2].getCharacter();
        final int w4 = elementList[3].getCharacter();
        final int w5 = elementList[4].getCharacter();
        final int w6 = elementList[5].getCharacter();

        StringBuilder sb = new StringBuilder();
        int idx = 0;
        if (w1 != INVALID) {
            featureList[idx++] = sb.append("UW1:").appendCodePoint(w1).toString();
        }
        if (w2 != INVALID) {
            sb.setLength(0);
            featureList[idx++] = sb.append("UW2:").appendCodePoint(w2).toString();
        }
        if (w3 != INVALID) {
            sb.setLength(0);
            featureList[idx++] = sb.append("UW3:").appendCodePoint(w3).toString();
        }
        if (w4 != INVALID) {
            sb.setLength(0);
            featureList[idx++] = sb.append("UW4:").appendCodePoint(w4).toString();
        }
        if (w5 != INVALID) {
            sb.setLength(0);
            featureList[idx++] = sb.append("UW5:").appendCodePoint(w5).toString();
        }
        if (w6 != INVALID) {
            sb.setLength(0);
            featureList[idx++] = sb.append("UW6:").appendCodePoint(w6).toString();
        }
        if (w2 != INVALID && w3 != INVALID) {
            sb.setLength(0);
            featureList[idx++] = sb.append("BW1:").appendCodePoint(w2).appendCodePoint(
                    w3).toString();
        }
        if (w3 != INVALID && w4 != INVALID) {
            sb.setLength(0);
            featureList[idx++] = sb.append("BW2:").appendCodePoint(w3).appendCodePoint(
                    w4).toString();
        }
        if (w4 != INVALID && w5 != INVALID) {
            sb.setLength(0);
            featureList[idx++] = sb.append("BW3:").appendCodePoint(w4).appendCodePoint(
                    w5).toString();
        }
        if (w1 != INVALID && w2 != INVALID && w3 != INVALID) {
            sb.setLength(0);
            featureList[idx++] = sb.append("TW1:").appendCodePoint(w1).appendCodePoint(
                    w2).appendCodePoint(w3).toString();
        }
        if (w2 != INVALID && w3 != INVALID && w4 != INVALID) {
            sb.setLength(0);
            featureList[idx++] = sb.append("TW2:").appendCodePoint(w2).appendCodePoint(
                    w3).appendCodePoint(w4).toString();
        }
        if (w3 != INVALID && w4 != INVALID && w5 != INVALID) {
            sb.setLength(0);
            featureList[idx++] = sb.append("TW3:").appendCodePoint(w3).appendCodePoint(
                    w4).appendCodePoint(w5).toString();
        }
        if (w4 != INVALID && w5 != INVALID && w6 != INVALID) {
            sb.setLength(0);
            featureList[idx++] = sb.append("TW4:").appendCodePoint(w4).appendCodePoint(
                    w5).appendCodePoint(w6).toString();
        }
        if (isValid(elementList[0])) {
            sb.setLength(0);
            featureList[idx++] = sb.append("UB1:").append(elementList[0].getUblock()).toString();
        }
        if (isValid(elementList[1])) {
            sb.setLength(0);
            featureList[idx++] = sb.append("UB2:").append(elementList[1].getUblock()).toString();
        }
        if (isValid(elementList[2])) {
            sb.setLength(0);
            featureList[idx++] = sb.append("UB3:").append(elementList[2].getUblock()).toString();
        }
        if (isValid(elementList[3])) {
            sb.setLength(0);
            featureList[idx++] = sb.append("UB4:").append(elementList[3].getUblock()).toString();
        }
        if (isValid(elementList[4])) {
            sb.setLength(0);
            featureList[idx++] = sb.append("UB5:").append(elementList[4].getUblock()).toString();
        }
        if (isValid(elementList[5])) {
            sb.setLength(0);
            featureList[idx++] = sb.append("UB6:").append(elementList[5].getUblock()).toString();
        }
        if (isValid(elementList[1]) && isValid(elementList[2])) {
            sb.setLength(0);
            featureList[idx++] = sb.append("BB1:").
                    append(elementList[1].getUblock()).
                    append(elementList[2].getUblock()).toString();
        }
        if (isValid(elementList[2]) && isValid(elementList[3])) {
            sb.setLength(0);
            featureList[idx++] = sb.append("BB2:").
                    append(elementList[2].getUblock()).
                    append(elementList[3].getUblock()).toString();
        }
        if (isValid(elementList[3]) && isValid(elementList[4])) {
            sb.setLength(0);
            featureList[idx++] = sb.append("BB3:").
                    append(elementList[3].getUblock()).
                    append(elementList[4].getUblock()).toString();
        }
        if (isValid(elementList[0]) && isValid(elementList[1]) && isValid(elementList[2])) {
            sb.setLength(0);
            featureList[idx++] = sb.append("TB1:").
                    append(elementList[0].getUblock()).
                    append(elementList[1].getUblock()).
                    append(elementList[2].getUblock()).toString();
        }
        if (isValid(elementList[1]) && isValid(elementList[2]) && isValid(elementList[3])) {
            sb.setLength(0);
            featureList[idx++] = sb.append("TB2:").
                    append(elementList[1].getUblock()).
                    append(elementList[2].getUblock()).
                    append(elementList[3].getUblock()).toString();
        }
        if (isValid(elementList[2]) && isValid(elementList[3]) && isValid(elementList[4])) {
            sb.setLength(0);
            featureList[idx++] = sb.append("TB3:").
                    append(elementList[2].getUblock()).
                    append(elementList[3].getUblock()).
                    append(elementList[4].getUblock()).toString();
        }
        if (isValid(elementList[3]) && isValid(elementList[4]) && isValid(elementList[5])) {
            sb.setLength(0);
            featureList[idx++] = sb.append("TB4:").
                    append(elementList[3].getUblock()).
                    append(elementList[4].getUblock()).
                    append(elementList[5].getUblock()).toString();
        }
        int score = fNegativeSum;
        for (int j = 0; j < idx; j++) {
            if (fModel.containsKey(featureList[j])) {
                score += (2 * fModel.get(featureList[j]));
            }
        }
        if (score > 0) {
            boundary.add(index);
        }
    }

    /**
     * Initialize the element list from the input string.
     *
     * @param inString    A input string to be segmented.
     * @param elementList A list to store the first six characters and their unicode block codes.
     * @param numCodePts  The number of code points of input string
     * @return The number of the code units of the first six characters in inString.
     */
    private int initElementList(CharacterIterator inString, Element[] elementList,
            int numCodePts) {
        int index = 0;
        inString.setIndex(index);
        int w1, w2, w3, w4, w5, w6;
        w1 = w2 = w3 = w4 = w5 = w6 = INVALID;
        if (numCodePts > 0) {
            w3 = current32(inString);
            index += Character.charCount(w3);
        }
        if (numCodePts > 1) {
            w4 = next32(inString);
            index += Character.charCount(w3);
        }
        if (numCodePts > 2) {
            w5 = next32(inString);
            index += Character.charCount(w5);
        }
        if (numCodePts > 3) {
            w6 = next32(inString);
            index += Character.charCount(w6);
        }

        final String b1 = INVALID_STRING;
        final String b2 = b1;
        final String b3 = getUnicodeBlock(w3);
        final String b4 = getUnicodeBlock(w4);
        final String b5 = getUnicodeBlock(w5);
        final String b6 = getUnicodeBlock(w6);

        elementList[0] = new Element();
        elementList[0].setCharAndUblock(w1, b1);
        elementList[1] = new Element();
        elementList[1].setCharAndUblock(w2, b2);
        elementList[2] = new Element();
        elementList[2].setCharAndUblock(w3, b3);
        elementList[3] = new Element();
        elementList[3].setCharAndUblock(w4, b4);
        elementList[4] = new Element();
        elementList[4].setCharAndUblock(w5, b5);
        elementList[5] = new Element();
        elementList[5].setCharAndUblock(w6, b6);

        return index;
    }

    /**
     * Get the character's unicode block code defined in UBlockCode.
     *
     * @param ch A char.
     * @return The unicode block code which is 3 digits with '0' added in the beginning if the code
     * is less than 3 digits.
     */
    private String getUnicodeBlock(int ch) {
        int blockId = UCharacter.UnicodeBlock.of(ch).getID();
        if (blockId == UCharacter.UnicodeBlock.NO_BLOCK.getID()
                || blockId == UCharacter.UnicodeBlock.INVALID_CODE_ID) {
            return INVALID_STRING;
        } else {
            return String.format("%03d", blockId);
        }
    }

    /**
     * Load the machine learning's model file.
     */
    private void loadMLModel() {
        int index = 0;
        UResourceBundle rb = UResourceBundle.getBundleInstance(ICUData.ICU_BRKITR_BASE_NAME,
                "jaml");
        UResourceBundle keyBundle = rb.get("modelKeys");
        UResourceBundle valueBundle = rb.get("modelValues");
        int[] value = valueBundle.getIntVector();
        UResourceBundleIterator iterator = keyBundle.getIterator();
        while (iterator.hasNext()) {
            fNegativeSum -= value[index];
            fModel.put(iterator.nextString(), value[index++]);
        }
    }
}
