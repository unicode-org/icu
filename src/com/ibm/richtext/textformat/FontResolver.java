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
package com.ibm.richtext.textformat;

import com.ibm.richtext.textlayout.attributes.AttributeMap;
import com.ibm.richtext.textlayout.attributes.TextAttribute;

import com.ibm.richtext.textlayout.FontUtils;

import java.util.Hashtable;
import java.awt.Font;

final class FontResolver {

    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";

    static {
// Even though it violates the Prime Directive I'll conditionalize
// this anyway, since it is just a 1.2 workaround which I greatly
// resent.
///*JDK12IMPORTS
        java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
//JDK12IMPORTS*/
    }

    private Hashtable styleMap;
    private final AttributeMap fDefaultFontMap;

    public FontResolver(AttributeMap defaults) {

        styleMap = new Hashtable();
        Hashtable tempMap = new Hashtable();
        tempMap.put(TextAttribute.FAMILY, defaults.get(TextAttribute.FAMILY));
        tempMap.put(TextAttribute.WEIGHT, defaults.get(TextAttribute.WEIGHT));
        tempMap.put(TextAttribute.POSTURE, defaults.get(TextAttribute.POSTURE));
        tempMap.put(TextAttribute.SIZE, defaults.get(TextAttribute.SIZE));
        fDefaultFontMap = new AttributeMap(tempMap);
    }

    /**
     * Fetch result of resolve(style) from cache, if present.
     */
    public AttributeMap applyFont(AttributeMap style) {

        Object cachedMap = styleMap.get(style);

        if (cachedMap == null) {
            AttributeMap resolvedMap = resolve(style);
            styleMap.put(style, resolvedMap);
            return resolvedMap;
        }
        else {
            return (AttributeMap) cachedMap;
        }
    }

    /**
     * Return an AttributeMap containing a Font computed from the
     * attributes in <tt>style</tt>.
     */
    public AttributeMap resolve(AttributeMap style) {

        if (style.get(TextAttribute.FONT) != null) {
            return style;
        }

        Font font = FontUtils.getFont(fDefaultFontMap.addAttributes(style));

        return style.addAttribute(TextAttribute.FONT, font);
    }
}
