// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 1998-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * Created on Dec 3, 2003
 *
 *******************************************************************************
 */
package com.ibm.icu.dev.tool.layout;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;

public class ArabicShaping {

    // arabic   shaping type code

    // shaping bit masks
    static final int MASK_SHAPE_RIGHT   = 1; // if this bit set, shapes to right
    static final int MASK_SHAPE_LEFT = 2;   // if this bit set, shapes to left
    static final int MASK_TRANSPARENT   = 4; // if this bit set, is transparent (ignore other bits)
    static final int MASK_NOSHAPE   = 8; // if this bit set, don't shape this char, i.e. tatweel

    // shaping values
    public static final int VALUE_NONE = 0;
    public static final int VALUE_RIGHT = MASK_SHAPE_RIGHT;
    public static final int VALUE_LEFT = MASK_SHAPE_LEFT;
    public static final int VALUE_DUAL = MASK_SHAPE_RIGHT | MASK_SHAPE_LEFT;
    public static final int VALUE_TRANSPARENT = MASK_TRANSPARENT;
    public static final int VALUE_NOSHAPE_DUAL = MASK_NOSHAPE | VALUE_DUAL;
    public static final int VALUE_NOSHAPE_NONE = MASK_NOSHAPE;

    public static int getShapeType(char ch)
    {
        int tt = UCharacter.getIntPropertyValue(ch, UProperty.JOINING_TYPE);
        
        switch(tt) {
            case UCharacter.JoiningType.JOIN_CAUSING:
                return VALUE_NOSHAPE_DUAL;
                
            case UCharacter.JoiningType.LEFT_JOINING:
                return VALUE_LEFT;
            
            case UCharacter.JoiningType.RIGHT_JOINING:
                return VALUE_RIGHT;
            
            case UCharacter.JoiningType.DUAL_JOINING:
                return VALUE_DUAL;
            
            case UCharacter.JoiningType.TRANSPARENT:
                return VALUE_TRANSPARENT;
                
            case UCharacter.JoiningType.NON_JOINING:
            default:
                return VALUE_NOSHAPE_NONE;                
        }
    }

    /*
     * Chars in logical order.
     * leftType is shaping code of char to logical left of range
     * rightType is shaping code of char to logical right of range
     */

    public static void shape(char[] chars, int leftType, int rightType, ClassTable isolClassTable) {
        // iterate in logical order from left to right
        //
        // the effective right char is the most recently encountered
        // non-transparent char
        //
        // four boolean states:
        //   the effective right char shapes
        //   the effective right char causes right shaping
        //   the current char shapes
        //   the current char causes left shaping
        //
        // if both cause shaping, then
        //   right += 2 (isolate to initial, or final to medial)
        //   cur += 1 (isolate to final)

        // ern is effective right logical index
        int ern = -1;

        boolean rightShapes = false;
        boolean rightCauses = (rightType & MASK_SHAPE_LEFT) != 0;

        for (int n = 0; n < chars.length; n++) {
            char c = chars[n];
            int t = getShapeType(c);

            if ((t & MASK_TRANSPARENT) != 0) {
                continue;
            }

            boolean curShapes = (t & MASK_NOSHAPE) == 0;
            boolean curCauses = (t & MASK_SHAPE_RIGHT) != 0;

            if (rightCauses && curCauses) {
                if (rightShapes) {
                    chars[ern] += 2;
                }

                if (curShapes) {
                    chars[n] = (char) (isolClassTable.getGlyphClassID(c) + 1);
                }
            } else {
                if (curShapes) {
                    chars[n] = (char) isolClassTable.getGlyphClassID(c);
                }
            }

            rightShapes = curShapes;
            rightCauses = (t & MASK_SHAPE_LEFT) != 0;
            ern = n;
        }

        if (rightShapes && rightCauses && (leftType & MASK_SHAPE_RIGHT) != 0) {
            chars[ern] += 2;
        }
    }
}
