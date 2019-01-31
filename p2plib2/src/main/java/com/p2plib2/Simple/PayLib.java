package com.p2plib2.Simple;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import com.google.gson.Gson;
import com.p2plib2.CallSmsResult;
import com.p2plib2.Logger;
import com.p2plib2.common.CommonFunctions;
import com.p2plib2.common.FilesLoader;
import com.p2plib2.ussd.USSDController;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static com.p2plib2.ussd.USSDController.verifyAccesibilityAccess;


public class PayLib {
    /**Constants
     * @version - lib`s version
     * @pathOfFile - operators date from web or from local data file
     * @PREFERENCES - for operators data to another activity
     * @operatorSettings - for operators data to another activity
     * @operatorInfo - map for operators data
     * @filters - map for text filters, where key - contents of filter, value - field type
     * @simCounter - quantity for sim-cards
     * @feedback - for feedback to Main application (outside)
     * @flagok - for turn on/off Accessibility service, true  is on
     * @currentOperation
     * @curMesage - set of current active output sms for monitoring
     * @simNum - chosen sim-card for current operation
     * @operatorName - chosen operator for current operation
     * */
    private String version = "1.0.1";
    private String pathOfFile = "web";
    public static final String PREFERENCES = "operSetting";

    private SharedPreferences operatorSettings;
    private HashMap<String, OperatorInfo> operatorInfo = new HashMap<>();
    private HashMap<String, String> filters = new HashMap<>();
    public static int simCounter = 0;
    public static CallSmsResult feedback;
    public static String operName = "";
    public static Context cnt;
    public static Activity act;
    public static Boolean flagok = false;
    public static Operation currentOperation;
    public static HashSet<String> curMesage = new HashSet();
    String result;
    int simNum;
    String operatorName;

    /**Update data or initialization*/
    public void updateData(Activity act, Context cnt, CallSmsResult feedback, Boolean flag, int simNum, String operName) {
        this.feedback = feedback;
        this.cnt = cnt;
        this.act = act;
        this.simNum = simNum;
        this.operatorName = operName;
        curSMSOut = null;
        currentMsg = null;
        operName = CommonFunctions.operName(cnt);
        if (flag == true) {
            //CommonFunctions.permissionCheck(cnt, act);
            verifyAccesibilityAccess(act);
            operatorSettings = cnt.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
            /*Create necessary objects*/
            if (isSimAvailable(cnt)) {
                new DownloadApkTask().execute();
                MyContentObserver contentObserver = new MyContentObserver();
                ContentResolver contentResolver = cnt.getContentResolver();
                contentResolver.registerContentObserver(Uri.parse("content://sms"), true, contentObserver);
            } else {
                feedback.callResult("Code P2P-013: no active sim-card ");
            }
        }
    }

    /**Feedback to outside application
     * if current process id is not null return message with it
     * if current process id is null, but operationIds massive is not empty return message with it
     * else return only message with error code*/
    public static void codeFeedback(String str, Long id) {
        if (id != null) {
            feedback.callResult("Process " + id + " " + str);
        } else {
            if (!operationIds.isEmpty()) {
                feedback.callResult("Process " + operationIds + " " + str);
            } else {
                feedback.callResult(str);
            }
        }
    }

    /**
     * @return availability of sim-card, status == TelephonyManager.SIM_STATE_READY*/
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

 /**For download operators data by web, or, if internet access is denied, from local data file*/
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
            // https://drive.google.com/open?id=1cP7AGOYNJNkjo0hrJxSCgyGi5TpSna-v
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
            result = input;
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
            //setOperatorData(true, true);
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

    /***@return - returns number of sim card for specific operator @operName
     **/
    public int getSimCardNumByName(String operName) {
        int result = -1;
        final SubscriptionManager subscriptionManager;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            subscriptionManager = SubscriptionManager.from(cnt);
            if (ActivityCompat.checkSelfPermission(cnt, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                final List<SubscriptionInfo> activeSubscriptionInfoList = subscriptionManager.getActiveSubscriptionInfoList();
                for (SubscriptionInfo subscriptionInfo : activeSubscriptionInfoList) {
                    String carrierName = CommonFunctions.formatOperMame(subscriptionInfo.getCarrierName().toString());
                    Logger.lg("oper name " + operName + "  carrierName " + carrierName);
                    if (carrierName.contains(operName)) {
                        result = subscriptionInfo.getSimSlotIndex();
                    }
                }
            } else {
                codeFeedback(" Code P2P-015: отсутствует разоешение READ_PHONE_STATE", null);
            }
        } else {
            codeFeedback(" Code P2P-011: текущая вверсия системы не поддерживает dual sim", null);
        }
        return result;
    }

