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
// Requires Java2
package com.ibm.richtext.textlayout.attributes;

import java.util.Hashtable;
import java.text.AttributedCharacterIterator.Attribute;
import java.io.InvalidObjectException;

/**
 * This class contains a number of keys (or attributes) for specifying text styles
 * in a Map.  A text style is a collection of key-value pairs, where
 * the key represents a certain characteristic (such as size) and
 * the value tells how that characteristic is realized (ie what the
 * size is).
 * <p>
 * All of the attributes in TextAttribute specify a class for their value.
 * Map does not enforce these class specifications;  however, text rendering code
 * will tend to fail with a ClassCastException if a key's value
 * has an unexpected class.
 * <p>
 * Some attributes are meaningful for entire paragraphs, not individual
 * characters.  These attributes are documented as applying to paragraphs.
 * All other attributes apply to characters.
 * <p>
 * Many of the field names and values in this class
 * are taken from the JDK 1.2
 * <code>java.awt.font.TextAttribute class</code>.
 * @see Map
 */
public class TextAttribute extends Attribute {

    private static final Hashtable instanceMap = new Hashtable(9);

    protected TextAttribute(String name) {
        super(name);
        if (this.getClass() == TextAttribute.class) {
            instanceMap.put(name, this);
        }
    }

    protected Object readResolve() throws InvalidObjectException {
        if (this.getClass() != TextAttribute.class) {
            throw new InvalidObjectException("subclass didn't correctly implement readResolve");
        }

        TextAttribute instance = (TextAttribute) instanceMap.get(getName());
        if (instance != null) {
            return instance;
        } else {
            throw new InvalidObjectException("unknown attribute name");
        }
    }

    public static final Object FONT = java.awt.font.TextAttribute.FONT;

    public static final Object FAMILY = java.awt.font.TextAttribute.FAMILY;

    public static final Object WEIGHT = java.awt.font.TextAttribute.WEIGHT;
    public static final Float WEIGHT_BOLD = java.awt.font.TextAttribute.WEIGHT_BOLD;

    public static final Object POSTURE = java.awt.font.TextAttribute.POSTURE;
    public static final Float POSTURE_OBLIQUE = java.awt.font.TextAttribute.POSTURE_OBLIQUE;

    public static final Object SIZE = java.awt.font.TextAttribute.SIZE;

    public static final Object SUPERSCRIPT = java.awt.font.TextAttribute.SUPERSCRIPT;
    public static final Integer SUPERSCRIPT_SUPER = java.awt.font.TextAttribute.SUPERSCRIPT_SUPER;
    public static final Integer SUPERSCRIPT_SUB = java.awt.font.TextAttribute.SUPERSCRIPT_SUB;

    /**
     * Attribute key for the foreground and background color adornment.
     */
    public static final Object FOREGROUND = java.awt.font.TextAttribute.FOREGROUND;
    public static final Object BACKGROUND = java.awt.font.TextAttribute.BACKGROUND;

    public static final Object UNDERLINE = java.awt.font.TextAttribute.UNDERLINE;
    public static final Integer UNDERLINE_ON = java.awt.font.TextAttribute.UNDERLINE_ON;

    public static final Object STRIKETHROUGH = java.awt.font.TextAttribute.STRIKETHROUGH;
    public static final Boolean STRIKETHROUGH_ON = java.awt.font.TextAttribute.STRIKETHROUGH_ON;

    public static final Object OFFSET = new TextAttribute("offset");

    public static final Object CHAR_REPLACEMENT = java.awt.font.TextAttribute.CHAR_REPLACEMENT;
// Paragraph Styles

// values are Floats:
    public static final Object EXTRA_LINE_SPACING = new TextAttribute("extra_line_spacing");
    public static final Object EXTRA_FIRST_LINE_SPACING = new TextAttribute("extra_first_line_spacing");

    /**
     * Amount beyond leading margin to indent the first line of a paragraph.
     */
    public static final Object FIRST_LINE_INDENT = new TextAttribute("first_line_indent");

    public static final Object MIN_LINE_SPACING = new TextAttribute("min_line_spacing");
    public static final Object MIN_FIRST_LINE_SPACING = new TextAttribute("min_first_line_spacing");

    /**
     * Flush lines left, right, or center.
     */
    public static final Object LINE_FLUSH = new TextAttribute("line_flush");

    public static final Integer FLUSH_LEADING = new Integer(0);
    public static final Integer FLUSH_CENTER = new Integer(1);
    public static final Integer FLUSH_TRAILING = new Integer(2);
    /**
     * Value of <code>LINE_FLUSH</code> for full justification.
     */
    public static final Integer FULLY_JUSTIFIED = new Integer(3);

    /**
     * Leading and trailing margin in paragraph.
     */
    public static final Object LEADING_MARGIN = new TextAttribute("leading_margin");
    public static final Object TRAILING_MARGIN = new TextAttribute("trailing_margin");

    /**
     * Tab ruler.  Values are MTabRuler instances.
     */
    public static final Object TAB_RULER = new TextAttribute("tab_ruler");
    
