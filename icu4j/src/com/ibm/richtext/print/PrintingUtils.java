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
package com.ibm.richtext.print;

import com.ibm.richtext.styledtext.MConstText;

import com.ibm.richtext.textlayout.attributes.AttributeMap;

import java.awt.Frame;

/**
 * PrintingUtils contains a static method for printing styled text.
 * @see com.ibm.richtext.styledtext.MConstText
 */
public final class PrintingUtils {

    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";

    // Keep it out of Javadoc...
    private PrintingUtils() {
    }
    
    /**
     * Print the given text.  A Print dialog is presented to the user;
     * unless the user cancels, the text is printed.
     * @param text the text to print
     * @param defaultStyles default values for unspecified attributes
     * @param frame the parent of the Print dialog
     * @param jobTitle the title of the PrintJob
     */
    public static void userPrintText(MConstText text,
                                     AttributeMap defaultStyles,
                                     Frame frame,
                                     String jobTitle) {

        PrintContext.userPrintText(text, defaultStyles, frame, jobTitle);
    }
}