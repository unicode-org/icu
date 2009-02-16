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
package com.ibm.richtext.styledtext;

/**
 * This interface represents a sequence of TabStops, ordered by position.
 * The first
 * TabStop in the ruler can be obtained with the <code>firstTab</code>
 * method;  subsequent TabStops are obtained with the <code>nextTab</code>
 * method.
 * <p>
 * If a TabStop with type <code>TabStop.kAuto</code> is returned, all tabs
 * after that TabStop will also have type <code>TabStop.kAuto</code>, and
 * their positions will be multiples of <code>autoSpacing</code>.
 * @see TabStop
 */
public abstract class MTabRuler
{
    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    /**
     * Return first tab in the ruler.  If an autoTab, it is at position zero, and
     * all subsequent tabs will be autotabs at autoSpacing intervals.
     */
    public abstract TabStop firstTab();

    /**
     * Return the first tab in the ruler with fPosition > position.  If it is an
     * autotab, it is at an increment of autoSpacing, and all subsequent tabs will be
     * autotabs at autoSpacing intervals.
     */
    public abstract TabStop nextTab(int position);

    /**
     * Return the interval for autotabs.
     */
    public abstract int autoSpacing();

    /**
     * Compute the hashCode for this ruler.  The hashCode is the
     * hashCode of the first tab multiplied by the autoSpacing
     * interval.
     */
    public final int hashCode() {

        return firstTab().hashCode() * autoSpacing();
    }

    /**
     * Return true if this tab ruler contains the given tab.
     * @param tabToTest the tab to search for
     * @return true if this tab ruler contains <code>tabToTest</code>
     */
    public boolean containsTab(TabStop tabToTest) {

        for (TabStop tab = firstTab();
                        tab.getType() != TabStop.kAuto;
                        tab = nextTab(tab.getPosition())) {
            if (tab.getPosition() >= tabToTest.getPosition()) {
                return tabToTest.equals(tab);
            }
        }

        return false;
    }

    /**
     * Return a tab ruler identical to this ruler, except with the
     * given tab added.  This ruler is not modified.
     * @param tabToAdd the tab to add to the new tab ruler
     * @return an MTabRuler resulting from this operation
     */
    public MTabRuler addTab(TabStop tabToAdd) {

        return StandardTabRuler.addTabToRuler(this, tabToAdd);
    }

    /**
     * Return a tab ruler identical to the given ruler, except with the
     * tab at the given position removed.  This ruler is not modified.
     * @param position the position of the tab to remove from the new tab ruler
     * @return an MTabRuler resulting from this operation
     */
    public MTabRuler removeTab(int position) {

        return StandardTabRuler.removeTabFromRuler(this, position);
    }

    /**
     * Return a tab ruler identical to this ruler, except with the
     * tab at position <code>fromPosition</code> moved to position
     * <code>toPosition</code>.  This ruler is not modified.
     * @param fromPosition the position of the tab to move
     * @param toPosition the new position of the tab
     * @return an MTabRuler resulting from this operation
     */
    public MTabRuler moveTab(int fromPosition, int toPosition) {

        return StandardTabRuler.moveTabOnRuler(this, fromPosition, toPosition);
    }
}