package com.r3944realms.bus;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.IntFunction;

public class LockHelper<K, V> {
    public static <K, V> LockHelper<K, V> withHashMap() {
        //根据默认载荷系数将尺寸转换为容量
        return new LockHelper<>(size -> new HashMap<>((size + 2) * 4 / 3));
    }
    public static <K, V> LockHelper<K, V> withIdentityHashMap() {
        return new LockHelper<>(IdentityHashMap::new);
    }
    private final IntFunction<Map<K, V>> mapConstructor;
    /**
     * 仅拿到锁的对象可以修改这个映射表
     */
    private final Map<K, V> backingMap;
    @Nullable
    private volatile Map<K, V> readOnlyView = null;
    private Object lock = new Object();
    private LockHelper(IntFunction<Map<K, V>> mapConstructor) {
        this.mapConstructor = mapConstructor;
        this.backingMap = mapConstructor.apply(32); // 合理的初始尺寸
    }

    Map<K, V> getReadMap() {
        var map = readOnlyView;
        if (map == null) {
            synchronized (lock) {
                var updatedMap = mapConstructor.apply(backingMap.size());
                updatedMap.putAll(backingMap);
                readOnlyView = map = updatedMap;
            }
        }
        return map;
    }

    public V get(K key) {
        return getReadMap().get(key);
    }

    public boolean containsKey(K key) {
        return getReadMap().containsKey(key);
    }

    public V computeIfAbsent(K key, Function<K, V> factory) {
        return computeIfAbsent(key, factory, Function.identity());
    }

    public <I> V computeIfAbsent(K key, Function<K, I> factory, Function<I, V> finalizer) {
        var ret = get(key);
        if (ret != null)
            return ret;
        var intermediate = factory.apply(key);

        synchronized (lock) {
            ret = backingMap.get(key);
            if (ret == null) {
                ret = finalizer.apply(intermediate);

                backingMap.put(key, ret);
                readOnlyView = null;
            }
        }
        return ret;
    }

    public void clearAll() {
        backingMap.clear();
        readOnlyView = null;
        lock = new Object();
    }
}
