/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/text/UCD/MLStreamWriter.java,v $
* $Date: 2001/08/31 00:30:17 $
* $Revision: 1.2 $
*
*******************************************************************************
*/

package com.ibm.text.utility;

import java.io.*;
import java.util.*;
import com.ibm.text.UCD.*;

public class MLStreamWriter extends Writer {
    public static final String copyright =
      "Copyright (C) 2000, IBM Corp. and others. All Rights Reserved.";

    public MLStreamWriter (PrintWriter output, boolean HTML) {
        out = output;
        isHTML = HTML;
    }

    public MLStreamWriter (PrintWriter output) {
        this(output,true);
    }

    public MLStreamWriter el(String elementName) {
        closeIfOpen();
        print('<', AFTER);
        print(elementName, elementName.equals("!--") ? AFTER+FORCE : AFTER);
        stack.add(elementName);
        inElement = true;
        return this;
    }

    private MLStreamWriter closeIfOpen() {
        if (inElement && !"!--".equals(stack.get(stack.size()-1))) {
            print('>',BEFORE+FORCE);
        }
        inElement = false;
        return this;
    }

    final public MLStreamWriter cel(String elementName) {
        return cl().tx(elementName);
    }

    public MLStreamWriter at(String attributeName, String attributeValue) {
        if (!inElement) {
            throw new IllegalArgumentException("attribute \"" + attributeName + "\" not in element");
        }
        print(' ', BOTH);
        print(attributeName, AFTER);
        print('=', AFTER);
        print('"');
        print(quoted(attributeValue));
        print('"', AFTER);
        return this;
    }

    public MLStreamWriter at(String attributeName, int value) {
        return at(attributeName, String.valueOf(value));
    }

    public MLStreamWriter CR() {
        closeIfOpen();
        out.println();
        return this;
    }

    /*public MLStreamWriter comment() {
        closeIfOpen();
        print("<!--");
        CR();
        return this;
    }

    public MLStreamWriter endComment() {
        print("-->");
        return this;
    }
    */

    public MLStreamWriter tx(String text) {
        closeIfOpen();
        print(quoted(text));
        return this;
    }

    final public MLStreamWriter tx(char text) {
        return tx(String.valueOf(text));
    }

    final public MLStreamWriter tx(int text) {
        return tx(String.valueOf(text));
    }

    final public MLStreamWriter tx16(String text) {
        return tx(hex(text));
    }

    final public MLStreamWriter tx16(char text) {
        return tx(hex(text));
    }

    final public MLStreamWriter tx16(int text) {
        return tx(hex(text));
    }

    public MLStreamWriter cl(String closingElement) {
        closeIfOpen();
        String lastElement = (String)stack.remove(stack.size()-1);
        if (closingElement != null && !closingElement.equals(lastElement)) {
            throw new IllegalArgumentException("mismatch when closing \"" + closingElement
                + "\", current active element is \"" + lastElement + "\"");
        }
        if (lastElement.equals("!--")) {// hack for XML/HTML
            print("-->",BEFORE+FORCE);
        } else {
            print("</");
            print(lastElement);
            print('>',BEFORE);
        }
        return this;
    }

    final public MLStreamWriter cl() {
        return cl(null);
    }

    public MLStreamWriter closeAllElements() {
        for (int i = stack.size()-1; i >= 0; --i) {
            cl(null);
        }
        return this;
    }

    // stream stuff

    public void write(char[] source, int start, int len) {
        closeIfOpen();
        // later make more efficient!!
        out.print(quoted(new String(source, start, len)));
    }

    public void close() {
        closeAllElements();
        out.close();
    }

    public void flush() {
        out.flush();
    }

    // Utility methods

    final public MLStreamWriter cell(String ch, String type, String codepoint, String cat) {
        if (codepoint == null) codepoint = ch;
        int dotpos = type.indexOf('.');
        if (dotpos == -1) el(type);
        else {
            el(type.substring(0,dotpos));
            at("class",type.substring(dotpos+1));
        }
        /*
        if (color == -1) {
            el("th");
        } else {
            el("td");
            if (color != 0xFFFFFF) {
                at("bgcolor","#"+hex(color,6));
            }
        }
        */
        tx(ch).el("br").el("tt").tx16(codepoint);
        if (cat != null) tx(" ").tx(cat);
        cl().cl().cl();
        return this;
    }

