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

import java.awt.Color;
import java.io.Serializable;
import java.util.Hashtable;

import com.ibm.richtext.textlayout.attributes.AttributeMap;
import com.ibm.richtext.textlayout.attributes.TextAttribute;
import com.ibm.richtext.styledtext.StandardTabRuler;

/**
 * This class contains settings used when constructing an MTextPanel.
 * The settings controled by this class include:
 * <ul>
 * <li>whether the text in the MTextPanel can be scrolled</li>
 * <li>whether scroll bars in the MTextPanel are visible</li>
 * <li>whether the text in the MTextPanel can be selected</li>
 * <li>whether the text in the MTextPanel can be edited</li>
 * <li>whether lines of text wrap to the MTextPanel's width, or
 * only end at paragraph separators</li>
 * <li>the default values for unspecified styles</li>
 * </ul>
 * Some settings are dependent on others.  Scroll bars are visible
 * only if the text is scrollable.  Also, text which is not editable
 * if it is not selectable.
 * <p>
 *
 * @see MTextPanel
 */
public final class TextPanelSettings implements Cloneable, Serializable {

    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    
    private static final AttributeMap DEFAULTS;
    static {
        final Float floatZero = new Float(0.0f);

        Hashtable defaults = new Hashtable();
        defaults.put(TextAttribute.FAMILY, "Serif");
        defaults.put(TextAttribute.WEIGHT, new Float(1.0f));
        defaults.put(TextAttribute.POSTURE, floatZero);
        defaults.put(TextAttribute.SIZE, new Float(18.0f));
        defaults.put(TextAttribute.SUPERSCRIPT, new Integer(0));
        defaults.put(TextAttribute.FOREGROUND, Color.black);
        defaults.put(TextAttribute.UNDERLINE, new Integer(-1));
        defaults.put(TextAttribute.STRIKETHROUGH, Boolean.FALSE);

        defaults.put(TextAttribute.EXTRA_LINE_SPACING, floatZero);
        defaults.put(TextAttribute.FIRST_LINE_INDENT, floatZero);
        defaults.put(TextAttribute.MIN_LINE_SPACING, floatZero);
        defaults.put(TextAttribute.LINE_FLUSH, TextAttribute.FLUSH_LEADING);
        defaults.put(TextAttribute.LEADING_MARGIN, floatZero);
        defaults.put(TextAttribute.TRAILING_MARGIN, floatZero);
        defaults.put(TextAttribute.TAB_RULER, new StandardTabRuler());

        DEFAULTS = new AttributeMap(defaults);
    }
    
    private boolean fScrollable = true;
    private boolean fScrollBarsVisible = true;
    private boolean fSelectable = true;
    private boolean fEditable = true;
    private boolean fWraps = true;
    private AttributeMap fDefaultValues = DEFAULTS;
    
    /**
     * Create a TextPanelSettings instance with all settings
     * set to true.
     */
    public TextPanelSettings() {
    }

    /**
     * Return a new TextPanelSettings instance with the
     * same settings as this.
     * @return a new TextPanelSettings instance
     */
    public Object clone() {

        TextPanelSettings rhs = new TextPanelSettings();

        rhs.fScrollable = fScrollable;
        rhs.fScrollBarsVisible = fScrollBarsVisible;
        rhs.fSelectable = fSelectable;
        rhs.fEditable = fEditable;
        rhs.fWraps = fWraps;
        rhs.fDefaultValues = fDefaultValues;
        
        return rhs;
    }

    /**
     * Return the scrollable setting, which determines whether text
     * in an MTextPanel can be scrolled.
     * @return the scrollable setting
     */
    public boolean getScrollable() {

        return fScrollable;
    }

    /**
     * Set the scrollable setting.
     * @param scrollable the scrollable setting.  If false,
     * the scrollBarsVisible setting is also set to false.
     */
    public void setScrollable(boolean scrollable) {

        fScrollable = scrollable;
        fScrollBarsVisible &= scrollable;
    }

