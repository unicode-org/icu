/*
 *******************************************************************************
 * Copyright (C) 2014, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.NoSuchElementException;

/**
 * Thin wrapper of java.util.LinkedList implementing Java 6 Deque on
 * Java 5 runtime environment. This class might be removed when the minimum
 * Java runtime version is updated to 6 or later.
 */
public class Deque<E> {
    private LinkedList<E> ll = new LinkedList<E>();

    public Deque() {
    }

    public boolean isEmpty() {
        return ll.isEmpty();
    }

    public Object[] toArray() {
        return ll.toArray();
    }

    public <T> T[] toArray(T[] a) {
        return ll.toArray(a);
    }

    public boolean containsAll(Collection<?> c) {
        return ll.containsAll(c);
    }

    public boolean addAll(Collection<? extends E> c) {
        return ll.addAll(c);
    }

    public boolean removeAll(Collection<?> c) {
        return ll.removeAll(c);
    }

    public boolean retainAll(Collection<?> c) {
        return ll.retainAll(c);
    }

    public void clear() {
        ll.clear();
    }

    public void addFirst(E e) {
        ll.addFirst(e);
    }

    public void addLast(E e) {
        ll.addLast(e);
    }

    public boolean offerFirst(E e) {
        ll.addFirst(e);
        return true;
    }

    public boolean offerLast(E e) {
        ll.addLast(e);
        return true;
    }

    public E removeFirst() {
        return ll.removeFirst();
    }

    public E removeLast() {
        return ll.removeLast();
    }

    public E pollFirst() {
        return ll.poll();
    }

    public E pollLast() {
        E e;
        try {
            e = ll.removeLast();
        } catch (NoSuchElementException ex) {
            // ignore the exception and return null
            e = null;
        }
        return e;
    }

    public E getFirst() {
        return ll.getFirst();
    }

    public E getLast() {
        return ll.getLast();
    }

    public E peekFirst() {
        return ll.peek();
    }

    public E peekLast() {
        E e;
        try {
            e = ll.getLast();
        } catch (NoSuchElementException ex) {
            // ignore the exception and return null
            e = null;
        }
        return e;
    }

    public boolean removeFirstOccurrence(Object o) {
        return ll.remove(o);
    }

    public boolean removeLastOccurrence(Object o) {
        ListIterator<E> litr = ll.listIterator(ll.size());
        while (litr.hasPrevious()) {
            E e = litr.previous();
            if ((o == null && e == null) || (o != null && o.equals(e))) {
                litr.remove();
                return true;
            }
        }
        return false;
    }

    public boolean add(E e) {
        return ll.add(e);
    }

    public boolean offer(E e) {
        return ll.offer(e);
    }

    public E remove() {
        return ll.remove();
    }

    public E poll() {
        return ll.poll();
    }

    public E element() {
        return ll.element();
    }

    public E peek() {
        return ll.peek();
    }

    public void push(E e) {
        ll.addFirst(e);
    }

    public E pop() {
        return ll.removeFirst();
    }

    public boolean remove(Object o) {
        return ll.remove(o);
    }

    public boolean contains(Object o) {
        return ll.contains(o);
    }

    public int size() {
        return ll.size();
    }

    public Iterator<E> iterator() {
        return ll.iterator();
    }

    public Iterator<E> descendingIterator() {
        return new DescendingIterator();
    }

    private class DescendingIterator implements Iterator<E> {
        private ListIterator<E> litr;

        DescendingIterator() {
            litr = ll.listIterator(ll.size());
        }

        /* (non-Javadoc)
         * @see java.util.Iterator#hasNext()
         */
        public boolean hasNext() {
            return litr.hasPrevious();
        }

        /* (non-Javadoc)
         * @see java.util.Iterator#next()
         */
        public E next() {
            return litr.previous();
        }

        /* (non-Javadoc)
         * @see java.util.Iterator#remove()
         */
        public void remove() {
            litr.remove();
        }
        
    }
}
