/*
 ******************************************************************************
 * Copyright (C) 2005, International Business Machines Corporation and        *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
*/
package com.ibm.icu.dev.test.util;

/**
 * Provides a flexible mechanism for controlling access, without requiring that a class be immutable.
 * Once locked, an object can never be unlocked, so it is thread-safe from that point onward.
 * The implementation of both methods must be synchronized.
 * Once the object has been locked, it must guarantee that no changes can be made to it.
 * Any attempt to alter it must raise an UnsupportedOperationException exception.
 * This means that when the object returns internal objects,
 * or if anyone has references to those internal objects, that those internal objects must either be immutable,
 * or must also raise exceptions if any attempt to modify them is made. Of course, the object can return clones
 * of internal objects, since those are safe. * @author davis
 */
public interface Lockable extends Cloneable {
	/**
	 * Determines whether the object has been locked or not.
	 */
	public boolean isLocked();
	/**
	 * Locks the object.
	 * @return the object itself.
	 */
	public Object lock();
	/**
	 * Provides for the clone operation. Any clone is initially unlocked.
	 */
	public Object clone();
}