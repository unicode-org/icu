/*
 *******************************************************************************
 * Copyright (C) 2010, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 */
package com.ibm.icu.dev.test.bidi;

import java.io.BufferedReader;
import java.io.IOException;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.test.TestUtil;
import com.ibm.icu.lang.UCharacterDirection;
import com.ibm.icu.text.Bidi;
import com.ibm.icu.text.BidiClassifier;

/**
 * @author Markus W. Scherer
 * BiDi conformance test, using the Unicode BidiTest.txt file.
 * Ported from ICU4C intltest/bidiconf.cpp .
 */
public class BiDiConformanceTest extends TestFmwk {
    public static void main(String[] args) throws Exception {
        new BiDiConformanceTest().run(args);
    }
    public BiDiConformanceTest() {}

    public void TestBidiTest() throws IOException {
        BufferedReader bidiTestFile=TestUtil.getDataReader("unicode/BidiTest.txt");
        Bidi ubidi=new Bidi();
        ubidi.setCustomClassifier(new ConfTestBidiClassifier());
        lineNumber=0;
        levelsCount=0;
        orderingCount=0;
        errorCount=0;
outerLoop:
        while(errorCount<10 && (line=bidiTestFile.readLine())!=null) {
            ++lineNumber;
            lineIndex=0;
            // Remove trailing comments and whitespace.
            int commentStart=line.indexOf('#');
            if(commentStart>=0) {
                line=line.substring(0, commentStart);
            }
            if(!skipWhitespace()) {
                continue;  // Skip empty and comment-only lines.
            }
            if(line.charAt(lineIndex)=='@') {
                ++lineIndex;
                if(line.startsWith("Levels:", lineIndex)) {
                    lineIndex+=7;
                    parseLevels();
                } else if(line.startsWith("Reorder:", lineIndex)) {
                    lineIndex+=8;
                    parseOrdering();
                }
                // Skip unknown @Xyz: ...
            } else {
                parseInputStringFromBiDiClasses();
                if(!skipWhitespace() || line.charAt(lineIndex++)!=';') {
                    errln("missing ; separator on input line "+line);
                    return;
                }
                int bitset=Integer.parseInt(line.substring(lineIndex).trim(), 16);
                // Loop over the bitset.
                for(int i=0; i<=3; ++i) {
                    if((bitset&(1<<i))!=0) {
                        ubidi.setPara(inputString, paraLevels[i], null);
                        byte actualLevels[]=ubidi.getLevels();
                        if(!checkLevels(actualLevels, paraLevelNames[i])) {
                            continue outerLoop;
                        }
                        if(!checkOrdering(ubidi, paraLevelNames[i])) {
                            continue outerLoop;
                        }
                    }
                }
            }
        }
    }
    private static final byte paraLevels[]={
        Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT,
        0,
        1,
        Bidi.DIRECTION_DEFAULT_RIGHT_TO_LEFT
    };
    private static final String paraLevelNames[]={ "auto/LTR", "LTR", "RTL", "auto/RTL" };

