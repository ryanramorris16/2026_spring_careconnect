package com.careconnect.sandbox;

public class NullDeref {
    public static int run() {
        // SpotBugs NP_NULL_ON_SOME_PATH: guaranteed null deref
        String s = null;
        return s.length();
    }
}

