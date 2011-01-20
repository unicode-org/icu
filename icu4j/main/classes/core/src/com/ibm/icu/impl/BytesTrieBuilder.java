/*
*******************************************************************************
*   Copyright (C) 2011, International Business Machines
*   Corporation and others.  All Rights Reserved.
*******************************************************************************
*   created on: 2011jan05
*   created by: Markus W. Scherer
*   ported from ICU4C bytestriebuilder.h/.cpp
*/

package com.ibm.icu.impl;

import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * Builder class for BytesTrie.
 *
 * <p>This class is not intended for public subclassing.
 *
 * @author Markus W. Scherer
 */
public final class BytesTrieBuilder extends StringTrieBuilder {
    /**
     * Constructs an empty builder.
     */
    public BytesTrieBuilder() {}

    /**
     * Adds a (byte sequence, value) pair.
     * The byte sequence must be unique.
     * Bytes 0..length-1 will be copied; the builder does not keep
     * a reference to the input array.
     * @param sequence The array that contains the byte sequence, starting at index 0.
     * @param length The length of the byte sequence.
     * @param value The value associated with this byte sequence.
     * @return this
     */
    public BytesTrieBuilder add(byte[] sequence, int length, int value) {
        if(bytesLength>0) {
            // Cannot add elements after building.
            throw new IllegalStateException("Cannot add (string, value) pairs after build().");
        }
        // Binary search in the sorted elements array to find
        // a) whether the byte sequence is a duplicate, and, if not,
        // b) the insertion index.
        int start=0, limit=elements.size();
        while(start<limit) {
            int i=(start+limit)/2;
            int diff=elements.get(i).compareStringTo(sequence, length, strings);
            if(diff<0) {  // elements[i]<sequence
                start=i+1;
            } else if(diff==0) {
                throw new IllegalArgumentException("Duplicate string.");
            } else {  // elements[i]>sequence
                limit=i;
            }
        }
        elements.add(start, new Element(sequence, length, value, this));
        return this;
    }

    /**
     * Builds a BytesTrie for the add()ed data.
     * Once built, no further data can be add()ed until clear() is called.
     *
     * <p>Multiple calls to build() or buildByteBuffer() return tries or buffers
     * which share the builder's byte array, without rebuilding.
     * <em>The byte array must not be modified via the buildByteBuffer() result object.</em>
     * After clear() has been called, a new array will be used.
     * @param buildOption Build option, see StringTrieBuilder.Option.
     * @return A new BytesTrie for the add()ed data.
     */
    public BytesTrie build(StringTrieBuilder.Option buildOption) {
        buildImpl(buildOption);
        return new BytesTrie(bytes, bytes.length-bytesLength);
    }

    /**
     * Builds a BytesTrie for the add()ed data and byte-serializes it.
     * Once built, no further data can be add()ed until clear() is called.
     *
     * <p>Multiple calls to build() or buildByteBuffer() return tries or buffers
     * which share the builder's byte array, without rebuilding.
     * <em>Do not modify the bytes in the buffer!</em>
     * After clear() has been called, a new array will be used.
     *
     * <p>The serialized BytesTrie is accessible via the buffer's
     * array()/arrayOffset()+position() or remaining()/get(byte[]) etc.
     * @param buildOption Build option, see StringTrieBuilder.Option.
     * @return A ByteBuffer with the byte-serialized BytesTrie for the add()ed data.
     *         The buffer is not read-only and array() can be called.
     */
    public ByteBuffer buildByteBuffer(StringTrieBuilder.Option buildOption) {
        buildImpl(buildOption);
        return ByteBuffer.wrap(bytes, bytes.length-bytesLength, bytesLength);
    }

    private void buildImpl(StringTrieBuilder.Option buildOption) {
        if(bytesLength>0) {
            // Already built.
            return;
        }
        if(elements.isEmpty()) {
            throw new IndexOutOfBoundsException("No (string, value) pairs were added.");
        }
        // Create and byte-serialize the trie for the elements.
        int capacity=stringsLength;
        if(capacity<1024) {
            capacity=1024;
        }
        if(bytes==null || bytes.length<capacity) {
            bytes=new byte[capacity];
        }
        build(buildOption, elements.size());
    }