    private void parseLevels() {
        directionBits=0;
        levelsCount=0;
        if(skipWhitespace()) {
            String[] levelStrings=line.substring(lineIndex).split("[ \t]+");
            for(String levelString: levelStrings) {
                if(levelString.equals("x")) {
                    levels[levelsCount++]=Bidi.LEVEL_DEFAULT_LTR;
                } else {
                    int value=Integer.parseInt(levelString);
                    if(value<0 || value>(Bidi.MAX_EXPLICIT_LEVEL+1)) {
                        throw new IllegalArgumentException(
                            "@Levels: parse error at "+levelString+" in "+line);
                    }
                    levels[levelsCount++]=(byte)value;
                    directionBits|=(1<<(value&1));
                }
            }
        }
    }
    private void parseOrdering() {
        orderingCount=0;
        if(skipWhitespace()) {
            String[] orderingStrings=line.substring(lineIndex).split("[ \t]+");
            for(String orderingString: orderingStrings) {
                int value=Integer.parseInt(orderingString);
                if(value>=1000) {
                    throw new IllegalArgumentException(
                        "@Reorder: parse error at "+orderingString+" in "+line);
                }
                ordering[orderingCount++]=value;
            }
        }
    }
    private static char charFromBiDiClass[]={
        0x6c,   // 'l' for L
        0x52,   // 'R' for R
        0x33,   // '3' for EN
        0x2d,   // '-' for ES
        0x25,   // '%' for ET
        0x39,   // '9' for AN
        0x2c,   // ',' for CS
        0x2f,   // '/' for B
        0x5f,   // '_' for S
        0x20,   // ' ' for WS
        0x3d,   // '=' for ON
        0x65,   // 'e' for LRE
        0x6f,   // 'o' for LRO
        0x41,   // 'A' for AL
        0x45,   // 'E' for RLE
        0x4f,   // 'O' for RLO
        0x2a,   // '*' for PDF
        0x60,   // '`' for NSM
        0x7c    // '|' for BN
    };
    private class ConfTestBidiClassifier extends BidiClassifier {
        public ConfTestBidiClassifier() {
            super(null);
        }
        @Override
        public int classify(int c) {
            for(int i=0; i<charFromBiDiClass.length; ++i) {
                if(c==charFromBiDiClass[i]) {
                    return i;
                }
            }
            // Character not in our hardcoded table.
            // Should not occur during testing.
            return Bidi.CLASS_DEFAULT;
        }
    }
    private static final int biDiClassNameLengths[]={
        1, 1, 2, 2, 2, 2, 2, 1, 1, 2, 2, 3, 3, 2, 3, 3, 3, 3, 2, 0
    };
    private void parseInputStringFromBiDiClasses() {
        inputStringBuilder.delete(0, 0x7fffffff);
        /*
         * Lengthy but fast BiDi class parser.
         * A simple parser could terminate or extract the name string and use
         *   int32_t biDiClassInt=u_getPropertyValueEnum(UCHAR_BIDI_CLASS, bidiClassString);
         * but that makes this test take significantly more time.
         */
        char c0, c1, c2;
        while(skipWhitespace() && (c0=line.charAt(lineIndex))!=';') {
            int biDiClass=UCharacterDirection.CHAR_DIRECTION_COUNT;
            // Compare each character once until we have a match on
            // a complete, short BiDi class name.
            if(c0=='L') {
                if((lineIndex+2)<line.length() && line.charAt(lineIndex+1)=='R') {
                    if((c2=line.charAt(lineIndex+2))=='E') {
                        biDiClass=UCharacterDirection.LEFT_TO_RIGHT_EMBEDDING;
                    } else if(c2=='O') {
                        biDiClass=UCharacterDirection.LEFT_TO_RIGHT_OVERRIDE;
                    }
                } else {
                    biDiClass=UCharacterDirection.LEFT_TO_RIGHT;
                }
            } else if(c0=='R') {
                if((lineIndex+2)<line.length() && line.charAt(lineIndex+1)=='L') {
                    if((c2=line.charAt(lineIndex+2))=='E') {
                        biDiClass=UCharacterDirection.RIGHT_TO_LEFT_EMBEDDING;
                    } else if(c2=='O') {
                        biDiClass=UCharacterDirection.RIGHT_TO_LEFT_OVERRIDE;
                    }
                } else {
                    biDiClass=UCharacterDirection.RIGHT_TO_LEFT;
                }
            } else if(c0=='E') {
                if((lineIndex+1)>=line.length()) {
                    // too short
                } else if((c1=line.charAt(lineIndex+1))=='N') {
                    biDiClass=UCharacterDirection.EUROPEAN_NUMBER;
                } else if(c1=='S') {
                    biDiClass=UCharacterDirection.EUROPEAN_NUMBER_SEPARATOR;
                } else if(c1=='T') {
                    biDiClass=UCharacterDirection.EUROPEAN_NUMBER_TERMINATOR;
                }
            } else if(c0=='A') {
                if((lineIndex+1)>=line.length()) {
                    // too short
                } else if((c1=line.charAt(lineIndex+1))=='L') {
                    biDiClass=UCharacterDirection.RIGHT_TO_LEFT_ARABIC;
                } else if(c1=='N') {
                    biDiClass=UCharacterDirection.ARABIC_NUMBER;
                }
            } else if(c0=='C' && (lineIndex+1)<line.length() && line.charAt(lineIndex+1)=='S') {
                biDiClass=UCharacterDirection.COMMON_NUMBER_SEPARATOR;
            } else if(c0=='B') {
                if((lineIndex+1)<line.length() && line.charAt(lineIndex+1)=='N') {
                    biDiClass=UCharacterDirection.BOUNDARY_NEUTRAL;
                } else {
                    biDiClass=UCharacterDirection.BLOCK_SEPARATOR;
                }
            } else if(c0=='S') {
                biDiClass=UCharacterDirection.SEGMENT_SEPARATOR;
            } else if(c0=='W' && (lineIndex+1)<line.length() && line.charAt(lineIndex+1)=='S') {
                biDiClass=UCharacterDirection.WHITE_SPACE_NEUTRAL;
            } else if(c0=='O' && (lineIndex+1)<line.length() && line.charAt(lineIndex+1)=='N') {
                biDiClass=UCharacterDirection.OTHER_NEUTRAL;
            } else if(c0=='P' && (lineIndex+2)<line.length() &&
                      line.charAt(lineIndex+1)=='D' && line.charAt(lineIndex+2)=='F') {
                biDiClass=UCharacterDirection.POP_DIRECTIONAL_FORMAT;
            } else if(c0=='N' && (lineIndex+2)<line.length() &&
                      line.charAt(lineIndex+1)=='S' && line.charAt(lineIndex+2)=='M') {
                biDiClass=UCharacterDirection.DIR_NON_SPACING_MARK;
            }
            // Now we verify that the class name is terminated properly,
            // and not just the start of a longer word.
            int biDiClassNameLength=biDiClassNameLengths[biDiClass];
            char c;
            if( biDiClass==UCharacterDirection.CHAR_DIRECTION_COUNT ||
                ((lineIndex+biDiClassNameLength)<line.length() &&
                 !isInvWhitespace(c=line.charAt(lineIndex+biDiClassNameLength)) &&
                 c!=';')
            ) {
                throw new IllegalArgumentException(
                    "BiDi class string not recognized at "+line.substring(lineIndex)+" in "+line);
            }
            inputStringBuilder.append(charFromBiDiClass[biDiClass]);
            lineIndex+=biDiClassNameLength;
        }
        inputString=inputStringBuilder.toString();
    }

