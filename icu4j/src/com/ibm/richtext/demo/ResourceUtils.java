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

import java.awt.MenuItem;
import java.awt.MenuShortcut;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Utility class for dealing with resource data.
 */
public final class ResourceUtils {

    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    private static ResourceBundle BUNDLE;
    static {
        String bundleName = "com.ibm.richtext.demo.EditorResources";
        try {
            BUNDLE = ResourceBundle.getBundle(bundleName);
        }
        catch(MissingResourceException e) {
            System.out.println("Couldn't load " + bundleName +
                               ";  Exception: " + e);
            BUNDLE = new EditorResources();
        }
    }

    public static String getString(String key) {

        try {
            return BUNDLE.getString(key);
        }
        catch(MissingResourceException e) {
            return key;
        }
    }

    public static MenuData getMenuData(String key) {
    
        try {
            return (MenuData) BUNDLE.getObject(key);
        }
        catch(MissingResourceException e) {
            return new MenuData(key);
        }
    }
        
    public static MenuItem createMenuItem(String key) {

        MenuData menuData = getMenuData(key);

        if (menuData.hasShortcut()) {
            MenuShortcut shortcut = new MenuShortcut(menuData.getShortcut());
            return new MenuItem(menuData.getName(), shortcut);
        }
        else {
            return new MenuItem(menuData.getName());
        }
    }
}