package com.p2plib2;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.p2plib.R;
import com.p2plib2.common.CommonFunctions;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity {
    private Button btnSMS;
    private Button btnSMS2;
    private Button btnUssdMTS;
    private Button btnUssdBee;
    private Button btnUssdTele2;
    private Button btnUssdMegafon;
    private Button   btnDelete;
    String operDest;
    private TextView textView;
    com.p2plib2.PayLib main;
    final Activity act = this;
    final Context cnt = this;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity);
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        String operName = telephonyManager.getNetworkOperatorName().toUpperCase();
        Logger.lg("SmsManager Default " + getSystemService(Context.TEXT_SERVICES_MANAGER_SERVICE).toString()
                + " operName " + operName + " sim count " + telephonyManager.getPhoneCount() + "  SmsManager DefaultSmsSubscriptionId " + SmsManager.getDefaultSmsSubscriptionId());
        TextView operNamView = findViewById(R.id.textView2);
        operNamView.setText(operName);
        btnSMS = findViewById(R.id.btnSms);
        btnSMS2 = findViewById(R.id.btnSms2);
        btnUssdMTS = findViewById(R.id.btnUssdMTS);
        btnUssdMegafon = findViewById(R.id.btnUssdMegafon);
        btnUssdBee = findViewById(R.id.btnUssdBEE);
        btnUssdTele2 = findViewById(R.id.btnUssdTELE2);
        btnDelete = findViewById(R.id.btnDelete);
        btnDiactivate();
        textView = findViewById(R.id.textView);

        Button btnIni = findViewById(R.id.butIni);
        CommonFunctions.permissionCheck(this, this);
        main = new PayLib();
        final CallSmsResult callResult = new CallSmsResult();
        main.updateData(act, cnt,  callResult);
        btnIni.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                operDest = "MTS";
                btnDiactivate();
                main.updateData(act, cnt, callResult);
            }
        });

        btnUssdMTS.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                operDest = "MTS";
                main.sendUssd(operDest, act);
            }
        });
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

        btnSMS.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                main.sendSms(true, act, cnt);
            }
        });
        btnSMS2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                flag = true;
                main.sendSms(false, act, cnt);
            }
        });
        btnDelete.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                flag = true;
                main.checkSmsDefaultApp(true, code);
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
                main.sendSms(false, act, cnt);
                main.deleteSMS(new HashMap<String, String>());
                flag = false;
                main.checkSmsDefaultApp(true, code);
            }
        }
    }

    public class CallSmsResult implements com.p2plib2.CallSmsResult {
        @Override
        public void callResult(String s) {
            if (s.contains("P2P-001")) {
                btnActivated();
            }
            if (s.contains("P2P-005")) {
                main.checkSmsDefaultApp(false, code);
            }
            if(s.contains("P2P-002")){
                main.simChooser(cnt, "SMS");
            }
            Logger.lg("Catch message: " + s);
            textView.setText(s);
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
}
