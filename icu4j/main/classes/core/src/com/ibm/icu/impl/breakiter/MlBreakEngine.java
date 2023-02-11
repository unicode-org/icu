// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

package com.ibm.icu.impl.breakiter;

import static com.ibm.icu.impl.CharacterIteration.DONE32;
import static com.ibm.icu.impl.CharacterIteration.current32;
import static com.ibm.icu.impl.CharacterIteration.next32;
import static com.ibm.icu.impl.CharacterIteration.previous32;

import com.ibm.icu.impl.ICUData;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.UResourceBundle;
import com.ibm.icu.util.UResourceBundleIterator;

import java.text.CharacterIterator;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

enum ModelIndex {
    kUWStart(0), kBWStart(6), kTWStart(9);
    private final int value;

    private ModelIndex(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}

public class MlBreakEngine {
    // {UW1, UW2, ... UW6, BW1, ... BW3, TW1, TW2, ... TW4} 6+3+4= 13
    private static final int MAX_FEATURE = 13;
    private UnicodeSet fDigitOrOpenPunctuationOrAlphabetSet;
    private UnicodeSet fClosePunctuationSet;
    private List<HashMap<String, Integer>> fModel;
    private int fNegativeSum;

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
        fModel = new ArrayList<HashMap<String, Integer>>(MAX_FEATURE);
        for (int i = 0; i < MAX_FEATURE; i++) {
            fModel.add(new HashMap<String, Integer>());
        }
        fNegativeSum = 0;
        loadMLModel();
    }

