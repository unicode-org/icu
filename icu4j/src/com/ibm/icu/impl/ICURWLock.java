package com.ibm.icu.impl;

// See Allan Holub's 1999 column in JavaWorld.

import java.util.LinkedList;

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
    private LinkedList ww;  // list of waiting writers
    private int wwc; // waiting writers
    private int rc; // active readers, -1 if there's an active writer
    private int wrc; // waiting readers

    private int stat_rc; // any read access granted
    private int stat_mrc; // multiple read access granted
    private int stat_wrc; // wait for read
    private int stat_wc; // write access granted
    private int stat_wwc; // wait for write 

    /**
     * Internal class used to gather statistics on the RWLock.
     */
    public final static class Stats {
	/**
	 * Number of times read access granted (read count).
	 */
	public final int _rc;

	/**
	 * Number of times concurrent read access granted (multiple read count).
	 */
	public final int _mrc;

	/**
	 * Number of times blocked for read (waiting reader count).
	 */
	public final int _wrc; // wait for read

	/**
	 * Number of times write access granted (writer count).
	 */
	public final int _wc;

	/**
	 * Number of times blocked for write (waiting writer count).
	 */
	public final int _wwc;

	private Stats(ICURWLock lock) {
	    this(lock.stat_rc, lock.stat_mrc, lock.stat_wrc, lock.stat_wc, lock.stat_wwc);
	}

	private Stats(int rc, int mrc, int wrc, int wc, int wwc) {
	    _rc = rc; _mrc = mrc; _wrc = wrc; _wc = wc; _wwc = wwc;
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
     * Reset the stats.
     */
    public void clearStats() {
	stat_rc = stat_mrc = stat_wrc = stat_wc = stat_wwc = 0;
    }
    
    /**
     * Return a snapshot of the current stats.  This does not clear the stats.
     */
    public Stats getStats() {
	return new Stats(this);
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
    public synchronized void acquireRead() {
	if (rc >= 0 && wwc == 0) {
	    ++rc;
	    ++stat_rc;
	    if (rc > 1) ++stat_mrc;
	} else {
	    ++wrc;
	    ++stat_wrc;
	    try {
		wait();
	    }
	    catch (InterruptedException e) {
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
    public synchronized void releaseRead() {
	if (rc > 0) {
	    if (0 == --rc) {
		notifyWaitingWriter();
	    }
	} else {
	    throw new InternalError("no current reader to release");
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
	// Do a quick check up top, to save us the lock allocation and
	// extra synch in the case where there is no contention for
	// writing.  This is common in ICUService.

	synchronized (this) {
	    if (rc == 0 && wwc == 0) {
		rc = -1;
		++stat_wc;
		return;
	    }
	}

	// We assume at this point that there is an active reader or a
	// waiting writer, so we don't recheck, though we could.

	// Create a lock for this thread only, it will be the only
	// thread notified when the lock comes to the front of the
	// waiting writer list.  We synchronize on the lock first, and
	// then this, because when we release the lock on this, the
	// lock will be available to the world.  If another thread
	// removed and notified that lock before we synchronized and
	// waited on it, we'd miss the only notification, and we would
	// wait on it forever.  So we synchronized on it before we
	// make it available, so that we're guaranteed to be waiting on
	// it before any notification can occur.

	Object lock = new Object();
	synchronized (lock) {
	    synchronized (this) {
		// again, we've assumed we don't have multiple waiting
		// writers, so we leave this null until we need it.
		// Once created, we keep it around.
		if (ww == null) {
		    ww = new LinkedList();
		}
		ww.addLast(lock);
		++wwc;
		++stat_wwc;
	    }
	    try {
		lock.wait();
	    }
	    catch (InterruptedException e) {
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
    public synchronized void releaseWrite() {
	if (rc < 0) {
	    if (wrc > 0) {
		rc = wrc;
		wrc = 0;
		if (rc > 0) {
		    stat_rc += rc;
		    if (rc > 1) {
			stat_mrc += rc - 1;
		    }
		}
		notifyAll();
	    } else {
		rc = 0;
		notifyWaitingWriter();
	    }
	} else {
	    throw new InternalError("no current writer to release");
	}
    }

    /**
     * If there is a waiting writer thread, mark us as active for
     * writing, remove it from the list, and notify it.  
     */
    private void notifyWaitingWriter() {
	// only called within a block synchronized on this
	// we don't assume there is necessarily a waiting writer,
	// no no error if there isn't.
	if (wwc > 0) {
	    rc = -1;
	    Object lock = ww.removeFirst();
	    --wwc;
	    ++stat_wc;
	    synchronized (lock) {
		lock.notify();
	    }
	}
    }
}
