package com.ride.android.codegen;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map;

/**
 * This is the environment of codegen process.
 * When a variable (function param or function name) is used, it is queried here.
 * Environment entries contain one of
 * - field declarations of defined functions
 * - local wrappers of function params
 */
public class CodegenEnvironment {
    private LinkedList<Map<String, Generator.EnvironmentEntry>> frames = new LinkedList<>();

    public CodegenEnvironment() {
        push();
    }

    public final void push() {
        frames.add(new HashMap<>());
    }

    public final void add(String name, Generator.EnvironmentEntry entry) {
        frames.getLast().put(name, entry);
    }

    public final void pop() {
        frames.removeLast();
    }

    public Generator.EnvironmentEntry lookup(String symbol) {
        ListIterator<Map<String, Generator.EnvironmentEntry>> iterator = frames.listIterator(frames.size());
        while (iterator.hasPrevious()) {
            Map<String, Generator.EnvironmentEntry> frame = iterator.previous();
            if (frame.get(symbol) != null) {
                return frame.get(symbol);
            }
        }
        return null;
    }
}