    /**
     * Removes all (byte sequence, value) pairs.
     * New data can then be add()ed and a new trie can be built.
     * @return this
     */
    public BytesTrieBuilder clear() {
        stringsLength=0;
        elements.clear();
        bytes=null;
        bytesLength=0;
        return this;
    }

    private static final class Element {
        public Element(byte[] sequence, int length, int val, BytesTrieBuilder builder) {
            if(length>0xffff) {
                // Too long: We store the length in 1 or 2 bytes.
                throw new IndexOutOfBoundsException("The maximum byte sequence length is 0xffff.");
            }
            int offset=builder.stringsLength;
            if(length>0xff) {
                offset=~offset;
                builder.stringsAppend((byte)(length>>8));
            }
            builder.stringsAppend((byte)length);
            stringOffset=offset;
            value=val;
            builder.stringsAppend(sequence, length);
        }

        public int getStringLength(byte[] strings) /*const*/ {
            int offset=stringOffset;
            if(offset>=0) {
                return strings[offset]&0xff;
            } else {
                offset=~offset;
                return ((strings[offset]&0xff)<<8)|(strings[offset+1]&0xff);
            }
        }

        public byte charAt(int index, byte[] strings) /*const*/ { return strings[getStringOffset(strings)+index]; }

        public int getValue() /*const*/ { return value; }

        public int compareStringTo(byte[] other, int otherLength, byte[] strings) /*const*/ {
            int thisOffset=stringOffset;
            int thisLength;
            if(thisOffset>=0) {
                thisLength=strings[thisOffset++]&0xff;
            } else {
                thisOffset=~thisOffset;
                thisLength=((strings[thisOffset]&0xff)<<8)|(strings[thisOffset+1]&0xff);
                thisOffset+=2;
            }
            int lengthDiff=thisLength-otherLength;
            int commonLength;
            if(lengthDiff<=0) {
                commonLength=thisLength;
            } else {
                commonLength=otherLength;
            }
            int otherOffset=0;
            while(commonLength>0) {
                int diff=(strings[thisOffset++]&0xff)-(other[otherOffset++]&0xff);
                if(diff!=0) {
                    return diff;
                }
                --commonLength;
            }
            return lengthDiff;
        }

        private int getStringOffset(byte[] strings) /*const*/ {  // C++: const char *data(strings)
            int offset=stringOffset;
            if(offset>=0) {
                ++offset;
            } else {
                offset=~offset+2;
            }
            return offset;
        }

        // If the stringOffset is non-negative, then the first strings byte contains
        // the string length.
        // If the stringOffset is negative, then the first two strings bytes contain
        // the string length (big-endian), and the offset needs to be bit-inverted.
        // (Compared with a stringLength field here, this saves 3 bytes per string for most strings.)
        private int stringOffset;
        private int value;
    };

    protected int getElementStringLength(int i) /*const*/ {
        return elements.get(i).getStringLength(strings);
    }
    protected char getElementUnit(int i, int byteIndex) /*const*/ {
        return (char)(elements.get(i).charAt(byteIndex, strings)&0xff);
    }
    protected int getElementValue(int i) /*const*/ {
        return elements.get(i).getValue();
    }

    protected int getLimitOfLinearMatch(int first, int last, int byteIndex) /*const*/ {
        Element firstElement=elements.get(first);
        Element lastElement=elements.get(last);
        int minStringLength=firstElement.getStringLength(strings);
        while(++byteIndex<minStringLength &&
                firstElement.charAt(byteIndex, strings)==
                lastElement.charAt(byteIndex, strings)) {}
        return byteIndex;
    }

