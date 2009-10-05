/*
 *******************************************************************************
 * Copyright (C) 1996-2009, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.text;

import com.ibm.icu.impl.ICUBinary;

import java.io.IOException;
import java.io.InputStream;
import java.io.DataInputStream;
import java.text.CharacterIterator;

/**
 * This is a class used to load in the compact trie dictionary file
 * used for dictionary based break iteration. 
 * @internal
 * @deprecated This API is ICU internal only.
 */
class BreakCTDictionary {
    private CompactTrieHeader fData;

    static class CompactTrieHeader {
        int size; // Size of data in bytes

        int magic; // Magic number (including versions)

        int nodeCount; // Number of entries in offsets[]

        int root; // Node number of the root node

        int []offset; // Offsets to nodes from start of data

        CompactTrieHeader() {
            size = 0;
            magic = 0;
            nodeCount = 0;
            root = 0;
            offset = null;
        }
    }

    static final class CompactTrieNodeFlags {
        static final int kVerticalNode = 0x1000; // This is a vertical node

        static final int kParentEndsWord = 0x2000; // The node whose equal link
                                                    // points to this ends a
                                                    // word

        static final int kReservedFlag1 = 0x4000;

        static final int kReservedFlag2 = 0x8000;

        static final int kCountMask = 0x0FFF; // The count portion of
                                                // flagscount

        static final int kFlagMask = 0xF000; // The flags portion of
                                                // flagscount
    }

    // The two node types are distinguished by the kVerticalNode flag.
    static class CompactTrieHorizontalNode {
        char ch; // UChar

        int equal; // Equal link node index

        CompactTrieHorizontalNode(char newCh, int newEqual) {
            ch = newCh;
            equal = newEqual;
        }
    }

    static class CompactTrieVerticalNode {
        int equal; // Equal link node index

        char chars[]; // Code units

        CompactTrieVerticalNode() {
            equal = 0;
            chars = null;
        }
    }

    private CompactTrieNodes getCompactTrieNode(int node) {
        return nodes[node];
    }

    // private class to hold both node information
    static class CompactTrieNodes {
        short flagscount; // Count of sub-entries, plus flags

        CompactTrieHorizontalNode[] hnode;

        CompactTrieVerticalNode vnode;

        CompactTrieNodes() {
            flagscount = 0;
            hnode = null;
            vnode = null;
        }
    }

    private CompactTrieNodes[] nodes;

    // Constructor
    public BreakCTDictionary(InputStream is) throws IOException {
        ICUBinary.readHeader(is, DATA_FORMAT_ID, null);

        DataInputStream in = new DataInputStream(is);
        // Get header information
        fData = new CompactTrieHeader();
        fData.size = in.readInt();
        fData.magic = in.readInt();
        fData.nodeCount = in.readShort();
        fData.root = in.readShort();

        loadBreakCTDictionary(in);
    }

    // Loads the compact trie dictionary file into the CompactTrieNodes
    private void loadBreakCTDictionary(DataInputStream in) throws IOException {
        // skip over offset information
        for (int i = 0; i < fData.nodeCount; i++) {
            in.readInt();
        }

        // Create compact trie dictionary
        nodes = new CompactTrieNodes[fData.nodeCount];
        nodes[0] = new CompactTrieNodes();

        // Load in compact trie dictionary
        for (int j = 1; j < fData.nodeCount; j++) {
            nodes[j] = new CompactTrieNodes();
            nodes[j].flagscount = in.readShort();

            int count = nodes[j].flagscount & CompactTrieNodeFlags.kCountMask;

            if (count != 0) {
                boolean isVerticalNode = (nodes[j].flagscount & CompactTrieNodeFlags.kVerticalNode) != 0;

                // Vertical node
                if (isVerticalNode) {
                    nodes[j].vnode = new CompactTrieVerticalNode();
                    nodes[j].vnode.equal = in.readShort();

                    nodes[j].vnode.chars = new char[count];
                    for (int l = 0; l < count; l++) {
                        nodes[j].vnode.chars[l] = in.readChar();
                    }
                } else { // Horizontal node
                    nodes[j].hnode = new CompactTrieHorizontalNode[count];
                    for (int n = 0; n < count; n++) {
                        nodes[j].hnode[n] = new CompactTrieHorizontalNode(in
                                .readChar(), in.readShort());
                    }
                }
            }
        }
    }

    /**
     * Find dictionary words that match the text.
     * 
     * @param text A CharacterIterator representing the text. The iterator is
     *            left after the longest prefix match in the dictionary.
     * @param maxLength The maximum number of code units to match.
     * @param lengths An array that is filled with the lengths of words that matched.
     * @param count Filled with the number of elements output in lengths.
     * @param limit The size of the lengths array; this limits the number of words output.
     * @return The number of characters in text that were matched.
     */
    public int matches(CharacterIterator text, int maxLength, int lengths[],
            int count[], int limit) {
        // Current implementation works in UTF-16 space
        CompactTrieNodes node = getCompactTrieNode(fData.root);
        int mycount = 0;

        char uc = text.current();
        int i = 0;
        boolean exitFlag = false;

        while (node != null) {
            // Check if the node we just exited ends a word
            if (limit > 0
                    && (node.flagscount & CompactTrieNodeFlags.kParentEndsWord) != 0) {
                lengths[mycount++] = i;
                --limit;
            }
            // Check that we haven't exceeded the maximum number of input
            // characters.
            // We have to do that here rather than in the while condition so
            // that
            // we can check for ending of a word above.
            if (i >= maxLength) {
                break;
            }

            int nodeCount = (node.flagscount & CompactTrieNodeFlags.kCountMask);
            if (nodeCount == 0) {
                // Special terminal node; return now
                break;
            }
            if ((node.flagscount & CompactTrieNodeFlags.kVerticalNode) != 0) {
                // Vertical node; check all the characters in it
                CompactTrieVerticalNode vnode = node.vnode;
                for (int j = 0; j < nodeCount && i < maxLength; j++) {
                    if (uc != vnode.chars[j]) {
                        // We hit a non equal character return;
                        exitFlag = true;
                        break;
                    }
                    text.next();
                    uc = text.current();
                    i++;
                }
                if (exitFlag) {
                    break;
                }
                // To get here, we must have come through the whole list successfully;
                // go on to the next node. Note that a word cannot end in the middle 
                // of a vertical node.
                node = getCompactTrieNode(vnode.equal);
            } else {
                // Horizontal node; do binary search
                CompactTrieHorizontalNode[] hnode = node.hnode;
                int low = 0;
                int high = nodeCount - 1;
                int middle;
                node = null; // If we don't find a match, we'll fall out of the loop
                while (high >= low) {
                    middle = (high + low) / 2;
                    if (uc == hnode[middle].ch) {
                        // We hit a match; get the next node and next character
                        node = getCompactTrieNode(hnode[middle].equal);
                        text.next();
                        uc = text.current();
                        i++;
                        break;
                    } else if (uc < hnode[middle].ch) {
                        high = middle - 1;
                    } else {
                        low = middle + 1;
                    }
                }
            }
        }

        count[0] = mycount;
        return i;
    }

    // Use for reading the header portion of the file
    private static final byte DATA_FORMAT_ID[] = { (byte) 0x54, (byte) 0x72,
            (byte) 0x44, (byte) 0x63 };
}
