package com.example.dkolosovskiy.p2plib.sms;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.example.dkolosovskiy.p2plib.Logger;
import com.example.dkolosovskiy.p2plib.PayLib;
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
        String operName = PayLib.getOperName();
        switch (operName) {
            case "MTS":
                Logger.lg("answer mts");
                PayLib.sendAnswer(smsBody);
                break;
        }
    }

}
