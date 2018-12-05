package com.example.dkolosovskiy.p2plib.operators;

import android.app.Activity;
import android.content.Context;

import com.example.dkolosovskiy.p2plib.PayLib;

public class Operator {
    /**operator info */
    public String name;
    protected String smsNum;
    protected String ussdNum;
    protected String target;
    protected String sum;
    /**Additional settings*/
    private Boolean sendWithSaveOutput;
    private Boolean sendWithSaveInput;
    private Context cnt;

    public Operator( String name, String smsNum, String ussdNum, String target, String sum,
            Boolean sendWithSaveOutput, Boolean sendWithSaveInput, Context cnt) {
        this.name = name;
        this.smsNum = smsNum;
        this.ussdNum = ussdNum;
        this.target = target;
        this.sum = sum;
        this.sendWithSaveOutput = sendWithSaveOutput;
        this.sendWithSaveInput = sendWithSaveInput;
        this.cnt = cnt;
    }

    public Operator(String operName) {
        this.name = operName;
    }


    public  void sendSMS(){

    }

    public  void sendAnswer(){

    }

    public  void sendUssd(){

    }

    public void setData(String sms_number, String target, String sum, String ussd) {
        this.smsNum = sms_number;
        this.target = target;
        this.sum = sum;
        this.ussdNum = ussd;
    }
}
