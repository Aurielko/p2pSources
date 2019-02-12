package com.p2plib2;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.Telephony;
import android.support.v4.app.ActivityCompat;
import android.telephony.SmsManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import com.p2plib2.common.CommonFunctions;
import com.p2plib2.common.FilesLoader;
import com.p2plib2.common.MapData;
import com.p2plib2.entity.MessageEntity;
import com.p2plib2.operators.Operator;
import com.p2plib2.sms.SmsMonitor;
import com.google.gson.Gson;

import static com.p2plib2.Constants.dataAddress;
import static com.p2plib2.PayLib.Operation.ALL;
import static com.p2plib2.PayLib.Operation.SMS;
import static com.p2plib2.PayLib.Operation.USSD;
import static com.p2plib2.PayLib.dataSource.LOCAL;
import static com.p2plib2.PayLib.dataSource.WEB;
import static com.p2plib2.common.MapData.OperatorNames;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static com.p2plib2.ussd.USSDController.verifyAccesibilityAccess;


public class PayLib {
    /**
     * @param version - lib`s version
     * @param dataSource - operators date from web or from local data file
     * @param PREFERENCES - for operators data to another activity
     * @param operatorSettings - for operators data to another activity
     * @param operatorInfo - map for operators data
     * @param filters - map for text filters, where key - contents of filter, value - field type
     * @param simCounter - quantity for sim-cards
     * @param feedback - for feedback to Main application (outside)
     * @param serviceActivation - for turn on/off Accessibility service, true  is on
     * @param currentOperation
     * @param currentMessages - set of current active output sms for monitoring
     * @param simNum - chosen sim-card for current operation
     * @param operatorName - chosen operator for current operation
     */
    private String version = "1.0.1";
    public static final String PREFERENCES = "operSetting";

    private String defaultSmsApp;
    private SharedPreferences operatorSettings;
    private HashMap<String, OperatorInfo> operatorInfo = new HashMap<>();
    private HashMap<String, String> filters = new HashMap<>();
    public static int simCounter = 0;
    public static CallSmsResult feedback;
    // public static String operName = "";
    public static Context cnt;
    public static Activity act;
    public static Boolean serviceActivation = false;
    public static Operation currentOperation;
    public static HashSet<MessageEntity> currentMessages = new HashSet();
    String result;
    public static Cursor curSMSOut;
    public static String currentMsg = "";
    String dataSourceStr = LOCAL.toString();
    private static HashMap<Operation, Operator> operatorsAgents = new HashMap();

    /**
     * Feddback to application, which creates this feedback entity
     */
    public static void getSMSResult(String smsBody) {
        Logger.lg("SmsBody from lib: " + smsBody);
        feedback.callResult("Code P2P-003: " + smsBody);
    }

    public enum dataSource {
        WEB, LOCAL
    }

    public PayLib(Activity act, Context cnt, CallSmsResult feedback, Boolean flag) {
        updateData(act, cnt, feedback, flag);
    }

    public static Operator getOperatorsAgent(Operation operation) {
        Logger.lg(operation + " " + operatorsAgents.values() + "  " + operatorsAgents.keySet());
        if (operatorsAgents.containsKey(operation)) {
            return operatorsAgents.get(operation);
        } else if (operatorsAgents.containsKey(ALL)) {
            return operatorsAgents.get(ALL);
        } else {
            return null;
        }
    }