    /***@return - returns number of subscription id for specific operator @operName
     **/
    public int getSubIdByName(String operName) {
        int result = -1;
        final SubscriptionManager subscriptionManager;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            subscriptionManager = SubscriptionManager.from(cnt);
            if (ActivityCompat.checkSelfPermission(cnt, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                final List<SubscriptionInfo> activeSubscriptionInfoList = subscriptionManager.getActiveSubscriptionInfoList();
                for (SubscriptionInfo subscriptionInfo : activeSubscriptionInfoList) {
                    String carrierName = CommonFunctions.formatOperMame(subscriptionInfo.getCarrierName().toString());
                    Logger.lg("oper name " + operName + "  carrierName " + carrierName);
                    if (carrierName.contains(operName)) {
                        result = subscriptionInfo.getSubscriptionId();
                    }
                }
            } else {
                codeFeedback(" Code P2P-015: отсутствует разоешение READ_PHONE_STATE", null);
            }
        } else {
            codeFeedback(" Code P2P-011: текущая вверсия системы не поддерживает dual sim", null);
        }
        return result;
    }


    /***@return - returns   operator @operName for specific subscription id
     **/
    public String getOperatorBySubId(int subscriptiond) {
        String result = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            final SubscriptionManager subscriptionManager = SubscriptionManager.from(cnt);
            if (ActivityCompat.checkSelfPermission(cnt, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                final List<SubscriptionInfo> activeSubscriptionInfoList = subscriptionManager.getActiveSubscriptionInfoList();
                for (SubscriptionInfo subscriptionInfo : activeSubscriptionInfoList) {
                    int subId = subscriptionInfo.getSubscriptionId();
                    Logger.lg("subscriptiond " + subscriptiond + " subId " + subId);
                    if (subId == subscriptiond) {
                        result = CommonFunctions.formatOperMame(subscriptionInfo.getCarrierName().toString());
                    }
                }
            } else {
                codeFeedback(" Code P2P-015: отсутствует разоешение READ_PHONE_STATE", null);
            }
        } else {
            codeFeedback(" Code P2P-011: текущая вверсия системы не поддерживает dual sim", null);
        }
        return result;
    }

    /***@return - returns operator @operName for specific sim number
     **/
    private String getOperatorBySimId(int which) {
        String result = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            final SubscriptionManager subscriptionManager = SubscriptionManager.from(cnt);
            if (ActivityCompat.checkSelfPermission(cnt, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                final List<SubscriptionInfo> activeSubscriptionInfoList = subscriptionManager.getActiveSubscriptionInfoList();
                for (SubscriptionInfo subscriptionInfo : activeSubscriptionInfoList) {
                    int subId = subscriptionInfo.getSimSlotIndex();
                    Logger.lg("getOperatorBySimId " + which + " subId " + subId);
                    if (subId == which) {
                        result = CommonFunctions.formatOperMame(subscriptionInfo.getCarrierName().toString());
                    }
                }
            } else {
                codeFeedback(" Code P2P-015: отсутствует разоешение READ_PHONE_STATE", null);
            }
        } else {
            codeFeedback("Code P2P-011: текущая вверсия системы не поддерживает dual sim", null);
        }
        return result;
    }

    /**@return  HashMap key - sim number; value - operator name
     */