    /**
     * Return the scrollBarsVisible setting, which determines whether
     * scroll bars in an MTextPanel are visible.
     * @return the scrollBarsVisible setting
     */
    public boolean getScrollBarsVisible() {

        return fScrollBarsVisible;
    }

    /**
     * Set the scrollBarsVisible setting.
     * @param vis the scrollBarsVisible setting.  If true,
     * the scrollable setting is also set to true.
     */
    public void setScrollBarsVisible(boolean vis) {

        fScrollBarsVisible = vis;
        fScrollable |= vis;
    }

    /**
     * Return the selectable setting, which determines whether
     * text in an MTextPanel can be selected.
     * @return the selectable setting
     */
    public boolean getSelectable() {

        return fSelectable;
    }

    /**
     * Set the selectable setting.
     * @param selectable the selectable setting.  If false,
     * the editable setting is also set to false.
     */
    public void setSelectable(boolean selectable) {

        fSelectable = selectable;
        fEditable &= selectable;
    }

    /**
     * Return the editable setting, which determines whether
     * text in an MTextPanel can be edited.
     * @return the editable setting
     */
    public boolean getEditable() {

        return fEditable;
    }

    /**
     * Set the editable setting.
     * @param editable the editable setting.  If true,
     * the selectable setting is also set to true.
     */
    public void setEditable(boolean editable) {

        fEditable = editable;
        fSelectable |= editable;
    }

    /**
     * Return the wraps setting, which determines whether
     * lines of text wrap to the length of the MTextPanel,
     * or only at paragraph separators.
     * @return the wraps setting
     */
    public boolean getWraps() {

        return fWraps;
    }

    /**
     * Set the wraps setting.
     * @param wraps the wraps setting
     */
    public void setWraps(boolean wraps) {

        fWraps = wraps;
    }
    
    /**
     * Return the AttributeMap of default values for certain keys.
     * When a key in this AttributeMap is not specified, its value
     * is taken from this AttributeMap.
     * @return the AttributeMap of default values
     * @see MTextPanel#getDefaultValues
     */
    public AttributeMap getDefaultValues() {
    
        return fDefaultValues;
    }
    
    /**
     * Add the key-value pairs in the given AttributeMap to the
     * default values.  If a key does not appear in the given
     * AttributeMap, its value in the default value map is
     * unchanged.
     * @param map an AttributeMap containing new default values
     */
    public void addDefaultValues(AttributeMap map) {
    
        fDefaultValues = fDefaultValues.addAttributes(map);
    }
    
    /**
     * Compare this to another Object.  This is equal
     * to another Object if the other Object is a
     * TextPanelSettings instance with the same
     * settings as this one.
     * @param rhs the Object to compare to
     */
    public boolean equals(Object rhs) {

        if (rhs == this) {
            return true;
        }

        if (rhs == null) {
            return false;
        }

        TextPanelSettings other;
        try {
            other = (TextPanelSettings) rhs;
        }
        catch(ClassCastException e) {
            return false;
        }
        
        return other.fScrollable == this.fScrollable &&
               other.fScrollBarsVisible == this.fScrollBarsVisible &&
               other.fSelectable == this.fSelectable &&
               other.fEditable == this.fEditable &&
               other.fWraps == this.fWraps &&
               other.fDefaultValues.equals(this.fDefaultValues);
    }

    /**
     * Return the hash code for this Object.
     * @return the hash code for this Object
     */
    public int hashCode() {

        int code = fDefaultValues.hashCode();
        code = code*2 + (fScrollable? 1:0);
        code = code*2 + (fScrollBarsVisible? 1:0);
        code = code*2 + (fSelectable? 1:0);
        code = code*2 + (fEditable? 1:0);
        code = code*2 + (fWraps? 1:0);

        return code;
    }
}
