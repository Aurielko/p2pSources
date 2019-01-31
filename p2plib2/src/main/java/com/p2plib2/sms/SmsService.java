package  com.p2plib2.sms;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.p2plib2.Logger;
import com.p2plib2.PayLib;

/**For switch sms applications*/

public class SmsService extends Service {

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        processSms(intent);
        return START_STICKY;
    }

    private void processSms(Intent intent) {
        String smsBody = intent.getExtras().getString("sms_body");
        String smsSender = intent.getExtras().getString("smsSender");
        Logger.lg("SmsBody  " + smsBody );
        PayLib.sendAnswer(smsBody, smsSender);
    }
}
