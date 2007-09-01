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
package com.ibm.richtext.demo;

import java.util.ListResourceBundle;
import java.awt.event.KeyEvent;

public final class EditorResources extends ListResourceBundle {

    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    // menu names - values are Strings
    public static final String FILE = "File";

    // file menu items - values are MenuData instances
    public static final String NEW = "New";
    public static final String NEW_WINDOW = "New Window";
    public static final String OPEN = "Open...";
    public static final String SAVE = "Save";
    public static final String SAVE_AS = "Save As...";
    public static final String SAVE_AS_STYLED = "Save As Styled Text...";
    public static final String SAVE_AS_TEXT = "Save As Plain Text...";
    public static final String CLOSE = "Close";
    public static final String PRINT = "Print";
    public static final String EXIT = "Exit";

    // button labels - values are Strings
    public static final String YES = "Yes";
    public static final String NO = "No";
    public static final String CANCEL = "Cancel";

    // message strings - values are Strings
    public static final String SAVE_MSG = "Save {0} before closing?";
    public static final String UNTITLED_MSG = "Untitled {0}";

    // window and dialog titles - values are Strings
    public static final String OPEN_TITLE = "Open Document";
    public static final String SAVE_TITLE = "Save As";

    /**
     * Convenience method that returns a two-element
     * Object array.  The first element is name, the
     * second is a MenuData instance with the given
     * shortcut.
     */
    private static Object[] makeEntry(String name,
                                      char shortCut,
                                      int keyCode) {

        return new Object[] { name, new MenuData(name, shortCut, keyCode) };
    }

    /**
     * Convenience method that returns a two-element
     * Object array.  The first element is name, the
     * second is a MenuData instance.
     */
    private static Object[] makeEntry(String name) {

        return new Object[] { name, new MenuData(name) };
    }

    /**
     * Convenience method that returns a two-element
     * Object array in which both elements are obj.
     */
    private static Object[] duplicate(Object obj) {

        return new Object[] { obj, obj };
    }

    protected Object[][] getContents() {

        return new Object[][] {
            duplicate(FILE),
            duplicate(YES),
            duplicate(NO),
            duplicate(CANCEL),
            duplicate(SAVE_MSG),
            duplicate(SAVE_TITLE),
            duplicate(OPEN_TITLE),
            duplicate(UNTITLED_MSG),
            makeEntry(NEW, 'n', KeyEvent.VK_N),
            makeEntry(NEW_WINDOW),
            makeEntry(OPEN, 'o', KeyEvent.VK_O),
            makeEntry(SAVE, 's', KeyEvent.VK_S),
            makeEntry(SAVE_AS),
            makeEntry(SAVE_AS_STYLED),
            makeEntry(SAVE_AS_TEXT),
            makeEntry(CLOSE),
            makeEntry(PRINT),
            makeEntry(EXIT),
        };
    }
}
