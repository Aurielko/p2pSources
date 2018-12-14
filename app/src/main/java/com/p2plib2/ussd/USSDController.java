package com.p2plib2.ussd;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityManager;


import com.p2plib2.Logger;
import com.p2plib2.operators.Operator;

import java.util.HashMap;
import java.util.HashSet;

import static com.p2plib2.Constants.pBody;
import static com.p2plib2.PayLib.flagok;
import static com.p2plib2.operators.Operator.simNum;


/**
 * @author Romell Dominguez
 * @version 1.1.c 27/09/2018
 * @since 1.0.a
 */
public class USSDController implements USSDInterface {

    protected static USSDController instance;

    protected Activity context;

    protected HashMap<String, HashSet<String>> map;

    protected CallbackInvoke callbackInvoke;

    protected CallbackMessage callbackMessage;

    protected static final String KEY_LOGIN = "KEY_LOGIN";

    protected static final String KEY_ERROR = "KEY_ERROR";

    private USSDInterface ussdInterface;

    /**
     * The Sinfleton building method
     *
     * @param activity An activity that could call
     * @return An instance of USSDController
     */
    public static USSDController getInstance(Activity activity) {
        if (instance == null)
            instance = new USSDController(activity);
        return instance;
    }

    private USSDController(Activity activity) {
        ussdInterface = this;
        context = activity;
    }

    /**
     * Invoke a dial-up calling a ussd number
     *
     * @param ussdPhoneNumber ussd number
     * @param map             Map of Login and problem messages
     * @param callbackInvoke  a callback object from return answer
     */
    @SuppressLint("MissingPermission")
    public void callUSSDInvoke(String ussdPhoneNumber, HashMap<String, HashSet<String>> map, CallbackInvoke callbackInvoke) {
        Logger.lg("controller " + flagok);
        if (flagok) {
            this.callbackInvoke = callbackInvoke;
            this.map = map;
            Logger.lg("controller2 " + flagok);
            if (map == null || (map != null && (!map.containsKey(KEY_ERROR) || !map.containsKey(KEY_LOGIN)))) {
                callbackInvoke.over("Bad Mapping structure");
                return;
            }
            Logger.lg("controller3 " + flagok);
            if (ussdPhoneNumber.isEmpty()) {
                callbackInvoke.over("Bad ussd number");
                return;
            }
            Logger.lg("controller24 " + flagok);
            // if (verifyAccesibilityAccess(context)) {
            String uri = Uri.encode("#");
            if (uri != null) {
                ussdPhoneNumber = ussdPhoneNumber.replace("#", uri);
            }
            Logger.lg("controller5 " + ussdPhoneNumber);
            Uri uriPhone = Uri.parse("tel:" + ussdPhoneNumber);
            Logger.lg(ussdPhoneNumber + " ussdPhoneNumber ");
            if (uriPhone != null) {
                Intent intent = new Intent(Intent.ACTION_CALL, uriPhone);
                Logger.lg("Operator.simNum" + Operator.simNum);
                if (Operator.simNum != null) {
                    intent.putExtra("com.android.phone.extra.slot", simNum);
                }
                context.startActivity(intent);
            }
        }
    }


    public void cleanCallbackMessage() {
        this.callbackMessage = null;
    }

    @Override
    public void sendData(String text) {
        AccessService.send(text);
    }

    public void send(String text, CallbackMessage callbackMessage) {
        this.callbackMessage = callbackMessage;
        ussdInterface.sendData(text);
    }

    static boolean flag = false;

    public static boolean verifyAccesibilityAccess(Activity act) {
        boolean isEnabled = USSDController.isAccessiblityServicesEnable(act);
        Logger.lg("isEnabled " + isEnabled);
        if (!isEnabled && !flag) {
            openSettingsAccessibility(act);
            flag = true;
        }
        return flag;
    }

    private static void openSettingsAccessibility(final Activity activity) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(activity);
        alertDialogBuilder.setTitle("Not enough rights");
        ApplicationInfo applicationInfo = activity.getApplicationInfo();
        int stringId = applicationInfo.labelRes;
        /*String name = applicationInfo.labelRes == 0 ?
                applicationInfo.nonLocalizedLabel.toString() : activity.getString(stringId);*/
        alertDialogBuilder
                .setMessage(pBody);
        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                activity.startActivityForResult(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS), 1);
            }
        });
        AlertDialog alertDialog = alertDialogBuilder.create();
        if (alertDialog != null) {
            alertDialog.show();
        }
    }


    protected static boolean isAccessiblityServicesEnable(Context context) {
        AccessibilityManager am = (AccessibilityManager) context
                .getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (am != null) {
            for (AccessibilityServiceInfo service : am.getInstalledAccessibilityServiceList()) {
                if (service.getId().contains(context.getPackageName())) {
                    return USSDController.isAccessibilitySettingsOn(context, service.getId());
                }
            }
        }
        return false;
    }

    protected static boolean isAccessibilitySettingsOn(Context context, final String service) {
        int accessibilityEnabled = 0;
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                    context.getApplicationContext().getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_ENABLED);
        } catch (Settings.SettingNotFoundException e) {
            //
        }
        if (accessibilityEnabled == 1) {
            String settingValue = Settings.Secure.getString(
                    context.getApplicationContext().getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            Logger.lg("settingValue  " + settingValue);
            if (settingValue != null) {
                TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(':');
                splitter.setString(settingValue);
                while (splitter.hasNext()) {
                    String accessabilityService = splitter.next();
                    if (accessabilityService.equalsIgnoreCase(service)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public interface CallbackInvoke {
        void responseInvoke(String message);

        void over(String message);
    }

    public interface CallbackMessage {
        void responseMessage(String message);
    }
}