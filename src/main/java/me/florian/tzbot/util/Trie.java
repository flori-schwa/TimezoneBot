package me.florian.tzbot.util;

import java.util.*;
import java.util.stream.Stream;

public class Trie<V> {

    private final Node<V> _root = new Node<>(null);

    public Stream<V> search(String key) {
        Node<V> node = _root;

        for (int i = 0; node != null && i < key.length(); i++) {
            node = node.getChildNode(key.charAt(i));
        }

        if (node == null) {
            return Stream.empty();
        }

        return node.values();
    }

    public boolean insert(String key, V value) {
        Objects.requireNonNull(value);
        Node<V> node = _root;

        for (int i = 0; i < key.length(); i++) {
            node = node.getOrCreateChildNode(key.charAt(i));
        }

        if (Objects.equals(value, node.getValue())) {
            return false;
        }

        node.setValue(value);
        return true;
    }

    private static class Node<V> {
        V _value;

        final SortedMap<Character, Node<V>> _children = new TreeMap<>();

        Node(V value) {
            _value = value;
        }

        boolean hasValue() {
            return _value != null;
        }

        V getValue() {
            return _value;
        }

        void setValue(V value) {
            _value = value;
        }

        Node<V> getChildNode(char c) {
            return _children.get(c);
        }

        Node<V> addChild(char c) {
            if (_children.containsKey(c)) {
                throw new IllegalStateException("Child with key " + c + " already exists");
            }

            Node<V> node = new Node<>(null);
            _children.put(c, node);
            return node;
        }

        Node<V> getOrCreateChildNode(char c) {
            Node<V> child = getChildNode(c);

            if (child == null) {
                child = addChild(c);
            }

            return child;
        }

        Stream<V> values() {
            Stream<V> childValues = _children.values().stream().flatMap(Node::values);

            if (hasValue()) {
                return Stream.concat(
                        Stream.of(_value),
                        childValues
                );
            }

            return childValues;
        }
    }
}