    protected int countElementUnits(int start, int limit, int byteIndex) /*const*/ {
        int length=0;  // Number of different bytes at byteIndex.
        int i=start;
        do {
            byte b=elements.get(i++).charAt(byteIndex, strings);
            while(i<limit && b==elements.get(i).charAt(byteIndex, strings)) {
                ++i;
            }
            ++length;
        } while(i<limit);
        return length;
    }
    protected int skipElementsBySomeUnits(int i, int byteIndex, int count) /*const*/ {
        do {
            byte b=elements.get(i++).charAt(byteIndex, strings);
            while(b==elements.get(i).charAt(byteIndex, strings)) {
                ++i;
            }
        } while(--count>0);
        return i;
    }
    protected int indexOfElementWithNextUnit(int i, int byteIndex, char unit) /*const*/ {
        byte b=(byte)unit;
        while(b==elements.get(i).charAt(byteIndex, strings)) {
            ++i;
        }
        return i;
    }

    protected boolean matchNodesCanHaveValues() /*const*/ { return false; }

    protected int getMaxBranchLinearSubNodeLength() /*const*/ { return BytesTrie.kMaxBranchLinearSubNodeLength; }
    protected int getMinLinearMatch() /*const*/ { return BytesTrie.kMinLinearMatch; }
    protected int getMaxLinearMatchLength() /*const*/ { return BytesTrie.kMaxLinearMatchLength; }

    private static final class BTLinearMatchNode extends LinearMatchNode {
        public BTLinearMatchNode(int offset, int len, Node nextNode, BytesTrieBuilder b) {
            super(len, nextNode);
            btBuilder=b;
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
            BTLinearMatchNode o=(BTLinearMatchNode)other;
            return btBuilder.stringsAreEqual(stringOffset, length, o.stringOffset, o.length);
        }
        public void write(StringTrieBuilder builder) {
            BytesTrieBuilder b=(BytesTrieBuilder)builder;
            next.write(builder);
            b.write(stringOffset, length);
            offset=b.write(b.getMinLinearMatch()+length-1);
        }

        private BytesTrieBuilder btBuilder;
        private int stringOffset;
    }

    protected Node createLinearMatchNode(int i, int byteIndex, int length, Node nextNode) /*const*/ {
        return new BTLinearMatchNode(
                elements.get(i).getStringOffset(strings)+byteIndex,
                length,
                nextNode,
                this);
    }

    private void ensureCapacity(int length) {
        if(length>bytes.length) {
            int newCapacity=bytes.length;
            do {
                newCapacity*=2;
            } while(newCapacity<=length);
            byte[] newBytes=new byte[newCapacity];
            System.arraycopy(bytes, bytes.length-bytesLength,
                             newBytes, newBytes.length-bytesLength, bytesLength);
            bytes=newBytes;
        }
    }
    protected int write(int b) {
        int newLength=bytesLength+1;
        ensureCapacity(newLength);
        bytesLength=newLength;
        bytes[bytes.length-bytesLength]=(byte)b;
        return bytesLength;
    }
    private int write(int offset, int length) {
        int newLength=bytesLength+length;
        ensureCapacity(newLength);
        bytesLength=newLength;
        System.arraycopy(strings, offset, bytes, bytes.length-bytesLength, length);
        return bytesLength;
    }
    private int write(byte[] b, int length) {
        int newLength=bytesLength+length;
        ensureCapacity(newLength);
        bytesLength=newLength;
        System.arraycopy(b, 0, bytes, bytes.length-bytesLength, length);
        return bytesLength;
    }
    protected int writeElementUnits(int i, int byteIndex, int length) {
        return write(elements.get(i).getStringOffset(strings)+byteIndex, length);
    }

    // For writeValueAndFinal() and writeDeltaTo().
    private final byte[] intBytes=new byte[5];

