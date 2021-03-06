package com.p2plib2;

import android.app.Activity;
import android.content.Context;

import java.util.ArrayList;
import java.util.HashMap;

public interface PayInterface {
    String getVersion();

    void operation(String operType, Boolean sendWithSaveOutput, Activity act, Context cnt, String operDestination, String phoneNum, String sum);
    void setFilter(HashMap<String, String> filters);

    void updateData(Activity act, Context cnt, CallSmsResult res, Boolean flag);
    HashMap<Integer, String> operatorChooser(Context cnt, String operation, int param);
    void deleteSMS(HashMap<String, String> filters, Context cnt);
}