    /**
     * Update data or initialization
     *
     * @param flag if true - this is initialization; false - update
     */
    public void updateData(Activity act, Context cnt, CallSmsResult feedback, Boolean flag) {
        this.feedback = feedback;
        this.cnt = cnt;
        this.act = act;
        curSMSOut = null;
        currentMsg = null;
        CommonFunctions.permissionCheck(cnt, act);
        verifyAccesibilityAccess(act);
        MapData.ini();
        if (flag == true) {
            operatorSettings = cnt.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
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

    /**
     * @return availability of sim-card, status == TelephonyManager.SIM_STATE_READY
     */
    public boolean isSimAvailable(Context cnt) {
        boolean isAvailable = false;
        TelephonyManager telMgr = (TelephonyManager) cnt.getSystemService(Context.TELEPHONY_SERVICE);
        int simState = telMgr.getSimState();
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

    /**
     * For observing of output sms
     */
    private class MyContentObserver extends ContentObserver {
        public MyContentObserver() {
            super(null);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            Cursor cur = cnt.getContentResolver().query(Uri.parse("content://sms/"), null, null, null, null);
            if (cur.moveToNext()) {
                String message_id = cur.getString(cur.getColumnIndex("_id"));
                String type = cur.getString(cur.getColumnIndex("type"));
                String address = cur.getString(cur.getColumnIndex("address")).trim();
                String status = cur.getString(cur.getColumnIndex("status")).trim();
                String body = cur.getString(cur.getColumnIndex("body")).trim();
                //0: _id//1: thread_id//2: address//3: person//4: date//5: protocol
                //6: read//7: status//8: type//9: reply_path_present//10: subject
                //11: body//12: service_center//13: locked
                Logger.lg("MessageEntity id " + message_id + " type " + type + " number " + address + " status " + status + " currentMsg  " + currentMsg
                        + " date " + cur.getString(cur.getColumnIndex("date")) + " body " + body);
                if (currentMsg != null) {
                    if ((currentMsg.substring(0, currentMsg.indexOf("[]")).contains(address)
                            /* || CommonFunctions.formatOperName(address).contains(operatorSMS.operatorName)*/)
                            && currentMsg.substring(currentMsg.indexOf("[]") + 2).contains(body)) {
                        feedback.callResult("Process id " + currentMsg.substring(currentMsg.indexOf("operationId") + 11) + " Code P2P-010: ошибка отправки смс " + status + " на номер " + address);
                    }
                    if ((status.equals("-1") || status == null) && !currentMsg.equals("")) {
                        curSMSOut = cur;
                    } else if (!status.equals("-1") && status != null && !currentMsg.equals("")) {
                        curSMSOut = null;
                    }
                } else {
                    Logger.lg("Error: currentMsg " + currentMsg);
                    curSMSOut = null;
                }
            }
            serviceActivation = false;
        }

        @Override
        public boolean deliverSelfNotifications() {
            return false;
        }
    }

    /**
     * For download operators data by web, or, if internet access is denied, from local data file
     */

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
            String input = load.downloadJson(dataAddress);
            dataSourceStr = WEB.toString();
            if (input == null) {
                byte[] buffer = null;
                try {
                    InputStream is = cnt.getAssets().open("p2p_data.json");
                    buffer = new byte[is.available()];
                    is.read(buffer);
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                input = new String(buffer);
                dataSourceStr = LOCAL.toString();
            }
            SharedPreferences.Editor editor = operatorSettings.edit();
            editor.putString(PREFERENCES, input);
            editor.apply();
            Gson g = new Gson();
            result = input;
            OperatorList operator = g.fromJson(input, OperatorList.class);
            //TODO normal parsing
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
            feedback.callResult("Code P2P-001: Данные обновлены с помощью " + dataSourceStr + "\n" + operatorInfo.toString() + "\n");
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
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1
                && ActivityCompat.checkSelfPermission(cnt, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            for (SubscriptionInfo subscriptionInfo : SubscriptionManager.from(cnt).getActiveSubscriptionInfoList()) {
                if (CommonFunctions.formatOperName(subscriptionInfo.getCarrierName().toString()).contains(operName)) {
                    result = subscriptionInfo.getSimSlotIndex();
                }
            }
        } else {
            feedback.callResult("Code P2P-011: текущая версия системы не поддерживает dual sim или не дано  разрешение READ_PHONE_STATE");
        }
        return result;
    }

    /***@return - returns number of subscription id for specific operator @operName
     **/
    public int getSubIdByName(String operName) {
        int result = -1;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1
                && ActivityCompat.checkSelfPermission(cnt, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            for (SubscriptionInfo subscriptionInfo : SubscriptionManager.from(cnt).getActiveSubscriptionInfoList()) {
                if (CommonFunctions.formatOperName(subscriptionInfo.getCarrierName().toString()).contains(operName)) {
                    result = subscriptionInfo.getSubscriptionId();
                }
            }
        } else {
            feedback.callResult("Code P2P-011: текущая вверсия системы не поддерживает dual sim или отсутствует разрешение READ_PHONE_STATE");
        }
        return result;
    }

    /***@return - returns   operator @operName for specific subscription id
     **/
    public String getOperatorBySubId(int subscriptiond) {
        String result = "";
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1
                && ActivityCompat.checkSelfPermission(cnt, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
//            final List<SubscriptionInfo> activeSubscriptionInfoList = SubscriptionManager.from(cnt).getActiveSubscriptionInfoList();
            for (SubscriptionInfo subscriptionInfo : SubscriptionManager.from(cnt).getActiveSubscriptionInfoList()) {
                if (subscriptionInfo.getSubscriptionId() == subscriptiond) {
                    result = CommonFunctions.formatOperName(subscriptionInfo.getCarrierName().toString());
                }
            }
        } else {
            feedback.callResult("Code P2P-011: текущая вверсия системы не поддерживает dual sim или  отсутствует разрешение READ_PHONE_STATE");
        }
        return result;
    }

    /**
     * @return - returns operator @operName for specific sim number
     **/
    private String getOperatorBySimId(int which) {
        String result = "";
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1
                && ActivityCompat.checkSelfPermission(cnt, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            for (SubscriptionInfo subscriptionInfo : SubscriptionManager.from(cnt).getActiveSubscriptionInfoList()) {
                if (subscriptionInfo.getSimSlotIndex() == which) {
                    result = CommonFunctions.formatOperName(subscriptionInfo.getCarrierName().toString());
                    Logger.lg("Sim  " + which + "  " + subscriptionInfo.getSimSlotIndex() + " " + subscriptionInfo.getCarrierName()
                            + " " + result);
                }
            }
        } else {
            feedback.callResult("Code P2P-011: текущая вверсия системы не поддерживает dual sim или отсутствует разрешение READ_PHONE_STATE");
        }
        Logger.lg("Sim  " + which + " Result  " + result);
        return result;
    }

    private void clear() {
        currentMessages.clear();
        curSMSOut = null;
        currentMsg = null;
        currentOperation = null;
    }

    /**
     * Operation for test mode with chosen num and sum
     */
    public void operation(Long operationId, String operType, Activity act, Context cnt, String operDestination, String target, String sum, Integer simNum) {
        clear();
        Logger.lg("operType " + operType + " operdestination " + operDestination + " target " + target + " " + simNum);
        if (checkPermissionForOperation(operType, cnt)) {
            if (simNum != null) {
                if (operType.toUpperCase().equals(Operation.SMS.toString())) {
                    Operator operator = new Operator(getOperatorBySimId(simNum), true, true,
                            cnt, operationId, Operation.SMS);
                    setOperatorData(operator, simNum);
                    Logger.lg("create sms agent " + simNum + " " + operator.operatorName + " " + getOperatorBySimId(simNum));
                    operatorsAgents.put(Operation.SMS, operator);
                    operator.setParameters(target, sum, simNum);
                    currentOperation = SMS;
                    operator.sendSMS(cnt);
                } else if (operType.toUpperCase().equals(USSD.toString())) {
                    Operator operator = new Operator(getOperatorBySimId(simNum), true, true,
                            cnt, operationId, USSD);
                    setOperatorData(operator, simNum);
                    currentOperation = USSD;
                    operatorsAgents.put(USSD, operator);
                    operator.setParameters(target, sum, simNum);
                    serviceActivation = true;
                    operator.sendUssd(operDestination, act);
                }
            } else if (simNum == null) {
                /**for dev mode*/
                operationDefault(operType, act, cnt, operDestination, target, sum);
            }
        } else {
            feedback.callResult("Code P2P-015: отсутствует разрешение SEND_SMS или CALL_PHONE 0");
        }
    }

    private void operationDefault(String operType, Activity act, Context cnt, String operDestination, String target, String sum) {
        if (operType.toUpperCase().equals(Operation.SMS.toString())) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                Logger.lg("carrierName " + SmsManager.getDefaultSmsSubscriptionId() + " " + CommonFunctions.formatOperName(getOperatorBySubId(SmsManager.getDefaultSmsSubscriptionId())));
                Operator operator = new Operator(getOperatorBySubId(SmsManager.getDefaultSmsSubscriptionId()), true, true,
                        cnt, null, Operation.SMS);
                setOperatorData(operator);
                operatorsAgents.put(Operation.SMS, operator);
                currentOperation = SMS;
                operator.sendSMS(cnt);
            } else {
                feedback.callResult("Code P2P-015: данная версия не поддерживает функцию автоматического чтения операторов");
            }
        } else if (operType.toUpperCase().equals(USSD.toString())) {
            TelephonyManager tManager = (TelephonyManager) act.getBaseContext()
                    .getSystemService(Context.TELEPHONY_SERVICE);
            String carrierName = CommonFunctions.formatOperName(tManager.getNetworkOperatorName());
            Logger.lg("carrierName " + carrierName);
            Operator operator = new Operator(carrierName, true, true,
                    cnt, null, USSD);
            setOperatorData(operator);
            currentOperation = USSD;
            operatorsAgents.put(USSD, operator);
            serviceActivation = true;
            operator.sendUssd(operDestination, act);
        }
    }

    /**
     * Operation for test mode with chosen num and sum
     */
    public void operation(String operType, Context cnt, String target, String sum, Boolean saveFlag) {
        clear();
        if (checkPermissionForOperation(operType, cnt)
                && operType.toUpperCase().equals(SMS.toString())
                && getOperatorsAgent(SMS) != null) {
            Operator operator = getOperatorsAgent(SMS);
            operator.setParameters(target, sum, null);
            operator.setFlagSave(saveFlag, saveFlag);
            operatorsAgents.put(SMS, operator);
            currentOperation = SMS;
            operator.setParameters(target, sum, null);
            operator.sendSMS(cnt);
        } else {
            feedback.callResult("Code P2P-015: отсутствует разрешение SEND_SMS или CALL_PHONE 1");
        }
    }

    /**
     * Operation proceeding
     *
     * @param operType        - operation type - sms or ussd
     * @param operDestination - operator for destination, required for some operators
     */
    public void operation(String operType, Activity act, Context cnt, String operDestination, Boolean sendWithSaveOutput, Boolean sendWithSaveInput) {
        clear();
        if (checkPermissionForOperation(operType, cnt)) {
            Logger.lg("operatorsAgents.keySet() " + operatorsAgents.keySet());
            if (operType.toUpperCase().equals(SMS.toString())) {
                Operator operator = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
                    operator = new Operator(getOperatorBySubId(SmsManager.getDefaultSmsSubscriptionId()), true, true,
                            cnt, null, Operation.SMS);

                    operator.setFlagSave(sendWithSaveOutput, sendWithSaveInput);
                    operatorsAgents.put(SMS, operator);
                    setOperatorData(operator);
                    operator.sendSMS(cnt);
                } else {
                    feedback.callResult("Code P2P-015: данная версия Android не поддерживает автоматическую настройку отправки СМС");
                }
            } else if (operType.toUpperCase().equals(USSD.toString())) {
                TelephonyManager tManager = (TelephonyManager) act.getBaseContext()
                        .getSystemService(Context.TELEPHONY_SERVICE);
                String carrierName = CommonFunctions.formatOperName(tManager.getNetworkOperatorName());
                Operator operator = new Operator(carrierName, true, true,
                        cnt, null, USSD);
                operator.setFlagSave(sendWithSaveOutput, sendWithSaveInput);
                operatorsAgents.put(USSD, operator);
                setOperatorData(operator);
                serviceActivation = true;
                operator.sendUssd(operDestination, act);
            }
        } else {
            feedback.callResult("Code P2P-015: отсутствует разрешение SEND_SMS или CALL_PHONE 2");
        }
    }

