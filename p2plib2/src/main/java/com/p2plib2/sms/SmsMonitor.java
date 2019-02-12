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
import com.p2plib2.common.MapData;
import com.p2plib2.operators.Operator;

import java.util.HashSet;

import static com.p2plib2.PayLib.feedback;
import static com.p2plib2.PayLib.serviceActivation;

public class SmsMonitor extends BroadcastReceiver {
    String regex = "[\\)\\-\\ ]";

    /**
     * Monitor all receiving sms and filter required for payment
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        Logger.lg("Receive new sms! Intent is not null " + (intent != null));
        if (intent.getAction().equals(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)) {
            String smsSender = "";
            String smsBody = "";
            int status = 0;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                for (SmsMessage smsMessage : Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
                    smsSender = smsMessage.getDisplayOriginatingAddress();
                    smsBody += smsMessage.getMessageBody();
                    status = smsMessage.getStatus();
                }
            } else {
                Bundle smsBundle = intent.getExtras();
                if (smsBundle != null) {
                    Object[] pdus = (Object[]) smsBundle.get("pdus");
                    if (pdus == null) {// Display some error to the user
                        Logger.lg("SmsBundle had no pdus key");
                        return;
                    }
                    SmsMessage[] messages = new SmsMessage[pdus.length];
                    for (int i = 0; i < messages.length; i++) {
                        messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                        smsBody += messages[i].getMessageBody();
                    }
                    smsSender = messages[0].getOriginatingAddress();
                    status = messages[0].getStatus();
                }
            }
            Operator operator = PayLib.getOperatorsAgent(PayLib.currentOperation);
            if (operator != null) {
                Logger.lg(operator.operatorName + " " + PayLib.currentMsg);
                if (smsSender != null && operator.smsNum != null) {
                    Logger.lg("Sender " + smsSender + " operators known sms-portals " + operator.smsNum
                            + ". Is operator portals nums contains this sender " + operator.smsNum.contains(smsSender) + " or name " + smsSender.toUpperCase().equals(operator.operatorName)
                            + "  smsBody contains target " + smsBody.replaceAll("[\\)\\-\\ ]", "").contains(operator.target)
                            + " body  " + smsBody + " status " + status + " target "
                            + operator.target + " " + checkContents(smsBody.toLowerCase(),
                            MapData.paymentDenied.get(MapData.OperatorNames.ALL)) );
                    if ((operator.smsNum.contains(smsSender) || smsSender.toUpperCase().equals(operator.operatorName)
                            || smsBody.replaceAll(regex, "").contains(operator.target))
                            && (!checkContents(smsBody, MapData.paymentDenied.get(MapData.OperatorNames.ALL))
                    || checkContents(smsBody, MapData.codeRequest.get(MapData.OperatorNames.ALL)))) {
                        serviceActivation = true;
                        PayLib.getSMSResult(smsBody);
                        PayLib.sendAnswer(smsBody, smsSender);
                    } else if (checkContents(smsBody, MapData.paymentDenied.get(MapData.OperatorNames.ALL))) {
                        feedback.callResult("Process " + operator.operationId +  "Code P2P-003: " + smsBody);
                    }
                } else {
                    feedback.callResult("Process " + operator.operationId + " Code P2P-003:  + smsNum is null!");
                }
            }
        }
    }

    /**
     * Check contains in
     *
     * @param body    onу ща the templates from
     * @param strings
     */
    public static boolean checkContents(String body, HashSet<String> strings) {
        boolean result = false;
        for (String str : strings) {
            if (body.contains(str)) {
                result = true;
                break;
            }
        }
        return result;
    }
}
