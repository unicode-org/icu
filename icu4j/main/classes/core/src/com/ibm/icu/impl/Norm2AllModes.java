/*
*******************************************************************************
*   Copyright (C) 2009-2010, International Business Machines
*   Corporation and others.  All Rights Reserved.
*******************************************************************************
*/
package com.ibm.icu.impl;

import java.io.InputStream;
import java.io.IOException;

import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.Normalizer2;

public final class Norm2AllModes {
    // Public API dispatch via Normalizer2 subclasses -------------------------- ***

    // Normalizer2 implementation for the old UNORM_NONE.
    public static final class NoopNormalizer2 extends Normalizer2 {
        @Override
        public StringBuilder normalize(CharSequence src, StringBuilder dest) {
            if(dest!=src) {
                dest.setLength(0);
                return dest.append(src);
            } else {
                throw new IllegalArgumentException();
            }
        }
        @Override
        public Appendable normalize(CharSequence src, Appendable dest) {
            if(dest!=src) {
                try {
                    return dest.append(src);
                } catch(IOException e) {
                    throw new RuntimeException(e);  // Avoid declaring "throws IOException".
                }
            } else {
                throw new IllegalArgumentException();
            }
        }
        @Override
        public StringBuilder normalizeSecondAndAppend(StringBuilder first, CharSequence second) {
            if(first!=second) {
                return first.append(second);
            } else {
                throw new IllegalArgumentException();
            }
        }
        @Override
        public StringBuilder append(StringBuilder first, CharSequence second) {
            if(first!=second) {
                return first.append(second);
            } else {
                throw new IllegalArgumentException();
            }
        }
        @Override
        public boolean isNormalized(CharSequence s) { return true; }
        @Override
        public Normalizer.QuickCheckResult quickCheck(CharSequence s) { return Normalizer.YES; }
        @Override
        public int spanQuickCheckYes(CharSequence s) { return s.length(); }
        @Override
        public boolean hasBoundaryBefore(int c) { return true; }
        @Override
        public boolean hasBoundaryAfter(int c) { return true; }
        @Override
        public boolean isInert(int c) { return true; }
    }

    // Intermediate class:
    // Has Normalizer2Impl and does boilerplate argument checking and setup.
    public static abstract class Normalizer2WithImpl extends Normalizer2 {
        public Normalizer2WithImpl(Normalizer2Impl ni) {
            impl=ni;
        }

        // normalize
        @Override
        public StringBuilder normalize(CharSequence src, StringBuilder dest) {
            if(dest==src) {
                throw new IllegalArgumentException();
            }
            dest.setLength(0);
            normalize(src, new Normalizer2Impl.ReorderingBuffer(impl, dest, src.length()));
            return dest;
        }
        @Override
        public Appendable normalize(CharSequence src, Appendable dest) {
            if(dest==src) {
                throw new IllegalArgumentException();
            }
            Normalizer2Impl.ReorderingBuffer buffer=
                new Normalizer2Impl.ReorderingBuffer(impl, dest, src.length());
            normalize(src, buffer);
            buffer.flush();
            return dest;
        }
        protected abstract void normalize(CharSequence src, Normalizer2Impl.ReorderingBuffer buffer);

        // normalize and append
        @Override
        public StringBuilder normalizeSecondAndAppend(StringBuilder first, CharSequence second) {
            return normalizeSecondAndAppend(first, second, true);
        }
        @Override
        public StringBuilder append(StringBuilder first, CharSequence second) {
            return normalizeSecondAndAppend(first, second, false);
        }
        public StringBuilder normalizeSecondAndAppend(
                StringBuilder first, CharSequence second, boolean doNormalize) {
            if(first==second) {
                throw new IllegalArgumentException();
            }
            normalizeAndAppend(
                second, doNormalize,
                new Normalizer2Impl.ReorderingBuffer(impl, first, first.length()+second.length()));
            return first;
        }
        protected abstract void normalizeAndAppend(
                CharSequence src, boolean doNormalize, Normalizer2Impl.ReorderingBuffer buffer);

        // quick checks
        @Override
        public boolean isNormalized(CharSequence s) {
            return s.length()==spanQuickCheckYes(s);
        }
        @Override
        public Normalizer.QuickCheckResult quickCheck(CharSequence s) {
            return isNormalized(s) ? Normalizer.YES : Normalizer.NO;
        }

        public int getQuickCheck(int c) {
            return 1;
        }

        public final Normalizer2Impl impl;
    }