    /**
     * Reply answer code for
     *
     * @param smsBody   - body of incoming sms
     * @param smsSender - sms sender number or operator operatorName
     */
    public static void sendAnswer(String smsBody, String smsSender) {
        Logger.lg("DEBYF " + smsBody + " " + smsSender + " " + currentOperation + " " + operatorsAgents.keySet());
        if (currentOperation != null) {
            if (currentOperation.equals(USSD)) {
                operatorsAgents.get(USSD).sendAnswer(smsBody, smsSender);
            }
            if (currentOperation.equals(Operation.SMS)) {
                operatorsAgents.get(Operation.SMS).sendAnswer(smsBody, smsSender);
            }
        }
    }

//    /**Send sms for
//     * @param sendWithSaveOutput - save or not outgoing sms, true - save
//     */
//    public void sendSms(Boolean sendWithSaveOutput, Context cnt) {
//        serviceActivation = true;
//        currentOperation = Operation.SMS;
//        operatorSMS.sendSMS(cnt);
//    }
//
//    /**
//     * Send ussd for
//     *
//     * @param operDestination - destination operator
//     */
//    public void sendUssd(String operDestination, Activity act) {
//        currentOperation = Operation.USSD;
//        serviceActivation = true;
//        operatorUssd.sendUssd(operDestination, act);
//    }

