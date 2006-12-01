/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/text/utility/XMLParse.java,v $
* $Date: 2001/08/31 00:19:16 $
* $Revision: 1.2 $
*
*******************************************************************************
*/

package com.ibm.text.utility;

/**
 * Very dumb XML parser, designed for restricted environment where transmitter is guaranteed
 * to limit types of XML files generated.
 *
 * RESTRICTIONS
 *  Requires document to be well-formed. Doesn't properly signal errors if it is not.
 *  No DTDs, !DOCTYPE, !ATTLIST, !ELEMENT, ![, !NOTATION, !ENTITY, CDATA
 *  No processing instructions
 *  Does do character references, lt, gt, amp, apos, quot
 *  The encoding is specified by the user, by using the right Reader
 *  On creation, you supply a buffer for the textual elements.  Use a buffer that is as large
 * as the largest possible piece of text (e.g. attribute value or element text) in the file.
 *
 * @author Mark Davis
 */
import java.io.*;

public final class XMLParse implements XMLParseTypes {

    /** Create a parser.
     */
    public XMLParse(Reader stream, char[] buffer) {
        this.stream = stream;
        this.buffer = buffer;
    }

    /** Create a parser.
     */
    public XMLParse(String fileName, char[] buffer) throws FileNotFoundException {
        stream = new BufferedReader(new FileReader(fileName),32*1024);
        this.buffer = buffer;
    }

    /** Get the textual value associated with this item.
     * Only valid for ELEMENT_TAG*, ATTRIBUTE*, TEXT.
     */
    public String getValue() {
        return String.valueOf(buffer, 0, bufferCount);
    }

    /** Get length of the textual value associated with this item.
     * Only valid for ELEMENT_TAG*, ATTRIBUTE*, TEXT.
     */
    public int getValueCount() {
        return bufferCount;
    }

    /** Get the buffer that was passed in on creation.
     */
    public char[] getValueArray() {
        return buffer;
    }

    /** Get the "kind" of the last item (see XMLParseTypes)
     */
    public int getKind() {
        return kind;
    }

    /** Get the next element, returning a "Kind" (see XMLParseTypes)
     */