    private static char printLevel(byte level) {
        if(level<Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT) {
            return (char)('0'+level);
        } else {
            return 'x';
        }
    }

    private static int getDirectionBits(byte actualLevels[]) {
        int actualDirectionBits=0;
        for(int i=0; i<actualLevels.length; ++i) {
            actualDirectionBits|=(1<<(actualLevels[i]&1));
        }
        return actualDirectionBits;
    }
    private boolean checkLevels(byte actualLevels[], String paraLevelName) {
        boolean isOk=true;
        if(levelsCount!=actualLevels.length) {
            errln("Wrong number of level values; expected "+levelsCount+" actual "+actualLevels.length);
            isOk=false;
        } else {
            for(int i=0; i<actualLevels.length; ++i) {
                if(levels[i]!=actualLevels[i] && levels[i]<Bidi.LEVEL_DEFAULT_LTR) {
                    if(directionBits!=3 && directionBits==getDirectionBits(actualLevels)) {
                        // ICU used a shortcut:
                        // Since the text is unidirectional, it did not store the resolved
                        // levels but just returns all levels as the paragraph level 0 or 1.
                        // The reordering result is the same, so this is fine.
                        break;
                    } else {
                        errln("Wrong level value at index "+i+"; expected levels[i] actual "+actualLevels[i]);
                        isOk=false;
                        break;
                    }
                }
            }
        }
        if(!isOk) {
            printErrorLine(paraLevelName);
            StringBuilder els=new StringBuilder("Expected levels:   ");
            int i;
            for(i=0; i<levelsCount; ++i) {
                els.append(' ').append(printLevel(levels[i]));
            }
            StringBuilder als=new StringBuilder("Actual   levels:   ");
            for(i=0; i<actualLevels.length; ++i) {
                als.append(' ').append(printLevel(actualLevels[i]));
            }
            errln(els.toString());
            errln(als.toString());
        }
        return isOk;
    }

