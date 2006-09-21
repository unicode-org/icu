/*
 * *****************************************************************************
 * Copyright (C) 2006, International Business Machines Corporation and others.
 * All Rights Reserved.
 * *****************************************************************************
 */
package com.ibm.icu.impl;

import java.util.ArrayList;
import java.util.List;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.UTF16;

/**
 * TextTrieMap is a trie implementation for supporting
 * fast prefix match for the key.
 */
public class TextTrieMap {
    /**
     * Costructs a TextTrieMap object.
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
     * @return The previous value associated with specified text,
     * or null if there was no mapping for the text.
     */
    public synchronized Object put(String text, Object o) {
        CharacterNode node = root;
        for (int i = 0; i < text.length(); i++) {
            int ch = UTF16.charAt(text, i);
            node = node.addChildNode(ch);
            if (UTF16.getCharCount(ch) == 2) {
                i++;
            }
        }
        Object prevObj = node.getObject();
        node.setObject(o);
        return prevObj;
    }

    /**
     * Gets the object associated with the longest prefix
     * matching string key.
     * 
     * @param text The text to be matched with prefixes.
     * @return The object associated with the longet prefix matching
     * matching key, or null if no matching entry is found.
     */
    public Object get(String text) {
        return get(root, text, 0);
    }

    /**
     * Gets the object associated with the longest prefix
     * matching string key starting at the specified position.
     * 
     * @param text The text to be matched with prefixes.
     * @param start The start index of of the text
     * @return The object associated with the longet prefix matching
     * matching key, or null if no matching entry is found.
     */
    public Object get(String text, int start) {
        return get(root, text, start);
    }

    /**
     * Gets the object associated with the longet prefix
     * matching string key under the specified node.
     * 
     * @param node The character node in this trie.
     * @param text The text to be matched with prefixes.
     * @param index The current index within the text.
     * @return The object associated with the longest prefix
     * match under the node.
     */
    private synchronized Object get(CharacterNode node, String text, int index) {
        Object obj = node.getObject();
        if (index < text.length()) {
            List childNodes = node.getChildNodes();
            if (childNodes == null) {
                return obj;
            }
            int ch = UTF16.charAt(text, index);
            int chLen = UTF16.getCharCount(ch);
            for (int i = 0; i < childNodes.size(); i++) {
                CharacterNode child = (CharacterNode)childNodes.get(i);
                if (compare(ch, child.getCharacter())) {
                    Object tmp = get(child, text, index + chLen);
                    if (tmp != null) {
                        obj = tmp;
                    }
                    break;
                }
            }
        }
        return obj;
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
        List children;
        Object obj;

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
         * Sets the object to the node.  Only a leaf node has
         * the reference of an object associated with a key.
         * 
         * @param obj The object set in the leaf node.
         */
        public void setObject(Object obj) {
            this.obj = obj;
        }

        /**
         * Gets the object associated the leaf node.
         * 
         * @return The object.
         */
        public Object getObject() {
            return obj;
        }

        /**
         * Adds a child node for the characer under this character
         * node in the trie.  When the matching child node already
         * exists, the reference of the existing child node is
         * returned.
         * 
         * @param ch The character associated with a child node.
         * @return The child node.
         */
        public CharacterNode addChildNode(int ch) {
            if (children == null) {
                children = new ArrayList();
                CharacterNode newNode = new CharacterNode(ch);
                children.add(newNode);
                return newNode;
            }
            CharacterNode node = null;
            for (int i = 0; i < children.size(); i++) {
                CharacterNode cur = (CharacterNode)children.get(i);
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
        public List getChildNodes() {
            return children;
        }
    }
}