    public HashMap<Integer, String> operatorChooser(Context cnt, Activity act) {
        if (simCounter == 0) {
            TelephonyManager telephonyManager = (TelephonyManager) act.getSystemService(Context.TELEPHONY_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                simCounter = telephonyManager.getPhoneCount();
            }
        }
        final HashMap<Integer, String> mass = new HashMap<Integer, String>();
        final SubscriptionManager subscriptionManager;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            subscriptionManager = SubscriptionManager.from(cnt);
            if (ActivityCompat.checkSelfPermission(cnt, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                final List<SubscriptionInfo> activeSubscriptionInfoList = subscriptionManager.getActiveSubscriptionInfoList();
                Logger.lg("activeSubscriptionInfoList  " + activeSubscriptionInfoList.size());
                for (SubscriptionInfo subscriptionInfo : activeSubscriptionInfoList) {
                    final String carrierName = subscriptionInfo.getCarrierName().toString().toUpperCase();
                    final Integer simId = subscriptionInfo.getSimSlotIndex();
                    Logger.lg(carrierName + " sim card " + simId);
                    mass.put(simId, carrierName);
                }
                if (mass.size() > 0) {
                    final String m[] = new String[mass.size()];
                    int s = 0;
                    Logger.lg(mass.keySet() + " keys ");
                    for (Map.Entry entry : mass.entrySet()) {
                        m[s] = "simcard № " + entry.getKey().toString() + " " + entry.getValue();
                        s++;
                    }
                    Logger.lg(mass.size() + " len " + m[0] + " " + mass.get(0));
                }
            } else {
                codeFeedback(" Code P2P-015: отсутствует разрешение READ_PHONE_STATE", null);
            }
        } else {
            codeFeedback(" Code P2P-011: текущая вверсия системы не поддерживает dual sim", null);
        }
        for (int i = 0; i < mass.size() - 1; i++) {
            Logger.lg("mass mass " + mass.get(i));
        }
        return mass;
    }

    public static Cursor curSMSOut;
    public static String currentMsg = "";

