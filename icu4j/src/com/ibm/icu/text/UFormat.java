/*
 *******************************************************************************
 * Copyright (C) 2003, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/text/UFormat.java,v $ 
 * $Date: 2003/11/21 00:16:34 $ 
 * $Revision: 1.2 $
 *
 *******************************************************************************
 */
package com.ibm.icu.text;

import java.text.Format;
import com.ibm.icu.util.ULocale;

/**
 * An abstract class that extends from java.text.Format class. This class is 
 * intended for adding additional functionality to the base class.
 * @author weiv
 * @draft ICU 2.8
 */
public abstract class UFormat extends Format {
	/**
     * Return the locale used by the format object depending on the type
     * 
     * @param type The type fo the locale that should returned.
     * @return ULocale object for the type requested
     * @see ULocale.ULocaleDataType
     * @see ULocale
     * @draft ICU 2.8
     */
	public abstract ULocale getLocale(ULocale.ULocaleDataType type);

}
