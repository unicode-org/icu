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
package com.ibm.richtext.awtui;

import java.awt.Frame;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.Window;

import com.ibm.richtext.textpanel.MTextPanel;

import com.ibm.richtext.uiimpl.resources.FrameResources;

import com.ibm.richtext.uiimpl.*;
import com.ibm.richtext.uiimpl.DialogItem.DialogFactory;

// TO DO:  Don't hard-code menu configurations.  Instead, specify them with
// strings somehow.  This is an improvement over what we had, and it'll do
// for now.

/**
 * AwtMenuBuilder creates a set of AWT menus for interacting
 * with an MTextPanel.  Future versions of this class may allow
 * clients to control the menu contents.
 * @see MTextPanel
 */
public final class AwtMenuBuilder extends MenuBuilder {
    
    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    private static final AwtMenuBuilder INSTANCE = new AwtMenuBuilder();
    
    /**
     * Id for an Edit menu.  The Edit menu has the following items:
     * <ul>
     * <li><b>Undo</b> - invoke undo() on the MTextPanel</li>
     * <li><b>Redo</b> - invoke redo() on the MTextPanel</li>
     * <li><b>Cut</b> - invoke cut() on the MTextPanel</li>
     * <li><b>Copy</b> - invoke copy() on the MTextPanel</li>
     * <li><b>Paste</b> - invoke paste() on the MTextPanel</li>
     * <li><b>Clear</b> - invoke clear() on the MTextPanel</li>
     * <li><b>Select All</b> - invoke selectAll() on the MTextPanel</li>
     * </ul>
     */
    public static final int EDIT = MenuBuilder.EDIT;
    /**
     * Id for the point sizes menu.  The menu has items that set the size of a character
     * in a typeface.
     */
    public static final int SIZE = MenuBuilder.SIZE;
    /**
     * Id for a Style menu.  The Style menu has the following items:
     * <ul>
     * <li><b>Plain</b> - remove <code>WEIGHT</code>,
     *                    <code>POSTURE</code>,
     *                    <code>UNDERLINE</code> and
     *                    <code>STRIKETHROUGH</code> attributes from the
     * current selection</li>
     * <li><b>Bold</b> - add <code>{WEIGHT,WEIGHT_BOLD}</code> to
     * the current selection</li>
     * <li><b>Italic</b> - add <code>{POSTURE,POSTURE_ITALIC}</code> to
     * the current selection</li>
     * <li><b>Underline</b> - add <code>{UNDERLINE,UNDERLINE_ON}</code> to
     * the current selection</li>
     * <li><b>Strikethrough</b> - add <code>{STRIKETHROUGH,STRIKETHROUGH_ON}</code>
     * to the current selection</li>
     * <li><b>Font...</b> - display a dialog allowing the user to
     * select a typeface (font family) for the current selection</li>
     * <li><b>Forecolor...</b> - display a dialog allowing the user to
     * select a foreground color for the current selection</li>
     * <li><b>Backcolor...</b> - display a dialog allowing the user to
     * select a background color for the current selection</li>
     * </ul>
     */
    public static final int STYLE = MenuBuilder.STYLE;
    /**
     * Id for a paragraph alignment menu.  The menu has the following items:
     * <ul>
     * <li><b>Leading</b> - give selected paragraph(s) LEADING flush</li>
     * <li><b>Center</b> - give selected paragraph(s) CENTER flush</li>
     * <li><b>Trailing</b> - give selected paragraph(s) TRAILING flush</li>
     * <li><b>Justified</b> - give selected paragraph(s) full justification</li>
     * </ul>
     */
    public static final int FLUSH = MenuBuilder.FLUSH;
    /**
     * Id for a menu that sets the KeyRemap
     * on an MTextPanel.  The menu has the following items:
     * <ul>
     * <li><b>Default</b> - set KeyRemap to identity remap</li>
     * <li><b>Arabic</b> - set KeyRemap to Arabic transliteration</li>
     * <li><b>Hebrew</b> - set KeyRemap to Hebrew transliteration</li>
     * <li><b>Israel Nikud</b> - set KeyRemap to Israel Nikud</li>
     * <li><b>Thai Ketmanee</b> - set KeyRemap to Thai Ketmanee</li>
     * </ul>
     */
    public static final int KEYMAP = MenuBuilder.KEYMAP;
    /**
     * Id for a menu that sets
     * the primary run direction for a paragraph.  Run direction can be left-to-right,
     * right-to-left, or can use the default run direction from the Unicode bidi algorithm.
     */
    public static final int BIDI = MenuBuilder.BIDI;
    /**
     * Id for a menu with an <b>About</b> item.  When selected, 
     * the item displays a Frame containing some
     * self-promotional text.
     */
    public static final int ABOUT = MenuBuilder.ABOUT;
    
