package com.ymcmp.function;

@FunctionalInterface
public interface TriPredicate<S, T, U> {

    public boolean test(S s, T t, U u);

    public default TriPredicate<S, T, U> and(TriPredicate<? super S, ? super T, ? super U> other) {
        return (s, t, u) -> test(s, t, u) && other.test(s, t, u);
    }

    public default TriPredicate<S, T, U> negate() {
        return (s, t, u) -> !test(s, t, u);
    }

    public default TriPredicate<S, T, U> or(TriPredicate<? super S, ? super T, ? super U> other) {
        return (s, t, u) -> test(s, t, u) || other.test(s, t, u);
    }
}