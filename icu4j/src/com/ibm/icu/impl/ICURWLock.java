/**
 *******************************************************************************
 * Copyright (C) 2001-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl;

// See Allan Holub's 1999 column in JavaWorld, and Doug Lea's code for RWLocks with writer preference.


/**
 * <p>A simple Reader/Writer lock.  This assumes that there will
 * be little writing contention.  It also doesn't allow 
 * active readers to acquire and release a write lock, or
 * deal with priority inversion issues.</p>
 *
 * <p>Access to the lock should be enclosed in a try/finally block
 * in order to ensure that the lock is always released in case of
 * exceptions:<br><pre>
 * try {
 *     lock.acquireRead();
 *     // use service protected by the lock
 * }
 * finally {
 *     lock.releaseRead();
 * }
 * </pre></p>
 *
 * <p>The lock provides utility methods getStats and clearStats
 * to return statistics on the use of the lock.</p>
 */
public class ICURWLock {
    private Object writeLock = new Object();
    private Object readLock = new Object();
    private int wwc; // waiting writers
    private int rc; // active readers, -1 if there's an active writer
    private int wrc; // waiting readers

    private Stats stats = new Stats(); // maybe don't init to start...

    /**
     * Internal class used to gather statistics on the RWLock.
     */
    public final static class Stats {
    /**
     * Number of times read access granted (read count).
     */
    public int _rc;

    /**
     * Number of times concurrent read access granted (multiple read count).
     */
    public int _mrc;

    /**
     * Number of times blocked for read (waiting reader count).
     */
    public int _wrc; // wait for read

    /**
     * Number of times write access granted (writer count).
     */
    public int _wc;

    /**
     * Number of times blocked for write (waiting writer count).
     */
    public int _wwc;

    private Stats() {
    }

    private Stats(int rc, int mrc, int wrc, int wc, int wwc) {
        this._rc = rc;
        this._mrc = mrc;
        this._wrc = wrc;
        this._wc = wc;
        this._wwc = wwc;
    }

    private Stats(Stats rhs) {
        this(rhs._rc, rhs._mrc, rhs._wrc, rhs._wc, rhs._wwc);
    }

    /**
     * Return a string listing all the stats.
     */
    public String toString() {
        return " rc: " + _rc +
        " mrc: " + _mrc + 
        " wrc: " + _wrc +
        " wc: " + _wc +
        " wwc: " + _wwc;
    }
    }

    /**
     * Reset the stats.  Returns existing stats, if any.
     */
    public synchronized Stats resetStats() {
    Stats result = stats;
    stats = new Stats();
    return result;
    }

    /**
     * Clear the stats (stop collecting stats).  Returns existing stats, if any.
     */
    public synchronized Stats clearStats() {
    Stats result = stats;
    stats = null;
    return result;
    }
    
    /**
     * Return a snapshot of the current stats.  This does not reset the stats.
     */
    public synchronized Stats getStats() {
    return stats == null ? null : new Stats(stats);
    }

    // utilities

    private synchronized boolean gotRead() {
    ++rc;
    if (stats != null) {
        ++stats._rc;
        if (rc > 1) ++stats._mrc;
    }
    return true;
    }

    private synchronized boolean getRead() {
    if (rc >= 0 && wwc == 0) {
        return gotRead();
    }
    ++wrc;
    return false;
    }

    private synchronized boolean retryRead() {
    if (stats != null) ++stats._wrc;
    if (rc >= 0 && wwc == 0) {
        --wrc;
        return gotRead();
    }
    return false;
    }

    private synchronized boolean finishRead() {
    if (rc > 0) {
        return (0 == --rc && wwc > 0);
    }
    throw new InternalError("no current reader to release");
    }
    
    private synchronized boolean gotWrite() {
    rc = -1;
    if (stats != null) {
        ++stats._wc;
    }
    return true;
    }

    private synchronized boolean getWrite() {
    if (rc == 0) {
        return gotWrite();
    }
    ++wwc;
    return false;
    }

    private synchronized boolean retryWrite() {
    if (stats != null) ++stats._wwc;
    if (rc == 0) {
        --wwc;
        return gotWrite();
    }
    return false;
    }

    private static final int NOTIFY_NONE = 0;
    private static final int NOTIFY_WRITERS = 1;
    private static final int NOTIFY_READERS = 2;

    private synchronized int finishWrite() {
    if (rc < 0) {
        rc = 0;
        if (wwc > 0) {
        return NOTIFY_WRITERS;
        } else if (wrc > 0) {
        return NOTIFY_READERS;
        } else {
        return NOTIFY_NONE;
        }
    }
    throw new InternalError("no current writer to release");
    }
    
    /**
     * <p>Acquire a read lock, blocking until a read lock is
     * available.  Multiple readers can concurrently hold the read
     * lock.</p>
     *
     * <p>If there's a writer, or a waiting writer, increment the
     * waiting reader count and block on this.  Otherwise
     * increment the active reader count and return.  Caller must call
     * releaseRead when done (for example, in a finally block).</p> 
     */
    public void acquireRead() {
    if (!getRead()) {
        for (;;) {
        try {
            synchronized (readLock) {
            readLock.wait();
            }
            if (retryRead()) {
            return;
            }
        }
        catch (InterruptedException e) {
        }
        }
    }
    }

    /**
     * <p>Release a read lock and return.  An error will be thrown
     * if a read lock is not currently held.</p>
     *
     * <p>If this is the last active reader, notify the oldest
     * waiting writer.  Call when finished with work
     * controlled by acquireRead.</p>
     */
    public void releaseRead() {
    if (finishRead()) {
        synchronized (writeLock) {
        writeLock.notify();
        }
    }
    }

    /**
     * <p>Acquire the write lock, blocking until the write lock is
     * available.  Only one writer can acquire the write lock, and
     * when held, no readers can acquire the read lock.</p>
     *
     * <p>If there are no readers and no waiting writers, mark as
     * having an active writer and return.  Otherwise, add a lock to the
     * end of the waiting writer list, and block on it.  Caller
     * must call releaseWrite when done (for example, in a finally
     * block).<p> 
     */
    public void acquireWrite() {
    if (!getWrite()) {
        for (;;) {
        try {
            synchronized (writeLock) {
            writeLock.wait();
            }
            if (retryWrite()) {
            return;
            }
        }
        catch (InterruptedException e) {
        }
        }
    }
    }

    /**
     * <p>Release the write lock and return.  An error will be thrown
     * if the write lock is not currently held.</p>
     *
     * <p>If there are waiting readers, make them all active and
     * notify all of them.  Otherwise, notify the oldest waiting
     * writer, if any.  Call when finished with work controlled by
     * acquireWrite.</p> 
     */
    public void releaseWrite() {
    switch (finishWrite()) {
    case NOTIFY_WRITERS:
        synchronized (writeLock) {
        writeLock.notify();
        }
        break;
    case NOTIFY_READERS:
        synchronized (readLock) {
        readLock.notifyAll();
        }
        break;
    case NOTIFY_NONE:
        break;
    }
    }
}
