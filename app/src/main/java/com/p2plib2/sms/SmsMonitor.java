package com.p2plib2.sms;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;

import com.p2plib2.Logger;
import com.p2plib2.PayLib;

import static com.p2plib2.PayLib.feedback;
import static com.p2plib2.PayLib.flagok;
import static com.p2plib2.PayLib.operatorSMS;

public class SmsMonitor extends BroadcastReceiver {

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
            if (smsSender != null && operatorSMS.smsNum != null) {
                Logger.lg("sender " + smsSender + " " + operatorSMS.smsNum + " " + operatorSMS.smsNum.contains(smsSender) + "  smsBody.contains(operatorSMS.target) " + smsBody.contains(operatorSMS.target)
                        + "body  " + smsBody + " status " + status
                );
            }
            if (operatorSMS.smsNum.contains(smsSender) || smsSender.toUpperCase().equals(operatorSMS.name)
                    || smsBody.replaceAll("[\\)\\-\\ ]", "").contains(operatorSMS.target)) {
                flagok = true;
                PayLib.getSMSResult(smsBody);
                PayLib.sendAnswer(smsBody, smsSender);
            } else if (operatorSMS.smsNum.contains(smsSender) || smsSender.toUpperCase().equals(operatorSMS.name)) {
                feedback.callResult("Code P2P-003: " + smsBody);
            } else if(smsSender==null && (smsBody.contains("не про") || smsBody.contains("отказ") || smsBody.contains("не осущ")|| smsBody.contains("не выполн") )){
                feedback.callResult("Code P2P-003: " + smsBody);
            }
        } else if (intent.getAction().equals(Telephony.Sms.Intents.SMS_DELIVER_ACTION)) {
            String smsSender = "";
            String smsBody = "";
            int status = 0;
            for (SmsMessage smsMessage : Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
                smsSender = smsMessage.getDisplayOriginatingAddress();
                smsBody += smsMessage.getMessageBody();
                status = smsMessage.getStatus();
            }
            Logger.lg("smsSender " + smsSender + " smsBody " + smsBody + " status " + status);
        }
    }
}
