package com.p2plib.common;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.telephony.TelephonyManager;

import java.util.ArrayList;

public class CommonFunctions {

    static TelephonyManager telephonyManager;

    public static void permissionCheck(Context cnt, Activity act) {
        ArrayList<String> tmp = new ArrayList<>();
        if (ActivityCompat.checkSelfPermission(cnt, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            tmp.add(Manifest.permission.CALL_PHONE);
        }
        if (ActivityCompat.checkSelfPermission(cnt, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            tmp.add(Manifest.permission.READ_SMS);
        }
        if (ActivityCompat.checkSelfPermission(cnt, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            tmp.add(Manifest.permission.SEND_SMS);
        }
        if (ActivityCompat.checkSelfPermission(cnt, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            tmp.add(Manifest.permission.READ_PHONE_STATE);
        }
        if (ActivityCompat.checkSelfPermission(cnt, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            tmp.add(Manifest.permission.RECEIVE_SMS);
        }
        if (ActivityCompat.checkSelfPermission(cnt, Manifest.permission.BIND_ACCESSIBILITY_SERVICE) != PackageManager.PERMISSION_GRANTED) {
            tmp.add(Manifest.permission.BIND_ACCESSIBILITY_SERVICE);
        }
        if (ActivityCompat.checkSelfPermission(cnt, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            tmp.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (ActivityCompat.checkSelfPermission(cnt, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            tmp.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        if (!tmp.isEmpty()) {
            ActivityCompat.requestPermissions(act, (tmp).toArray(new String[0]), 200);
        }
    }

    public static String operName(Context cnt) {
        telephonyManager = (TelephonyManager) cnt.getSystemService(Context.TELEPHONY_SERVICE);
        String operName = telephonyManager.getNetworkOperatorName().toUpperCase();
        if(operName.contains("MTS")){
            operName = "MTS";
        }
        if(operName.contains("BEELINE")){
            operName = "BEELINE";
        }
        if(operName.contains("TELE")){
            operName = "TELE";
        }
        if(operName.contains("MEGAFON")){
            operName = "MEGAFON";
        }
        return operName;
    }
}
