package com.p2plib2;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.telecom.Call;
import android.telecom.PhoneAccount;
import android.telephony.SmsManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.p2plib.R;
import com.p2plib2.common.CommonFunctions;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private Button btnSMS;
    private Button btnSMS2;
    private Button btnUssdMTS;
    private Button btnUssdBee;
    private Button btnUssdTele2;
    private Button btnUssdMegafon;
    String number;
    String operDest;
    TextView textView;
    com.p2plib2.PayLib main;
    Activity act;
    Context cnt;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // setContentView(R.layout.simple);
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        Logger.lg("SmsManager.getDefault().getCarrierConfigValues().toString() " + getSystemService(Context.TEXT_SERVICES_MANAGER_SERVICE).toString());
        String operName = telephonyManager.getNetworkOperatorName().toUpperCase();
//        String simSerialNumber = telephonyManager.getSimSerialNumber();
//        String simLineNumber = telephonyManager.getLine1Number();
//        Logger.lg("operName " + operName + " simSerialNumber " + simSerialNumber + " simLineNumber " + simLineNumber
//        + " " + TelephonyManager.CALL_STATE_RINGING);
        //SubscriptionManager mng = (SubscriptionManager) getActiveSubscriptionInfoList
//        final SubscriptionManager subscriptionManager = SubscriptionManager.from(this);
//        final List<SubscriptionInfo> activeSubscriptionInfoList = subscriptionManager.getActiveSubscriptionInfoList();
//        for (SubscriptionInfo subscriptionInfo : activeSubscriptionInfoList) {
//            final CharSequence carrierName = subscriptionInfo.getCarrierName();
//            final CharSequence displayName = subscriptionInfo.getDisplayName();
//            final String subscriptionInfoNumber = subscriptionInfo.getNumber();
//            Logger.lg(subscriptionInfo.toString());
//            Logger.lg(subscriptionInfoNumber + " carrierName " + carrierName + " displayName " + displayName);
//        }
        Logger.lg("count " + telephonyManager.getPhoneCount() + " "   + SmsManager.getDefaultSmsSubscriptionId());
        final Activity act = this;
        final Context cnt = this;
        if (operName.toUpperCase().contains("MTS") || operName.toUpperCase().contains("MEGAFON")) {
            setContentView(R.layout.mts_activity);
        } else {
            setContentView(R.layout.activity_main);
        }
//        for (int i = 0; i < 2; i++) {
//            Logger.lg("For sim " + i);
//            Logger.lg("getSimOperatorName " + getOutput(this, "getActiveSubscriptionInfoList", i));
//            Logger.lg("getSubscriberId " + getOutput(this, "getSubscriberId", i));
//            Logger.lg("getServiceStateForSubscriber " + getOutput(this, "getServiceStateForSubscriber", i));
//            Logger.lg("getDefaultSim " + getOutput(this, "getDefaultSim", i));
//            Logger.lg("getNetworkOperatorName " + getOutput(this, "getNetworkOperatorName", i));
//            Logger.lg("getNetworkOperatorForPhone " + getOutput(this, "getNetworkOperatorForPhone", i));
//            Logger.lg("getSimState " + getOutput(this, "getSimState", i));
//            Logger.lg("getSmsSendCapable " + getOutput(this, "getSmsSendCapable", i));
//            Logger.lg("isIdle " + getOutput(this, "isIdle", i));
//            Logger.lg("getSubscriptionId " + getOutput(this, "getSubscriptionId", i));
//        }
        TextView operNamView = findViewById(R.id.textView2);
        operNamView.setText(operName);
        btnSMS = findViewById(R.id.btnSms);
        btnSMS2 = findViewById(R.id.btnSms2);
        btnUssdMTS = findViewById(R.id.btnUssdMTS);
        btnUssdMegafon = findViewById(R.id.btnUssdMegafon);
        btnUssdBee = findViewById(R.id.btnUssdBEE);
        btnUssdTele2 = findViewById(R.id.btnUssdTELE2);
        btnDiactivate();
        textView = findViewById(R.id.textView);
        Button btnIni = findViewById(R.id.butIni);
        CommonFunctions.permissionCheck(this, this);
        main = new PayLib();
        final CallSmsResult smsResult = new CallSmsResult();
        main.updateData(act, cnt, smsResult);
        btnIni.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                operDest = "MTS";
                btnDiactivate();
                main.updateData(act, cnt, smsResult);
            }
        });

        btnUssdMTS.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                operDest = "MTS";
                main.sendUssd(operDest, act);
