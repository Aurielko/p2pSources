package com.p2plib2.common;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.telephony.TelephonyManager;

import com.p2plib2.Logger;

import java.util.ArrayList;

/**This class provides methods for normalizing operator operatorName and checks permissions*
 *
 */
public class CommonFunctions {

    static TelephonyManager telephonyManager;

    /**The function for asking all required permissions.
     * There are CALL_PHONE, READ_SMS, SEND_SMS, READ_PHONE_STATE, RECEIVE_SMS, BIND_ACCESSIBILITY_SERVICE, MODIFY_PHONE_STATE.
     * Display permissions dialog with request code 200
     * TODO: input - list of requirements, output - permission dialog for them*/
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
        if (ActivityCompat.checkSelfPermission(cnt, Manifest.permission.MODIFY_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            tmp.add(Manifest.permission.MODIFY_PHONE_STATE);
        }
        if (!tmp.isEmpty()) {
            ActivityCompat.requestPermissions(act, (tmp).toArray(new String[0]), 200);
        }
    }

    /**@return default telephone operator in normal format*/
    public static String operName(Context cnt) {
        telephonyManager = (TelephonyManager) cnt.getSystemService(Context.TELEPHONY_SERVICE);
        String operName = telephonyManager.getNetworkOperatorName().toUpperCase();
        return formatOperName(operName);
    }

    /** @param oName - String param, which may contains operator operatorName in different styles
     * @return operator operatorName in Uppercase without additional text, such as "Rus 1" and etc.
     * Work with operator names, which contains "MTS", "BEELINE", "MEGAFON", "TELE"
     * TODO: in input rules for normalizing operators and ability to add new operator or move this function in ORM*/
    public static String formatOperName(String oName) {
        String operName = oName.toUpperCase();
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


    /**Compare operator operatorName with enums @see #OperatorNames
     * @return true if operator name_1 is equals to operator name_2*/
    public static Boolean equalsOperators(String name, MapData.OperatorNames constantName) {
        Logger.lg("name " + name + " constantName  " + constantName + " " + (name.equals(constantName.toString())));
        return name.equals(constantName.toString());
    }
}