    public byte next() {

        char c = '\u0000';
        char type = c;

        while (c != 0xFFFF) {
            try {

                // First read the character. If there is a buffered char, use it instead

                if (bufferChar != 0) {
                    c = bufferChar;
                    bufferChar = 0;
                } else {
                    c = (char) stream.read();
                }

                // Now set the right type. Since we assume validity, anything but the syntax chars
                // can be classed as IDENTIFIER

                switch (c) {
                    case ' ': case '\r': case '\n': case '\t':
                        type = ' ';
                        break;
                    case '<': case '>':  case '#': case ';': case '/': case '\'': case '"':
                    case '=': case '?': case '!': case '-':
                        type = c;
                        break;
                    case '&':   // CR, either numerical or lt, gt, quot, amp, apos

                        // gather characters

                        int crCount = 0;
                        while (true) {
                            c = (char) stream.read();
                            if (c == ';') break;
                            crBuffer[crCount++] = c;
                        }

                        // parse it, and break into two pieces if necessary

                        int x = parseCR(crBuffer, crCount);
                        c = (char)x;
                        if (x > 0xFFFF) {            // Supplementary
                            x -= 0x10000;
                            c = (char) (0xD800 + (x >> 10));
                            bufferChar = (char) (0xDC00 + (x & 0x3FF));
                        }

                        // Since we assume validity, any CRs are not syntax characters

                        type = IDENTIFIER; // everything else
                        break;
                    default:
                        type = IDENTIFIER; // everything else
                        break;
                }
            } catch (Exception e) {
                c = '\uFFFF';
            }

            // We now have a character. Throw it at our little state machine

            if (SHOW) System.out.println(c + ", " + type + ", " + stateNames[state]);
            switch (state) {
                case IN_TEXT:
                    if (type == '<') {
                        state = START_ELEMENT;
                        if (bufferCount != 0) {
                            kind = TEXT;
                            return kind;
                        }
                        break;
                    }
                    buffer[bufferCount++] = c;
                    break;
                case START_ELEMENT: // must be either '/' or more than one ID char
                    bufferCount = 0;
                    switch (type) {
                        case '/':
                            elementType = ELEMENT_TAG_SLASH;
                            state = IN_ELEMENT;
                            break;
                        case '!':
                            buffer[bufferCount++] = c;
                            elementType = ELEMENT_TAG_COMMENT;
                            state = IN_COMMENT;
                            break;
                        case '?':
                            elementType = ELEMENT_TAG_QUESTION;
                            state = IN_ELEMENT;
                            break;
                        default:
                            elementType = ELEMENT_TAG;
                            buffer[bufferCount++] = c;
                            state = IN_ELEMENT;
                            break;
                    }
                    break;
                case IN_COMMENT:
                    buffer[bufferCount++] = c;
                    if (type == '-') state = IN_COMMENT2;
                    else state = IN_COMMENT;
                    break;
                case IN_COMMENT2:
                    buffer[bufferCount++] = c;
                    if (type == '-') state = IN_COMMENT3;
                    else state = IN_COMMENT;
                    break;
                case IN_COMMENT3:
                    if (type == '>') {
                        kind = ELEMENT_TAG_COMMENT;
                        bufferChar = c;
                        state = IN_ATTRIBUTES;
                        elementType = END_ELEMENT_COMMENT;
                        return kind;
                    } else if (type != '-') {
                        state = IN_COMMENT;
                    }
                    buffer[bufferCount++] = c;
                    break;
                case IN_ELEMENT:
                    if (type != IDENTIFIER) {
                        state = IN_ATTRIBUTES;
                        kind = elementType;
                        elementType = END_ELEMENT;
                        bufferChar = c;
                        return kind;
                    }
                    buffer[bufferCount++] = c;
                    break;
                case IN_ATTRIBUTES:
                    bufferCount = 0;
                    if (type == '/') {
                        elementType = END_ELEMENT_SLASH;
                    } else if (type == '?') {
                        elementType = END_ELEMENT_QUESTION;
                    } else if (type == '>') {
                        state = IN_TEXT;
                        kind = elementType;
                        return kind;
                    } else if (type == IDENTIFIER) {
                        state = IN_ATTR;
                        buffer[bufferCount++] = c;
                        break;
                    }
                    break;
                case IN_ATTR:
                    if (type != IDENTIFIER) {
                        state = START_VALUE;
                        kind = ATTRIBUTE_TAG;
                        return kind;
                    }
                    buffer[bufferCount++] = c;
                    break;
                case START_VALUE:   // must have <s>* = ( ' | " )
                    if (type == '\'' || type == '"') {
                        lastQuote = c;
                        state = IN_VALUE;
                        bufferCount = 0;
                    }
                    break;
                case IN_VALUE: // only terminated by lastQuote
                    if (type == lastQuote) {
                        state = IN_ATTRIBUTES;
                        kind = ATTRIBUTE_VALUE;
                        return kind;
                    }
                    buffer[bufferCount++] = c;
                    break;
            }
        }
        return DONE;
    }

    /** Utility for doing XML quotes. Flags control which characters are handled and how.
     * (see XMLParseTypes for values)
     */

    public static String quote(int c) {
        return quote(c, 0);
    }

    /** Utility for doing XML quotes. Flags control which characters are handled and how.
     * (see XMLParseTypes for values)
     */

    public static String quote(int c, int flags) {
        String result = quoteGuts(c, flags);
        if (result != null) return result;
        return String.valueOf((char)c);
    }

    /** Utility for doing XML quotes. Flags control which characters are handled and how.
     * (see XMLParseTypes for values)
     */

    public static String quote(String source) {
        return quote(source, 0);
    }

    /** Utility for doing XML quotes. Flags control which characters are handled and how.
     * (see XMLParseTypes for values)
     */

    public static String quote(String source, int flags) {
        StringBuffer result = new StringBuffer();
        String temp;
        for (int i = 0; i < source.length(); ++i) {
            int c = UTF32.char32At(source, i);
            if (c > 0xFFFF) ++i;
            temp = quoteGuts(c, flags);
            if (temp != null) result.append(temp);
            else if (c <= 0xFFFF) result.append((char)c);
            else result.append(source.substring(i-1,i+1)); // surrogates
        }
        return result.toString();
    }

