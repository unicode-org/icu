/*
 *******************************************************************************
 * Copyright (C) 2002-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************/
package com.ibm.richtext.uiimpl.resources;

import java.util.ListResourceBundle;
import java.awt.event.KeyEvent;

public final class FrameResources extends ListResourceBundle {

    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
/*
 * These Strings are keys to other String resources.
 */
    // Menu names
    public static final String EDIT = "Edit";
    public static final String SIZE = "Size";
    public static final String FONT = "Font";
    public static final String STYLE = "Style";
    public static final String FLUSH = "Flush";
    public static final String KEYMAP = "Keymap";
    public static final String ABOUT_MENU = "About";
    public static final String BIDI = "Bidi";
    
    // Buttons
    public static final String OK = "OK";
    public static final String CANCEL = "Cancel";

    // Colors
    public static final String BLACK = "Black";
    public static final String WHITE = "White";
    public static final String GREEN = "Green";
    public static final String BLUE = "Blue";
    public static final String CYAN = "Cyan";
    public static final String GRAY = "Gray";
    public static final String DARK_GRAY = "Dark Gray";
    public static final String LIGHT_GRAY = "Light Gray";
    public static final String MAGENTA = "Magenta";
    public static final String ORANGE = "Orange";
    public static final String PINK = "Pink";
    public static final String RED = "Red";
    public static final String YELLOW = "Yellow";
    public static final String NONE = "None";

    // Dialog titles and messages
    public static final String SET_SIZE_TITLE = "Set Font Size";
    public static final String SET_SIZE_LABEL = "Font Size:";

    public static final String SET_SUPERSCRIPT_TITLE = "Set Superscript";
    public static final String SET_SUPERSCRIPT_LABEL = "Superscript:";
    public static final String SET_SUBSCRIPT_TITLE = "Set Subscript";
    public static final String SET_SUBSCRIPT_LABEL = "Subscript:";
    public static final String SET_FOREGROUND_TITLE = "Set Foreground";
    public static final String SET_FOREGROUND_LABEL = "Foreground:";
    public static final String SET_BACKGROUND_TITLE = "Set Background";
    public static final String SET_BACKGROUND_LABEL = "Background:";
    public static final String SET_FONT_TITLE = "Set Font";
    public static final String SET_FONT_LABEL = "Font:";

    public static final String ABOUT_TITLE = "About the RichEdit Control";

    // This is the only String which is not its own value:
    public static final String ABOUT_TEXT = "About text";
    private static final String ACTUAL_ABOUT_TEXT =
        "Copyright (C) IBM Corp. 1996-2002 All rights reserved.\n\n" +
        "John Raley\n" +
        "Stephen F. Booth\n" +
        "Doug Felt\n" +
        "John Fitzpatrick\n" +
        "Rich Gillam";
/*
 * The following Strings are keys to MenuData resources.  They
 * also double as the default menu label text.
 */
    // Edit menu
    public static final String UNDO = "Undo";
    public static final String REDO = "Redo";
    public static final String CUT = "Cut";
    public static final String COPY = "Copy";
    public static final String PASTE = "Paste";
    public static final String CLEAR = "Clear";
    public static final String SELECT_ALL = "Select All";

    // Flush menu
    public static final String LEADING = "Leading";
    public static final String TRAILING = "Trailing";
    public static final String CENTER = "Center";
    public static final String JUSTIFIED = "Justified";

    // About menu
    public static final String ABOUT_ITEM = "About...";

    // Keymap menu
    public static final String DEFAULT = "Default";
    public static final String ARABIC = "Arabic";
    public static final String HEBREW = "Hebrew";
    public static final String ISRAEL_NIKUD = "Israel Nikud";
    public static final String THAI_KETMANEE = "Thai Ketmanee";

    // Style menu
    public static final String PLAIN = "Plain";
    public static final String BOLD = "Bold";
    public static final String ITALIC = "Italic";
    public static final String UNDERLINE = "Underline";
    public static final String STRIKETHROUGH = "Strikethrough";
    public static final String SUPERSCRIPT = "Superscript";
    public static final String SUBSCRIPT = "Subscript";
    public static final String SUPERSCRIPT_DIALOG = "Superscript...";
    public static final String SUBSCRIPT_DIALOG = "Subscript...";
    public static final String FORECOLOR_DIALOG = "Forecolor...";
    public static final String BACKCOLOR_DIALOG = "Backcolor...";
    public static final String FONT_DIALOG = "Font...";
    