    /**
     * Return an instance of AwtMenuBuilder.
     */
    public static AwtMenuBuilder getInstance() {
        
        return INSTANCE;
    }
    
    private MenuBar fMenuBar;
    
    private AwtMenuBuilder() {
    }
    
    /**
     * Add a standard set of menus to the given menu bar.  The menus 
     * will interact with the given MTextPanel.
     * @param menuBar the MenuBar to which menus are added
     * @param textPanel the MTextPanel with which the menus interact
     * @param frame a Frame to use as the parent of any dialogs created by a 
     *   a menu item.  If null, menu items which create dialogs will be omitted.
     */
    public void createMenus(MenuBar menuBar, 
                            MTextPanel textPanel,
                            Frame frame) {
        
        createMenus(menuBar, textPanel, frame, defaultMenus);
    }
    
    /**
     * Add a set of menus to the given menu bar.  The menus 
     * will interact with the given MTextPanel.
     * @param menuBar the MenuBar to which menus are added
     * @param textPanel the MTextPanel with which the menus interact
     * @param frame a Frame to use as the parent of any dialogs created by a 
     *   a menu item.  If null, menu items which create dialogs will be omitted.
     * @param menus an array of integer menu id's.  Each element of the
     *   array must be one of this class's menu id constants.  If null,
     *   the default menus are created.
     */
    public void createMenus(MenuBar menuBar, 
                            MTextPanel textPanel,
                            Frame frame,
                            int[] menus) {
        
        if (menus == null) {
            menus = defaultMenus;
        }
        
        synchronized (MItem.LOCK) {
            
            fMenuBar = menuBar;
            doCreateMenus(textPanel, frame, menus);
            fMenuBar = null;
        }
    }
    
    protected void handleAddMenu(String key) {
        
        Menu menu = new Menu(ResourceUtils.getResourceString(key));
        fMenuBar.add(menu);
        MItem.setItemFactory(new AwtMenuFactory(menu));
    }

    protected DialogFactory createObjectDialogFactory(final String dialogTitle,
                                                      final String dialogMessage,
                                                      final Object key,
                                                      final boolean character,
                                                      final String[] names,
                                                      final Object[] values) {
        
        final Frame dialogParent = fDialogParent;

        return new DialogFactory() {    
            public Window createDialog(MTextPanel textPanel) {
                return new ObjectDialog(dialogParent,
                                        dialogTitle,
                                        dialogMessage,
                                        textPanel,
                                        key,
                                        character,
                                        names,
                                        values);
            }
        };
    }
    
    protected DialogFactory createNumberDialogFactory(final String dialogTitle,
                                                      final String dialogMessage,
                                                      final Object key,
                                                      final boolean character) {
        
        final Frame dialogParent = fDialogParent;
            
        return new DialogFactory() {
            public Window createDialog(MTextPanel textPanel) {
                return new NumberDialog(dialogParent,
                                        dialogTitle,
                                        dialogMessage,
                                        textPanel,
                                        key,
                                        character,
                                        1);
            }
        };
    }    
    
    protected DialogFactory createAboutDialogFactory() {
        
        return new DialogFactory() {
            public Window createDialog(MTextPanel textPanel) {
                String title = ResourceUtils.getResourceString(FrameResources.ABOUT_TITLE);
                return new MessageDialog(title, AboutText.getAboutText());
            }
        };
    }
}