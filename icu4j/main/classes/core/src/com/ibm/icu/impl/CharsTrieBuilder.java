/*
*******************************************************************************
*   Copyright (C) 2011, International Business Machines
*   Corporation and others.  All Rights Reserved.
*******************************************************************************
*   created on: 2011jan07
*   created by: Markus W. Scherer
*   ported from ICU4C ucharstriebuilder/.cpp
*/

package com.ibm.icu.impl;

import java.nio.CharBuffer;
import java.util.ArrayList;

/**
 * Builder class for CharsTrie.
 *
 * <p>This class is not intended for public subclassing.
 *
 * @author Markus W. Scherer
 */
public final class CharsTrieBuilder extends StringTrieBuilder {
    /**
     * Constructs an empty builder.
     */
    public CharsTrieBuilder() {}

    /**
     * Adds a (string, value) pair.
     * The string must be unique.
     * The string contents will be copied; the builder does not keep
     * a reference to the input CharSequence.
     * @param s The input string.
     * @param value The value associated with this char sequence.
     * @return this
     */
    public CharsTrieBuilder add(CharSequence s, int value) {
        if(charsLength>0) {
            // Cannot add elements after building.
            throw new IllegalStateException("Cannot add (string, value) pairs after build().");
        }
        // Binary search in the sorted elements array to find
        // a) whether the string is a duplicate, and, if not,
        // b) the insertion index.
        int start=0, limit=elements.size();
        while(start<limit) {
            int i=(start+limit)/2;
            int diff=elements.get(i).compareStringTo(s, strings);
            if(diff<0) {  // elements[i]<sequence
                start=i+1;
            } else if(diff==0) {
                throw new IllegalArgumentException("Duplicate string.");
            } else {  // elements[i]>s
                limit=i;
            }
        }
        elements.add(start, new Element(s, value, strings));
        return this;
    }

    /**
     * Builds a CharsTrie for the add()ed data.
     * Once built, no further data can be add()ed until clear() is called.
     *
     * <p>Multiple calls to build() or buildCharSequence() return tries or sequences
     * which share the builder's char array, without rebuilding.
     * After clear() has been called, a new array will be used.
     * @param buildOption Build option, see StringTrieBuilder.Option.
     * @return A new CharsTrie for the add()ed data.
     */
    public CharsTrie build(StringTrieBuilder.Option buildOption) {
        return new CharsTrie(buildCharSequence(buildOption), 0);
    }

    /**
     * Builds a CharsTrie for the add()ed data and char-serializes it.
     * Once built, no further data can be add()ed until clear() is called.
     *
     * <p>Multiple calls to build() or buildCharSequence() return tries or sequences
     * which share the builder's char array, without rebuilding.
     * After clear() has been called, a new array will be used.
     * @param buildOption Build option, see StringTrieBuilder.Option.
     * @return A CharSequence with the char-serialized CharsTrie for the add()ed data.
     */
    public CharSequence buildCharSequence(StringTrieBuilder.Option buildOption) {
        buildImpl(buildOption);
        return CharBuffer.wrap(chars, chars.length-charsLength, charsLength);
    }

    public void buildImpl(StringTrieBuilder.Option buildOption) {
        if(charsLength>0) {
            // Already built.
            return;
        }
        if(elements.isEmpty()) {
            throw new IndexOutOfBoundsException("No (string, value) pairs were added.");
        }
        // Create and char-serialize the trie for the elements.
        int capacity=strings.length();
        if(capacity<1024) {
            capacity=1024;
        }
        if(chars==null || chars.length<capacity) {
            chars=new char[capacity];
        }
        build(buildOption, elements.size());
    }

    /**
     * Removes all (string, value) pairs.
     * New data can then be add()ed and a new trie can be built.
     * @return this
     */
    public CharsTrieBuilder clear() {
        strings.setLength(0);
        elements.clear();
        chars=null;
        charsLength=0;
        return this;
    }

    private static final class Element {
        public Element(CharSequence s, int val, StringBuilder strings) {
            int length=s.length();
            if(length>0xffff) {
                // Too long: We store the length in 1 unit.
                throw new IndexOutOfBoundsException("The maximum string length is 0xffff.");
            }
            stringOffset=strings.length();
            strings.append((char)length);
            value=val;
            strings.append(s);
        }

        public int getStringLength(CharSequence strings) /*const*/ {
            return strings.charAt(stringOffset);
        }

