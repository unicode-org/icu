/*
******************************************************************************
* Copyright (C) 2007-2009, International Business Machines Corporation and   *
* others. All Rights Reserved.                                               *
******************************************************************************
*/

package com.ibm.icu.impl.duration.impl;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import com.ibm.icu.lang.UCharacter;

public class XMLRecordReader implements RecordReader {
    private Reader r;

    private List<String> nameStack;

    private boolean atTag;

    private String tag; // cache

    public XMLRecordReader(Reader r) {
        this.r = r;
        this.nameStack = new ArrayList<String>();

        // skip XML prologue
        if (getTag().startsWith("?xml")) {
            advance();
        }

        // skip FIRST comment
        if (getTag().startsWith("!--")) {
            advance();
        }
    }

    public boolean open(String title) {
        if (getTag().equals(title)) {
            nameStack.add(title);
            advance();
            return true;
        }
        return false;
    }

    public boolean close() {
        int ix = nameStack.size() - 1;
        String name = nameStack.get(ix);
        if (getTag().equals("/" + name)) {
            nameStack.remove(ix);
            advance();
            return true;
        }
        return false;
    }

    public boolean bool(String name) {
        String s = string(name);
        if (s != null) {
            return "true".equals(s);
        }
        return false;
    }

    public boolean[] boolArray(String name) {
        String[] sa = stringArray(name);
        if (sa != null) {
            boolean[] result = new boolean[sa.length];
            for (int i = 0; i < sa.length; ++i) {
                result[i] = "true".equals(sa[i]);
            }
            return result;
        }
        return null;
    }

    public char character(String name) {
        String s = string(name);
        if (s != null) {
            return s.charAt(0);
        }
        return '\uffff';
    }

    public char[] characterArray(String name) {
        String[] sa = stringArray(name);
        if (sa != null) {
            char[] result = new char[sa.length];
            for (int i = 0; i < sa.length; ++i) {
                result[i] = sa[i].charAt(0);
            }
            return result;
        }
        return null;
    }

    public byte namedIndex(String name, String[] names) {
        String sa = string(name);
        if (sa != null) {
            for (int i = 0; i < names.length; ++i) {
                if (sa.equals(names[i])) {
                    return (byte) i;
                }
            }
        }
        return (byte) -1;
    }

    public byte[] namedIndexArray(String name, String[] names) {
        String[] sa = stringArray(name);
        if (sa != null) {
            byte[] result = new byte[sa.length];
            loop: for (int i = 0; i < sa.length; ++i) {
                String s = sa[i];
                for (int j = 0; j < names.length; ++j) {
                    if (names[j].equals(s)) {
                        result[i] = (byte) j;
                        continue loop;
                    }
                }
                result[i] = (byte) -1;
            }
            return result;
        }
        return null;
    }

    public String string(String name) {
        if (match(name)) {
            String result = readData();
            if (match("/" + name)) {
                return result;
            }
        }
        return null;
    }

    public String[] stringArray(String name) {
        if (match(name + "List")) {
            List<String> list = new ArrayList<String>();
            String s;
            while (null != (s = string(name))) {
                if ("Null".equals(s)) {
                    s = null;
                }
                list.add(s);
            }
            if (match("/" + name + "List")) {
                return (String[]) list.toArray(new String[list.size()]);
            }
        }
        return null;
    }

    public String[][] stringTable(String name) {
        if (match(name + "Table")) {
            List<String[]> list = new ArrayList<String[]>();
            String[] sa;
            while (null != (sa = stringArray(name))) {
                list.add(sa);
            }
            if (match("/" + name + "Table")) {
                return list.toArray(new String[list.size()][]);
            }
        }
        return null;
    }

    private boolean match(String target) {
        if (getTag().equals(target)) {
            // System.out.println("match '" + target + "'");
            advance();
            return true;
        }
        return false;
    }

    private String getTag() {
        if (tag == null) {
            tag = readNextTag();
        }
        return tag;
    }

    private void advance() {
        tag = null;
    }

    private String readData() {
        StringBuffer sb = new StringBuffer();
        boolean inWhitespace = false;
        // boolean inAmp = false;
        while (true) {
            int c = readChar();
            if (c == -1 || c == '<') {
                atTag = c == '<';
                break;
            }
            if (c == '&') {
                c = readChar();
                if (c == '#') {
                    StringBuffer numBuf = new StringBuffer();
                    int radix = 10;
                    c = readChar();
                    if (c == 'x') {
                        radix = 16;
                        c = readChar();
                    }
                    while (c != ';' && c != -1) {
                        numBuf.append((char) c);
                        c = readChar();
                    }
                    try {
                        int num = Integer.parseInt(numBuf.toString(), radix);
                        c = (char) num;
                    } catch (NumberFormatException ex) {
                        System.err.println("numbuf: " + numBuf.toString()
                                + " radix: " + radix);
                        throw ex;
                    }
                } else {
                    StringBuffer charBuf = new StringBuffer();
                    while (c != ';' && c != -1) {
                        charBuf.append((char) c);
                        c = readChar();
                    }
                    String charName = charBuf.toString();
                    if (charName.equals("lt")) {
                        c = '<';
                    } else if (charName.equals("gt")) {
                        c = '>';
                    } else if (charName.equals("quot")) {
                        c = '"';
                    } else if (charName.equals("apos")) {
                        c = '\'';
                    } else if (charName.equals("amp")) {
                        c = '&';
                    } else {
                        System.err.println("unrecognized character entity: '"
                                + charName + "'");
                        continue;
                    }
                }
            }

            if (UCharacter.isWhitespace(c)) {
                if (inWhitespace) {
                    continue;
                }
                c = ' ';
                inWhitespace = true;
            } else {
                inWhitespace = false;
            }
            sb.append((char) c);
        }
        //System.err.println("read data: '" + sb.toString() + "'");
        return sb.toString();
    }

    private String readNextTag() {
        int c = '\0';
        while (!atTag) {
            c = readChar();
            if (c == '<' || c == -1) {
                if (c == '<') {
                    atTag = true;
                }
                break;
            }
            if (!UCharacter.isWhitespace(c)) {
                System.err.println("Unexpected non-whitespace character "
                        + Integer.toHexString(c));
                break;
            }
        }

        if (atTag) {
            atTag = false;
            StringBuffer sb = new StringBuffer();
            while (true) {
                c = readChar();
                if (c == '>' || c == -1) {
                    break;
                }
                sb.append((char) c);
            }
            // System.err.println("read tag: '" + sb.toString() + "'");
            return sb.toString();
        }
        return null;
    }

    int readChar() {
        try {
            return r.read();
        } catch (IOException e) {
            // assume end of input
        }
        return -1;
    }
}