    // Size menu
    public static final String OTHER_DIALOG = "Other...";
    
    // Bidi menu
    public static final String DEFAULT_DIRECTION = "Default Paragraph Direction";
    public static final String LTR_DIRECTION = "Left-to-right Paragraph Direction";
    public static final String RTL_DIRECTION = "Right-to-left Paragraph Direction";

    private static Object[] makeMenuData(String name,
                                         char shortCutChar,
                                         int shortCutKey) {

        return new Object[] { name, new MenuData(name, shortCutChar, shortCutKey) };
    }

    private static Object[] makeMenuData(String name) {

        return new Object[] { name, new MenuData(name) };
    }

    private static Object[] duplicate(Object obj) {

        return new Object[] { obj, obj };
    }

    protected Object[][] getContents() {

        return new Object[][] {
            makeMenuData(UNDO, 'z', KeyEvent.VK_Z),
            makeMenuData(REDO, 'r', KeyEvent.VK_R),
            makeMenuData(CUT, 'x', KeyEvent.VK_X),
            makeMenuData(COPY, 'c', KeyEvent.VK_C),
            makeMenuData(PASTE, 'v', KeyEvent.VK_V),
            makeMenuData(CLEAR),
            makeMenuData(SELECT_ALL),
            makeMenuData(LEADING),
            makeMenuData(CENTER),
            makeMenuData(TRAILING),
            makeMenuData(JUSTIFIED),
            makeMenuData(ABOUT_ITEM),
            makeMenuData(DEFAULT),
            makeMenuData(HEBREW),
            makeMenuData(ARABIC),
            makeMenuData(ISRAEL_NIKUD),
            makeMenuData(THAI_KETMANEE),
            makeMenuData(PLAIN),
            makeMenuData(BOLD, 'b', KeyEvent.VK_B),
            makeMenuData(ITALIC, 'i', KeyEvent.VK_I), // why doesn't this work in Swing?
                                                      // this is a Tab in AWT!!!
            makeMenuData(UNDERLINE, 'u', KeyEvent.VK_U),
            makeMenuData(STRIKETHROUGH),
            makeMenuData(SUPERSCRIPT),
            makeMenuData(SUBSCRIPT),
            makeMenuData(SUPERSCRIPT_DIALOG),
            makeMenuData(SUBSCRIPT_DIALOG),
            makeMenuData(FORECOLOR_DIALOG),
            makeMenuData(BACKCOLOR_DIALOG),
            makeMenuData(FONT_DIALOG),
            makeMenuData(OTHER_DIALOG),
            makeMenuData(DEFAULT_DIRECTION),
            makeMenuData(LTR_DIRECTION),
            makeMenuData(RTL_DIRECTION),
            duplicate(OK),
            duplicate(CANCEL),
            duplicate(BLACK),
            duplicate(WHITE),
            duplicate(GREEN),
            duplicate(BLUE),
            duplicate(CYAN),
            duplicate(GRAY),
            duplicate(DARK_GRAY),
            duplicate(LIGHT_GRAY),
            duplicate(MAGENTA),
            duplicate(ORANGE),
            duplicate(PINK),
            duplicate(RED),
            duplicate(YELLOW),
            duplicate(NONE),
            duplicate(SET_SIZE_TITLE),
            duplicate(SET_SIZE_LABEL),
            duplicate(SET_SUPERSCRIPT_TITLE),
            duplicate(SET_SUPERSCRIPT_LABEL),
            duplicate(SET_SUBSCRIPT_TITLE),
            duplicate(SET_SUBSCRIPT_LABEL),
            duplicate(SET_FOREGROUND_TITLE),
            duplicate(SET_FOREGROUND_LABEL),
            duplicate(SET_BACKGROUND_TITLE),
            duplicate(SET_BACKGROUND_LABEL),
            duplicate(SET_FONT_TITLE),
            duplicate(SET_FONT_LABEL),
            duplicate(EDIT),
            duplicate(SIZE),
            duplicate(FONT),
            duplicate(STYLE),
            duplicate(FLUSH),
            duplicate(KEYMAP),
            duplicate(BIDI),
            duplicate(ABOUT_MENU),
            duplicate(ABOUT_TITLE),
            { ABOUT_TEXT, ACTUAL_ABOUT_TEXT }
        };
    }
}
