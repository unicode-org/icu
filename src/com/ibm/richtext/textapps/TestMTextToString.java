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
import java.io.File;

public final class TestMTextToString {

    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    public static void main(String[] args) {

        if (args.length != 2 || args[0].equals(args[1])) {
            usage();
        }
        else {
            boolean success = testMTextToString(args[0], args[1]);
            System.out.println(success? "PASSED" : "FAILED");
        }
    }

    private static void usage() {

        System.out.println("Usage: TestMTextToString mtextFile stringFile");
        System.out.println("Compares the characters in mtextFile to the");
        System.out.println("String in stringFile.");
        System.exit(0);
    }

    public static boolean testMTextToString(String mtextFile, String stringFile) {

        boolean success = false;

        File mtext = new File(mtextFile);
        MConstText text = FileUtils.loadMText(mtext);
        if (text != null) {
            String str = StringToMText.loadString(new File(stringFile));
            if (str != null) {
                success = compareMTextToString(text, str);
            }
            else {
                System.out.println("Couldn't load String.");
            }
        }
        else {
            System.out.println("Couldn't load MText.");
        }

        return success;
    }

    public static boolean compareMTextToString(MConstText text, String str) {

        if (text.length() != str.length()) {
            return false;
        }
        for (int i=str.length()-1; i >= 0; i--) {
            if (text.at(i) != str.charAt(i)) {
                return false;
            }
        }
        return true;
    }
}