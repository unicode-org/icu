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
import com.ibm.richtext.styledtext.StyledText;
import com.ibm.richtext.textlayout.attributes.AttributeMap;

import java.io.*;

public final class StringToMText {

    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    public static void main(String[] args) {


        if (args.length != 2 || args[0].equals(args[1])) {
            usage();
        }
        else {
            String str = loadString(new File(args[0]));
            if (str == null) {
                throw new Error("Couldn't load String from file " + args[0]);
            }
            MConstText text = new StyledText(str, AttributeMap.EMPTY_ATTRIBUTE_MAP);
            FileUtils.saveMText(new File(args[1]), text);
        }
    }

    private static void usage() {

        System.out.println("Usage: StringToMText inFile outFile");
        System.out.println("inFile must be a serialized String");
        System.out.println("On exit, outFile will be a serialized MText ");
        System.out.println("containing the characters in the string.");
        System.out.println("inFile and outFile must not be the same.");
        System.exit(1);
    }

    public static String loadString(File file) {

        Throwable error;

        try {
            FileInputStream inStream = new FileInputStream(file);
            ObjectInputStream objStream = new ObjectInputStream(inStream);

            String str = (String) objStream.readObject();
            inStream.close();
            return str;
        }
        catch(IOException e) {
            error = e;
        }
        catch(ClassNotFoundException e) {
            error = e;
        }
        catch(ClassCastException e) {
            error = e;
        }

        error.printStackTrace();
        return null;
    }
}