    // Note: ubidi_setReorderingOptions(ubidi, UBIDI_OPTION_REMOVE_CONTROLS);
    // does not work for custom BiDi class assignments
    // and anyway also removes LRM/RLM/ZWJ/ZWNJ which is not desirable here.
    // Therefore we just skip the indexes for BiDi controls while comparing
    // with the expected ordering that has them omitted.
    private boolean checkOrdering(Bidi ubidi, String paraLevelName) {
        boolean isOk=true;
        int resultLength=ubidi.getResultLength();  // visual length including BiDi controls
        int i, visualIndex;
        // Note: It should be faster to call ubidi_countRuns()/ubidi_getVisualRun()
        // and loop over each run's indexes, but that seems unnecessary for this test code.
        for(i=visualIndex=0; i<resultLength; ++i) {
            int logicalIndex=ubidi.getLogicalIndex(i);
            if(levels[logicalIndex]>=Bidi.LEVEL_DEFAULT_LTR) {
                continue;  // BiDi control, omitted from expected ordering.
            }
            if(visualIndex<orderingCount && logicalIndex!=ordering[visualIndex]) {
                errln("Wrong ordering value at visual index "+visualIndex+"; expected "+
                      ordering[visualIndex]+" actual "+logicalIndex);
                isOk=false;
                break;
            }
            ++visualIndex;
        }
        // visualIndex is now the visual length minus the BiDi controls,
        // which should match the length of the BidiTest.txt ordering.
        if(isOk && orderingCount!=visualIndex) {
            errln("Wrong number of ordering values; expected "+orderingCount+" actual "+visualIndex);
            isOk=false;
        }
        if(!isOk) {
            printErrorLine(paraLevelName);
            StringBuilder eord=new StringBuilder("Expected ordering: ");
            for(i=0; i<orderingCount; ++i) {
                eord.append(' ').append((char)('0'+ordering[i]));
            }
            StringBuilder aord=new StringBuilder("Actual   ordering: ");
            for(i=0; i<resultLength; ++i) {
                int logicalIndex=ubidi.getLogicalIndex(i);
                if(levels[logicalIndex]<Bidi.LEVEL_DEFAULT_LTR) {
                    aord.append(' ').append((char)('0'+logicalIndex));
                }
            }
            errln(eord.toString());
            errln(aord.toString());
        }
        return isOk;
    }

    private void printErrorLine(String paraLevelName) {
        ++errorCount;
        errln(String.format("Input line %5d:   %s", lineNumber, line));
        errln("Input string:       "+inputString);
        errln("Para level:         "+paraLevelName);
    }

    private static boolean isInvWhitespace(char c) {
        return ((c)==' ' || (c)=='\t' || (c)=='\r' || (c)=='\n');
    }
    /**
     * Skip isInvWhitespace() characters.
     * @return true if line.charAt[lineIndex] is a non-whitespace, false if lineIndex>=line.length()
     */
    private boolean skipWhitespace() {
        while(lineIndex<line.length()) {
            if(!isInvWhitespace(line.charAt(lineIndex))) {
                return true;
            }
            ++lineIndex;
        }
        return false;
    }

    private String line;
    private int lineIndex;
    private byte levels[]=new byte[1000];  // UBiDiLevel
    private int directionBits;
    private int ordering[]=new int[1000];
    private int lineNumber;
    private int levelsCount;
    private int orderingCount;
    private int errorCount;
    private String inputString;
    private StringBuilder inputStringBuilder=new StringBuilder();
}
