/*
 *******************************************************************************
 * Copyright (C) 1996-2012, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.text;

/*
 * Empty stub
 */
public class MessagePattern {
    private MessagePattern() {}

    public enum ApostropheMode {
        /**
         * A literal apostrophe is represented by
         * either a single or a double apostrophe pattern character.
         * Within a MessageFormat pattern, a single apostrophe only starts quoted literal text
         * if it immediately precedes a curly brace {},
         * or a pipe symbol | if inside a choice format,
         * or a pound symbol # if inside a plural format.
         * <p>
         * This is the default behavior starting with ICU 4.8.
         * @draft ICU 4.8
         * @provisional This API might change or be removed in a future release.
         */
        DOUBLE_OPTIONAL,
        /**
         * A literal apostrophe must be represented by
         * a double apostrophe pattern character.
         * A single apostrophe always starts quoted literal text.
         * <p>
         * This is the behavior of ICU 4.6 and earlier, and of the JDK.
         * @draft ICU 4.8
         * @provisional This API might change or be removed in a future release.
         */
        DOUBLE_REQUIRED
    }
}
