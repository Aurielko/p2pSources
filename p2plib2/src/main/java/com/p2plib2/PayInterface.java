package com.p2plib2;

import android.app.Activity;
import android.content.Context;

import java.util.HashMap;

public interface PayInterface {
    String getVersion();

    void operation(String operType, Boolean sendWithSaveOutput, Activity act, Context cnt, String operDestination, String phoneNum);
    void setFilter(HashMap<String, String> filters);

    void updateData(Activity act, Context cnt, CallSmsResult res);
    String[] operatorChooser(Context cnt, String operation, int param);
}
