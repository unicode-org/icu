/*
 *******************************************************************************
 * Copyright (C) 2012, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.text;

/**
 * Display context settings.
 * Note, the specific numeric values are internal and may change.
 * @draft ICU 51
 * @provisional This API might change or be removed in a future release.
 */
public enum DisplayContext {
    /**
     * ================================
     * Settings for DIALECT_HANDLING (use one)
     */
    /**
     * A possible setting for DIALECT_HANDLING:
     * use standard names when generating a locale name,
     * e.g. en_GB displays as 'English (United Kingdom)'.
     * @draft ICU 51
     * @provisional This API might change or be removed in a future release.
     */
    STANDARD_NAMES(Type.DIALECT_HANDLING, 0),
    /**
     * A possible setting for DIALECT_HANDLING:
     * use dialect names, when generating a locale name,
     * e.g. en_GB displays as 'British English'.
     * @draft ICU 51
     * @provisional This API might change or be removed in a future release.
     */
    DIALECT_NAMES(Type.DIALECT_HANDLING, 1),
    /**
     * ================================
     * Settings for CAPITALIZATION (use one)
     */
    /**
     * A possible setting for CAPITALIZATION:
     * The capitalization context to be used is unknown (this is the default value).
     * @draft ICU 51
     * @provisional This API might change or be removed in a future release.
     */
    CAPITALIZATION_NONE(Type.CAPITALIZATION, 0),
    /**
     * A possible setting for CAPITALIZATION:
     * The capitalization context if a date, date symbol or display name is to be
     * formatted with capitalization appropriate for the middle of a sentence.
     * @draft ICU 51
     * @provisional This API might change or be removed in a future release.
     */
    CAPITALIZATION_FOR_MIDDLE_OF_SENTENCE(Type.CAPITALIZATION, 1),
    /**
     * A possible setting for CAPITALIZATION:
     * The capitalization context if a date, date symbol or display name is to be
     * formatted with capitalization appropriate for the beginning of a sentence.
     * @draft ICU 51
     * @provisional This API might change or be removed in a future release.
     */
    CAPITALIZATION_FOR_BEGINNING_OF_SENTENCE(Type.CAPITALIZATION, 2),
    /**
     * A possible setting for CAPITALIZATION:
     * The capitalization context if a date, date symbol or display name is to be
     * formatted with capitalization appropriate for a user-interface list or menu item.
     * @draft ICU 51
     * @provisional This API might change or be removed in a future release.
     */
    CAPITALIZATION_FOR_UI_LIST_OR_MENU(Type.CAPITALIZATION, 3),
    /**
     * A possible setting for CAPITALIZATION:
     * The capitalization context if a date, date symbol or display name is to be
     * formatted with capitalization appropriate for stand-alone usage such as an
     * isolated name on a calendar page.
     * @draft ICU 51
     * @provisional This API might change or be removed in a future release.
     */
    CAPITALIZATION_FOR_STANDALONE(Type.CAPITALIZATION, 4);

    /**
     * Type values for DisplayContext
     * @draft ICU 51
     * @provisional This API might change or be removed in a future release.
     */
    public enum Type {
        /**
         * DIALECT_HANDLING can be set to STANDARD_NAMES or DIALECT_NAMES.
         * @draft ICU 51
         * @provisional This API might change or be removed in a future release.
         */
        DIALECT_HANDLING,
        /**
         * CAPITALIZATION can be set to one of CAPITALIZATION_NONE through
         * CAPITALIZATION_FOR_STANDALONE.
         * @draft ICU 51
         * @provisional This API might change or be removed in a future release.
         */
        CAPITALIZATION
    }

    private final Type type;
    private final int value;
    private DisplayContext(Type type, int value) {
        this.type = type;
        this.value = value;
    }
    /**
     * Get the Type part of the enum item
     * (e.g. CAPITALIZATION)
     * @draft ICU 51
     * @provisional This API might change or be removed in a future release.
     */
    public Type type() {
        return type;
    }
    /**
     * Get the value part of the enum item
     * (e.g. CAPITALIZATION_FOR_STANDALONE)
     * @draft ICU 51
     * @provisional This API might change or be removed in a future release.
     */
    public int value() {
        return value;
    }
}
