package com.example.dkolosovskiy.p2plib;

import android.app.Activity;
import android.content.Context;

public interface PayInterface {
    String getVersion();

    String sendSms(Boolean sendWithSaveOutput, Boolean sendWithSaveInput, Activity act, Context cnt);

    void sendUssd(String operDestination, Activity act);

    void updateData(Activity act, Context cnt, CallSmsResult res);
}
