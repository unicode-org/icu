// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2013-2014, International Business Machines
 * Corporation and others.  All Rights Reserved.
 *******************************************************************************
 * SharedObject.java, ported from sharedobject.h/.cpp
 *
 * C++ version created on: 2013dec19
 * created by: Markus W. Scherer
 */

package com.ibm.icu.impl.coll;

import com.ibm.icu.util.ICUCloneNotSupportedException;
import java.lang.ref.Cleaner;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Base class for shared, reference-counted, auto-deleted objects. Java subclasses are mutable and
 * must implement clone().
 *
 * <p>In C++, the SharedObject base class is used for both memory and ownership management. In Java,
 * memory management (deletion after last reference is gone) is up to the garbage collector, but the
 * reference counter is still used to see whether the referent is the sole owner.
 *
 * <p>Usage:
 *
 * <pre>
 * class S extends SharedObject {
 *     public clone() { ... }
 * }
 *
 * // Use the nest class Reference (which costs an extra allocation),
 * class U {
 *     // For read-only access, use s.readOnly().
 *     // For writable access, use S ownedS = s.copyOnWrite();
 *     private SharedObject.Reference&lt;S&gt; s;
 *     public U(Reference&lt;U&gt; ref) {
 *         ...
 *     }
 *     // Returns a writable version of s.
 *     // If there is exactly one owner, then s itself is returned.
 *     // If there are multiple owners, then s is replaced with a clone,
 *     // and that is returned.
 *     private S getOwnedS() {
 *         return s.copyOnWrite();
 *     }
 * }
 * </pre>
 *
 * Either use only Java memory management, or use addRef()/removeRef(). Sharing requires
 * reference-counting.
 *
 * <p>TODO: Consider making this more widely available inside ICU, or else adopting a different
 * model.
 */
public class SharedObject implements Cloneable {
    /**
     * A class wrapping SharedObject. We need a final field in Reference class to access a
     * SharedObject. This class implements Runnable interface which will be used for clean up a
     * SharedObject with no references.
     */
    private static final class SharedObjectHolder<T extends SharedObject> implements Runnable {
        private T sharedObj;

        SharedObjectHolder(T so) {
            this.sharedObj = so;
        }

        void set(T otherSo) {
            this.sharedObj = otherSo;
        }

        T get() {
            return sharedObj;
        }

        /*
         * This method is triggered by the Cleaner in Reference class below.
         */
        @Override
        public void run() {
            if (sharedObj != null) {
                sharedObj.removeRef();
                sharedObj = null;
            }
        }
    }

    public static final class Reference<T extends SharedObject> implements AutoCloseable {
        private static final Cleaner CLEANER = Cleaner.create();

        // We need a final field to reach the SharedObject to be cleaned when
        // it is no longer used.
        private final SharedObjectHolder<T> sharedObjectHolder;
        private final Cleaner.Cleanable cleanable;

        public Reference(T so) {
            this.sharedObjectHolder = new SharedObjectHolder<T>(so);
            if (so != null) {
                so.addRef();
            }
            // Registers this object to the CLEANER with a sharedObjectHolder implementing
            // Runnable used for clean up action.
            this.cleanable = CLEANER.register(this, sharedObjectHolder);
        }

        /**
         * Copy constructor. Previously this class implemented Cloneable and had clone() method.
         * Cloning an object with a final field which require modification is tricky (need to use
         * reflection to force updating the field) and discouraged. The new implementation uses this
         * copy constructor instead of clone().
         *
         * @param ref A Reference object pointing a SharedObject. This constructor automatically
         *     increment reference count of the SharedObject.
         */
        public Reference(Reference<T> ref) {
            this(ref.readOnly());
        }

        public T readOnly() {
            return sharedObjectHolder.get();
        }

        public T copyOnWrite() {
            T so = sharedObjectHolder.get();
            if (so.getRefCount() <= 1) {
                return so;
            }
            @SuppressWarnings("unchecked")
            T cso = (T) so.clone();
            so.removeRef();
            cso.addRef();
            sharedObjectHolder.set(cso);
            return cso;
        }

        /**
         * It's recommended to implement AutoCloseable in a class utilizing Java Cleaner. Although
         * it's currently not used by ICU4J implementation, this design allow ICU4J developer to use
         * try-with-resource statement to close the resource automatically.
         */
        @Override
        public void close() throws Exception {
            cleanable.clean();
        }
    }

    /** Initializes refCount to 0. */
    public SharedObject() {}

    /** Initializes refCount to 0. */
    @Override
    public SharedObject clone() {
        SharedObject c;
        try {
            c = (SharedObject) super.clone();
        } catch (CloneNotSupportedException e) {
            // Should never happen.
            throw new ICUCloneNotSupportedException(e);
        }
        c.refCount = new AtomicInteger();
        return c;
    }

    /** Increments the number of references to this object. Thread-safe. */
    public final void addRef() {
        refCount.incrementAndGet();
    }

    /**
     * Decrements the number of references to this object, and auto-deletes "this" if the number
     * becomes 0. Thread-safe.
     */
    public final void removeRef() {
        // Deletion in Java is up to the garbage collector.
        refCount.decrementAndGet();
    }

    /** Returns the reference counter. Uses a memory barrier. */
    public final int getRefCount() {
        return refCount.get();
    }

    public final void deleteIfZeroRefCount() {
        // Deletion in Java is up to the garbage collector.
    }

    private AtomicInteger refCount = new AtomicInteger();
}