    public static final class DecomposeNormalizer2 extends Normalizer2WithImpl {
        public DecomposeNormalizer2(Normalizer2Impl ni) {
            super(ni);
        }

        @Override
        protected void normalize(CharSequence src, Normalizer2Impl.ReorderingBuffer buffer) {
            impl.decompose(src, 0, src.length(), buffer);
        }
        @Override
        protected void normalizeAndAppend(
                CharSequence src, boolean doNormalize, Normalizer2Impl.ReorderingBuffer buffer) {
            impl.decomposeAndAppend(src, doNormalize, buffer);
        }
        @Override
        public int spanQuickCheckYes(CharSequence s) {
            return impl.decompose(s, 0, s.length(), null);
        }
        @Override
        public int getQuickCheck(int c) {
            return impl.isDecompYes(impl.getNorm16(c)) ? 1 : 0;
        }
        @Override
        public boolean hasBoundaryBefore(int c) { return impl.hasDecompBoundary(c, true); }
        @Override
        public boolean hasBoundaryAfter(int c) { return impl.hasDecompBoundary(c, false); }
        @Override
        public boolean isInert(int c) { return impl.isDecompInert(c); }
    }

    public static final class ComposeNormalizer2 extends Normalizer2WithImpl {
        public ComposeNormalizer2(Normalizer2Impl ni, boolean fcc) {
            super(ni);
            onlyContiguous=fcc;
        }

        @Override
        protected void normalize(CharSequence src, Normalizer2Impl.ReorderingBuffer buffer) {
            impl.compose(src, 0, src.length(), onlyContiguous, true, buffer);
        }
        @Override
        protected void normalizeAndAppend(
                CharSequence src, boolean doNormalize, Normalizer2Impl.ReorderingBuffer buffer) {
            impl.composeAndAppend(src, doNormalize, onlyContiguous, buffer);
        }

        @Override
        public boolean isNormalized(CharSequence s) {
            // 5: small destCapacity for substring normalization
            return impl.compose(s, 0, s.length(),
                                onlyContiguous, false,
                                new Normalizer2Impl.ReorderingBuffer(impl, new StringBuilder(), 5));
        }
        @Override
        public Normalizer.QuickCheckResult quickCheck(CharSequence s) {
            int spanLengthAndMaybe=impl.composeQuickCheck(s, 0, s.length(), onlyContiguous, false);
            if((spanLengthAndMaybe&1)!=0) {
                return Normalizer.MAYBE;
            } else if((spanLengthAndMaybe>>>1)==s.length()) {
                return Normalizer.YES;
            } else {
                return Normalizer.NO;
            }
        }
        @Override
        public int spanQuickCheckYes(CharSequence s) {
            return impl.composeQuickCheck(s, 0, s.length(), onlyContiguous, true)>>>1;
        }
        @Override
        public int getQuickCheck(int c) {
            return impl.getCompQuickCheck(impl.getNorm16(c));
        }
        @Override
        public boolean hasBoundaryBefore(int c) { return impl.hasCompBoundaryBefore(c); }
        @Override
        public boolean hasBoundaryAfter(int c) {
            return impl.hasCompBoundaryAfter(c, onlyContiguous, false);
        }
        @Override
        public boolean isInert(int c) {
            return impl.hasCompBoundaryAfter(c, onlyContiguous, true);
        }

        private final boolean onlyContiguous;
    }

    public static final class FCDNormalizer2 extends Normalizer2WithImpl {
        public FCDNormalizer2(Normalizer2Impl ni) {
            super(ni);
        }

        @Override
        protected void normalize(CharSequence src, Normalizer2Impl.ReorderingBuffer buffer) {
            impl.makeFCD(src, 0, src.length(), buffer);
        }
        @Override
        protected void normalizeAndAppend(
                CharSequence src, boolean doNormalize, Normalizer2Impl.ReorderingBuffer buffer) {
            impl.makeFCDAndAppend(src, doNormalize, buffer);
        }
        @Override
        public int spanQuickCheckYes(CharSequence s) {
            return impl.makeFCD(s, 0, s.length(), null);
        }
        @Override
        public int getQuickCheck(int c) {
            return impl.isDecompYes(impl.getNorm16(c)) ? 1 : 0;
        }
        @Override
        public boolean hasBoundaryBefore(int c) { return impl.hasFCDBoundaryBefore(c); }
        @Override
        public boolean hasBoundaryAfter(int c) { return impl.hasFCDBoundaryAfter(c); }
        @Override
        public boolean isInert(int c) { return impl.isFCDInert(c); }
    }

