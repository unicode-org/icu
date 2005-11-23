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
package com.ibm.richtext.textpanel;

import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

import com.ibm.richtext.styledtext.MConstText;

/**
 * This class allows MConstText instances to be the contents
 * of a Clipboard.  To store an MConstText on the clipboard,
 * construct a TransferableText from the MConstText, and make
 * the TransferableText the clipboard contents.
 *
 */
/*
 * Note:  this class inherits from StringSelection because of
 * a bug in the 1.1.7 system clipboard implementation.  The
 * system clipboard won't put text on the OS clipboard unless
 * the content is a StringSelection.
 */
final class TransferableText extends StringSelection
{
    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    private MConstText fText;

    private static String textToString(MConstText text) {
        char[] chars = new char[text.length()];
        text.extractChars(0, chars.length, chars, 0);
        return new String(chars);
    }

    /**
     * Create a TransferableText for the given text.
     * @param text the text to go on the Clipboard.  The text is
     *     adopted by this object.
     */
    public TransferableText(MConstText text) {

        super(textToString(text));

        fText = text;
    }

    public DataFlavor[] getTransferDataFlavors() {

        DataFlavor[] flavors = super.getTransferDataFlavors();
        DataFlavor[] result = new DataFlavor[flavors.length+1];
        result[0] = MConstText.styledTextFlavor;
        System.arraycopy(flavors, 0, result, 1, flavors.length);
        return result;
    }

    public boolean isDataFlavorSupported(DataFlavor flavor) {

        if (flavor.equals(MConstText.styledTextFlavor)) {
            return true;
        }
        else {
            return super.isDataFlavorSupported(flavor);
        }
    }

    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {

        if (flavor.equals(MConstText.styledTextFlavor)) {
            return fText;
        }
        else {
            return super.getTransferData(flavor);
        }
    }
}
