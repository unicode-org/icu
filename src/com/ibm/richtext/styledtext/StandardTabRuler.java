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

import java.util.Vector;

import java.io.Externalizable;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.IOException;

/**
 * This class is a standard implementation of MTabRuler.
 * It can have a finite number of client-specified TabStops.  After
 * the client-specified TabStops, all TabStops have type
 * <code>TabStop.kAuto</code> and are at the autospace intervals.
 * @see TabStop
 */
public final class StandardTabRuler extends MTabRuler
                                    implements Externalizable
{
    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    private static final int CURRENT_VERSION = 1;
    private static final long serialVersionUID = 22356934;

    private static final TabStop AUTO_ZERO = new TabStop(0, TabStop.kAuto);

    private TabStop[] fTabs = null;
    private int fAutoSpacing = 36; // every 1/2 inch.

    /**
     * Create a StandardTabRuler with only auto tabs, with spacing of 36.
     */
    public StandardTabRuler()
    {
    }

    /**
     * Create a StandardTabRuler with only auto tabs, with the
     * given autoSpacing.
     * @param autoSpacing the autoSpacing for this tab ruler
     */
    public StandardTabRuler(int autoSpacing)
    {
        fAutoSpacing = autoSpacing;
    }

    /**
     * Create a StandardTabRuler.  The first TabStops on the ruler will be
     * the TabStops in the <code>tabs</code> array.  After these tabs all
     * tabs are auto tabs.
     * @param tabs an array of TabStops.  The TabStops in the array must
     *    be in strictly increasing order (of positions), and cannot have
     *    type <code>TabStop.kAuto</code>.
     * @param autoSpacing the autoSpacing interval to use after the last
     *    client-specified tab.
     */
    public StandardTabRuler(TabStop[] tabs, int autoSpacing)
    {
        if (tabs.length > 0) {
            validateTabArray(tabs);
            fTabs = (TabStop[]) tabs.clone();
        }
        else {
            fTabs = null;
        }
        fAutoSpacing = autoSpacing;
    }

    /** Tabs as provided, then autoSpacing after the last tab to eternity.  Use this constructor when
        munging a ruler, it does no validation on the tabs in the vector. Vector may not be null. */

    /*public*/ StandardTabRuler(Vector v, int autoSpacing)
    {
        fTabs = tabArrayFromVector(v);
        fAutoSpacing = autoSpacing;
    }

    /** Construct from another ruler. No validation. Ruler may not be null. */

    /*public*/ StandardTabRuler(MTabRuler ruler)
    {
        if (ruler == null) {
            throw new IllegalArgumentException("ruler may not be null");
        }

        fTabs = tabArrayFromVector(vectorFromTabRuler(ruler));
        fAutoSpacing = ruler.autoSpacing();
    }

    public void readExternal(ObjectInput in) throws IOException,
                                            ClassNotFoundException {

        int version = in.readInt();
        if (version != CURRENT_VERSION) {
            throw new IOException("Invalid version of StyledText: " + version);
        }
        fTabs = (TabStop[]) in.readObject();
        fAutoSpacing = in.readInt();
    }

    public void writeExternal(ObjectOutput out) throws IOException {

        out.writeInt(CURRENT_VERSION);
        out.writeObject(fTabs);
        out.writeInt(fAutoSpacing);
    }

    /**
     * Return first tab in the ruler.  If an autoTab, it is at position zero, and
     * all subsequent tabs will be autotabs at autoSpacing intervals.
     */
    public TabStop firstTab()
    {
        if (fTabs != null && fTabs.length > 0) {
            return fTabs[0];
        }

        return AUTO_ZERO;
    }

    /**
     * Return the first tab in the ruler with fPosition > position.  If it is an
     * autotab, it is at an increment of autoSpacing, and all subsequent tabs will be
     * autotabs at autoSpacing intervals.
     * @param position the position of the TabStop returned will be greater than this parameter
     */
    public TabStop nextTab(int position)
    {
        if (fTabs != null) {
            for (int i = 0; i < fTabs.length; ++i) {
                if (position < fTabs[i].getPosition())
                    return fTabs[i];
            }
        }

        if (position >= 4000) { // debug: sanity check
            System.out.println("auto tab past 4000");
        }

        return new TabStop(((position / fAutoSpacing) + 1) * fAutoSpacing, TabStop.kAuto);
    }

    /**
     * Return the interval for autotabs.
     */
    public int autoSpacing()
    {
        return fAutoSpacing;
    }

    /**
     * Compare this to another Object. Returns true if the object
     * is an MTabRuler with the same autoSpacing and tabs.
     */
    public boolean equals(Object o)
    {
        if (o == this) {
            return true;
        }
        else if (o == null) {
            return false;
        }
        
        MTabRuler rhs;
        try {
            rhs = (MTabRuler)o;
        }
        catch(ClassCastException e) {
            return false;
        }

        if (fAutoSpacing != rhs.autoSpacing())
            return false;

        TabStop rhsTab = rhs.firstTab();

        if (fTabs != null) {
            for (int i = 0; i < fTabs.length; ++i) {
                if (!fTabs[i].equals(rhsTab))
                    return false;

                rhsTab = rhs.nextTab(rhsTab.getPosition());
            }
        }

        return rhsTab.getType() == TabStop.kAuto;
    }

    /**
     * Return debug information about this tab ruler.
     */
    public String toString()
    {
        StringBuffer buffer = new StringBuffer(super.toString());
        buffer.append(" auto: ");
        buffer.append(Integer.toString(fAutoSpacing));

        if (fTabs != null) {
            for (int i = 0; i < fTabs.length; ++i) {
                buffer.append(fTabs[i].toString());
            }
        }

        return buffer.toString();
    }

    /** Utility to convert a vector of tabs to an array. */

    private static TabStop[] tabArrayFromVector(Vector v)
    {
        int count = v.size();
        TabStop[] tabs = new TabStop[count];
        for (int i = 0; i < count; ++i) {
            tabs[i] = (TabStop)v.elementAt(i);
        }

        return tabs;
    }

    /** Utility to convert a ruler to a vector of tabs, for munging. */

    private static Vector vectorFromTabRuler(MTabRuler ruler)
    {
        Vector v = new Vector();
        for (TabStop tab = ruler.firstTab(); tab != null && tab.getType() != TabStop.kAuto; tab = ruler.nextTab(tab.getPosition())) {
            v.addElement(tab);
        }

        return v;
    }

    /** Utility to validate an array of tabs.  The array must not be null, must not contain null
        entries, must not be kAuto, and positions must in increasing order. */

    private static void validateTabArray(TabStop[] tabs)
    {
        int pos = Integer.MIN_VALUE;
        for (int i = 0; i < tabs.length; ++i) {
            if (tabs[i].getType() == TabStop.kAuto) {
                throw new IllegalArgumentException("can't explicitly specify an auto tab.");
            }
            int nextpos = tabs[i].getPosition();
            if (nextpos <= pos) {
                throw new IllegalArgumentException("tab positions must be in increasing order.");
            }
            pos = nextpos;
        }
    }

    /**
     * Return a tab ruler identical to the given ruler, except with the
     * given tab added.
     * @param ruler the original ruler.  The MTabRuler will be the same as
     *   this except for the additional tab.  <code>ruler</code> is not modified.
     * @param tabToAdd the tab to add to the new tab ruler
     * @return an MTabRuler resulting from this operation
     */
    /*public*/ static MTabRuler addTabToRuler(MTabRuler ruler, TabStop tabToAdd)
    {
        if (ruler == null || tabToAdd == null)
            throw new IllegalArgumentException("ruler and tabToAdd may not be null");

        Vector vector = new Vector();

        int pos = 0;
        boolean added = false;
        for (TabStop tab = ruler.firstTab(); tab.getType() != TabStop.kAuto; tab = ruler.nextTab(pos)) {
            pos = tab.getPosition();

            if (!added && pos >= tabToAdd.getPosition()) {
                if (pos == tabToAdd.getPosition())
                    tab = null;
                vector.addElement(tabToAdd);
                added = true;
            }

            if (tab != null)
                vector.addElement(tab);
        }
        if (!added)
            vector.addElement(tabToAdd);

        return new StandardTabRuler(vector, ruler.autoSpacing());
    }

    /**
     * Return a tab ruler identical to the given ruler, except with the
     * given tab removed.
     * @param ruler the original ruler.  The MTabRuler will be the same as
     *   this except for the removed tab.  <code>ruler</code> is not modified.
     * @param position the position of the tab to remove from the new tab ruler
     * @return an MTabRuler resulting from this operation
     */
    /*public*/ static MTabRuler removeTabFromRuler(MTabRuler ruler, int position)
    {
        if (ruler == null)
            throw new IllegalArgumentException("ruler may not be null");

        Vector vector = new Vector();

        int pos = 0;
        boolean removed = false;
        for (TabStop tab = ruler.firstTab(); tab.getType() != TabStop.kAuto; tab = ruler.nextTab(pos)) {
            pos = tab.getPosition();

            if (!removed && pos >= position) {
                if (pos == position) {
                    removed = true;
                    continue; // skip this tab and continue with the remainder
                }
                break; // we didn't remove a tab, but skipped position, so don't bother with the rest
            }

            vector.addElement(tab);
        }
        if (!removed) // no change
            return ruler;

        if (vector.size() == 0)
            return new StandardTabRuler(ruler.autoSpacing());

        return new StandardTabRuler(vector, ruler.autoSpacing());
    }

    /**
     * Return a tab ruler identical to the given ruler, except with the
     * tab at position <code>fromPosition</code> moved to position
     * <code>toPosition</code>.
     * @param ruler the original ruler.  The MTabRuler will be the same as
     *   this except for the moved tab.  <code>ruler</code> is not modified.
     * @param fromPosition the position of the tab to move
     * @param toPosition the new position of the tab
     * @return an MTabRuler resulting from this operation
     */
    /*public*/ static MTabRuler moveTabOnRuler(MTabRuler ruler, int fromPosition, int toPosition)
    {
        if (ruler == null)
            throw new IllegalArgumentException("ruler may not be null");

        Vector vector = new Vector();

        int pos = 0;
        boolean moved = false;
        for (TabStop tab = ruler.firstTab(); tab.getType() != TabStop.kAuto; tab = ruler.nextTab(pos)) {
            pos = tab.getPosition();

            if (!moved && pos == fromPosition) {
                moved = true;
                tab = new TabStop(toPosition, tab.getType()); // copy it
            }

            vector.addElement(tab);
        }
        if (!moved) // no change
            return ruler;

        return new StandardTabRuler(vector, ruler.autoSpacing());
    }

}