    final public MLStreamWriter cell(String ch) {
        return cell(ch,"td",null,null);
    }

    final public MLStreamWriter cell(String ch, String type) {
        return cell(ch,type,null,null);
    }

    final public MLStreamWriter cell(String ch, String type, String codepoint) {
        return cell(ch,type,codepoint,null);
    }

    static public String hex(int i, int width) {
        String result = Long.toString(i & 0xFFFFFFFFL, 16).toUpperCase();
        return "00000000".substring(result.length(),width) + result;
    }

    /**
     * Supplies a zero-padded hex representation of an integer (without 0x)
     */
    static public String hex(int i) {
        return hex(i,8);
    }

    /**
     * Supplies a zero-padded hex representation of a Unicode character (without 0x, \\u)
     */
    static public String hex(char i) {
        return hex(i,4);
    }

    /**
     * Supplies a zero-padded hex representation of a Unicode String (without 0x, \\u)
     *@param sep can be used to give a sequence, e.g. hex("ab", ",") gives "0061,0062"
     */
    static public String hex(String s, String sep) {
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < s.length(); ++i) {
            if (i != 0) result.append(sep);
            result.append(hex(s.charAt(i)));
        }
        return result.toString();
    }

    static public String hex(String s) {
        return hex(s," ");
    }


    public void author(String name, String url) {
        el("font").at("size","-3").tx("[").el("a").at("href",url).tx(name).cl("a").el("script").el("!--");
        tx("document.write(', ', document.lastModified);");
        cl("!--").cl("script").tx("]").cl("font");
    }

    // ================== PRIVATES =================

    PrintWriter out;
    boolean isHTML;
    ArrayList stack = new ArrayList();
    boolean inElement = false;
    Normalizer formC = new Normalizer(Normalizer.NFC);
    int len;
    int maxLineLength = 60;
    // later, add better line end management, indenting

    static final int NONE=0, BEFORE=1, AFTER=2, BOTH=3, FORCE = 4; // chosen for bits!!

    final void print(String s) {
        print(s,NONE);
    }

    final void print(char c) {
        print(c,NONE);
    }

    final void print(String s, int doesBreak) {
        if ((doesBreak & BEFORE) != 0) tryBreak(s.length(), doesBreak);
        len += s.length();
        out.print(s);
        if ((doesBreak & AFTER) != 0) tryBreak(0, doesBreak);
    }

    final void print(char c, int doesBreak) {
        if ((doesBreak & BEFORE) != 0) tryBreak(1, doesBreak);
        ++len;
        out.print(c);
        if ((doesBreak & AFTER) != 0) tryBreak(0, doesBreak);
    }

    void tryBreak(int toAdd, int doesBreak) {
        if ((doesBreak & FORCE) != 0 || (len + toAdd) > maxLineLength) {
            out.println();
            len = stack.size();
            for (int i = 0; i < len; ++i) out.print(' ');
        }
    }

    public String quoted(String source) {
        source = formC.normalize(source);
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < source.length(); ++i) {
            char ch = source.charAt(i);
            switch(ch) {
            case '\'':
                if (!isHTML) {
                    result.append("&apos;");
                } else {
                    result.append(ch);
                }
                break;
            case '\"':
                result.append("&quot;");
                break;
            case '<':
                result.append("&lt;");
                break;
            case '&':
                result.append("&amp;");
                break;
            case '>':
                result.append("&gt;");
                break;
            case '\n': case '\r': case '\t':
                result.append(ch);
                break;
            default: if (ch < ' ' // do surrogates later
                || ch >= '\u007F' && ch <= '\u009F'
                || ch >= '\uD800' && ch <= '\uDFFF'
                || ch >= '\uFFFE') {
                    result.append('\uFFFD');
                } else {
                    result.append(ch);
                }
                break;
            }
        }
        return result.toString();
    }

}