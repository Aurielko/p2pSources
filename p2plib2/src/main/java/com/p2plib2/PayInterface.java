package com.p2plib2;

import android.app.Activity;
import android.content.Context;

import java.util.HashMap;

public interface PayInterface {
    String getVersion();

    void sendSms(Boolean sendWithSaveOutput, Boolean sendWithSaveInput, Activity act, Context cnt);

    void sendUssd(String operDestination, Activity act);

    void updateData(Activity act, Context cnt, CallSmsResult res, Boolean sendWithSaveOutput, Boolean sendWithSaveInput);
    void setFilter(HashMap<String, String> filters);
}
