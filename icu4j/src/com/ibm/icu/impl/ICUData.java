/*
 * Created on Feb 4, 2004
 */
package com.ibm.icu.impl;

import java.io.InputStream;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * @author dougfelt
 *
 * Provides access to ICU data files as InputStreams.  Implements security checking.
 */
public final class ICUData {
   /*
    * Return a URL to the ICU resource names resourceName.  The
    * resource name should either be an absolute path, or a path relative to
    * com.ibm.icu.impl (e.g., most likely it is 'data/foo').  If required
    * is true, throw an InternalError instead of returning a null result.
    */
	public static boolean exists(final String resourceName) {
		URL i = null;
		if (System.getSecurityManager() != null) {
			i = (URL)AccessController.doPrivileged(
			new PrivilegedAction() {
				public Object run() {
					return ICUData.class.getResource(resourceName);
				}
			});
		} else {
			i = ICUData.class.getResource(resourceName);
		}
		return i != null;
	}
	
	private static InputStream getStream(final String resourceName, boolean required) {
		InputStream i = null;
		if (System.getSecurityManager() != null) {
			i = (InputStream)AccessController.doPrivileged(
			new PrivilegedAction() {
				public Object run() {
					return ICUData.class.getResourceAsStream(resourceName);
				}
			});
		} else {
			i = ICUData.class.getResourceAsStream(resourceName);
		}
		if (i == null && required) {
			throw new InternalError("could not locate data " + resourceName);
		}
		return i;
	}
	
	/*
	 * Convenience override that calls get(resourceName, false);
	 */
	public static InputStream getStream(String resourceName) {
		return getStream(resourceName, false);
	}
	
	/*
	 * Convenience method that calls get(resourceName, true).
	 */
	public static InputStream getRequiredStream(String resourceName) {
		return getStream(resourceName, true);
	}
}
