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
package com.ibm.richtext.swingui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JComponent;

import com.ibm.richtext.textpanel.MTextPanel;
import com.ibm.richtext.styledtext.MTabRuler;

import com.ibm.richtext.uiimpl.TabRulerImpl;
import com.ibm.richtext.awtui.MTabRulerComponent;

/**
 * JTabRuler is an implementation of MTabRulerComponent in a Swing component.
 */
public final class JTabRuler extends JComponent implements MTabRulerComponent {
    
    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    private TabRulerImpl fImpl;
    
    /**
     * Create a new TabRuler.
     * @param baseline the y-coordinate of the ruler's baseline
     * @param origin the x-coordinate in this Component where
     *     the left margin appears
     * @param textPanel the MTextPanel to listen to.  This TabRuler
     *     will reflect the MTextPanel's paragraph styles, and update
     *     the paragraph styles when manipulated.
     */
    public JTabRuler(int baseline, int origin, MTextPanel textPanel) {
        
        fImpl = new TabRulerImpl(baseline, origin, textPanel, this);
    }
    
    /**
     * Listen to the given MTextPanel and reflect its changes,
     * and update its paragraph styles when TabRuler is
     * manipulated.
     * @param textPanel the MTextPanel to listen to
     */
    public void listenToTextPanel(MTextPanel textPanel) {

        fImpl.listenToTextPanel(textPanel);
    }
    
    /**
     * Return the background color of this TabRuler.
     * @return the background color of this TabRuler
     */
    public Color getBackColor() {

        return fImpl.getBackColor();
    }
    
    /**
     * Set the background color of this TabRuler.
     * @param backColor the new background color of this TabRuler
     */
    public void setBackColor(Color backColor) {

        fImpl.setBackColor(backColor);
    }
    
    /**
     * Return the MTabRuler represented by this TabRuler.
     * @return the MTabRuler represented by this TabRuler
     */
    public MTabRuler getRuler() {
        
        return fImpl.getRuler();
    }

    /**
     * Return the leading margin of this TabRuler.
     * @return the leading margin of this TabRuler
     */
    public int getLeadingMargin() {
        
        return fImpl.getLeadingMargin();
    }

    /**
     * Return the first line indent of this TabRuler.
     * @return the first line indent of this TabRuler
     */
    public int getFirstLineIndent() {
        
        return fImpl.getFirstLineIndent();
    }

    /**
     * Return the trailing margin of this TabRuler.
     * @return the trailing margin of this TabRuler
     */
    public final int getTrailingMargin() {
        
        return fImpl.getTrailingMargin();
    }
    
    // The following are Component methods which need to be delegated to
    // the implementation:
    
    public void paint(Graphics g) {
        
        fImpl.paint(g);
    }
    
    public Dimension getPreferredSize() {
        
        return fImpl.getPreferredSize();
    }

    public Dimension getMinimumSize() {
        
        return fImpl.getMinimumSize();
    }
}
