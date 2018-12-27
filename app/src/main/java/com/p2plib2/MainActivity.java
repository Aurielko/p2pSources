package com.p2plib2;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.p2plib.R;
import com.p2plib2.common.CommonFunctions;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private Button btnSMS;
    private Button btnSMS2;
    private Button btnUssd;
    private Button btnUssdNew;
    private Button btnSMSNewSave;
    private Button btnSMSUnSave;
    private Button btnDelete;
    private Button btnShowOper;
    private TextView operNamView;
    private Button btnIni;
    static TextView textView;


    String number = null;
    String operDest;
    com.p2plib2.PayLib main;
    static Context cnt;

    /***/
    EditText newNumField;
    TextView textNum;
    EditText numberNew;
    String regex = "[0-9]+";
    Boolean sendWithSaveOutput = true;
    String curOperation = null;
    final String[] operators = new String[]{"MTS", "Megafon", "Beeline", "Tele2"};

    Boolean flagogek = true;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        String operName = telephonyManager.getNetworkOperatorName().toUpperCase();
        final Activity act = this;
        cnt = this;
        setContentView(R.layout.main_activity);
        /**Show message */
        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                String text = (String) msg.obj;
                Logger.lg("text " + text);
                textView.setText(text);
            }
        };
        /**Inicialization button and etc.*/
        operNamView = findViewById(R.id.textView2);
        operNamView.setText(operName);
        btnSMS = findViewById(R.id.btnSms);
        btnSMS2 = findViewById(R.id.btnSms2);
        btnDelete = findViewById(R.id.btnDelete);
        btnShowOper = findViewById(R.id.btnShowOper);
        textView = findViewById(R.id.textView);
        btnIni = findViewById(R.id.butIni);
        btnUssd = findViewById(R.id.btnUssd);
        btnUssdNew = findViewById(R.id.btnUssdNew);
        btnSMSNewSave = findViewById(R.id.btnSmsSave);
        btnSMSUnSave = findViewById(R.id.btnSmsUnSave);
        btnDiactivate();
        /**INI lib*/
        main = new PayLib();
        final CallSmsResult smsResult = new CallSmsResult();
        main.updateData(act, cnt, smsResult);
        CommonFunctions.permissionCheck(this, this);


        /**Operator destination chooser and Ussd receiver**/
        final AlertDialog.Builder builderOperator = new AlertDialog.Builder(cnt);
        builderOperator.setTitle("Choose operator for destination ussd");

        LayoutInflater li = LayoutInflater.from(cnt);
        View prompts = li.inflate(R.layout.prompt, null);
        final AlertDialog.Builder builderNumber = new AlertDialog.Builder(cnt);
        builderNumber.setTitle("Set new num");
        builderNumber.setView(prompts);
        textNum = prompts.findViewById(R.id.textNum);
        numberNew = prompts.findViewById(R.id.number);
        newNumField = findViewById(R.id.textViewNum);

        Button btnSaveNum = findViewById(R.id.numSave);
        btnSaveNum.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String newNum = newNumField.getText().toString();
                if ((newNum != null || newNum != "") && newNum.length() == 10 && newNum.matches(regex)) {
                    number = newNum;
                } else {
                    Message msg = new Message();
                    msg.obj = "Not correct number!";
                    handler.sendMessage(msg);
                }
            }
        });
        builderOperator.setItems(operators, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                operDest = operators[which];
                Logger.lg("Choose " + operDest + " " + number);
                String num;
                if (flagogek == true) {
                    num = number;
                } else {
                    num = null;
                }
                main.operation("ussd", true, act, cnt, operDest, num, null);
                dialog.dismiss();
                dialog.cancel();
            }
        });
        btnUssd.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                flagogek = false;
                main.updateData(act, cnt, smsResult);
                AlertDialog alertD = builderOperator.create();
                alertD.show();
            }
        });
        /**SMS*/
        btnSMS.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Logger.lg("SMS button onclick " + " act!=null " + (act != null) + " " + (cnt != null));
                number = null;
                main.updateData(act, cnt, smsResult);
                main.operation("sms", true, act, cnt, operDest, number, null);
            }
        });
        btnSMS2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                number = null;
                main.updateData(act, cnt, smsResult);
                main.operation("sms", false, act, cnt, operDest, number, null);
            }
        });
        /**Button for delete*/
        btnDelete.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                flag = true;
                main.checkSmsDefaultApp(true, code);
            }
        });
        /**reinizialization*/
        btnIni.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                btnDiactivate();
                TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
                String operName = telephonyManager.getNetworkOperatorName().toUpperCase();
                newNumField.setText("");
                main.updateData(act, cnt, smsResult);
                number = null;
            }
        });
        /**For available sim cards**/
        btnShowOper.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String result = "";
                String mass[] = main.operatorChooser(MainActivity.cnt, null, 0);
                for (int k = 0; k < mass.length; k++) {
                    result = result + " SimCard № " + k + " operator " + mass[k] + " ";
                }
                operNamView.setText(result);
            }
        });
        /**New number dialog builder*/
      /*   builderNumber.setCancelable(false).setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //Вводим текст и отображаем в строке ввода на основном экране:
                        String newNum = numberNew.getText().toString();
                        textView.setText("New number" + newNum);
                        if ((newNum != null || newNum != "") && newNum.length() == 10 && newNum.matches(regex)
                                && curOperation != null) {
                            number = newNum;
                            Logger.lg("newNum " + newNum);
                            if (curOperation == "sms") {
                                main.updateData(act, cnt, smsResult);
                                main.operation(curOperation, sendWithSaveOutput, act, cnt, operDest, newNum);
                            } else {
                                AlertDialog alertD = builderOperator.create();
                                alertD.show();
                            }
                        } else {
                            textView.setText("Error in NUMBER!");
                        }
                        Logger.lg("kiLL");
                        dialog.cancel();
                        dialog.dismiss();
                    }
                });*/
        /**New Number chooser operation**/
        btnSMSUnSave.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                curOperation = "sms";
                sendWithSaveOutput = false;
                main.updateData(act, cnt, smsResult);
                main.operation("sms", false, act, cnt, operDest, number, null);
            }
        });
        btnSMSNewSave.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                curOperation = "sms";
                main.updateData(act, cnt, smsResult);
                main.operation("sms", false, act, cnt, operDest, number, null);
            }
        });
        btnUssdNew.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                flagogek = true;
                main.updateData(act, cnt, smsResult);
                AlertDialog alertD = builderOperator.create();
                alertD.show();
            }
        });

        /****/


    }

    static Handler handler;
    Integer code = 777;
    Boolean flag = true;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Logger.lg("RequestCode " + requestCode);
        if (requestCode == 777) {
            boolean isDefault = resultCode == Activity.RESULT_OK;
            Logger.lg("IsDefault " + isDefault + " " + flag);
            if (isDefault && flag) {
                main.deleteSMS(new HashMap<String, String>(), cnt);
                flag = false;
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
            if (s.contains("P2P-002")) {
                main.operatorChooser(MainActivity.cnt, "SMS", 1);
            }
            if (s.contains("P2P-013")) {
                btnIni.setEnabled(false);
            }
            Logger.lg("Catch  " + s);
            Message msg = new Message();
            msg.obj = s;
            handler.sendMessage(msg);
        }

    }

    public void btnDiactivate() {
        btnSMS.setEnabled(false);
        btnSMS2.setEnabled(false);
        btnUssd.setEnabled(false);
        btnUssdNew.setEnabled(false);
        btnSMSNewSave.setEnabled(false);
        btnSMSUnSave.setEnabled(false);
        btnDelete.setEnabled(false);
    }

    public void btnActivated() {
        btnSMS.setEnabled(true);
        btnSMS2.setEnabled(true);
        btnUssd.setEnabled(true);
        btnUssdNew.setEnabled(true);
        btnSMSNewSave.setEnabled(true);
        btnSMSUnSave.setEnabled(true);
        btnDelete.setEnabled(true);
    }
}