/*
 * ********************************************************************************
 * Copyright (C) 2007-2009, International Business Machines Corporation and others.
 * All Rights Reserved.
 * ********************************************************************************
 */
package com.ibm.icu.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.UTF16;

/**
 * TextTrieMap is a trie implementation for supporting
 * fast prefix match for the key.
 */
public class TextTrieMap<V> {
    /**
     * Constructs a TextTrieMap object.
     * 
     * @param ignoreCase true to use case insensitive match
     */
    public TextTrieMap(boolean ignoreCase) {
        this.ignoreCase = ignoreCase;
    }

    /**
     * Adds the text key and its associated object in this object.
     * 
     * @param text The text.
     * @param o The object associated with the text.
     */
    public synchronized void put(String text, V o) {
        CharacterNode node = root;
        for (int i = 0; i < text.length(); i++) {
            int ch = UTF16.charAt(text, i);
            node = node.addChildNode(ch);
            if (UTF16.getCharCount(ch) == 2) {
                i++;
            }
        }
        node.addObject(o);
    }

    /**
     * Gets an iterator of the objects associated with the
     * longest prefix matching string key.
     * 
     * @param text The text to be matched with prefixes.
     * @return An iterator of the objects associated with
     * the longest prefix matching matching key, or null
     * if no matching entry is found.
     */
    public Iterator<V> get(String text) {
        return get(text, 0);
    }

    /**
     * Gets an iterator of the objects associated with the
     * longest prefix matching string key starting at the 
     * specified position.
     * 
     * @param text The text to be matched with prefixes.
     * @param start The start index of of the text
     * @return An iterator of the objects associated with the
     * longest prefix matching matching key, or null if no 
     * matching entry is found.
     */
    public Iterator<V> get(String text, int start) {
        LongestMatchHandler<V> handler = new LongestMatchHandler<V>();
        find(text, start, handler);
        return handler.getMatches();
    }

    public void find(String text, ResultHandler<V> handler) {
        find(text, 0, handler);
    }
    
    public void find(String text, int start, ResultHandler<V> handler) {
        find(root, text, start, start, handler);
    }

    /*
     * Find an iterator of the objects associated with the
     * longest prefix matching string key under the specified node.
     * 
     * @param node The character node in this trie.
     * @param text The text to be matched with prefixes.
     * @param start The start index within the text.
     * @param index The current index within the text.
     * @param handler The result handler, ResultHandler#handlePrefixMatch
     * is called when any prefix match is found.
     */
    private synchronized void find(CharacterNode node, String text,
            int start, int index, ResultHandler<V> handler) {
        Iterator<V> itr = node.iterator();
        if (itr != null) {
            if (!handler.handlePrefixMatch(index - start, itr)) {
                return;
            }
        }
        if (index < text.length()) {
            List<CharacterNode> childNodes = node.getChildNodes();
            if (childNodes == null) {
                return;
            }
            int ch = UTF16.charAt(text, index);
            int chLen = UTF16.getCharCount(ch);
            for (int i = 0; i < childNodes.size(); i++) {
                CharacterNode child = childNodes.get(i);
                if (compare(ch, child.getCharacter())) {
                    find(child, text, start, index + chLen, handler);
                    break;
                }
            }
        }
    }
    
    /**
     * A private method used for comparing two characters.
     * 
     * @param ch1 The first character.
     * @param ch2 The second character.
     * @return true if the first character matches the second.
     */
    private boolean compare(int ch1, int ch2) {
        if (ch1 == ch2) {
            return true;
        }
        else if (ignoreCase) {
            if (UCharacter.toLowerCase(ch1) == UCharacter.toLowerCase(ch2)) {
                return true;
            }
            else if (UCharacter.toUpperCase(ch1) == UCharacter.toUpperCase(ch2)) {
                return true;
            }
        }
        return false;
    }

    // The root node of this trie
    private CharacterNode root = new CharacterNode(0);

    // Character matching option
    boolean ignoreCase;

    /**
     * Inner class representing a character node in the trie.
     */
    private class CharacterNode {
        int character;
        List<CharacterNode> children;
        List<V> objlist;

        /**
         * Constructs a node for the character.
         * 
         * @param ch The character associated with this node.
         */
        public CharacterNode(int ch) {
            character = ch;
        }

        /**
         * Gets the character associated with this node.
         * 
         * @return The character
         */
        public int getCharacter() {
            return character;
        }

        /**
         * Adds the object to the node.
         *  
         * @param obj The object set in the leaf node.
         */
        public void addObject(V obj) {
            if (objlist == null) {
                objlist = new LinkedList<V>();
            }
            objlist.add(obj);
        }

        /**
         * Gets an iterator of the objects associated with
         * the leaf node.
         * 
         * @return The iterator or null if no objects are
         * associated with this node.
         */
        public Iterator<V> iterator() {
            if (objlist == null) {
                return null;
            }
            return objlist.iterator();
        }

        /**
         * Adds a child node for the character under this character
         * node in the trie.  When the matching child node already
         * exists, the reference of the existing child node is
         * returned.
         * 
         * @param ch The character associated with a child node.
         * @return The child node.
         */
        public CharacterNode addChildNode(int ch) {
            if (children == null) {
                children = new ArrayList<CharacterNode>();
                CharacterNode newNode = new CharacterNode(ch);
                children.add(newNode);
                return newNode;
            }
            CharacterNode node = null;
            for (int i = 0; i < children.size(); i++) {
                CharacterNode cur = children.get(i);
                if (compare(ch, cur.getCharacter())) {
                    node = cur;
                    break;
                }               
            }
            if (node == null) {
                node = new CharacterNode(ch);
                children.add(node);
            }
            return node;
        }

        /**
         * Gets the list of child nodes under this node.
         * 
         * @return The list of child nodes.
         */
        public List<CharacterNode> getChildNodes() {
            return children;
        }
    }

    /**
     * Callback handler for processing prefix matches used by
     * find method.
     */
    public interface ResultHandler<V> {
        /**
         * Handles a prefix key match
         * 
         * @param matchLength Matched key's length
         * @param values An iterator of the objects associated with the matched key
         * @return Return true to continue the search in the trie, false to quit.
         */
        public boolean handlePrefixMatch(int matchLength, Iterator<V> values);
    }

    private static class LongestMatchHandler<V> implements ResultHandler<V> {
        private Iterator<V> matches = null;
        private int length = 0;

        public boolean handlePrefixMatch(int matchLength, Iterator<V> values) {
            if (matchLength > length) {
                length = matchLength;
                matches = values;
            }
            return true;
        }

        public Iterator<V> getMatches() {
            return matches;
        }
    }
}
