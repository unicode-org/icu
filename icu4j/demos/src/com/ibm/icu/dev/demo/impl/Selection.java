// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 1996-2010, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.demo.impl;
import java.text.BreakIterator;

public final class Selection {

    public int anchor;
    public int caret;
    public boolean clickAfter;

    public int getStart() {
        return anchor < caret ? anchor : caret;
    }

    public int getEnd() {
        return anchor > caret ? anchor : caret;
    }

    public boolean isCaret() {
        return anchor == caret;
    }

    public Selection set(Selection other) {
        anchor = other.anchor;
        caret = other.caret;
        clickAfter = other.clickAfter;
        return this;
    }

    public Selection set(int anchor, int caret, boolean clickAfter) {
        this.anchor = anchor;
        this.caret = caret;
        this.clickAfter = clickAfter;
        return this;
    }

    public boolean equals(Object other) {
        Selection other2 = (Selection)other;
        return anchor == other2.anchor
          && caret == other2.caret
          && clickAfter == other2.clickAfter;
    }

    public boolean isLessThan(Selection other) {
        return getStart() < other.getEnd();
    }

    public Selection pin(String text) {
        if (anchor > text.length()) {
            anchor = text.length();
        } else if (anchor < 0) {
            anchor = 0;
        }
        if (caret > text.length()) {
            caret = text.length();
            clickAfter = true;
        } else if (caret < 0) {
            caret = 0;
            clickAfter = false;
        }
        return this;
    }

    public Selection swap(Selection after) {
        int temp = anchor;
        anchor = after.anchor;
        after.anchor = temp;
        temp = caret;
        caret = after.caret;
        after.caret = temp;
        boolean b = clickAfter;
        clickAfter = after.clickAfter;
        after.clickAfter = b;
        return this;
    }

    public Selection fixAfterReplace(int start, int end, int len) {
        if (anchor >= start) {
            if (anchor < end) anchor = end;
            anchor = start + len + anchor - end;
        }
        if (caret >= start) {
            if (caret < end) caret = end;
            caret = start + len + caret - end;
        }
        return this;
    }

        // Mac & Windows considerably different
        // Mac: end++. If start!=end, start=end
        //  SHIFT: move end right
        //  CTL: no different
        // Windows:
        //  UNSHIFTED: if start!=end, start = end, else start=end=end+1;
        //       anchor = tip = start
        //  SHIFT: tip++
        //  CTL: if start!=end, start = end = nextbound(end-1),
        //   else start=end=nextbound(end)
        //       anchor = tip = start
        //  CTL/SHIFT: tip = nextbound(tip)

    public Selection nextBound(BreakIterator breaker,
      int direction, boolean extend) {
        if (!extend && anchor != caret) caret -= direction;
        caret = next(caret, breaker, direction, true);
        if (!extend) anchor = caret;
        clickAfter = false;
        return this;
    }

    // expand start and end to word breaks--if they are not already on one
    public void expand(BreakIterator breaker) {
        if (anchor <= caret) {
            anchor = next(anchor,breaker,-1,false);
            caret = next(caret,breaker,1,false);
            /*
            try {
                breaker.following(anchor);
                anchor = breaker.previous();
            } catch (Exception e) {}
            try {
                caret = breaker.following(caret-1);
            } catch (Exception e) {}
            */
        } else {
            anchor = next(anchor,breaker,1,false);
            caret = next(caret,breaker,-1,false);
            /*
            try {
                breaker.following(caret);
                caret = breaker.previous();
            } catch (Exception e) {}
            try {
                anchor = breaker.following(anchor-1);
            } catch (Exception e) {}
            */
        }
    }

    // different = false - move to next boundary, unless on one
    // true - move to next boundary, even if on one
    public static int next(int position, BreakIterator breaker,
      int direction, boolean different) {
        if (!different) position -= direction;
        try {
            if (direction > 0) {
                position = breaker.following(position);
            } else {
                breaker.following(position-1);
                position = breaker.previous();
            }
        } catch (Exception e) {}
        return position;
    }
}

