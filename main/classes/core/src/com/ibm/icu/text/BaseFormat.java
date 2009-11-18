/*
 *******************************************************************************
 * Copyright (C) 2009, Google, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.text;

/**
 * @author markdavis
 *
 */
public interface BaseFormat<T, U extends Appendable, S extends CharSequence> extends Formatter<T, U>, Parser<T, S> {

}