    public String getLibVersion() {
        return version;
    }

    /**
     * Set filters for
     *
     * @see PayLib#deleteSMS(HashMap, Context)
     **/
    public void setFilter(HashMap<String, String> filters) {
        this.filters.putAll(filters);
    }

    /**
     * Check defaults sms application. Display dialog for chose sms default app
     */
    public void checkSmsDefaultApp(boolean deleteFlag, Integer code) {
        final String myPackageName = cnt.getPackageName();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            defaultSmsApp = Telephony.Sms.getDefaultSmsPackage(cnt);
        }
        Logger.lg(deleteFlag + " MyPackageName  " + myPackageName + "  defaultSmsApp now "
                + defaultSmsApp + " " + !defaultSmsApp.equals(myPackageName));
        act.startActivityForResult(new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT), code);
    }


    public Boolean checkPermissionForOperation(String operation, Context cnt) {
        Boolean result = false;
        if (operation.equals(Operation.SMS.toString()) && ActivityCompat.checkSelfPermission(cnt, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            result = true;
        } else if (operation.equals(USSD.toString())
                && ActivityCompat.checkSelfPermission(cnt, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(cnt, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            result = true;
        }
        return result;
    }


    /**
     * @param param 0 - operator massive, 1 - dialog
     * @return HashMap key - sim number; value - operator operatorName     *
     */
    public HashMap<Integer, String> operatorChooser(Context cnt, final String operation, int param) {
        if (simCounter == 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            simCounter = ((TelephonyManager) act.getSystemService(Context.TELEPHONY_SERVICE)).getPhoneCount();
        }
        HashMap<Integer, String> mass = new HashMap<Integer, String>();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1
                && ActivityCompat.checkSelfPermission(cnt, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            SubscriptionManager subscriptionManager = SubscriptionManager.from(cnt);
            List<SubscriptionInfo> activeSubscriptionInfoList = subscriptionManager.getActiveSubscriptionInfoList();
            Logger.lg("Active Subscription Info List size: " + activeSubscriptionInfoList.size());
            for (SubscriptionInfo subscriptionInfo : activeSubscriptionInfoList) {
                CharSequence carrierName = subscriptionInfo.getCarrierName();
                Integer simId = subscriptionInfo.getSimSlotIndex();
                Logger.lg(carrierName + " with sim-card  number " + simId);
                if (operatorInfo.containsKey(CommonFunctions.formatOperName(carrierName.toString()))) {
                    mass.put(simId, carrierName.toString());
                } else {
                    Logger.lg("This operator is unknown. It cannot be found id operators data");
                }
            }
            if (mass.size() > 0) {
                String m[] = new String[mass.size()];
                int s = 0;
                Logger.lg(mass.keySet() + " keys ");
                for (Map.Entry entry : mass.entrySet()) {
                    m[s] = "Simcard № " + entry.getKey().toString() + " " + entry.getValue();
                    s++;
                }
                if (param == 1) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(cnt);
                    builder.setTitle(Constants.textForSimChoser + operation);
                    Logger.lg(mass.size() + " len " + m[0] + " " + mass.get(0));
                    builder.setItems(m, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            int simNumber = Integer.parseInt(m[which].replaceAll("[^0-9]", ""));
                            Operator operator = null;
                            if (Operation.SMS.toString().equals(operation) && operatorsAgents.containsKey(Operation.SMS)) {
                                operator = operatorsAgents.get(Operation.SMS);
                            }
                            if (USSD.toString().equals(operation) && operatorsAgents.containsKey(USSD)) {
                                operator = operatorsAgents.get(USSD);
                            }
                            if (operator != null && operatorInfo.containsKey(operator.operatorName)) {
                                OperatorInfo info = operatorInfo.get(operator.operatorName);
                                operator.updateData(info.smsNum, info.ussdNum);
                                operator.setParameters(info.target, info.sum, simNumber);
                                Logger.lg("for operation " + operation + " choose sim-card " + simNumber + " with operator " + operator.operatorName);
                            }
                        }
                    });
                    builder.show();
                }
            }
        } else {
            feedback.callResult("Code P2P-011: текущая вверсия системы не поддерживает dual sim или отсутствует разрешение READ_PHONE_STATE");
        }
        return mass;
    }

    /**
     * @param filters - for filter required smsm from all
     */
    public void deleteSMS(HashMap<String, String> filters, Context cnt) {
        Operator operator = operatorsAgents.containsKey(SMS) ? operatorsAgents.get(SMS) : (operatorsAgents.containsKey(ALL) ? operatorsAgents.get(ALL) : null);
        if (operator != null) {
            Cursor c = cnt.getContentResolver().query(Uri.parse("content://sms"), null, null, null, null);
            int flag = 0;
            int flag2 = 0;
            int flag2Max = currentOperation.equals(Operation.SMS) ? 2 : 1;
            Boolean filtersExists = false;
            if (filters.isEmpty()) {
                for (MessageEntity str : currentMessages) {
                    filters.put(str.toString(), "body");
                }
                filtersExists = true;
            }
            Logger.lg("filters.toString() " + filters.toString());
            if (c != null && c.moveToFirst()) {
                do {
                    long id = c.getLong(0);
                    long threadId = c.getLong(1);
                    String address = c.getString(2);
                    String body = c.getString(c.getColumnIndex("body"));
                    String date = c.getString(3);
                    Logger.lg("MessageEntity  " + body + " id " + id + " date " + date + " " + address);
                    if (address != null) {
                        if ((operator.smsNum.contains(address) || address.toUpperCase().equals(operator.operatorName)) && flag < 2) {
                            int delResultLocal = cnt.getContentResolver().delete(
                                    Uri.parse("content://sms"), "_id=? and thread_id=?", new String[]{String.valueOf(id), String.valueOf(threadId)});
                            if (delResultLocal != 0) {
                                flag++;
                            }
                            Logger.lg("Delete result " + delResultLocal);
                        }
                        if (flag2 < flag2Max) {
                            for (Map.Entry<String, String> filter : filters.entrySet()) {
                                if (filtersExists) {
                                    if (filter.getKey().substring(0, filter.getKey().indexOf("[]")).contains(address) && body.contains(filter.getKey().substring(filter.getKey().indexOf("[]") + 2)))
                                        flag2 = cnt.getContentResolver().delete(
                                                Uri.parse("content://sms"), "_id=? and thread_id=?", new String[]{String.valueOf(id), String.valueOf(threadId)});
                                    if (flag2 != -1) {
                                        flag2++;
                                    }
                                } else {
                                    if (filter.getKey().contains(address) && body.contains(filter.getKey()))
                                        flag2 = cnt.getContentResolver().delete(
                                                Uri.parse("content://sms"), "_id=? and thread_id=?", new String[]{String.valueOf(id), String.valueOf(threadId)});
                                    if (flag2 != -1) {
                                        flag2++;

                                    }
                                }
                            }
                        }
                    }

                } while (c.moveToNext());
            }
            feedback.callResult("Code P2P-005: удалено " + flag + " входящих смс и " + flag2 + " исходящих");
        }
    }

    /**
     * Additional check send sms status
     */
    public static void checkSmsAdditional() {
        if (curSMSOut != null) {
            String message_id = curSMSOut.getString(curSMSOut.getColumnIndex("_id"));
            String type = curSMSOut.getString(curSMSOut.getColumnIndex("type"));
            String number = curSMSOut.getString(curSMSOut.getColumnIndex("address")).trim();
            String status = curSMSOut.getString(curSMSOut.getColumnIndex("status")).trim();
            String body = curSMSOut.getString(curSMSOut.getColumnIndex("body")).trim();
            Logger.lg("MessageEntity Id  " + message_id + " type " + type + " number " + number + " status " + status
                    + " currentMsg  " + currentMsg + " body " + body);
            if (status.equals("-1")) {
                if (SmsMonitor.checkContents(body.toLowerCase(), MapData.paymentGranted.get(OperatorNames.ALL))
                        && !SmsMonitor.checkContents(body.toLowerCase(), MapData.paymentDenied.get(OperatorNames.ALL))) {
                    feedback.callResult("Process id " + currentMsg.substring(currentMsg.indexOf("operationId") + 11)
                            + " Code P2P-012: отправка СМС на номер " + number + " завершена успешно");
                } else {
                    feedback.callResult("Process id " + currentMsg.substring(currentMsg.indexOf("operationId") + 11)
                            + " Code P2P-010: ошибка отправки СМС  на номер " + number + ". Status: " + status);
                }
            } else {
                feedback.callResult("Process id " + currentMsg.substring(currentMsg.indexOf("operationId") + 11)
                        + " Code P2P-012: отправка СМС на номер " + number + " завершена успешно");
            }
            serviceActivation = false;
            curSMSOut = null;
        }
    }

    public void setOperatorData(Operator operator, Integer simNum) {
        if (operatorInfo.containsKey(operator.operatorName)) {
            OperatorInfo info = operatorInfo.get(operator.operatorName);
            operator.updateData(info.smsNum, info.ussdNum);
            Logger.lg("For operation " + operator.getType() + " operator " + operator.operatorName + " " + info.smsNum + " " + info.target + " " + info.sum + " " + info.ussdNum + " ");
        } else {
            feedback.callResult(" not information data for operator " + operator.operatorName + " operation " + operator.getType());
        }
    }

    public void setOperatorData(Operator operator) {
        if (operatorInfo.containsKey(operator.operatorName)) {
            OperatorInfo info = operatorInfo.get(operator.operatorName);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                TelephonyManager telephonyManager = (TelephonyManager) act.getSystemService(Context.TELEPHONY_SERVICE);
                simCounter = telephonyManager.getPhoneCount();
            } else {
                simCounter = 1;
            }
            if (simCounter == 1) {
                operator.setType(ALL);
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                    SmsManager mgr = SmsManager.getDefault();
                    if (mgr.getSubscriptionId() >= 0) {
                        operator.operatorName = getOperatorBySubId(mgr.getSubscriptionId());
                        if (operatorInfo.containsKey(operator.operatorName)) {
                            info = operatorInfo.get(operator.operatorName);
                        }
                    } else {
                        feedback.callResult("Code: P2P-002. Вызовите функцию  operatorChooser с параметром \"SMS\" для выбора сим-карты для отправки смс");
                    }
                } else {
                    feedback.callResult("Code P2P-011: текущая вверсия системы не поддерживает dual sim");
                }
            }
            operator.updateData(info.smsNum, info.ussdNum);
            operator.setParameters(info.target, info.sum, getSimCardNumByName(operator.operatorName));
            Logger.lg("For operation " + operator.getType() + " operator " + operator.operatorName + " " + info.smsNum + " " + info.target + " " + info.sum + " " + info.ussdNum + " ");
        } else {
            feedback.callResult(" not information data for operator " + operator.operatorName + " operation " + operator.getType());
        }
    }

    public enum Operation {
        SMS, USSD, ALL
    }
}