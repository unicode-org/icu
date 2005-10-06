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
package com.ibm.richtext.uiimpl;

import java.awt.Color;
import java.awt.Frame;

import java.text.NumberFormat;

import com.ibm.richtext.textlayout.attributes.TextAttribute;
import com.ibm.richtext.textpanel.MTextPanel;
import com.ibm.richtext.textpanel.KeyRemap;

import com.ibm.richtext.uiimpl.resources.FrameResources;
import com.ibm.richtext.uiimpl.resources.MenuData;

import com.ibm.richtext.uiimpl.DialogItem.DialogFactory;

public abstract class MenuBuilder {

    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    
    public static final int EDIT = 0;
    public static final int SIZE = 1;
    public static final int STYLE = 2;
    public static final int FLUSH = 3;
    public static final int KEYMAP = 4;
    public static final int BIDI = 5;
    public static final int ABOUT = 6;
    
    private Color[] colors = { Color.black, Color.white, Color.green, Color.blue,
                                Color.cyan, Color.gray, Color.darkGray, Color.lightGray,
                                Color.magenta, Color.orange, Color.pink, Color.red,
                                Color.yellow, null };
    private String[] colorNames = {
                        ResourceUtils.getResourceString(FrameResources.BLACK),
                        ResourceUtils.getResourceString(FrameResources.WHITE),
                        ResourceUtils.getResourceString(FrameResources.GREEN),
                        ResourceUtils.getResourceString(FrameResources.BLUE),
                        ResourceUtils.getResourceString(FrameResources.CYAN),
                        ResourceUtils.getResourceString(FrameResources.GRAY),
                        ResourceUtils.getResourceString(FrameResources.DARK_GRAY),
                        ResourceUtils.getResourceString(FrameResources.LIGHT_GRAY),
                        ResourceUtils.getResourceString(FrameResources.MAGENTA),
                        ResourceUtils.getResourceString(FrameResources.ORANGE),
                        ResourceUtils.getResourceString(FrameResources.PINK),
                        ResourceUtils.getResourceString(FrameResources.RED),
                        ResourceUtils.getResourceString(FrameResources.YELLOW),
                        ResourceUtils.getResourceString(FrameResources.NONE)
    };
    
    protected int[] defaultMenus = { EDIT, 
                                     SIZE,
                                     STYLE,
                                     FLUSH, 
                                     KEYMAP,
                                     BIDI,
                                     ABOUT };

    protected MTextPanel fTextPanel;
    protected Frame fDialogParent;

    protected MenuBuilder() {
    }
    
    protected final void doCreateMenus(MTextPanel textPanel, Frame frame, int[] menus) {
    
        fTextPanel = textPanel;
        fDialogParent = frame;
        
        for (int i=0; i < menus.length; i++) {
            switch(menus[i]) {
                case EDIT:
                    createEditMenu();
                    break;
                case SIZE:
                    createSizeMenu();
                    break;
                case STYLE:
                    createStyleMenu();
                    break;
                case FLUSH:
                    createFlushMenu();
                    break;
                case KEYMAP:
                    createKeymapMenu();
                    break;
                case BIDI:
                    createBidiMenu();
                    break;
                case ABOUT:
                    createAboutMenu();
                    break;
                default:
                    throw new IllegalArgumentException("Illegal menu: " + menus[i]);
            }
        }
                    
        fTextPanel = null;
        fDialogParent = null;
    }
    
    protected abstract void handleAddMenu(String key);
    
    protected abstract DialogFactory createObjectDialogFactory(String dialogTitle,
                                                               String dialogText,
                                                               Object key,
                                                               boolean character,
                                                               String[] names,
                                                               Object[] values);

    protected abstract DialogFactory createNumberDialogFactory(String dialogTitle,
                                                               String dialogText,
                                                               Object key,
                                                               boolean character);

    protected abstract DialogFactory createAboutDialogFactory();

    private void createEditMenu() {
        
        handleAddMenu(FrameResources.EDIT);
        
        new CommandMenuItem.UndoRedo(ResourceUtils.getMenuData(FrameResources.UNDO),
                                    CommandMenuItem.UndoRedo.UNDO).setTextPanel(fTextPanel);
        new CommandMenuItem.UndoRedo(ResourceUtils.getMenuData(FrameResources.REDO),
                                    CommandMenuItem.UndoRedo.REDO).setTextPanel(fTextPanel);
        MItem.getItemFactory().createSeparator();
        new CommandMenuItem.CutCopyClear(ResourceUtils.getMenuData(FrameResources.CUT),
                                        CommandMenuItem.CutCopyClear.CUT).setTextPanel(fTextPanel);
        new CommandMenuItem.CutCopyClear(ResourceUtils.getMenuData(FrameResources.COPY),
                                        CommandMenuItem.CutCopyClear.COPY).setTextPanel(fTextPanel);
        new CommandMenuItem.Paste(ResourceUtils.getMenuData(FrameResources.PASTE)).setTextPanel(fTextPanel);
        new CommandMenuItem.CutCopyClear(ResourceUtils.getMenuData(FrameResources.CLEAR),
                                        CommandMenuItem.CutCopyClear.CLEAR).setTextPanel(fTextPanel);
        MItem.getItemFactory().createSeparator();
        new CommandMenuItem.SelectAll(ResourceUtils.getMenuData(FrameResources.SELECT_ALL)).setTextPanel(fTextPanel);
    }
    
