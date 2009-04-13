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

public final class MenuData {

    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    private String fName;
    private boolean fHasShortcut;
    private char fShortcut;
    private int fShortcutKeyCode;

    public MenuData(String name) {

        fName = name;
        fHasShortcut = false;
    }

    public MenuData(String name, char ch, int keyCode) {

        fName = name;
        fHasShortcut = true;
        fShortcut = ch;
        fShortcutKeyCode = keyCode;
    }

    public String getName() {

        return fName;
    }

    public char getShortcut() {

        if (!fHasShortcut) {
            throw new Error("Menu doesn't have shortcut");
        }
        return fShortcut;
    }
    
    public int getShortcutKeyCode() {
    
        return fShortcutKeyCode;
    }

    public boolean hasShortcut() {

        return fHasShortcut;
    }
}