        public char charAt(int index, CharSequence strings) /*const*/ {
            return strings.charAt(stringOffset+1+index);
        }

        public int getValue() /*const*/ { return value; }

        public int compareStringTo(CharSequence other, CharSequence strings) /*const*/ {
            // TODO: Add CharSequence comparison function to UTF16 class.
            int thisOffset=stringOffset;
            int thisLength=strings.charAt(thisOffset++);
            int otherLength=other.length();
            int lengthDiff=thisLength-otherLength;
            int commonLength;
            if(lengthDiff<=0) {
                commonLength=thisLength;
            } else {
                commonLength=otherLength;
            }
            int otherOffset=0;
            while(commonLength>0) {
                int diff=(int)strings.charAt(thisOffset++)-(int)other.charAt(otherOffset++);
                if(diff!=0) {
                    return diff;
                }
                --commonLength;
            }
            return lengthDiff;
        }

        private int getStringOffset(CharSequence strings) /*const*/ { return stringOffset+1; }

        // The first strings unit contains the string length.
        // (Compared with a stringLength field here, this saves 2 bytes per string.)
        private int stringOffset;
        private int value;
    }

    protected int getElementStringLength(int i) /*const*/ {
        return elements.get(i).getStringLength(strings);
    }
    protected char getElementUnit(int i, int unitIndex) /*const*/ {
        return elements.get(i).charAt(unitIndex, strings);
    }
    protected int getElementValue(int i) /*const*/ {
        return elements.get(i).getValue();
    }

    protected int getLimitOfLinearMatch(int first, int last, int unitIndex) /*const*/ {
        Element firstElement=elements.get(first);
        Element lastElement=elements.get(last);
        int minStringLength=firstElement.getStringLength(strings);
        while(++unitIndex<minStringLength &&
                firstElement.charAt(unitIndex, strings)==
                lastElement.charAt(unitIndex, strings)) {}
        return unitIndex;
    }

    protected int countElementUnits(int start, int limit, int unitIndex) /*const*/ {
        int length=0;  // Number of different units at unitIndex.
        int i=start;
        do {
            starts_.add(i);
            char unit=elements.get(i++).charAt(unitIndex, strings);
            while(i<limit && unit==elements.get(i).charAt(unitIndex, strings)) {
                ++i;
            }
            ++length;
        } while(i<limit);
        return length;
    }

    protected boolean matchNodesCanHaveValues() /*const*/ { return true; }

    protected int getMaxBranchLinearSubNodeLength() /*const*/ { return CharsTrie.kMaxBranchLinearSubNodeLength; }
    protected int getMinLinearMatch() /*const*/ { return CharsTrie.kMinLinearMatch; }
    protected int getMaxLinearMatchLength() /*const*/ { return CharsTrie.kMaxLinearMatchLength; }

    private static final class UCTLinearMatchNode extends LinearMatchNode {
        public UCTLinearMatchNode(int offset, int len, Node nextNode, CharsTrieBuilder b) {
            super(len, nextNode);
            ctBuilder=b;
            stringOffset=offset;
            hash=hash*37+b.stringHashCode(offset, len);
        }
        public boolean equals(Object other) /*const*/ {
            if(this==other) {
                return true;
            }
            if(!super.equals(other)) {
                return false;
            }
            UCTLinearMatchNode o=(UCTLinearMatchNode)other;
            return ctBuilder.stringsAreEqual(stringOffset, length, o.stringOffset, o.length);
        }
        public void write(StringTrieBuilder builder) {
            CharsTrieBuilder b=(CharsTrieBuilder)builder;
            next.write(builder);
            b.write(stringOffset, length);
            offset=b.writeValueAndType(hasValue, value, b.getMinLinearMatch()+length-1);
        }

        private CharsTrieBuilder ctBuilder;
        private int stringOffset;
    };

    protected Node createLinearMatchNode(int i, int unitIndex, int length,
                                        Node nextNode) /*const*/ {
        return new UCTLinearMatchNode(
                elements.get(i).getStringOffset(strings)+unitIndex,
                length,
                nextNode,
                this);
    }