    /**
     * Divide up a range of characters handled by this break engine.
     *
     * @param inText          An input text.
     * @param startPos        The start index of the input text.
     * @param endPos          The end index of the input text.
     * @param inString        A input string normalized from inText from startPos to endPos
     * @param codePointLength The number of code points of inString
     * @param charPositions   A map that transforms inString's code point index to code unit index.
     * @param foundBreaks     A list to store the breakpoint.
     * @return The number of breakpoints
     */
    public int divideUpRange(CharacterIterator inText, int startPos, int endPos,
            CharacterIterator inString, int codePointLength, int[] charPositions,
            DictionaryBreakEngine.DequeI foundBreaks) {
        if (startPos >= endPos) {
            return 0;
        }
        ArrayList<Integer> boundary = new ArrayList<Integer>(codePointLength);
        String inputStr = transform(inString);
        // The ML algorithm groups six char and evaluates whether the 4th char is a breakpoint.
        // In each iteration, it evaluates the 4th char and then moves forward one char like
        // sliding window. Initially, the first six values in the indexList are
        // [-1, -1, 0, 1, 2, 3]. After moving forward, finally the last six values in the indexList
        // are [length-4, length-3, length-2, length-1, -1, -1]. The "+4" here means four extra
        // "-1".
        int indexSize = codePointLength + 4;
        int indexList[] = new int[indexSize];
        int numCodeUnits = initIndexList(inString, indexList, codePointLength);

        // Add a break for the start.
        boundary.add(0, 0);

        for (int idx = 0; idx + 1 < codePointLength; idx++) {
            evaluateBreakpoint(inputStr, indexList, idx, numCodeUnits, boundary);
            if (idx + 4 < codePointLength) {
                indexList[idx + 6] = numCodeUnits;
                numCodeUnits += Character.charCount(next32(inString));
            }
        }

        // Add a break for the end if there is not one there already.
        if (boundary.get(boundary.size() - 1) != codePointLength) {
            boundary.add(codePointLength);
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

    /**
     * Transform a CharacterIterator into a String.
     */
    private String transform(CharacterIterator inString) {
        StringBuilder sb = new StringBuilder();
        inString.setIndex(0);
        for (char c = inString.first(); c != CharacterIterator.DONE; c = inString.next()) {
            sb.append(c);
        }
        return sb.toString();
    }

    /**
     * Evaluate whether the breakpointIdx is a potential breakpoint.
     *
     * @param inputStr     An input string to be segmented.
     * @param indexList    A code unit index list of the inputStr.
     * @param startIdx     The start index of the indexList.
     * @param numCodeUnits The current code unit boundary of the indexList.
     * @param boundary     A list including the index of the breakpoint.
     */
    private void evaluateBreakpoint(String inputStr, int[] indexList, int startIdx,
            int numCodeUnits, ArrayList<Integer> boundary) {
        int start = 0, end = 0;
        int score = fNegativeSum;

        for (int i = 0; i < 6; i++) {
            // UW1 ~ UW6
            start = startIdx + i;
            if (indexList[start] != -1) {
                end = (indexList[start + 1] != -1) ? indexList[start + 1] : numCodeUnits;
                score += fModel.get(ModelIndex.kUWStart.getValue() + i).getOrDefault(
                        inputStr.substring(indexList[start], end), 0);
            }
        }
        for (int i = 0; i < 3; i++) {
            // BW1 ~ BW3
            start = startIdx + i + 1;
            if (indexList[start] != -1 && indexList[start + 1] != -1) {
                end = (indexList[start + 2] != -1) ? indexList[start + 2] : numCodeUnits;
                score += fModel.get(ModelIndex.kBWStart.getValue() + i).getOrDefault(
                        inputStr.substring(indexList[start], end), 0);
            }
        }
        for (int i = 0; i < 4; i++) {
            // TW1 ~ TW4
            start = startIdx + i;
            if (indexList[start] != -1
                    && indexList[start + 1] != -1
                    && indexList[start + 2] != -1) {
                end = (indexList[start + 3] != -1) ? indexList[start + 3] : numCodeUnits;
                score += fModel.get(ModelIndex.kTWStart.getValue() + i).getOrDefault(
                        inputStr.substring(indexList[start], end), 0);
            }
        }
        if (score > 0) {
            boundary.add(startIdx + 1);
        }
    }

    /**
     * Initialize the index list from the input string.
     *
     * @param inString        An input string to be segmented.
     * @param indexList       A code unit index list of the inString.
     * @param codePointLength The number of code points of the input string
     * @return The number of the code units of the first six characters in inString.
     */
    private int initIndexList(CharacterIterator inString, int[] indexList, int codePointLength) {
        int index = 0;
        inString.setIndex(index);
        Arrays.fill(indexList, -1);
        if (codePointLength > 0) {
            indexList[2] = 0;
            index += Character.charCount(current32(inString));
            if (codePointLength > 1) {
                indexList[3] = index;
                index += Character.charCount(next32(inString));
                if (codePointLength > 2) {
                    indexList[4] = index;
                    index += Character.charCount(next32(inString));
                    if (codePointLength > 3) {
                        indexList[5] = index;
                        index += Character.charCount(next32(inString));
                    }
                }
            }
        }
        return index;
    }

    /**
     * Load the machine learning's model file.
     */
    private void loadMLModel() {
        int index = 0;
        UResourceBundle rb = UResourceBundle.getBundleInstance(ICUData.ICU_BRKITR_BASE_NAME,
                "jaml");
        initKeyValue(rb, "UW1Keys", "UW1Values", fModel.get(index++));
        initKeyValue(rb, "UW2Keys", "UW2Values", fModel.get(index++));
        initKeyValue(rb, "UW3Keys", "UW3Values", fModel.get(index++));
        initKeyValue(rb, "UW4Keys", "UW4Values", fModel.get(index++));
        initKeyValue(rb, "UW5Keys", "UW5Values", fModel.get(index++));
        initKeyValue(rb, "UW6Keys", "UW6Values", fModel.get(index++));
        initKeyValue(rb, "BW1Keys", "BW1Values", fModel.get(index++));
        initKeyValue(rb, "BW2Keys", "BW2Values", fModel.get(index++));
        initKeyValue(rb, "BW3Keys", "BW3Values", fModel.get(index++));
        initKeyValue(rb, "TW1Keys", "TW1Values", fModel.get(index++));
        initKeyValue(rb, "TW2Keys", "TW2Values", fModel.get(index++));
        initKeyValue(rb, "TW3Keys", "TW3Values", fModel.get(index++));
        initKeyValue(rb, "TW4Keys", "TW4Values", fModel.get(index++));
        fNegativeSum /= 2;
    }

    /**
     * In the machine learning's model file, specify the name of the key and value to load the
     * corresponding feature and its score.
     *
     * @param rb        A RedouceBundle corresponding to the model file.
     * @param keyName   The kay name in the model file.
     * @param valueName The value name in the model file.
     * @param map       A HashMap to store the pairs of the feature and its score.
     */
    private void initKeyValue(UResourceBundle rb, String keyName, String valueName,
            HashMap<String, Integer> map) {
        int idx = 0;
        UResourceBundle keyBundle = rb.get(keyName);
        UResourceBundle valueBundle = rb.get(valueName);
        int[] value = valueBundle.getIntVector();
        UResourceBundleIterator iterator = keyBundle.getIterator();
        while (iterator.hasNext()) {
            fNegativeSum -= value[idx];
            map.put(iterator.nextString(), value[idx++]);
        }
    }
}
