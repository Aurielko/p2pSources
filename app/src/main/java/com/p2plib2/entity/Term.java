package com.p2plib2.entity;

public class Term {
    int termId;
    TermType type;
    String content;

    public Term(int i, TermType type, String text) {
        this.termId = i;
        this.content = text;
        this.type = type;

    }

    enum TermType{
        text, number, sum, portalNum, code
    }
}