    /** Parses inside of CR. buffer should not contain the initial '&', or final ';'
     */
    static int parseCR(char[] crBuffer, int crCount) {
        int c;
        int start = 0;
        if (crCount == 0) return -1;
        switch (crBuffer[start++]) {
            case 'l':   c = '<'; break;     // lt
            case 'g':   c = '>'; break;     // gt
            case 'q':   c = '"'; break;     // quot
            case 'a':   // &amp;, &apos;
                if (crCount > start && crBuffer[start] == 'm') c = '&';
                else c = '\'';
                break;
            case '#':
                int radix = 10;
                if (crCount > start && crBuffer[start] == 'x') {
                    radix = 16;
                    ++start;
                }
                // Simple code for now. Could be sped up.
                c = Integer.parseInt(String.valueOf(crBuffer,start,crCount-start), radix);
                break;
            default:
                c = -1;
        }
        return c;
    }

    /** Utility for doing hex, padding with zeros
     */

    static public String hex(long i, int places) {
        String result = Long.toString(i, 16).toUpperCase();
        if (result.length() < places) {
            result = "0000000000000000".substring(result.length(),places) + result;
        }
        return result;
    }
    // =================== PRIVATES =================================

    private static final char[] buf2 = new char[2];

    private static final boolean SHOW = false;

    private char[] buffer;
    private int bufferCount;
    private byte kind = TEXT;

    private Reader stream;
    private char[] crBuffer = new char[10];
    private int state = IN_TEXT;
    private byte elementType;
    private char lastQuote;
    private char bufferChar;

    private static final byte IN_TEXT = 0, START_ELEMENT = 1, IN_ELEMENT = 2,
        IN_ATTR = 3, START_VALUE = 4, IN_VALUE = 5, IN_ATTRIBUTES = 6,
        IN_COMMENT = 7, IN_COMMENT2 = 8, IN_COMMENT3 = 9;

    private static final String[] stateNames = {"IN_TEXT", "START_ELEMENT", "IN_ELEMENT",
        "IN_ATTR", "START_VALUE", "IN_VALUE", "IN_ATTRIBUTES",
        "IN_COMMENT", "IN_COMMENT2", "IN_COMMENT3"};

    private static final char IDENTIFIER = 'a';


    private static String quoteGuts(int c, int flags) {
        String prefix = "&";
        switch (c) {
            case '<': return "&lt;";
            case '>': return "&gt;";
            case '&': return "&amp;";
            case '\'': return "&apos;";
            case '"': return "&quot;";

            // Optionally fix TAB, CR, LF

            case 0x09: case 0x0A: case 0x0D:
                if ((flags & QUOTE_TABCRLF) == 0) return null;
                break;

            // Fix controls, non-characters, since XML can't handle

            case 0x00: case 0x01: case 0x02: case 0x03: case 0x04: case 0x05: case 0x06: case 0x07:
            case 0x08: case 0x0B: case 0x0C: case 0x0E: case 0x0F:
            case 0x10: case 0x11: case 0x12: case 0x13: case 0x14: case 0x15: case 0x16: case 0x17:
            case 0x18: case 0x19: case 0x1A: case 0x1B: case 0x1C: case 0x1D: case 0x1E: case 0x1F:
            case 0x7F:
            case 0xFFFE: case 0xFFFF:
                prefix = "";
                break;

            // Optionally fix IE Bug characters

            case 0xFF00: case 0xFF01: case 0xFF02: case 0xFF03: case 0xFF04: case 0xFF05: case 0xFF06: case 0xFF07:
            case 0xFFF8: case 0xFFF9: case 0xFFFA: case 0xFFFB: case 0xFFFC: case 0xFFFD:
                if ((flags & QUOTE_IEBUG) == 0) return null;
                prefix = "";
                break;

            default:
                if (c <= 0x7E) {    // don't quote other ASCII
                    if ((flags & QUOTE_ASCII) == 0) return null;
                } else if (0xD800 <= c && c <= 0xDFFF) {// fix surrogates, since XML can't handle
                    prefix = "";
                } else if (c > 0xFFFF && (flags & QUOTE_IEBUG) != 0) {
                    prefix = "";
                } else if ((flags & QUOTE_NON_ASCII) == 0) {
                    return null;
                }
                break;
        }
        if ((flags & QUOTE_DECIMAL) == 0) {
            return prefix + "#x" + hex(c,1) + ";";
        } else {
            return prefix + "#" + Integer.toString(c) + ";";
        }
    }
}