    /**For oberving of output sms*/
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
                Logger.lg("flagok " + flagok);
                Logger.lg("Paylib  message_id  " + message_id + " type " + type + " numeroTelephone " + numeroTelephone + " status " + status + " currentMsg  " + currentMsg
                        + " date " + cur.getString(cur.getColumnIndex("date")) + " body " + body);
                if (currentMsg != null) {
                    if ((status.equals("-1") || status == null) && !currentMsg.equals("")) {
                        if ((currentMsg.substring(0, currentMsg.indexOf("[]")).contains(numeroTelephone)
                                || CommonFunctions.formatOperMame(numeroTelephone).contains(oper.name))
                                && currentMsg.substring(currentMsg.indexOf("[]") + 2).contains(body)) {
                            codeFeedback(" Code P2P-010: ошибка отправки смс " + status + " на номер " + numeroTelephone, null);
                            curSMSOut = cur;
                        }
                    } else if (!status.equals("-1") && status != null && !currentMsg.equals("")) {
                        if ((currentMsg.substring(0, currentMsg.indexOf("[]")).contains(numeroTelephone)
                                || CommonFunctions.formatOperMame(numeroTelephone).contains(oper.name))
                                && currentMsg.substring(currentMsg.indexOf("[]") + 2).contains(body)) {
                            codeFeedback(" Code P2P-012: отправка СМС завершена успешно " + status + " на номер " + numeroTelephone, null);
                            curSMSOut = null;
                        }
                    }
                } else {
                    Logger.lg("Error: currentMsg " + currentMsg);
                    //feedback.callResult("Code P2P-012: отправка СМС завершена успешно " + status + " на номер " + numeroTelephone);
                    curSMSOut = null;
                }
            }
            flagok = false;
        }

        @Override
        public boolean deliverSelfNotifications() {
            return false;
        }
    }

    public static Operator oper;
    /***/
    /**
     * sendWithSaveOutput - for sms
     * operDestination - for ussd
     */

   public static HashSet<Long> operationIds = new HashSet<>();

   /**Initialization process
    * @param operationId -  operator identifier
    * @param operType - operation type - sms or ussd
    * @param operDestination - operator for destination, required for some operators
    * @param phoneNum - target phone number for payment
    * @param sum - sum of payment
    * */
    public void operation(Long operationId, String operType, Activity act, Context cnt, String operDestination, String phoneNum, String sum) {
        curMesage.clear();
        curSMSOut = null;
        currentMsg = null;
        operationIds.add(operationId);
        oper = new Operator(operatorName, true, true, cnt, operationId);
        oper.operationId = operationId;
        oper.simNumSms = simNum;
        oper.simNumUssd = simNum;
        updateOperator(simNum, operatorName);
        switch (operType) {
            case "sms":
                if (ActivityCompat.checkSelfPermission(cnt, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
                    if (phoneNum != null) {
                        oper.target = phoneNum;
                    }
                    if (sum != null) {
                        oper.sum = sum;
                    }
                    sendSms(true, cnt);
                } else {
                    codeFeedback(" Code P2P-015: отсутствует разрешение SEND_SMS", null);
                }
                break;
            case "ussd":
                String permission = "Code P2P-015: отсутствует разрешение ";
                Boolean flagPermission = true;
                if (!USSDController.isAccessiblityServicesEnable(cnt)) {
                    permission = permission + "  Accessibility Services ";
                    flagPermission = false;
                }
                if (ActivityCompat.checkSelfPermission(cnt, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                    permission = permission + " CALL_PHONE";
                    flagPermission = false;
                }
                if (flagPermission) {
                    if (phoneNum != null) {
                        oper.target = phoneNum;
                        Logger.lg("phoneNum " + phoneNum);
                        oper.sendWithSaveOutput = true;
                    }
                    if (sum != null) {
                        oper.sum = sum;
                    }
                    sendUssd(operDestination, act);
                } else {
                    codeFeedback(permission, null);
                }
                break;
        }
    }

    /**
     * Update operator information by sim-card number
     * @param simNum
     * and  operator name
     * @param name*/
    private void updateOperator(int simNum, String name) {
        Logger.lg("Choose sim " + simNum + " operName " + name);
        OperatorInfo info = null;
        if (operatorInfo.containsKey(name)) {
            info = operatorInfo.get(name);
        }
        if (oper != null && info != null) {
            oper.name = name;
            Logger.lg(info + " " + oper);
            Logger.lg("name  " + info.operator + " " + info.smsNum + " " + info.target + " " + info.sum + " " + info.ussdNum);
            oper.setData(info.smsNum, info.target, info.sum, info.ussdNum);
            Operator.simNumSms = getSubIdByName(oper.name);
        }
    }

    /**List of available payment methods */
    public enum Operation {
        SMS, USSD
    }

    /**Format feedback to outside app*/
    public static void getSMSResult(String smsBody) {
        Logger.lg("SmsBody from lib: " + smsBody);
        codeFeedback(" code P2P-003: " + smsBody, null);
    }

    /**Check additional for send sms status in case sending dialog conformation appearance*/
    public static void checkSmsAdditional() {
        if (curSMSOut != null) {
            String message_id = curSMSOut.getString(curSMSOut.getColumnIndex("_id"));
            String type = curSMSOut.getString(curSMSOut.getColumnIndex("type"));
            String numeroTelephone = curSMSOut.getString(curSMSOut.getColumnIndex("address")).trim();
            String status = curSMSOut.getString(curSMSOut.getColumnIndex("status")).trim();
            String body = curSMSOut.getString(curSMSOut.getColumnIndex("body")).trim();
            Logger.lg("flagok " + flagok);
            Logger.lg("message_id  " + message_id + " type " + type + " numeroTelephone " + numeroTelephone + " status " + status + " currentMsg  " + currentMsg
                    + " body " + body);
            if (status.equals("-1")) {
                if (body.toLowerCase().contains("латеж выполнен")) {
                    codeFeedback(" Code P2P-012: отправка СМС завершена успешно " + status + " на номер " + numeroTelephone, null);
                } else {
                    codeFeedback(" Code P2P-010: ошибка отправки смс " + status + " на номер " + numeroTelephone, null);
                }
            } else {
                codeFeedback("Code P2P-012: отправка СМС завершена успешно " + status + " на номер " + numeroTelephone, null);
            }
            flagok = false;
            curSMSOut = null;
        }

    }

    /**
     * Initialize answer processing with
     * @param smsBody - receiving sms from specific operator and operation
     * @param smsSender - sender of this sms
     */
    public static void sendAnswer(String smsBody, String smsSender) {
        if (currentOperation != null) {
            oper.sendAnswer(smsBody, smsSender);

        }
    }


    /***Initialization of sms sending
     * @param sendWithSaveOutput - save output sms
     */
    public void sendSms(Boolean sendWithSaveOutput, Context cnt) {
        flagok = true;
        currentOperation = Operation.SMS;
        oper.sendWithSaveOutput = sendWithSaveOutput;
        oper.sendSMS(sendWithSaveOutput, cnt);
    }

    /***Initialization of ussd calling
     * @param operDestination - save output sms
     */
    public void sendUssd(String operDestination, Activity act) {
        currentOperation = Operation.USSD;
        flagok = true;
        oper.sendUssd(operDestination, act);
    }

    /**set filters for sms observing and monitoring*/
    public void setFilter(HashMap<String, String> filters) {
        this.filters.putAll(filters);
    }


}