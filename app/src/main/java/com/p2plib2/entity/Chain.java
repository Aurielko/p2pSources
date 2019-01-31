package com.p2plib2.entity;

import java.util.TreeMap;

public class Chain {
    int chainId;
    int idOperator;
    public TreeMap<Integer, EventPair> pairs = new TreeMap<>();

    public Chain(int chainId, int idOperator) {
        this.chainId = chainId;
        this.idOperator = idOperator;
    }
}