    protected int writeValueAndFinal(int i, boolean isFinal) {
        if(0<=i && i<=BytesTrie.kMaxOneByteValue) {
            return write(((BytesTrie.kMinOneByteValueLead+i)<<1)|(isFinal?1:0));
        }
        int length=1;
        if(i<0 || i>0xffffff) {
            intBytes[0]=(byte)BytesTrie.kFiveByteValueLead;
            intBytes[1]=(byte)(i>>24);
            intBytes[2]=(byte)(i>>16);
            intBytes[3]=(byte)(i>>8);
            intBytes[4]=(byte)i;
            length=5;
        // } else if(i<=BytesTrie.kMaxOneByteValue) {
        //     intBytes[0]=(byte)(BytesTrie.kMinOneByteValueLead+i);
        } else {
            if(i<=BytesTrie.kMaxTwoByteValue) {
                intBytes[0]=(byte)(BytesTrie.kMinTwoByteValueLead+(i>>8));
            } else {
                if(i<=BytesTrie.kMaxThreeByteValue) {
                    intBytes[0]=(byte)(BytesTrie.kMinThreeByteValueLead+(i>>16));
                } else {
                    intBytes[0]=(byte)BytesTrie.kFourByteValueLead;
                    intBytes[1]=(byte)(i>>16);
                    length=2;
                }
                intBytes[length++]=(byte)(i>>8);
            }
            intBytes[length++]=(byte)i;
        }
        intBytes[0]=(byte)((intBytes[0]<<1)|(isFinal?1:0));
        return write(intBytes, length);
    }
    protected int writeValueAndType(boolean hasValue, int value, int node) {
        int offset=write(node);
        if(hasValue) {
            offset=writeValueAndFinal(value, false);
        }
        return offset;
    }
    protected int writeDeltaTo(int jumpTarget) {
        int i=bytesLength-jumpTarget;
        assert(i>=0);
        if(i<=BytesTrie.kMaxOneByteDelta) {
            return write(i);
        }
        int length;
        if(i<=BytesTrie.kMaxTwoByteDelta) {
            intBytes[0]=(byte)(BytesTrie.kMinTwoByteDeltaLead+(i>>8));
            length=1;
        } else {
            if(i<=BytesTrie.kMaxThreeByteDelta) {
                intBytes[0]=(byte)(BytesTrie.kMinThreeByteDeltaLead+(i>>16));
                length=2;
            } else {
                if(i<=0xffffff) {
                    intBytes[0]=(byte)BytesTrie.kFourByteDeltaLead;
                    length=3;
                } else {
                    intBytes[0]=(byte)BytesTrie.kFiveByteDeltaLead;
                    intBytes[1]=(byte)(i>>24);
                    length=4;
                }
                intBytes[1]=(byte)(i>>16);
            }
            intBytes[1]=(byte)(i>>8);
        }
        intBytes[length++]=(byte)i;
        return write(intBytes, length);
    }

    private void ensureStringsCapacity(int length) {
        if(strings==null) {
            strings=new byte[Math.max(1024, 2*length)];
        } else if(length>strings.length) {
            byte[] newStrings=new byte[Math.max(4*strings.length, 2*length)];
            System.arraycopy(strings, 0, newStrings, 0, stringsLength);
            strings=newStrings;
        }
    }

    private void stringsAppend(byte b) {
        ensureStringsCapacity(stringsLength+1);
        strings[stringsLength++]=b;
    }

    private void stringsAppend(byte[] b, int length) {
        ensureStringsCapacity(stringsLength+length);
        System.arraycopy(b, 0, strings, stringsLength, length);
        stringsLength+=length;
    }

    private boolean stringsAreEqual(int offset1, int length1, int offset2, int length2) {
        if(length1!=length2) {
            return false;
        }
        if(offset1==offset2) {
            return true;
        }
        while(length1>0) {
            if(strings[offset1++]!=strings[offset2++]) {
                return false;
            }
            --length1;
        }
        return true;
    }

    private int stringHashCode(int offset, int length) {
        int hash=0;
        while(length>0) {
            hash=hash*37+(strings[offset++]&0xff);
            --length;
        }
        return hash;
    }

    private byte[] strings;
    private int stringsLength;
    private ArrayList<Element> elements=new ArrayList<Element>();

    // Byte serialization of the trie.
    // Grows from the back: bytesLength measures from the end of the buffer!
    private byte[] bytes;
    private int bytesLength;
}