//                Logger.lg("main.result " + main.result);
            }
        });
        main.simChooser(cnt, "Ussd");
        if (operName.toUpperCase().contains("MTS") || operName.toUpperCase().contains("MEGAFON")) {
            btnUssdMegafon.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    operDest = "Megafon";
                    main.sendUssd(operDest, act);

                }
            });
            btnUssdBee.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    operDest = "Beeline";
                    main.sendUssd(operDest, act);
                }
            });
            btnUssdTele2.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    operDest = "Tele2";
                    main.sendUssd(operDest, act);
                }
            });
        }
        btnSMS.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Logger.lg("SMS button onclick " + " act!=null " + (act != null) + " " + (cnt != null));
                main.sendSms(true, true, act, cnt);
            }
        });
        btnSMS2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Logger.lg("SMS button onclick " + " act!=null " + (act != null) + " " + (cnt != null));
                flag = true;
                main.checkSmsDefaultApp(true, code);
                //  main.sendSms(false, false, act, cnt);
            }
        });
    }

    Integer code = 777;
    Boolean flag = true;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Logger.lg("requestCode " + requestCode);
        if (requestCode == 777) {
            boolean isDefault = resultCode == Activity.RESULT_OK;
            Logger.lg("isDefault " + isDefault + " " + flag);
            if (isDefault && flag) {
                main.sendSms(false, false, act, cnt);
                main.deleteSMS(new HashMap<String, String>());
                flag = false;
                main.checkSmsDefaultApp(true, code);
            }
        }
    }

    public class CallSmsResult implements com.p2plib2.CallSmsResult {
        @Override
        public void callResult(String s) {
            textView.setText(s);
            if (s.contains("end of update")) {
                btnActivated();
            }
            if (s.contains("Delete success")) {
                main.checkSmsDefaultApp(false, code);
            }
            Logger.lg("Catch message: " + s);
        }
    }

    public void btnDiactivate() {
        btnSMS.setEnabled(false);
        btnSMS2.setEnabled(false);
        btnUssdMTS.setEnabled(false);
        if (btnUssdMegafon != null) btnUssdMegafon.setEnabled(false);
        if (btnUssdBee != null) btnUssdBee.setEnabled(false);
        if (btnUssdTele2 != null) btnUssdTele2.setEnabled(false);
    }

    public void btnActivated() {
        btnSMS.setEnabled(true);
        btnSMS2.setEnabled(true);
        btnUssdMTS.setEnabled(true);
        if (btnUssdMegafon != null) btnUssdMegafon.setEnabled(true);
        if (btnUssdBee != null) btnUssdBee.setEnabled(true);
        if (btnUssdTele2 != null) btnUssdTele2.setEnabled(true);
    }

    /**/
//    private static String getOutput(Context context, String methodName, int slotId) {
//        TelephonyManager telephony = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
//        Class<?> telephonyClass;
//        String reflectionMethod = null;
//        String output = null;
//        try {
//            telephonyClass = Class.forName(telephony.getClass().getName());
//            for (Method method : telephonyClass.getMethods()) {
//                String name = method.getName();
//                if (name.contains(methodName)) {
//                    Class<?>[] params = method.getParameterTypes();
//                    if (params.length == 1 && params[0].getName().equals("int")) {
//                        reflectionMethod = name;
//                    }
//                }
////                else {
////                    Logger.lg("name " + name);
////                }
//            }
//        } catch (ClassNotFoundException e) {
//            e.printStackTrace();
//        }
//        if (reflectionMethod != null) {
//            try {
//                output = getOpByReflection(telephony, reflectionMethod, slotId, false);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//
//        return output;
//    }
//
//    private static String getOpByReflection(TelephonyManager telephony, String predictedMethodName, int slotID, boolean isPrivate) {
//
//        Logger.lg("Method: " + predictedMethodName + " " + slotID);
//        String result = null;
//
//        try {
//
//            Class<?> telephonyClass = Class.forName(telephony.getClass().getName());
//
//            Class<?>[] parameter = new Class[1];
//            parameter[0] = int.class;
//            Method getSimID;
//            if (slotID != -1) {
//                if (isPrivate) {
//                    getSimID = telephonyClass.getDeclaredMethod(predictedMethodName, parameter);
//                } else {
//                    getSimID = telephonyClass.getMethod(predictedMethodName, parameter);
//                }
//            } else {
//                if (isPrivate) {
//                    getSimID = telephonyClass.getDeclaredMethod(predictedMethodName);
//                } else {
//                    getSimID = telephonyClass.getMethod(predictedMethodName);
//                }
//            }
//
//            Object ob_phone;
//            Object[] obParameter = new Object[1];
//            obParameter[0] = slotID;
//            if (getSimID != null) {
//                if (slotID != -1) {
//                    ob_phone = getSimID.invoke(telephony, obParameter);
//                } else {
//                    ob_phone = getSimID.invoke(telephony);
//                }
//
//                if (ob_phone != null) {
//                    result = ob_phone.toString();
//
//                }
//            }
//        } catch (Exception e) {
//            //e.printStackTrace();
//            return null;
//        }
//        //Log.i("Reflection", "Result: " + result);
//        return result;
//    }
}