    /**
     * Attribute key for the run direction of the line.
     *
     * <P><TABLE BORDER="0" CELLSPACING="0" CELLPADDING="1">
     * <TR>
     * <TH VALIGN="TOP" ALIGN="RIGHT"><P ALIGN=RIGHT>Key</TH>
     * <TD VALIGN="TOP">RUN_DIRECTION</TD></TR>
     * <TR>
     * <TH VALIGN="TOP" ALIGN="RIGHT"><P ALIGN=RIGHT>Value</TH>
     * <TD VALIGN="TOP">Boolean</TD></TR>
     * <TR>
     * <TH VALIGN="TOP" ALIGN="RIGHT"><P ALIGN=RIGHT>Constants</TH>
     * <TD VALIGN="TOP">RUN_DIRECTION_LTR = true, RUN_DIRECTION_RTL = false
     * </TD></TR>
     * <TR>
     * <TH VALIGN="TOP" ALIGN="RIGHT"><P ALIGN=RIGHT>Default</TH>
     * <TD VALIGN="TOP">Use the default Unicode base direction from the BIDI 
     * algorithm.</TD></TR>
     * <TR>
     * <TH VALIGN="TOP" ALIGN="RIGHT"><P ALIGN=RIGHT>Description</TH>
     * <TD VALIGN="TOP"><P>Specifies which base run direction to use when 
     * positioning mixed directional runs within a paragraph. If this value is
     * RUN_DIRECTION_DEFAULT, <code>TextLayout</code> uses the default Unicode
     * base direction from the BIDI algorithm.</P>
     * <P><I>This attribute should have the same value over the whole 
     * paragraph.</I></TD></TR>
     * </TABLE>
     */
    public static final Object RUN_DIRECTION = java.awt.font.TextAttribute.RUN_DIRECTION;

    /**
     * Left-to-right run direction.
     * @see #RUN_DIRECTION
     */
    public static final Boolean RUN_DIRECTION_LTR = java.awt.font.TextAttribute.RUN_DIRECTION_LTR;

    /**
     * Right-to-left run direction.
     * @see #RUN_DIRECTION
     */
    public static final Boolean RUN_DIRECTION_RTL = java.awt.font.TextAttribute.RUN_DIRECTION_RTL;

    /**
     * Attribute key for the embedding level for nested bidirectional runs.
     *
     * <P><TABLE BORDER="0" CELLSPACING="0" CELLPADDING="1">
     * <TR>
     * <TH VALIGN="TOP" ALIGN="RIGHT"><P ALIGN=RIGHT>Key</TH>
     * <TD VALIGN="TOP">BIDI_EMBEDDING</TD></TR>
     * <TR>
     * <TH VALIGN="TOP" ALIGN="RIGHT"><P ALIGN=RIGHT>Value</TH>
     * <TD VALIGN="TOP">Integer</TD></TR>
     * <TR>
     * <TH VALIGN="TOP" ALIGN="RIGHT"><P ALIGN=RIGHT>Limits</TH>
     * <TD VALIGN="TOP">Positive values 1 through 15 are <I>embedding</I>
     * levels, negative values<BR> through -15 are <I>override</I> levels
     * </TD></TR>
     * <TR>
     * <TH VALIGN="TOP" ALIGN="RIGHT"><P ALIGN=RIGHT>Default</TH>
     * <TD VALIGN="TOP">Use standard BIDI to compute levels from formatting
     * characters in the text.</TD></TR>
     * <TR>
     * <TH VALIGN="TOP" ALIGN="RIGHT"><P ALIGN=RIGHT>Description</TH>
     * <TD VALIGN="TOP">Specifies the bidi embedding level of the character.
     * When this attribute is present anywhere in a paragraph, then the 
     * Unicode characters RLO, LRO, RLE, LRE, PDF are disregarded in the BIDI 
     * analysis of that paragraph. 
     * See the Unicode Standard v. 2.0, section 3-11.
     * </TD></TR>
     * </TABLE>
     */
    public static final Object BIDI_EMBEDDING = java.awt.font.TextAttribute.BIDI_EMBEDDING;

    /**
     * Attribute key for the justification of a paragraph.
     *
     * <P><TABLE BORDER="0" CELLSPACING="0" CELLPADDING="1">
     * <TR>
     * <TH VALIGN="TOP" ALIGN="RIGHT"><P ALIGN=RIGHT>Key</TH>
     * <TD VALIGN="TOP">JUSTIFICATION</TD></TR>
     * <TR>
     * <TH VALIGN="TOP" ALIGN="RIGHT"><P ALIGN=RIGHT>Value</TH>
     * <TD VALIGN="TOP">Float</TD></TR>
     * <TR>
     * <TH VALIGN="TOP" ALIGN="RIGHT"><P ALIGN=RIGHT>Limits</TH>
     * <TD VALIGN="TOP">0.0 through1.0</TD></TR>
     * <TR>
     * <TH VALIGN="TOP" ALIGN="RIGHT"><P ALIGN=RIGHT>Default</TH>
     * <TD VALIGN="TOP">1.0</TD></TR>
     * <TR>
     * <TH VALIGN="TOP" ALIGN="RIGHT"><P ALIGN=RIGHT>Description</TH>
     * <TD VALIGN="TOP"><P>Specifies which fraction of the extra space to use 
     * when justification is requested. For example, if the line is 50 points
     * wide and the margins are 70 points apart, a value of 0.5 means that the
     * line is padded to reach a width of 60 points.</P>
     * <P><I>This attribute should have the same value over the whole
     * paragraph.</I></TD></TR>
     * </TABLE>
     */
    public static final Object JUSTIFICATION = java.awt.font.TextAttribute.JUSTIFICATION;

    /**
     * Justify the line to the full requested width.
     * @see #JUSTIFICATION
     */
    public static final Float JUSTIFICATION_FULL = java.awt.font.TextAttribute.JUSTIFICATION_FULL;

    /**
     * Do not allow the line to be justified.
     * @see #JUSTIFICATION
     */
    public static final Float JUSTIFICATION_NONE = java.awt.font.TextAttribute.JUSTIFICATION_NONE;
}
