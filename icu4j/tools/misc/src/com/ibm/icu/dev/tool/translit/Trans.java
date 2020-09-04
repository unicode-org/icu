// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/**
 *******************************************************************************
 * Copyright (C) 2001-2010, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.tool.translit;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import com.ibm.icu.text.Transliterator;

/**
 * A command-line interface to the ICU4J transliterators.
 * @author Alan Liu
 */
public class Trans {

    public static void main(String[] args) throws Exception {
        boolean isHTML = false;
        int pos = 0;

        String transName = null; // first untagged string is this
        String inText = null; // all other untagged strings are this
        String inName = null;
        String outName = null;

        while (pos < args.length) {
            if (args[pos].equals("-html")) {
                isHTML = true;
            } else if (args[pos].equals("-i")) {
                if (++pos == args.length) usage();
                inName = args[pos];
            } else if (args[pos].equals("-o")) {
                if (++pos == args.length) usage();
                outName = args[pos];
            } else if (transName == null) {
                transName = args[pos];
            } else {
                if (inText == null) {
                    inText = args[pos];
                } else {
                    inText = inText + " " + args[pos];
                }
            }
            ++pos;
        }

        if (inText != null && inName != null) {
            usage();
        }

        Transliterator trans = Transliterator.getInstance(transName);
        BufferedReader in = null;
        if (inName != null) {
            in = new BufferedReader(new InputStreamReader(new FileInputStream(inName), "UTF8"));
        }
        PrintWriter out = null;
        if (outName != null) {
            out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(outName), "UTF8"));
        } else {
            out = new PrintWriter(System.out);
        }
        trans(trans, inText, in, out, isHTML);
        out.close();
    }

    static void trans(Transliterator trans, String inText,
                      BufferedReader in, PrintWriter out, boolean isHTML) throws IOException {
        boolean inTag = false; // If true, we are within a <tag>
        for (;;) {
            String line = null;
            if (inText != null) {
                line = inText;
                inText = null;
            } else if (in != null) {
                line = in.readLine();
            }
            if (line == null) {
                break;
            }
            if (isHTML) {
                // Pass tags between < and > unchanged
                StringBuffer buf = new StringBuffer();
                int right = -1;
                if (inTag) {
                    right = line.indexOf('>');
                    if (right < 0) {
                        right = line.length()-1;
                    }
                    buf.append(line.substring(0, right+1));
                    if (DEBUG) System.out.println("*S:" + line.substring(0, right+1));
                    inTag = false;
                }
                for (;;) {
                    int left = line.indexOf('<', right+1);
                    if (left < 0) {
                        if (right < line.length()-1) {
                            buf.append(trans.transliterate(line.substring(right+1)));
                            if (DEBUG) System.out.println("T:" + line.substring(right+1));
                        }
                        break;
                    }
                    // Append transliterated segment right+1..left-1
                    buf.append(trans.transliterate(line.substring(right+1, left)));
                    if (DEBUG) System.out.println("T:" + line.substring(right+1, left));
                    right = line.indexOf('>', left+1);
                    if (right < 0) {
                        inTag = true;
                        buf.append(line.substring(left));
                        if (DEBUG) System.out.println("S:" + line.substring(left));
                        break;
                    }
                    buf.append(line.substring(left, right+1));
                    if (DEBUG) System.out.println("S:" + line.substring(left, right+1));
                }
                line = buf.toString();
            } else {
                line = trans.transliterate(line);
            }
            out.println(line);
        }
    }

    static final boolean DEBUG = false;

    /**
     * Emit usage and die.
     */
    static void usage() {
        System.out.println("Usage: java com.ibm.icu.dev.tool.translit.Trans [-html] <trans> ( <input> | -i <infile>) [ -o <outfile> ]");
        System.out.println("<trans>   Name of transliterator");
        System.out.println("<input>   Text to transliterate");
        System.out.println("<infile>  Name of input file");
        System.out.println("<outfile> Name of output file");
        System.out.println("-html     Only transliterate text outside of <tags>");
        System.out.println("Input may come from the command line or a file.\n");
        System.out.println("Ouput may go to stdout or a file.\n");
        System.exit(0);
    }
}