    private static final float[] DEFAULT_SIZES =
                                    {9, 10, 12, 14, 18, 24, 36, 48, 72};

    private void createSizeMenu() {
        
        createSizeMenu(DEFAULT_SIZES);
    }
    
    private void createSizeMenu(float[] sizes) {
                
        handleAddMenu(FrameResources.SIZE);

        if (sizes != DEFAULT_SIZES) {

            sizes = (float[]) sizes.clone();
            if (sizes.length == 0) {
                throw new IllegalArgumentException("sizes array has zero length");
            }

            float lastValue = sizes[0];
            for (int i=1; i < sizes.length; i++) {
                if (sizes[i] >= lastValue) {
                    throw new IllegalArgumentException(
                                                "sizes array must be increasing");
                }
                lastValue = sizes[i];
            }
        }
        
        Float[] values = new Float[sizes.length];
        MenuData[] mData = new MenuData[sizes.length];
        NumberFormat fmt = NumberFormat.getNumberInstance();
        
        for (int i=0; i < sizes.length; i++) {
            values[i] = new Float(sizes[i]);
            mData[i] = new MenuData(fmt.format(sizes[i]));
        }
        
        new StyleMenuItemSet(TextAttribute.SIZE,
                                values,
                                mData,
                                MenuItemSet.CHARACTER).setTextPanel(fTextPanel);
            
        if (fDialogParent != null) {
            String dialogTitle = ResourceUtils.getResourceString(FrameResources.SET_SIZE_TITLE);
            String dialogText = ResourceUtils.getResourceString(FrameResources.SET_SIZE_LABEL);
            DialogFactory factory = createNumberDialogFactory(dialogTitle,
                                                              dialogText,
                                                              TextAttribute.SIZE,
                                                              MenuItemSet.CHARACTER);
                                                              
            new DialogItem(ResourceUtils.getMenuData(FrameResources.OTHER_DIALOG), 
                            factory).setTextPanel(fTextPanel);
        }
    }
    
    private void createStyleMenu() {
        
        handleAddMenu(FrameResources.STYLE);

        Object[] keys =    { TextAttribute.WEIGHT,
                             TextAttribute.POSTURE,
                             TextAttribute.UNDERLINE,
                             TextAttribute.STRIKETHROUGH,
                             TextAttribute.SUPERSCRIPT,
                             TextAttribute.SUPERSCRIPT};
        Object[] values =  { TextAttribute.WEIGHT_BOLD,
                             TextAttribute.POSTURE_OBLIQUE,
                             TextAttribute.UNDERLINE_ON,
                             TextAttribute.STRIKETHROUGH_ON, 
                             new Integer(1),
                             new Integer(-1)};
        MenuData[] mData = { ResourceUtils.getMenuData(FrameResources.BOLD),
                             ResourceUtils.getMenuData(FrameResources.ITALIC),
                             ResourceUtils.getMenuData(FrameResources.UNDERLINE),
                             ResourceUtils.getMenuData(FrameResources.STRIKETHROUGH),
                             ResourceUtils.getMenuData(FrameResources.SUPERSCRIPT),
                             ResourceUtils.getMenuData(FrameResources.SUBSCRIPT)};

        new SubtractStyleMenuItem(keys, 
                                    ResourceUtils.getMenuData(FrameResources.PLAIN),
                                    MenuItemSet.CHARACTER).setTextPanel(fTextPanel);
                                                 
        for (int i=0; i < keys.length; i++) {
            new BooleanStyleMenuItem(keys[i],
                                        values[i],
                                        mData[i],
                                        MenuItemSet.CHARACTER).setTextPanel(fTextPanel);
        }
            
        if (fDialogParent != null) {
            
            MItem.getItemFactory().createSeparator();
            
            String[] fonts = FontList.getFontList();
            String title = ResourceUtils.getResourceString(FrameResources.SET_FONT_TITLE);
            String label = ResourceUtils.getResourceString(FrameResources.SET_FONT_LABEL);
            
            DialogFactory fontF = createObjectDialogFactory(title, 
                                                            label,
                                                            TextAttribute.FAMILY,
                                                            StyleMenuItemSet.CHARACTER,
                                                            fonts,
                                                            fonts);
            new DialogItem(ResourceUtils.getMenuData(FrameResources.FONT_DIALOG),
                                    fontF).setTextPanel(fTextPanel);

            DialogFactory foregroundF = createColorDialogFactory(true);
            DialogFactory backgroundF = createColorDialogFactory(false);
            
            new DialogItem(ResourceUtils.getMenuData(FrameResources.FORECOLOR_DIALOG),
                                    foregroundF).setTextPanel(fTextPanel);
            new DialogItem(ResourceUtils.getMenuData(FrameResources.BACKCOLOR_DIALOG),
                                    backgroundF).setTextPanel(fTextPanel);
            }
    }

