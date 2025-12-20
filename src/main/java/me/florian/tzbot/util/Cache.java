package me.florian.tzbot.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.locks.StampedLock;

public final class Cache<K, V> {

    @SuppressWarnings("rawtypes")
    private static final CacheEntry EMPTY = new CacheEntry() {
        @Override
        public boolean isPresent() {
            return false;
        }

        @Override
        public Object value() {
            throw new NoSuchElementException();
        }
    };

    private final StampedLock _lock = new StampedLock();

    private final Map<K, CacheEntryImpl<V>> _backingMap;

    public Cache(int maxEntries) {
        _backingMap = new LinkedHashMap<>() {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, CacheEntryImpl<V>> eldest) {
                return size() > maxEntries;
            }
        };
    }

    @SuppressWarnings("unchecked")
    private static <T> CacheEntry<T> empty() {
        return (CacheEntry<T>) EMPTY;
    }

    private CacheEntryImpl<V> internalGet(Object key) {
        {
            long optStamp = _lock.tryOptimisticRead();
            CacheEntryImpl<V> value = _backingMap.get(key);

            if (_lock.validate(optStamp)) {
                return value;
            }
        }

        final long readStamp = _lock.readLock();

        try {
            return _backingMap.get(key);
        } finally {
            _lock.unlockRead(readStamp);
        }
    }

    public CacheEntry<V> get(Object key) {
        CacheEntryImpl<V> value = internalGet(key);
        return value == null ? empty() : value;
    }

    public <E extends Throwable> V computeIfAbsent(K key, MappingFunction<? super K, ? extends V, E> mappingFunction) throws E {
        CacheEntryImpl<V> value = internalGet(key);

        if (value != null) {
            return value.value();
        }

        final long writeStamp = _lock.writeLock();

        try {
            value = _backingMap.get(key);

            if (value == null) {
                value = new CacheEntryImpl<>(mappingFunction.apply(key));
                _backingMap.put(key, value);
            }

            return value.value();
        } finally {
            _lock.unlockWrite(writeStamp);
        }
    }

    public CacheEntry<V> put(K key, V value) {
        long writeStamp = _lock.writeLock();
        CacheEntry<V> oldValue;

        try {
            oldValue = _backingMap.put(key, new CacheEntryImpl<>(value));
        } finally {
            _lock.unlockWrite(writeStamp);
        }

        return oldValue == null ? empty() : oldValue;
    }

    @FunctionalInterface
    public interface MappingFunction<K, V, E extends Throwable> {
        V apply(K key) throws E;
    }

    // To prevent storing Optionals in the Map
    private record CacheEntryImpl<T>(T value) implements CacheEntry<T> {
        @Override
        public boolean isPresent() {
            return true;
        }
    }

}
