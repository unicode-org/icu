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

import com.ibm.richtext.uiimpl.resources.FrameResources;

import com.ibm.richtext.styledtext.MConstText;
import com.ibm.richtext.styledtext.StyledText;
import com.ibm.richtext.styledtext.StyleModifier;

import com.ibm.richtext.textlayout.attributes.AttributeMap;
import com.ibm.richtext.textlayout.attributes.TextAttribute;

public final class AboutText {
    
    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    private static final Color[] COLORS = {
        Color.red, Color.blue, Color.white, Color.green
    };
    
    public static MConstText getAboutText() {

        String text = ResourceUtils.getResourceString(FrameResources.ABOUT_TEXT);
        StyledText styledText = new StyledText(text, AttributeMap.EMPTY_ATTRIBUTE_MAP);

        int length = styledText.length();
        int i=0;

        for (int paragraphStart = 0, paragraphLimit;
                    paragraphStart < length;
                    paragraphStart = paragraphLimit) {

            paragraphLimit = styledText.paragraphLimit(paragraphStart);
            StyleModifier modifier = StyleModifier.createAddModifier(
                                             TextAttribute.FOREGROUND,
                                             COLORS[(i++)%COLORS.length]);
            styledText.modifyCharacterStyles(paragraphStart,
                                             paragraphLimit,
                                             modifier);
        }

        StyleModifier modifier = StyleModifier.createAddModifier(
                                            TextAttribute.LINE_FLUSH,
                                            TextAttribute.FLUSH_CENTER);

        styledText.modifyParagraphStyles(0, text.length(), modifier);

        return styledText;
    }
}