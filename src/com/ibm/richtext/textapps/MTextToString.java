/*
 * (C) Copyright IBM Corp. 1998-2004.  All Rights Reserved.
 *
 * The program is provided "as is" without any warranty express or
 * implied, including the warranty of non-infringement and the implied
 * warranties of merchantibility and fitness for a particular purpose.
 * IBM will not be liable for any damages suffered by you as a result
 * of using the Program. In no event will IBM be liable for any
 * special, indirect or consequential damages or lost profits even if
 * IBM has been advised of the possibility of their occurrence. IBM
 * will not be liable for any third party claims against you.
 */
package com.ibm.richtext.textapps;

import com.ibm.richtext.styledtext.MConstText;

import java.io.*;

public final class MTextToString {

    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    public static void main(String[] args) {


        if (args.length != 2) {
            usage();
        }
        else {
            writeMTextAsString(args[0], args[1]);
        }
    }

    private static void usage() {

        System.out.println("Usage: MTextToString inFile outFile");
        System.out.println("inFile must be a serialized MConstText");
        System.out.println("On exit, outFile will be a serialized String ");
        System.out.println("containing the characters in the text.");
        System.out.println("inFile and outFile must not be the same.");
        System.exit(1);
    }

    public static void writeMTextAsString(String inFile, String outFile) {

        File file = new File(inFile);
        MConstText text = FileUtils.loadMText(file);

        if (text != null) {
            char[] ch = new char[text.length()];
            text.extractChars(0, ch.length, ch, 0);
            String str = new String(ch);
            writeString(str, outFile);
        }
        else {
            System.out.println("Can't read inFile.");
        }
    }

    public static void writeString(String stringToWrite, String outFile) {

        File file = new File(outFile);
        Throwable error = null;

        try {
            OutputStream outStream = new FileOutputStream(file);
            ObjectOutputStream objStream = new ObjectOutputStream(outStream);

            objStream.writeObject(stringToWrite);
            outStream.close();
            return;
        }
        catch(IOException e) {
            error = e;
        }
        catch(ClassCastException e) {
            error = e;
        }

        error.printStackTrace();
    }
}