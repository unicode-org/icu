/*
 * Created on Nov 11, 2003
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.icu.text;

import java.text.Format;
import com.ibm.icu.util.ULocale;

/**
 * @author weiv
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public abstract class UFormat extends Format {
	
	public abstract ULocale getLocale(ULocale.ULocaleDataType type);

}
