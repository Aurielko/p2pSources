package com.p2plib2.entity;

import java.util.Map;
import java.util.TreeMap;

public class Body {
    int id;
    public TreeMap<Integer, Term> terms = new TreeMap();

    public Body(int id) {
        this.id = id;
    }

    public String getText() {
        String result = "";
        for (Map.Entry<Integer, Term> term : terms.entrySet()) {
            if(term.getValue().type.equals(Term.TermType.text)) {
                result = result + term.getValue();
            }
        }
        System.out.println("result text " + result);
        return result;
    }

    public String getText(String message) {
        String result = "";
        for (Map.Entry<Integer, Term> term : terms.entrySet()) {
            if(term.getValue().type.equals(Term.TermType.text)) {
                result = result + term.getValue();
            } else if(term.getValue().type.equals(Term.TermType.code)){

            }
        }
        System.out.println("result " + result);
        return result;
    }
}
