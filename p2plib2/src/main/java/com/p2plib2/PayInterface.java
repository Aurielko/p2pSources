package com.p2plib2;

import android.app.Activity;
import android.content.Context;

import java.util.HashMap;

public interface PayInterface {
    String getVersion();

    void sendSms(Boolean sendWithSaveOutput, Activity act, Context cnt);

    void sendUssd(String operDestination, Activity act);

    void updateData(Activity act, Context cnt, CallSmsResult res);
    void setFilter(HashMap<String, String> filters);

    void  simChooser(Context cnt, String operation);
}
