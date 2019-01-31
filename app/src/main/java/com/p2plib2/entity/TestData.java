package com.p2plib2.entity;

import java.util.HashMap;

public class TestData {

  public static HashMap <Integer, Chain> operatorsChains = new HashMap();

    public void testData(){
        Body body0 = new Body(0);
        Term term0 = new Term(0,Term.TermType.text, "*145*30*9689604804#");
        body0.terms.put(0, term0);

        Body body1 = new Body(1);
        Term term1 = new Term(1,Term.TermType.code, "введите ");
        Term term2 = new Term(2,Term.TermType.code, ".");
        body1.terms.put(0, term1);
        body1.terms.put(1, term2);


        Event eventBee1 = new Event(Event.EventType.Send,  body0);
        Event eventBee2 = new Event(Event.EventType.Send,  body1);

        EventPair eventPair = new EventPair(0, eventBee1, eventBee2);

        Chain chain0 = new Chain(0, 2);
        chain0.pairs.put(0, eventPair);
        operatorsChains.put(2, chain0);
    }

}