    private void ensureCapacity(int length) {
        if(length>chars.length) {
            int newCapacity=chars.length;
            do {
                newCapacity*=2;
            } while(newCapacity<=length);
            char[] newChars=new char[newCapacity];
            System.arraycopy(chars, chars.length-charsLength,
                             newChars, newChars.length-charsLength, charsLength);
            chars=newChars;
        }
    }
    protected int write(int unit) {
        int newLength=charsLength+1;
        ensureCapacity(newLength);
        charsLength=newLength;
        chars[chars.length-charsLength]=(char)unit;
        return charsLength;
    }
    private int write(int offset, int length) {
        int newLength=charsLength+length;
        ensureCapacity(newLength);
        charsLength=newLength;
        int charsOffset=chars.length-charsLength;
        while(length>0) {
            chars[charsOffset++]=strings.charAt(offset++);
            --length;
        }
        return charsLength;
    }
    private int write(char[] s, int length) {
        int newLength=charsLength+length;
        ensureCapacity(newLength);
        charsLength=newLength;
        System.arraycopy(s, 0, chars, chars.length-charsLength, length);
        return charsLength;
    }
    protected int writeElementUnits(int i, int unitIndex, int length) {
        return write(elements.get(i).getStringOffset(strings)+unitIndex, length);
    }

    // For writeValueAndFinal(), writeValueAndType() and writeDeltaTo().
    private final char[] intUnits=new char[3];

    protected int writeValueAndFinal(int i, boolean isFinal) {
        if(0<=i && i<=CharsTrie.kMaxOneUnitValue) {
            return write(i|(isFinal ? CharsTrie.kValueIsFinal : 0));
        }
        int length;
        if(i<0 || i>CharsTrie.kMaxTwoUnitValue) {
            intUnits[0]=(char)(CharsTrie.kThreeUnitValueLead);
            intUnits[1]=(char)(i>>16);
            intUnits[2]=(char)i;
            length=3;
        // } else if(i<=CharsTrie.kMaxOneUnitValue) {
        //     intUnits[0]=(char)(i);
        //     length=1;
        } else {
            intUnits[0]=(char)(CharsTrie.kMinTwoUnitValueLead+(i>>16));
            intUnits[1]=(char)i;
            length=2;
        }
        intUnits[0]=(char)(intUnits[0]|(isFinal ? CharsTrie.kValueIsFinal : 0));
        return write(intUnits, length);
    }
    protected int writeValueAndType(boolean hasValue, int value, int node) {
        if(!hasValue) {
            return write(node);
        }
        int length;
        if(value<0 || value>CharsTrie.kMaxTwoUnitNodeValue) {
            intUnits[0]=(char)(CharsTrie.kThreeUnitNodeValueLead);
            intUnits[1]=(char)(value>>16);
            intUnits[2]=(char)value;
            length=3;
        } else if(value<=CharsTrie.kMaxOneUnitNodeValue) {
            intUnits[0]=(char)((value+1)<<6);
            length=1;
        } else {
            intUnits[0]=(char)(CharsTrie.kMinTwoUnitNodeValueLead+((value>>10)&0x7fc0));
            intUnits[1]=(char)value;
            length=2;
        }
        intUnits[0]|=(char)node;
        return write(intUnits, length);
    }
    protected int writeDeltaTo(int jumpTarget) {
        int i=charsLength-jumpTarget;
        assert(i>=0);
        if(i<=CharsTrie.kMaxOneUnitDelta) {
            return write(i);
        }
        int length;
        if(i<=CharsTrie.kMaxTwoUnitDelta) {
            intUnits[0]=(char)(CharsTrie.kMinTwoUnitDeltaLead+(i>>16));
            length=1;
        } else {
            intUnits[0]=(char)(CharsTrie.kThreeUnitDeltaLead);
            intUnits[1]=(char)(i>>16);
            length=2;
        }
        intUnits[length++]=(char)i;
        return write(intUnits, length);
    }

    private boolean stringsAreEqual(int offset1, int length1, int offset2, int length2) {
        // TODO: Make/use public function; see Normalizer2Impl.UTF16Plus.
        if(length1!=length2) {
            return false;
        }
        if(offset1==offset2) {
            return true;
        }
        while(length1>0) {
            if(strings.charAt(offset1++)!=strings.charAt(offset2++)) {
                return false;
            }
            --length1;
        }
        return true;
    }

    private int stringHashCode(int offset, int length) {
        int hash=0;
        while(length>0) {
            hash=hash*37+strings.charAt(offset++);
            --length;
        }
        return hash;
    }

    private StringBuilder strings=new StringBuilder();
    private ArrayList<Element> elements=new ArrayList<Element>();

    // char serialization of the trie.
    // Grows from the back: charsLength measures from the end of the buffer!
    private char[] chars;
    private int charsLength;
}
