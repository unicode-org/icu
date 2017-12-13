// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
/*
 * ********************************************************************************
 * Copyright (C) 2007-2011, International Business Machines Corporation and others.
 * All Rights Reserved.
 * ********************************************************************************
 */
package com.ibm.icu.impl;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.UTF16;

/**
 * TextTrieMap is a trie implementation for supporting
 * fast prefix match for the key.
 */
public class TextTrieMap<V> {

    private Node _root = new Node();
    boolean _ignoreCase;

    public static class Output {
        public int matchLength;
        public boolean partialMatch;
    }

    /**
     * Constructs a TextTrieMap object.
     *
     * @param ignoreCase true to use simple case insensitive match
     */
    public TextTrieMap(boolean ignoreCase) {
        _ignoreCase = ignoreCase;
    }

    /**
     * Adds the text key and its associated object in this object.
     *
     * @param text The text.
     * @param val The value object associated with the text.
     */
    public TextTrieMap<V> put(CharSequence text, V val) {
        CharIterator chitr = new CharIterator(text, 0, _ignoreCase);
        _root.add(chitr, val);
        return this;
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
    public Iterator<V> get(CharSequence text, int start) {
        return get(text, start, null);
    }

    public Iterator<V> get(CharSequence text, int start, Output output) {
        LongestMatchHandler<V> handler = new LongestMatchHandler<V>();
        find(text, start, handler, output);
        if (output != null) {
            output.matchLength = handler.getMatchLength();
        }
        return handler.getMatches();
    }

    public void find(CharSequence text, ResultHandler<V> handler) {
        find(text, 0, handler, new Output());
    }

    public void find(CharSequence text, int offset, ResultHandler<V> handler) {
        find(text, offset, handler, new Output());
    }

    private void find(CharSequence text, int offset, ResultHandler<V> handler, Output output) {
        CharIterator chitr = new CharIterator(text, offset, _ignoreCase);
        find(_root, chitr, handler, output);
    }

    private synchronized void find(Node node, CharIterator chitr, ResultHandler<V> handler, Output output) {
        Iterator<V> values = node.values();
        if (values != null) {
            if (!handler.handlePrefixMatch(chitr.processedLength(), values)) {
                return;
            }
        }

        Node nextMatch = node.findMatch(chitr, output);
        if (nextMatch != null) {
            find(nextMatch, chitr, handler, output);
        }
    }

    /**
     * Creates an object that consumes code points one at a time and returns intermediate prefix
     * matches.  Returns null if no match exists.
     *
     * @return An instance of {@link ParseState}, or null if the starting code point is not a
     * prefix for any entry in the trie.
     */
    public ParseState openParseState(int startingCp) {
      // Check to see whether this is a valid starting character.  If not, return null.
      if (_ignoreCase) {
        startingCp = UCharacter.foldCase(startingCp, true);
      }
      int count = Character.charCount(startingCp);
      char ch1 = (count == 1) ? (char) startingCp : UTF16.getLeadSurrogate(startingCp);
      if (!_root.hasChildFor(ch1)) {
        return null;
      }

      return new ParseState(_root);
    }

    /**
     * ParseState is mutable, not thread-safe, and intended to be used internally by parsers for
     * consuming values from this trie.
     */
    public class ParseState {
      private Node node;
      private int offset;
      private Node.StepResult result;

      ParseState(Node start) {
        node = start;
        offset = 0;
        result = start.new StepResult();
      }

      /**
       * Consumes a code point and walk to the next node in the trie.
       *
       * @param cp The code point to consume.
       */
      public void accept(int cp) {
        assert node != null;
        if (_ignoreCase) {
          cp = UCharacter.foldCase(cp, true);
        }
        int count = Character.charCount(cp);
        char ch1 = (count == 1) ? (char) cp : UTF16.getLeadSurrogate(cp);
        node.takeStep(ch1, offset, result);
        if (count == 2 && result.node != null) {
          char ch2 = UTF16.getTrailSurrogate(cp);
          result.node.takeStep(ch2, result.offset, result);
        }
        node = result.node;
        offset = result.offset;
      }

      /**
       * Gets the exact prefix matches for all code points that have been consumed so far.
       *
       * @return The matches.
       */
      public Iterator<V> getCurrentMatches() {
        if (node != null && offset == node.charCount()) {
          return node.values();
        }
        return null;
      }

      /**
       * Checks whether any more code points can be consumed.
       *
       * @return true if no more code points can be consumed; false otherwise.
       */
      public boolean atEnd() {
        return node == null || (node.charCount() == offset && node._children == null);
      }
    }

    public static class CharIterator implements Iterator<Character> {
        private boolean _ignoreCase;
        private CharSequence _text;
        private int _nextIdx;
        private int _startIdx;

        private Character _remainingChar;

        CharIterator(CharSequence text, int offset, boolean ignoreCase) {
            _text = text;
            _nextIdx = _startIdx = offset;
            _ignoreCase = ignoreCase;
        }

        /* (non-Javadoc)
         * @see java.util.Iterator#hasNext()
         */
        @Override
        public boolean hasNext() {
            if (_nextIdx == _text.length() && _remainingChar == null) {
                return false;
            }
            return true;
        }

        /* (non-Javadoc)
         * @see java.util.Iterator#next()
         */
        @Override
        public Character next() {
            if (_nextIdx == _text.length() && _remainingChar == null) {
                return null;
            }
            Character next;
            if (_remainingChar != null) {
                next = _remainingChar;
                _remainingChar = null;
            } else {
                if (_ignoreCase) {
                    int cp = UCharacter.foldCase(Character.codePointAt(_text, _nextIdx), true);
                    _nextIdx = _nextIdx + Character.charCount(cp);

                    char[] chars = Character.toChars(cp);
                    next = chars[0];
                    if (chars.length == 2) {
                        _remainingChar = chars[1];
                    }
                } else {
                    next = _text.charAt(_nextIdx);
                    _nextIdx++;
                }
            }
            return next;
        }

        /* (non-Javadoc)
         * @see java.util.Iterator#remove()
         */
        @Override
        public void remove() {
            throw new UnsupportedOperationException("remove() not supproted");
        }

        public int nextIndex() {
            return _nextIdx;
        }

        public int processedLength() {
            if (_remainingChar != null) {
                throw new IllegalStateException("In the middle of surrogate pair");
            }
            return _nextIdx - _startIdx;
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

        @Override
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

        public int getMatchLength() {
            return length;
        }
    }

    /**
     * Inner class representing a text node in the trie.
     */
    private class Node {
        private char[] _text;
        private List<V> _values;
        private List<Node> _children;

        private Node() {
        }

        private Node(char[] text, List<V> values, List<Node> children) {
            _text = text;
            _values = values;
            _children = children;
        }

        public int charCount() {
          return _text == null ? 0 : _text.length;
        }

        public boolean hasChildFor(char ch) {
          for (int i=0; _children != null && i < _children.size(); i++) {
            Node child = _children.get(i);
            if (ch < child._text[0]) break;
            if (ch == child._text[0]) {
              return true;
            }
          }
          return false;
        }

        public Iterator<V> values() {
            if (_values == null) {
                return null;
            }
            return _values.iterator();
        }

        public void add(CharIterator chitr, V value) {
            StringBuilder buf = new StringBuilder();
            while (chitr.hasNext()) {
                buf.append(chitr.next());
            }
            add(toCharArray(buf), 0, value);
        }

        public Node findMatch(CharIterator chitr, Output output) {
            if (_children == null) {
                return null;
            }
            if (!chitr.hasNext()) {
                output.partialMatch = true;
                return null;
            }
            Node match = null;
            Character ch = chitr.next();
            for (Node child : _children) {
                if (ch < child._text[0]) {
                    break;
                }
                if (ch == child._text[0]) {
                    if (child.matchFollowing(chitr, output)) {
                        match = child;
                    }
                    break;
                }
            }
            return match;
        }

        public class StepResult {
          public Node node;
          public int offset;
        }
        public void takeStep(char ch, int offset, StepResult result) {
          assert offset <= charCount();
          if (offset == charCount()) {
            // Go to a child node
            for (int i=0; _children != null && i < _children.size(); i++) {
              Node child = _children.get(i);
              if (ch < child._text[0]) break;
              if (ch == child._text[0]) {
                // Found a matching child node
                result.node = child;
                result.offset = 1;
                return;
              }
            }
            // No matching children; fall through
          } else if (_text[offset] == ch) {
            // Return to this node; increase offset
            result.node = this;
            result.offset = offset + 1;
            return;
          }
          // No matches
          result.node = null;
          result.offset = -1;
          return;
        }

        private void add(char[] text, int offset, V value) {
            if (text.length == offset) {
                _values = addValue(_values, value);
                return;
            }

            if (_children == null) {
                _children = new LinkedList<Node>();
                Node child = new Node(subArray(text, offset), addValue(null, value), null);
                _children.add(child);
                return;
            }

            // walk through children
            ListIterator<Node> litr = _children.listIterator();
            while (litr.hasNext()) {
                Node next = litr.next();
                if (text[offset] < next._text[0]) {
                    litr.previous();
                    break;
                }
                if (text[offset] == next._text[0]) {
                    int matchLen = next.lenMatches(text, offset);
                    if (matchLen == next._text.length) {
                        // full match
                        next.add(text, offset + matchLen, value);
                    } else {
                        // partial match, create a branch
                        next.split(matchLen);
                        next.add(text, offset + matchLen, value);
                    }
                    return;
                }
            }
            // add a new child to this node
            litr.add(new Node(subArray(text, offset), addValue(null, value), null));
        }

        private boolean matchFollowing(CharIterator chitr, Output output) {
            boolean matched = true;
            int idx = 1;
            while (idx < _text.length) {
                if(!chitr.hasNext()) {
                    output.partialMatch = true;
                    matched = false;
                    break;
                }
                Character ch = chitr.next();
                if (ch != _text[idx]) {
                    matched = false;
                    break;
                }
                idx++;
            }
            return matched;
        }

        private int lenMatches(char[] text, int offset) {
            int textLen = text.length - offset;
            int limit = _text.length < textLen ? _text.length : textLen;
            int len = 0;
            while (len < limit) {
                if (_text[len] != text[offset + len]) {
                    break;
                }
                len++;
            }
            return len;
        }

        private void split(int offset) {
            // split the current node at the offset
            char[] childText = subArray(_text, offset);
            _text = subArray(_text, 0, offset);

            // add the Node representing after the offset as a child
            Node child = new Node(childText, _values, _children);
            _values = null;

            _children = new LinkedList<Node>();
            _children.add(child);
        }

        private List<V> addValue(List<V> list, V value) {
            if (list == null) {
                list = new LinkedList<V>();
            }
            list.add(value);
            return list;
        }
    }

    private static char[] toCharArray(CharSequence text) {
        char[] array = new char[text.length()];
        for (int i = 0; i < array.length; i++) {
            array[i] = text.charAt(i);
        }
        return array;
    }

    private static char[] subArray(char[] array, int start) {
        if (start == 0) {
            return array;
        }
        char[] sub = new char[array.length - start];
        System.arraycopy(array, start, sub, 0, sub.length);
        return sub;
    }

    private static char[] subArray(char[] array, int start, int limit) {
        if (start == 0 && limit == array.length) {
            return array;
        }
        char[] sub = new char[limit - start];
        System.arraycopy(array, start, sub, 0, limit - start);
        return sub;
    }
}
