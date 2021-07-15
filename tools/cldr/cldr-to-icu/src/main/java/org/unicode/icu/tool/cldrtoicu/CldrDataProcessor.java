// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.unicode.cldr.api.CldrData;
import org.unicode.cldr.api.CldrData.PathOrder;
import org.unicode.cldr.api.CldrData.PrefixVisitor;
import org.unicode.cldr.api.CldrData.PrefixVisitor.Context;
import org.unicode.cldr.api.CldrPath;
import org.unicode.cldr.api.CldrValue;
import org.unicode.cldr.api.PathMatcher;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 * An immutable processor which can be configured to process CLDR data according to a series of
 * mappings from CLDR paths to "actions".
 *
 * <p>In typical use a processor would be statically created to bind paths and handler functions
 * (actions) together, and calling {@link CldrDataProcessor#process(CldrData, Object, PathOrder)}
 * once for each {@link CldrData} instance.
 *
 * <p>A processor is built by adding a mixture of "actions" to a builder. An action either defines
 * how to handle a single value (see {@link SubProcessor#addValueAction addValueAction()}) or how
 * to start a new sub-processor at a specific point in the data hierarchy (see {@link
 * SubProcessor#addAction addAction()} or {@link SubProcessor#addSubprocessor addSubprocessor()}).
 *
 * @param <T> the main "state" type used by the processor for the top-level processing.
 */
public class CldrDataProcessor<T> {
    /** Returns a processor builder which operates on a "state" of type {@code <T>}. */
    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    /**
     * A builder for processing a CLDR data sub-hierarchy.
     *
     * @param <T> the "state" type used by the processor.
     */
    public static abstract class SubProcessor<T> {
        final List<PrefixBuilder<?, T>> prefixActions = new ArrayList<>();
        final List<ValueAction<T>> valueActions = new ArrayList<>();

        private SubProcessor() { }

        /**
         * Binds a subtype action to a {@link PathMatcher} prefix pattern, returning a new builder
         * for the sub-hierarchy.
         *
         * <p>This method is intended for cases where the subtype state does not depend on the
         * parent state or the path prefix, but needs some post-processing. For example, the
         * subtype state might just be a {@code List} and the elements added to it must be
         * combined with the parent state after sub-hierarchy is processing is complete.
         *
         * <pre>{@code
         * processor
         *     .addAction("//parent/path", ArrayList::new, ParentState::addValues)
         *     .addValueAction("value/suffix", List::add);
         * }</pre>
         *
         * @param pattern the path pattern for the prefix where sub-processing starts.
         * @param newStateFn a supplier of subtype state instances for each sub-processing step.
         * @param doneFn called after each sub-processing step.
         */
        public <S> SubProcessor<S> addAction(
            String pattern, Supplier<S> newStateFn, BiConsumer<T, ? super S> doneFn) {
            return addAction(pattern, (t, p) -> newStateFn.get(), doneFn);
        }

        /**
         * Binds a subtype action to a {@link PathMatcher} prefix pattern, returning a new builder
         * for the sub-hierarchy.
         *
         * <p>This method is similar to {@link #addAction(String, Supplier, BiConsumer)} but is
         * intended for cases where the subtype state depends on the parent path prefix.
         *
         * <pre>{@code
         * processor
         *     .addAction("//parent/path[@type=*]", SubState::fromType, ParentState::addSubState)
         *     .addValueAction("value/suffix", SubState::collectValue);
         * }</pre>
         *
         * @param pattern the path pattern for the prefix where sub-processing starts.
         * @param newStateFn a supplier of subtype state instances for each sub-processing step.
         * @param doneFn called after each sub-processing step.
         */
        public <S> SubProcessor<S> addAction(
            String pattern, Function<CldrPath, S> newStateFn, BiConsumer<T, ? super S> doneFn) {
            return addAction(pattern, (t, p) -> newStateFn.apply(p), doneFn);
        }

        /**
         * Binds a subtype action to a {@link PathMatcher} prefix pattern, returning a new builder
         * for the sub-hierarchy.
         *
         * <p>This method is intended for the case where the subtype state is derived from the
         * parent state (e.g. an inner class) but does not depend on the path prefix at which the
         * sub-hierarchy is rooted.
         *
         * <pre>{@code
         * processor
         *     .addAction("//parent/path", ParentState::newValueCollector)
         *     .addValueAction("value/suffix", ValueCollector::addValue);
         * }</pre>
         *
         * @param pattern the path pattern for the prefix where sub-processing starts.
         * @param newStateFn a supplier of subtype state instances for each sub-processing step.
         */
        public <S> SubProcessor<S> addAction(String pattern, Function<T, S> newStateFn) {
            return addAction(pattern, (t, p) -> newStateFn.apply(t));
        }

        /**
         * Binds a subtype action to a {@link PathMatcher} prefix pattern, returning a new builder
         * for the sub-hierarchy.
         *
         * <p>This method is intended for the case where the subtype state is derived from the
         * parent state (e.g. an inner class) and the path prefix at which the sub-hierarchy is
         * rooted.
         *
         * <pre>{@code
         * processor
         *     .addAction("//parent/path[@type=*]", ParentState::newCollectorOfType)
         *     .addValueAction("value/suffix", ValueCollector::addValue);
         * }</pre>
         *
         * @param pattern the path pattern for the prefix where sub-processing starts.
         * @param newStateFn a supplier of subtype state instances for each sub-processing step.
         */
        public <S> SubProcessor<S> addAction(
            String pattern, BiFunction<T, CldrPath, S> newStateFn) {
            return addAction(pattern, newStateFn, (t, y) -> {});
        }

        /**
         * Binds a subtype action to a {@link PathMatcher} prefix pattern, returning a new builder
         * for the sub-hierarchy.
         *
         * <p>This method is the most general purpose way to add a sub-hierarchy action and is
         * intended for the most complex cases, where subtype state depends on parent state and
         * path prefix, and post processing is required. All other implementations of {@code
         * addAction} simply delegate to this one in one way or another.
         *
         * <pre>{@code
         * processor
         *     .addAction("//parent/path[@type=*]", ParentState::newCollector, ParentState::done)
         *     .addValueAction("value/suffix", ValueCollector::addValue);
         * }</pre>
         *
         * @param pattern the path pattern for the prefix where sub-processing starts.
         * @param newStateFn a supplier of subtype state instances for each sub-processing step.
         * @param doneFn called after each sub-processing step.
         */
        public <S> SubProcessor<S> addAction(
            String pattern,
            BiFunction<T, CldrPath, S> newStateFn,
            BiConsumer<T, ? super S> doneFn) {

            PrefixBuilder<S, T> action =
                new PrefixBuilder<>(getMatcher(pattern), newStateFn, doneFn);
            prefixActions.add(action);
            return action;
        }

        /**
         * Returns a new sub-processor for the specified sub-hierarchy rooted at the given
         * {@link PathMatcher} prefix pattern. The new processor builder has the same state type as
         * the parent.
         *
         * <p>This method is intended for the case where multiple sub-processors are needed below
         * a certain point in the hierarchy, but they all operate on the same state instance.
         *
         * <pre>{@code
         * SubBuilder<MyCollector> subprocessor = processor.addSubprocessor("//parent/path");
         * subprocessor.addValueAction("value/suffix", MyCollector::addValue);
         * subprocessor.addValueAction("other/suffix", MyCollector::addOtherValue);
         * }</pre>
         *
         * @param pattern the path pattern for the prefix where sub-processing starts.
         */
        public SubProcessor<T> addSubprocessor(String pattern) {
            return addAction(pattern, (t, p) -> t);
        }

        /**
         * Returns a new sub-processor for the specified sub-hierarchy rooted at the given
         * {@link PathMatcher} prefix pattern. The new processor builder has the same state type as
         * the parent.
         *
         * <p>This method is intended for the case where a some setup is required before a
         * sub-hierarchy is processed, but the sub-processor state is the same.
         *
         * <pre>{@code
         * SubBuilder<MyCollector> subprocessor = processor
         *     .addSubprocessor("//parent/path", MyCollector::startFn)
         *     .addValueAction("value/suffix", MyCollector::addValue);
         * }</pre>
         *
         * @param startFn a handler called when sub-processing begins
         * @param pattern the path pattern for the prefix where sub-processing starts.
         */
        public SubProcessor<T> addSubprocessor(String pattern, BiConsumer<T, CldrPath> startFn) {
            return addAction(pattern, (t, p) -> {
                startFn.accept(t, p);
                return t;
            });
        }

        /**
         * Adds an action to handle {@link CldrValue}s found in the current sub-hierarchy
         * visitation which match the given {@link PathMatcher} leaf-path pattern.
         *
         * <p>This method is expected to be called at least once for each sub-hierarchy processor
         * in order to handle the actual CLDR values being processed, and the path pattern should
         * match leaf-paths in the CLDR data hierarchy, rather than path prefixes.
         *
         * <p>Multiple value actions can be added to a sub-hierarchy processor, and paths are
         * matched in the order the actions are added. It is also possible to mix sub-hierarchy
         * actions and value actions on the same processor, but note that sub-hierarchy processors
         * will take precedence, so you cannot try to match the same value in both a sub-hierarchy
         * processor and a value action.
         *
         * For example:
         * <pre>{@code
         * processor
         *     .addAction("//parent/path", ...)
         *     .addValueAction("value/suffix", ...);
         * // This will never match any values since the sub-hierarchy processor takes precedence!
         * processor.addValueAction("//parent/path/value/suffix", ...);
         * }</pre>
         *
         * @param pattern the CLDR path suffix idenifying the values to be processed.
         * @param doFn the action to be carried out for each value.
         */
        public void addValueAction(String pattern, BiConsumer<T, CldrValue> doFn) {
            valueActions.add(new ValueAction<>(getMatcher(pattern), doFn));
        }

        abstract PathMatcher getMatcher(String pattern);
    }

    /**
     * A root builder of a CLDR data processor.
     *
     * @param <T> the processor state type.
     */
    public static final class Builder<T> extends SubProcessor<T> {
        private Builder() { }

        /** Returns the immutable CLDR data processor. */
        public CldrDataProcessor<T> build() {
            return new CldrDataProcessor<>(
                Lists.transform(prefixActions, PrefixBuilder::build), valueActions);
        }

        @Override
        PathMatcher getMatcher(String pattern) {
            return PathMatcher.of(pattern);
        }
    }

    /**
     * A sub-hierarchy data processor rooted at some specified path prefix.
     *
     * @param <S> the subtype processor state.
     * @param <T> the parent processor state.
     */
    private static class PrefixBuilder<S, T> extends SubProcessor<S> {
        private final PathMatcher matcher;
        private final BiFunction<T, CldrPath, S> newStateFn;
        private final BiConsumer<T, ? super S> doneFn;

        PrefixBuilder(
            PathMatcher matcher,
            BiFunction<T, CldrPath, S> newStateFn,
            BiConsumer<T, ? super S> doneFn) {
            this.matcher = checkNotNull(matcher);
            this.newStateFn = checkNotNull(newStateFn);
            this.doneFn = checkNotNull(doneFn);
        }

        PrefixAction<S, T> build() {
            List<PrefixAction<?, S>> actions = Lists.transform(prefixActions, PrefixBuilder::build);
            return new PrefixAction<>(actions, valueActions, matcher, newStateFn, doneFn);
        }

        @Override PathMatcher getMatcher(String pattern) {
            return matcher.withSuffix(pattern);
        }
    }

    private final ImmutableList<PrefixAction<?, T>> prefixActions;
    private final ImmutableList<ValueAction<T>> valueActions;

    private CldrDataProcessor(
        List<PrefixAction<?, T>> prefixActions,
        List<ValueAction<T>> valueActions) {
        this.prefixActions = ImmutableList.copyOf(prefixActions);
        this.valueActions = ImmutableList.copyOf(valueActions);
    }

    /**
     * Processes a CLDR data instance according to the actions registered for this processor in DTD
     * order. This method is preferred over {@link #process(CldrData, Object, PathOrder)} and
     * eventually the ability to even specify a path order for processing will be removed.
     *
     * <p>This is the main method used to drive the processing of some CLDR data and is typically
     * used like:
     *
     * <pre>{@code
     * MyResult result = CLDR_PROCESSOR.process(data, new MyResult(), DTD);
     * }</pre>
     * <p>or:*
     * <pre>{@code
     * MyResult result = CLDR_PROCESSOR.process(data, MyResult.newBuilder(), DTD).build();
     * }</pre>
     *
     * @param data the CLDR data to be processed.
     * @param state an instance of the "primary" state.
     * @return the given primary state (after modification).
     */
    public T process(CldrData data, T state) {
        return process(data, state, PathOrder.DTD);
    }

    /**
     * Processes a CLDR data instance according to the actions registered for this processor.
     * Callers should prefer using {@link #process(CldrData, Object)} whenever possible and avoid
     * relying on path ordering for processing.
     *
     * @param data the CLDR data to be processed.
     * @param state an instance of the "primary" state.
     * @param pathOrder the order in which CLDR paths should be visited.
     * @return the given primary state (after modification).
     */
    public T process(CldrData data, T state, PathOrder pathOrder) {
        data.accept(pathOrder, new DispatchingVisitor<>(this, state, s -> {}));
        return state;
    }

    private void dispatchPrefixActions(T state, CldrPath prefix, Context context) {
        for (PrefixAction<?, T> a : prefixActions) {
            if (a.matches(state, prefix, context)) {
                break;
            }
        }
    }

    private void dispatchValueActions(T state, CldrValue value) {
        for (ValueAction<T> a : valueActions) {
            if (a.matches(state, value)) {
                break;
            }
        }
    }

    /*
     * Implementation notes:
     *
     * "PrefixAction" is a critical part of the design of the path visitor. It acts as a bridge
     * between the parent visitation (with state type 'T') and child visitation (state type 'S').
     *
     * It is the only class to need to know about both types. Both types are known when the
     * CldrDataProcessor is made, but during visitation the caller of the "matches" method doesn't
     * need to know about the child type, which is why the parent can just have a list of
     * "PrefixAction<?, T>" and don't need any magical recasting.
     *
     * It might only be a few lines of code, but it can only exist in a class which knows about
     * both parent and child types (obtaining a new child state is a function of the parent state).
     */
    static final class PrefixAction<S, T> extends CldrDataProcessor<S> {
        private final PathMatcher matcher;
        private final BiFunction<T, CldrPath, S> newStateFn;
        private final BiConsumer<T, ? super S> doneFn;

        PrefixAction(
            List<PrefixAction<?, S>> prefixActions,
            List<ValueAction<S>> valueActions,
            PathMatcher matcher,
            BiFunction<T, CldrPath, S> newStateFn,
            BiConsumer<T, ? super S> doneFn) {
            super(prefixActions, valueActions);
            this.matcher = checkNotNull(matcher);
            this.newStateFn = checkNotNull(newStateFn);
            this.doneFn = checkNotNull(doneFn);
        }

        public boolean matches(T state, CldrPath prefix, Context context) {
            if (matcher.locallyMatches(prefix)) {
                Consumer<S> doneFn = childState -> this.doneFn.accept(state, childState);
                context.install(
                    new DispatchingVisitor<>(this, newStateFn.apply(state, prefix), doneFn),
                    DispatchingVisitor::done);
                return true;
            }
            return false;
        }
    }

    private static final class ValueAction<T> {
        private final PathMatcher matcher;
        private BiConsumer<T, CldrValue> doFn;

        ValueAction(PathMatcher matcher, BiConsumer<T, CldrValue> doFn) {
            this.matcher = checkNotNull(matcher);
            this.doFn = checkNotNull(doFn);
        }

        boolean matches(T state, CldrValue value) {
            if (matcher.locallyMatches(value.getPath())) {
                doFn.accept(state, value);
                return true;
            }
            return false;
        }
    }

    private static final class DispatchingVisitor<T> implements PrefixVisitor {
        CldrDataProcessor<T> processor;
        private final T state;
        private final Consumer<T> doneFn;

        DispatchingVisitor(CldrDataProcessor<T> processor, T state, Consumer<T> doneFn) {
            this.processor = checkNotNull(processor);
            this.state = checkNotNull(state);
            this.doneFn = checkNotNull(doneFn);
        }

        @Override
        public void visitPrefixStart(CldrPath prefix, Context context) {
            processor.dispatchPrefixActions(state, prefix, context);
        }

        @Override
        public void visitValue(CldrValue value) {
            processor.dispatchValueActions(state, value);
        }

        // Important: This is NOT visitPrefixEnd() since that happens multiple times and isn't
        // going to be called for the prefix at which this visitor was started.
        void done() {
            doneFn.accept(state);
        }
    }
}
