/*
**********************************************************************
* Copyright (c) 2004-2008, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
* Author: Alan Liu
* Created: March 16 2004
* Since: ICU 3.0
**********************************************************************
*/
package com.ibm.icu.impl.data;

import java.io.IOException;

import com.ibm.icu.impl.UCharacterProperty;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.UTF16;

/**
 * An iterator class that returns successive string tokens from some
 * source.  String tokens are, in general, separated by rule white
 * space in the source test.  Furthermore, they may be delimited by
 * either single or double quotes (opening and closing quotes must
 * match).  Escapes are processed using standard ICU unescaping.
 */
public class TokenIterator {

    private ResourceReader reader;
    private String line;
    private StringBuffer buf;
    private boolean done;
    private int pos;
    private int lastpos;

    /**
     * Construct an iterator over the tokens returned by the given
     * ResourceReader, ignoring blank lines and comment lines (first
     * non-blank character is '#').  Note that trailing comments on a
     * line, beginning with the first unquoted '#', are recognized.
     */
    public TokenIterator(ResourceReader r) {
        reader = r;
        line = null;
        done = false;
        buf = new StringBuffer();
        pos = lastpos = -1;
    }

    /**
     * Return the next token from this iterator, or null if the last
     * token has been returned.
     */
    public String next() throws IOException {
        if (done) {
            return null;
        }
        for (;;) {
            if (line == null) {
                line = reader.readLineSkippingComments();
                if (line == null) {
                    done = true;
                    return null;
                }
                pos = 0;
            }
            buf.setLength(0);
            lastpos = pos;
            pos = nextToken(pos);
            if (pos < 0) {
                line = null;
                continue;
            }
            return buf.toString();
        }
    }

    /**
     * Return the one-based line number of the line of the last token returned by
     * next(). Should only be called
     * after a call to next(); otherwise the return
     * value is undefined.
     */
    public int getLineNumber() {
        return reader.getLineNumber();
    }
    
    /**
     * Return a string description of the position of the last line
     * returned by readLine() or readLineSkippingComments().
     */
    public String describePosition() {
        return reader.describePosition() + ':' + (lastpos+1);
    }
    
    /**
     * Read the next token from 'this.line' and append it to
     * 'this.buf'.  Tokens are separated by rule white space.  Tokens
     * may also be delimited by double or single quotes.  The closing
     * quote must match the opening quote.  If a '#' is encountered,
     * the rest of the line is ignored, unless it is backslash-escaped
     * or within quotes.
     * @param position the offset into the string
     * @return offset to the next character to read from line, or if
     * the end of the line is reached without scanning a valid token,
     * -1
     */
    private int nextToken(int position) {
        position = Utility.skipWhitespace(line, position);
        if (position == line.length()) {
            return -1;
        }
        int startpos = position;
        char c = line.charAt(position++);
        char quote = 0;
        switch (c) {
        case '"':
        case '\'':
            quote = c;
            break;
        case '#':
            return -1;
        default:
            buf.append(c);
            break;
        }
        int[] posref = null;
        while (position < line.length()) {
            c = line.charAt(position); // 16-bit ok
            if (c == '\\') {
                if (posref == null) {
                    posref = new int[1];
                }
                posref[0] = position+1;
                int c32 = Utility.unescapeAt(line, posref);
                if (c32 < 0) {
                    throw new RuntimeException("Invalid escape at " +
                                               reader.describePosition() + ':' +
                                               position);
                }
                UTF16.append(buf, c32);
                position = posref[0];
            } else if ((quote != 0 && c == quote) ||
                       (quote == 0 && UCharacterProperty.isRuleWhiteSpace(c))) {
                return ++position;
            } else if (quote == 0 && c == '#') {
                return position; // do NOT increment
            } else {
                buf.append(c);
                ++position;
            }
        }
        if (quote != 0) {
            throw new RuntimeException("Unterminated quote at " +
                                       reader.describePosition() + ':' +
                                       startpos);
        }
        return position;
    }
}
