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

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.io.InputStream;

import java.awt.Toolkit;

import com.ibm.richtext.textlayout.attributes.AttributeMap;

import com.ibm.richtext.styledtext.MConstText;
import com.ibm.richtext.styledtext.StyledText;

/**
* Wrapper for java.awt.datatransfer.Clipboard
* Packages an MConstText in a transferable, and puts it on the clipboard.
*/

class StyledTextClipboard implements ClipboardOwner {

    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    // This class has a workaround for a bug in the Windows system clipboard.
    // The system clipboard will only return String content, even
    // though it has a reference to the contents.  So if our
    // clipboard is the system clipboard, we'll keep a reference
    // to the content and use that instead of what the Clipboard returns.

    private static Clipboard SYSTEM = null;
    static {
        try {
            SYSTEM = Toolkit.getDefaultToolkit().getSystemClipboard();
        }
        catch(Throwable th) {
        }
    }

    private static StyledTextClipboard fgSystemClipboard = null;

    public static StyledTextClipboard getClipboardFor(Clipboard clipboard) {

        if (clipboard == SYSTEM && SYSTEM != null) {
            synchronized(SYSTEM) {
                if (fgSystemClipboard == null) {
                    fgSystemClipboard = new StyledTextClipboard(SYSTEM, true);
                }
            }
            return fgSystemClipboard;
        }
        else {
            return new StyledTextClipboard(clipboard, false);
        }
    }

    private Clipboard fClipboard;
    private boolean fUseLocalContents;
    private Transferable fContents = null;

    private StyledTextClipboard(Clipboard clipboard, boolean useLocalContents) {

        if (clipboard == null) {
            fClipboard = new Clipboard("TextPanel clipboard");
        }
        else {
            fClipboard = clipboard;
        }

        fUseLocalContents = useLocalContents;
    }

    public void lostOwnership(Clipboard clipboard,
                              Transferable contents) {
        if (contents == fContents) {
            this.fContents = null;
        }
    }

    public void setContents(MConstText newContents) {

        TransferableText contents = new TransferableText(newContents);
        if (fClipboard == SYSTEM) {
            fContents = contents;
        }
        fClipboard.setContents(contents, this);
    }

    private Transferable getClipboardContents() {

        if (fUseLocalContents && fContents != null) {
            return fContents;
        }

        return fClipboard.getContents(this);
    }

    /**
     * Has contents - faster than getContents for finding out whether the
     * clipboard has text.
     */
    public boolean hasContents() {

        Transferable contents = getClipboardContents();

        if (contents == null) {
            return false;
        }

        return contents.isDataFlavorSupported(MConstText.styledTextFlavor) ||
               contents.isDataFlavorSupported(DataFlavor.stringFlavor) ||
               contents.isDataFlavorSupported(DataFlavor.plainTextFlavor);
    }

    private String getString(InputStream inStream) throws IOException {

        String value = new String();
        int bytesRead;

        do {
            byte inBytes[] = new byte[inStream.available()];
            bytesRead = inStream.read(inBytes);

            if (bytesRead != -1)
                value = value + new String(inBytes);

        } while (bytesRead != -1);

        return value;
    }

    /**
     * If the Clipboard has text content, return it as an
     * MConstText.  Otherwise return null.
     * @param defaultStyle the style to apply to unstyled
     *      text (such as a String).  If the clipboard
     *      has styled text this parameter is not used.
     */
    public MConstText getContents(AttributeMap defaultStyle) {

        Transferable contents = getClipboardContents();

        if (contents == null) {
            return null;
        }

        DataFlavor flavors[] = contents.getTransferDataFlavors();

        // search flavors for our flavor, String flavor and raw text flavor

        Exception ex = null;

        try {
           int i;

            for (i=0; i < flavors.length; i++) {
                if (flavors[i].equals(MConstText.styledTextFlavor))
                    break;
            }

            if (i < flavors.length) {

                Object data = contents.getTransferData(MConstText.styledTextFlavor);
                if (data == null)
                    System.out.println("Data is null.");
                return (MConstText) data;
            }

            for (i=0; i < flavors.length; i++) {
                if (flavors[i].equals(DataFlavor.stringFlavor))
                    break;
            }

            if (i < flavors.length) {

                Object data = contents.getTransferData(DataFlavor.stringFlavor);
                return new StyledText((String) data, defaultStyle);
            }

            for (i=0; i < flavors.length; i++) {
                if (flavors[i].equals(DataFlavor.plainTextFlavor))
                    break;
            }

            if (i < flavors.length) {

                Object data = contents.getTransferData(DataFlavor.plainTextFlavor);

                String textString = getString((InputStream) data);
                return new StyledText(textString, defaultStyle);
            }
        }
        catch(UnsupportedFlavorException e) {
            ex = e;
        }
        catch(IOException e) {
            ex = e;
        }

        System.out.println("Exception when retrieving data.  Exception:" + ex);
        return null;
    }
}