    private DialogFactory createColorDialogFactory(boolean foreground) {
        
        String title;
        String message;
        Object key;
        
        if (foreground) {
            title = ResourceUtils.getResourceString(FrameResources.SET_FOREGROUND_TITLE);
            message = ResourceUtils.getResourceString(FrameResources.SET_FOREGROUND_LABEL);
            key = TextAttribute.FOREGROUND;
        }
        else {
            title = ResourceUtils.getResourceString(FrameResources.SET_BACKGROUND_TITLE);
            message = ResourceUtils.getResourceString(FrameResources.SET_BACKGROUND_LABEL);
            key = TextAttribute.BACKGROUND;
        }
        return createObjectDialogFactory(title, 
                                         message,
                                         key,
                                         StyleMenuItemSet.CHARACTER,
                                         colorNames,
                                         colors);
    }
    
    private void createFontMenu() {
        
        handleAddMenu(FrameResources.FONT);
        
        String[] fonts = FontList.getFontList();
        MenuData[] mData = new MenuData[fonts.length];
        for (int i=0; i < mData.length; i++) {
            mData[i] = new MenuData(fonts[i]);
        }
        
        new StyleMenuItemSet(TextAttribute.FAMILY,
                                fonts,
                                mData,
                                StyleMenuItemSet.CHARACTER).setTextPanel(fTextPanel);
    }
    
    private void createFlushMenu() {
        
        handleAddMenu(FrameResources.FLUSH);
        
        Object[] values = {  TextAttribute.FLUSH_LEADING,
                             TextAttribute.FLUSH_CENTER,
                             TextAttribute.FLUSH_TRAILING,
                             TextAttribute.FULLY_JUSTIFIED };
        MenuData[] mData = { ResourceUtils.getMenuData(FrameResources.LEADING),
                             ResourceUtils.getMenuData(FrameResources.CENTER),
                             ResourceUtils.getMenuData(FrameResources.TRAILING),
                             ResourceUtils.getMenuData(FrameResources.JUSTIFIED) };
        
        new StyleMenuItemSet(TextAttribute.LINE_FLUSH,
                                values,
                                mData,
                                MenuItemSet.PARAGRAPH).setTextPanel(fTextPanel);
    }
    
    private void createKeymapMenu() {
        
        handleAddMenu(FrameResources.KEYMAP);
        
        KeyRemap[] values = { KeyRemap.getIdentityRemap(),
                             KeyRemap.getArabicTransliteration(),
                             KeyRemap.getHebrewTransliteration(),
                             KeyRemap.getIsraelNikud(),
                             KeyRemap.getThaiKetmanee() };
        MenuData[] mData = { ResourceUtils.getMenuData(FrameResources.DEFAULT),
                             ResourceUtils.getMenuData(FrameResources.ARABIC),
                             ResourceUtils.getMenuData(FrameResources.HEBREW),
                             ResourceUtils.getMenuData(FrameResources.ISRAEL_NIKUD),
                             ResourceUtils.getMenuData(FrameResources.THAI_KETMANEE) };
        
        new KeymapMenuItemSet(values, mData).setTextPanel(fTextPanel);
    }
    
    private void createBidiMenu() {
        
        handleAddMenu(FrameResources.BIDI);
        
        Object[] values = {  null,
                             TextAttribute.RUN_DIRECTION_LTR,
                             TextAttribute.RUN_DIRECTION_RTL };
        MenuData[] mData = { ResourceUtils.getMenuData(FrameResources.DEFAULT_DIRECTION),
                             ResourceUtils.getMenuData(FrameResources.LTR_DIRECTION),
                             ResourceUtils.getMenuData(FrameResources.RTL_DIRECTION), };

        new StyleMenuItemSet(TextAttribute.RUN_DIRECTION,
                                values,
                                mData,
                                MenuItemSet.PARAGRAPH).setTextPanel(fTextPanel);
    }
    
    private void createAboutMenu() {
        
        handleAddMenu(FrameResources.ABOUT_MENU);
        
        new DialogItem(ResourceUtils.getMenuData(FrameResources.ABOUT_ITEM), 
                       createAboutDialogFactory()).setTextPanel(fTextPanel);
    }
}
