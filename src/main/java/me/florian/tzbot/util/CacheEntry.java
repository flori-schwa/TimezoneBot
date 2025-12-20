package me.florian.tzbot.util;

import java.util.Optional;
import java.util.function.Function;

public interface CacheEntry<T> {

    boolean isPresent();

    T value();

    default <V> Optional<V> map(Function<T, V> mapper) {
        if (!isPresent()) {
            return Optional.empty();
        }

        return Optional.ofNullable(mapper.apply(value()));
    }

}
