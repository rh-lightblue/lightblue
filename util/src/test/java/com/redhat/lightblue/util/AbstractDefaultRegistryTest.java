/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.redhat.lightblue.util;

import java.util.HashMap;
import java.util.Map;
import junit.framework.Assert;
import org.junit.Test;

/**
 *
 * @author nmalik
 */
public abstract class AbstractDefaultRegistryTest<K, V> {

    protected abstract DefaultRegistry<K, V> createRegistery();

    protected abstract K createKey();

    protected abstract V createValue();

    @Test
    public void addKV_single() {
        DefaultRegistry<K, V> registery = createRegistery();
        K key = createKey();
        V value = createValue();
        registery.add(key, value);

        V found = registery.find(key);

        Assert.assertEquals(value, found);
    }

    @Test
    public void addKV_multiple() {
        Map<K, V> kvMap = new HashMap<>();
        DefaultRegistry<K, V> registery = createRegistery();
        for (int i = 0; i < 1000; i++) {
            K key = createKey();
            V value = createValue();
            if (!kvMap.containsKey(key)) {
                kvMap.put(key, value);
                registery.add(key, value);
            }
        }

        Assert.assertTrue(kvMap.size() > 500);

        for (K key : kvMap.keySet()) {
            V expected = kvMap.get(key);
            V found = registery.find(key);

            Assert.assertEquals(expected, found);
        }
    }

    @Test
    public void addResolver_single() {
        DefaultRegistry<K, V> registery = createRegistery();
        final K key = createKey();
        final V value = createValue();
        Resolver<K, V> resolver = new Resolver<K, V>() {

            @Override
            public V find(K name) {
                if (key.equals(name)) {
                    return value;
                }
                return null;
            }
        };

        registery.add(resolver);

        V found = registery.find(key);

        Assert.assertEquals(value, found);
    }

    @Test
    public void addResolver_multiple() {
        final Map<K, V> kvMap = new HashMap<>();
        DefaultRegistry<K, V> registery = createRegistery();
        for (int i = 0; i < 1000; i++) {
            K key = createKey();
            V value = createValue();
            if (!kvMap.containsKey(key)) {
                kvMap.put(key, value);
            }
        }

        Assert.assertTrue(kvMap.size() > 500);

        Resolver<K, V> resolver = new Resolver<K, V>() {

            @Override
            public V find(K name) {
                return kvMap.get(name);
            }
        };

        registery.add(resolver);

        for (K key : kvMap.keySet()) {
            V expected = kvMap.get(key);
            V found = registery.find(key);

            Assert.assertEquals(expected, found);
        }
    }

    @Test
    public void find() {
        // test when things are added by both K/V and resolver
        Map<K, V> kvMap = new HashMap<>();
        DefaultRegistry<K, V> registery = createRegistery();
        for (int i = 0; i < 1000; i++) {
            K key = createKey();
            V value = createValue();
            if (!kvMap.containsKey(key)) {
                kvMap.put(key, value);
                registery.add(key, value);
            }
        }

        Assert.assertTrue(kvMap.size() > 500);

        final Map<K, V> resolverMap = new HashMap<>();

        for (int i = 0; i < 1000; i++) {
            K key = createKey();
            V value = createValue();
            // don't add if it exists in either kv or resolver maps. this ensures hits on resolver don't fail if duplicated in kv map
            if (!kvMap.containsKey(key) && !resolverMap.containsKey(key)) {
                resolverMap.put(key, value);
            }
        }

        Assert.assertTrue(resolverMap.size() > 500);

        Resolver<K, V> resolver = new Resolver<K, V>() {

            @Override
            public V find(K name) {
                return resolverMap.get(name);
            }
        };

        registery.add(resolver);

        for (K key : kvMap.keySet()) {
            V expected = kvMap.get(key);
            V found = registery.find(key);

            Assert.assertEquals(expected, found);
        }

        for (K key : resolverMap.keySet()) {
            V expected = resolverMap.get(key);
            V found = registery.find(key);

            Assert.assertEquals(expected, found);
        }

    }
}