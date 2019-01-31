package com.p2plib2.sms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;

import com.p2plib2.Logger;
import com.p2plib2.PayLib;

import static com.p2plib2.PayLib.feedback;
import static com.p2plib2.PayLib.flagok;
import static com.p2plib2.PayLib.operatorSMS;
import static com.p2plib2.Simple.PayLib.oper;

public class SmsMonitor extends BroadcastReceiver {

    /**Monitor all receiving sms and filter required for payment*/
    @Override
    public void onReceive(Context context, Intent intent) {
        Logger.lg("New sms! " + intent.getAction() + " not null " + (intent != null));
        if (intent.getAction().equals(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)) {
            String smsSender = "";
            String smsBody = "";
            int status = 0;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                for (SmsMessage smsMessage : Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
                    smsSender = smsMessage.getDisplayOriginatingAddress();
                    smsBody += smsMessage.getMessageBody();
                    Logger.lg("FIRST");
                    status = smsMessage.getStatus();
                }
            } else {
                Bundle smsBundle = intent.getExtras();
                if (smsBundle != null) {
                    Logger.lg(smsBundle.keySet().toString());
                    Object[] pdus = (Object[]) smsBundle.get("pdus");
                    if (pdus == null) {
                        // Display some error to the user
                        Logger.lg("SmsBundle had no pdus key");
                        return;
                    }
                    SmsMessage[] messages = new SmsMessage[pdus.length];
                    for (int i = 0; i < messages.length; i++) {
                        messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                        smsBody += messages[i].getMessageBody();
                    }
                    smsSender = messages[0].getOriginatingAddress();
                    Logger.lg("Second");
                    status = messages[0].getStatus();
                }
            }
            if (smsSender != null && operatorSMS.smsNum != null ) {
                Logger.lg("sender " + smsSender + " " + operatorSMS.smsNum + " "
                        + operatorSMS.smsNum.contains(smsSender) + "  smsBody.contains(operatorSMS.target) " +
                        smsBody.contains(operatorSMS.target)
                        + "body  " + smsBody + " status " + status
                );
            } else if(smsSender != null && oper.smsNum!=null){
                Logger.lg("sender " + smsSender + " " + oper.smsNum + " " + oper.smsNum.contains(smsSender)
                        + "  smsBody.contains(operatorSMS.target) " + smsBody.contains(oper.target)
                        + "body  " + smsBody + " status " + status + " " + oper.smsNum.contains(smsSender)
                );
            }
            Logger.lg(" " + (operatorSMS.name)
            + " " + (oper.name) + " " + oper.smsNum  + " " + oper.operationId);
         if(oper!=null && oper.smsNum!=null && oper.operationId!=null && oper.name!=null){
                Logger.lg(oper.smsNum + " oper.smsNum ");
               if((oper.smsNum.contains(smsSender) || smsSender.toUpperCase().equals(oper.name)
                        || smsBody.replaceAll("[\\)\\-\\ ]", "").contains(oper.target))
                       && !((smsBody.contains("не про") || smsBody.contains("отказ") || smsBody.contains("не осущ")|| smsBody.contains("не выполн")))){
                    Logger.lg("For oper process");
                    com.p2plib2.Simple.PayLib.flagok = true;
                    com.p2plib2.Simple.PayLib.getSMSResult(smsBody);
                    com.p2plib2.Simple.PayLib.sendAnswer(smsBody, smsSender);
                } else if(/*smsSender==null && */(smsBody.contains("не про") || smsBody.contains("отказ") || smsBody.contains("не осущ")|| smsBody.contains("не выполн") )){
                    feedback.callResult("Code P2P-003: " + smsBody);
                    com.p2plib2.Simple.PayLib.codeFeedback("Code P2P-003: " + smsBody, oper.operationId);
                } else if (oper.smsNum.contains(smsSender) || smsSender.toUpperCase().equals(oper.name)
                       || smsBody.replaceAll("[\\)\\-\\ ]", "").contains(oper.target)){
                   flagok = true;
                   com.p2plib2.Simple.PayLib.getSMSResult(smsBody);
                   com.p2plib2.Simple.PayLib.sendAnswer(smsBody, smsSender);
               }
            } else if(operatorSMS!=null && operatorSMS.smsNum != null && com.p2plib2.Simple.PayLib.operationIds.isEmpty()){
             Logger.lg("operSMS");
             if (operatorSMS.smsNum.contains(smsSender) || smsSender.toUpperCase().equals(operatorSMS.name)
                     || smsBody.replaceAll("[\\)\\-\\ ]", "").contains(operatorSMS.target)) {
                 flagok = true;
                 PayLib.getSMSResult(smsBody);
                 PayLib.sendAnswer(smsBody, smsSender);
             } else if (operatorSMS.smsNum.contains(smsSender) || smsSender.toUpperCase().equals(operatorSMS.name)) {
                 feedback.callResult("Code P2P-003: " + smsBody);
             } else if(smsSender==null && (smsBody.contains("не про") || smsBody.contains("отказ") || smsBody.contains("не осущ")|| smsBody.contains("не выполн") )){
                 feedback.callResult("Code P2P-003: " + smsBody);
                 com.p2plib2.Simple.PayLib.codeFeedback("Code P2P-003: " + smsBody, null);
             }
         }
        }
//        else if (intent.getAction().equals(Telephony.Sms.Intents.SMS_DELIVER_ACTION)) {
//            String smsSender = "";
//            String smsBody = "";
//            int status = 0;
//            for (SmsMessage smsMessage : Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
//                smsSender = smsMessage.getDisplayOriginatingAddress();
//                smsBody += smsMessage.getMessageBody();
//                status = smsMessage.getStatus();
//            }
//            Logger.lg("smsSender " + smsSender + " smsBody " + smsBody + " status " + status);
//        }
    }
}
