package com.ibm.tools.translit;
import com.ibm.text.*;
import java.io.*;

/**
 * A command-line interface to the ICU4J transliterators.
 * @author Alan Liu
 */
public class Trans {

    public static void main(String[] args) throws Exception {
        boolean isHTML = false;
        int pos = 0;
        if (args.length < 3) { usage(); }
        if (args[pos].equals("-html")) {
            ++pos;
            isHTML = true;
        }
        if ((args.length-pos) != 3) { usage(); }
        String transName = args[pos++];
        String inName = args[pos++];
        String outName = args[pos++];
        Transliterator trans = Transliterator.getInstance(transName);
        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(inName), "UTF8"));
        PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(outName), "UTF8"));
        trans(trans, in, out, isHTML);
        out.close();
    }

    static void trans(Transliterator trans,
                      BufferedReader in, PrintWriter out, boolean isHTML) throws IOException {
        boolean inTag = false; // If true, we are within a <tag>
        for (;;) {
            String line = in.readLine();
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
        System.out.println("Usage: java com.ibm.tools.translit.Trans [-html] <trans> <infile> <outfile>");
        System.out.println("<trans>   Name of transliterator");
        System.out.println("<infile>  Name of input file");
        System.out.println("<outfile> Name of output file");
        System.out.println("-html     Only transliterate text outside of <tags>");
        System.exit(0);
    }
}