    // instance cache ---------------------------------------------------------- ***

    private Norm2AllModes(Normalizer2Impl ni) {
        impl=ni;
        comp=new ComposeNormalizer2(ni, false);
        decomp=new DecomposeNormalizer2(ni);
        fcd=new FCDNormalizer2(ni);
        fcc=new ComposeNormalizer2(ni, true);
    }

    public final Normalizer2Impl impl;
    public final ComposeNormalizer2 comp;
    public final DecomposeNormalizer2 decomp;
    public final FCDNormalizer2 fcd;
    public final ComposeNormalizer2 fcc;

    private static Norm2AllModes getInstanceFromSingletonNoIOException(Norm2AllModesSingleton singleton) {
        if(singleton.runtimeException!=null) {
            throw singleton.runtimeException;
        }
        return singleton.allModes;
    }
    public static Norm2AllModes getNFCInstanceNoIOException() {
        return getInstanceFromSingletonNoIOException(NFCSingleton.INSTANCE);
    }
    public static Norm2AllModes getNFKCInstanceNoIOException() {
        return getInstanceFromSingletonNoIOException(NFKCSingleton.INSTANCE);
    }
    public static Norm2AllModes getNFKC_CFInstanceNoIOException() {
        return getInstanceFromSingletonNoIOException(NFKC_CFSingleton.INSTANCE);
    }
    // For use in properties APIs.
    public static Normalizer2WithImpl getN2WithImpl(int index) {
        switch(index) {
        case 0: return getNFCInstanceNoIOException().decomp;  // NFD
        case 1: return getNFKCInstanceNoIOException().decomp; // NFKD
        case 2: return getNFCInstanceNoIOException().comp;    // NFC
        case 3: return getNFKCInstanceNoIOException().comp;   // NFKC
        default: return null;
        }
    }
    public static Norm2AllModes getInstance(InputStream data, String name) throws IOException {
        if(data==null) {
            Norm2AllModesSingleton singleton;
            if(name.equals("nfc")) {
                singleton=NFCSingleton.INSTANCE;
            } else if(name.equals("nfkc")) {
                singleton=NFKCSingleton.INSTANCE;
            } else if(name.equals("nfkc_cf")) {
                singleton=NFKC_CFSingleton.INSTANCE;
            } else {
                throw new UnsupportedOperationException();  // TODO
            }
            if(singleton.ioException!=null) {
                throw singleton.ioException;
            } else if(singleton.runtimeException!=null) {
                throw singleton.runtimeException;
            }
            return singleton.allModes;
        }
        throw new UnsupportedOperationException();  // TODO
    }

    public static final NoopNormalizer2 NOOP_NORMALIZER2=new NoopNormalizer2();
    /**
     * Gets the FCD normalizer, with the FCD data initialized.
     * @return FCD normalizer
     */
    public static Normalizer2 getFCDNormalizer2NoIOException() {
        Norm2AllModes allModes=getNFCInstanceNoIOException();
        allModes.impl.getFCDTrie();
        return allModes.fcd;
    }

    private static class Norm2AllModesSingleton {
        private Norm2AllModesSingleton(InputStream data, String name) {
            Normalizer2Impl impl;
            if(data==null) {
                try {
                    impl=new Normalizer2Impl().load(ICUResourceBundle.ICU_BUNDLE+"/"+name+".nrm");
                    allModes=new Norm2AllModes(impl);
                } catch(IOException e) {
                    ioException=e;
                    runtimeException=new RuntimeException(e);
                } catch(RuntimeException e) {
                    runtimeException=e;
                }
            } else {
                throw new UnsupportedOperationException();  // TODO
            }
        }

        private Norm2AllModes allModes;
        private IOException ioException;
        private RuntimeException runtimeException;
    }
    private static final class NFCSingleton extends Norm2AllModesSingleton {
        private NFCSingleton() {
            super(null, "nfc");
        }
        private static final NFCSingleton INSTANCE=new NFCSingleton();
    }
    private static final class NFKCSingleton extends Norm2AllModesSingleton {
        private NFKCSingleton() {
            super(null, "nfkc");
        }
        private static final NFKCSingleton INSTANCE=new NFKCSingleton();
    }
    private static final class NFKC_CFSingleton extends Norm2AllModesSingleton {
        private NFKC_CFSingleton() {
            super(null, "nfkc_cf");
        }
        private static final NFKC_CFSingleton INSTANCE=new NFKC_CFSingleton();
    }
}
