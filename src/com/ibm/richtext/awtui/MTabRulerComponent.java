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

import java.awt.Color;
import com.ibm.richtext.styledtext.MTabRuler;
import com.ibm.richtext.textpanel.MTextPanel;

/**
 * MTabRulerComponent is implemented by components which provide a tab-ruler
 * interface for interacting with an MTextPanel.
 * <p>
 * Users interact with MTabRulerComponent implementations as follows:
 * <ul>
 * <li>The leading margin can be set by dragging the bottom half
 * of the leftmost triangle.  The first-line indent will "stick" with
 * the leading margin.</li>
 * <li>The first-line indent can be set by dragging the top half of the
 * leftmost triangle.  The first-line indent applies to the first line of
 * a paragraph.</li>
 * <li>The trailing margin can be set by dragging the rightmost triangle.</li>
 * <li>Tabs can be added to the ruler by clicking the mouse on the ruler with the
 * control key pressed.  Four kinds of tabs are provided:  leading, trailing, center,
 * and decimal.  The type of a tab can be changed by double-clicking the tab.</li>
 * <li>Tabs can be repositioned by dragging them with the mouse.</li>
 * </ul>
 * <p>
 * MTabRulerComponent's appearance will reflect the paragraph styles in the
 * first selected paragraph.  Style changes performed with an 
 * MTabRulerComponent will apply to all selected paragraphs.
 */
public interface MTabRulerComponent {
    
    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";

    /**
     * Listen to the given MTextPanel and reflect its changes,
     * and update its paragraph styles when TabRuler is
     * manipulated.
     * @param textPanel the MTextPanel to listen to
     */
    public void listenToTextPanel(MTextPanel textPanel);

    /**
     * Return the background color of this TabRuler.
     * @return the background color of this TabRuler
     */
    public Color getBackColor();

    /**
     * Set the background color of this TabRuler.
     * @param backColor the new background color of this TabRuler
     */
    public void setBackColor(Color backColor);

    /**
     * Return the MTabRuler represented by this TabRuler.
     * @return the MTabRuler represented by this TabRuler
     */
    public MTabRuler getRuler();

    /**
     * Return the leading margin of this TabRuler.
     * @return the leading margin of this TabRuler
     */
    public int getLeadingMargin();

    /**
     * Return the first line indent of this TabRuler.
     * @return the first line indent of this TabRuler
     */
    public int getFirstLineIndent();

    /**
     * Return the trailing margin of this TabRuler.
     * @return the trailing margin of this TabRuler
     */
    public int getTrailingMargin();
}
