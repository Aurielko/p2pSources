package com.example.dkolosovskiy.p2plib;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Telephony.Sms;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.dkolosovskiy.p2plib.common.CommonFunctions;
import com.example.dkolosovskiy.p2plib.common.FilesLoader;
import com.example.dkolosovskiy.p2plib.operators.Beeline;
import com.example.dkolosovskiy.p2plib.operators.MTS;
import com.example.dkolosovskiy.p2plib.operators.Operator;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;

import static com.example.dkolosovskiy.p2plib.ussd.USSDController.verifyAccesibilityAccess;


public class PayLib extends AppCompatActivity implements PayInterface {
    private static String operName = "";
    private String version = "1.0";
    private String operDest = "";
    public String result;
    String defaultSmsApp;
    static MTS mts;
    static Beeline beeline;
    static Context cnt;
    static Activity act;
    public static Boolean flagok = false;
    SharedPreferences mSettings;
    public static final String PREFERENCES = "mysettings";
    public static final String INFO = "operatorList"; // operators info

    /**
     * info
     */
    public static String answer_SMS;
    private String ussd;
    private String target;
    private String sms_check;
    private String sms;
    static CallSmsResult smsResult;
    private String sum;
    /**
     * Common
     */
    private Operator operator;


    HashMap<String, OperatorInfo> operatorInfo = new HashMap<>();

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
        this.act = act;
        CommonFunctions.permissionCheck(cnt, act);
        verifyAccesibilityAccess(act);
        mSettings = cnt.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        /*Create necessary objects*/
        operName = CommonFunctions.operName(cnt);
        operator = new Operator(CommonFunctions.operName(cnt));
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
            OperatorList operator = g.fromJson(input, OperatorList.class);
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
                String opr = separated[0].substring(separated[0].indexOf("=") + 1);
                operatorInfo.put(opr.toUpperCase(), info);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            initData();
        }
    }

    /***Init data */
    public void initData() {
        Logger.lg("KeySetData " + operatorInfo.keySet().toString()
                +" " + getOperName());
        if (operatorInfo.containsKey(operator.name)) {
            OperatorInfo info = operatorInfo.get(getOperName());
            operator.setData(info.sms_number, info.sms_target, info.sum, info.ussd);
            /*Not needed in future*/
            sms = info.sms_number;
            ussd = info.ussd;
            target = info.sms_target;
            sms_check = info.sms_number;
            sum = info.sum;
            Logger.lg( info.operator + " sms " + sms + " "  + " ussd "+ ussd + " target " + target
                    + " sms_check " + sms_check + " sum " + sum);
        }
    }

    public static void sendAnswer(String smsBody) {
        switch (operName) {
            case "MTS":
                if (mts != null) {
                    mts.sendAnswer(answer_SMS, smsBody);
                } else {
                    mts = new MTS(true, true, cnt);
                }
                break;
            case "BEELINE":
                if (beeline != null) {
                    beeline.sendAnswer(answer_SMS, smsBody);
                } else {
                    beeline = new Beeline(true, true, cnt);
                }
                break;
        }

    }


    class OperatorList {
        ArrayList operators;
    }

    class OperatorInfo {
        String operator;
        String sms_number;
        String ussd;
        String sms_target;
        String sum;

        public OperatorInfo(String s, String s1, String s2, String s3, String s4) {
            this.operator = s;
            this.sms_number = s1;
            this.ussd = s2;
            this.sms_target = s3;
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
                mts = new MTS(target, sum, sendWithSaveOutput, sendWithSaveInput, cnt);
                result = mts.sendSMS();
                if (sendWithSaveInput == false) {
                    deleteSMS();
                }
                break;
            case "BEELINE":
                beeline = new Beeline(sms, target, sum, sendWithSaveOutput, sendWithSaveInput, cnt);
                result = beeline.sendSMS(act);
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
        String input = mSettings.getAll().get("mysettings").toString();
        Logger.lg("inpu " + mSettings.getAll().get("mysettings"));
        Gson g = new Gson();
        OperatorList operator = g.fromJson(input, OperatorList.class);
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
            String opr = separated[0].substring(separated[0].indexOf("=") + 1);
            operatorInfo.put(opr.toUpperCase(), info);
            Logger.lg("opr " + opr.toUpperCase());
        }
        if (operatorInfo.containsKey(getOperName())) {
            OperatorInfo info = operatorInfo.get(getOperName());
            switch (operName) {
                case "MTS":
                    mts = new MTS(true, true, cnt);
                    mts.sendUssd(info.ussd, info.sms_target, info.sum, operDestination, act);
                    break;
                case "BEELINE":
                    beeline = new Beeline(true, true, cnt);
                    beeline.sendUssd(info.ussd, info.sms_target, info.sum, operDestination, act);
                    break;
            }
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
    }

    private void launchAppChooser(String myPackageName) {
        Intent intent = new Intent(Sms.Intents.ACTION_CHANGE_DEFAULT);
        intent.putExtra(Sms.Intents.EXTRA_PACKAGE_NAME, myPackageName);
        cnt.startActivity(intent);
    }

    private Button btnSMS;
    private Button btnSMS2;
    private Button btnUssdMTS;
    private Button btnUssdBee;
    TextView textView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //  verifyAccesibilityAccess(this);
        final Activity act = this;
        final Context cnt = this;
        updateData(act, cnt, smsResult);
        btnSMS = findViewById(R.id.btnSms);
        btnSMS2 = findViewById(R.id.btnSms2);
        btnUssdMTS = findViewById(R.id.btnUssdMTS);
        btnUssdBee = findViewById(R.id.btnUssdBEE);
        textView = findViewById(R.id.textView);
        /***/
        btnUssdMTS.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                operDest = "MTS";
                sendUssd(operDest, act);
                Logger.lg("main.result " + result);
            }
        });


        btnUssdBee.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                operDest = "Beeline";
                sendUssd(operDest, act);
            }
        });

        btnSMS.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Logger.lg("SMS button onclick " + " act!=null " + (act != null) + " " + (cnt != null));
                sendSms(true, true, act, cnt);
            }
        });
        btnSMS2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Logger.lg("SMS button onclick " + " act!=null " + (act != null) + " " + (cnt != null));
                sendSms(false, false, act, cnt);
            }
        });
    }

}
