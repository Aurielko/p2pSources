package com.p2plib2;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Scroller;
import android.widget.TextView;

import com.p2plib.R;

import java.util.HashMap;


public class MainActivity extends AppCompatActivity {
    private Button btnIni;
    private Button showSimSetting;
    private Button btnSmsSave;
    private Button btnSmsUnSave;
    private Button btnSMSNewSave;
    private Button btnSMSNewUnSave;
    private Button btnUssd;
    private Button btnUssdNew;
    private Button btnDelete;
    private TextView operLabel;
    private TextView operList;
    private static TextView textView;

    private EditText mtcNum;
    private EditText beeNum;
    private EditText teleNum;
    private EditText megaNum;
    private EditText mtcSum;
    private EditText beeSum;
    private EditText teleSum;
    private EditText megaSum;
    private Button mtcSave;
    private Button beeSave;
    private Button teleSave;
    private Button megaSave;


    HashMap<String, String> nums = new HashMap<>();
    HashMap<String, String> sums = new HashMap<>();

    String operDest;
    com.p2plib2.PayLib main;
    static Context cnt;

    /***/

    String regex = "[0-9]+";
    Boolean sendWithSaveOutput = true;
    String curOperation = null;
    final String[] operators = new String[]{"MTS", "MEGAFON", "BEELINE", "TELE"};

    Boolean flagogek = true;
    String curOper;
    boolean curSave;
    Boolean operationFlag = false;
    /***/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        btnIni = findViewById(R.id.btnIni);
        showSimSetting = findViewById(R.id.btnSimParam);
        btnSmsSave = findViewById(R.id.btnSmsSave);
        btnSmsUnSave = findViewById(R.id.btnSmsUnSave);
        btnSMSNewSave = findViewById(R.id.btnSmsSaveNew);
        btnSMSNewUnSave = findViewById(R.id.btnSmsUnSaveNew);
        showSimSetting = findViewById(R.id.btnSimParam);
        btnUssd = findViewById(R.id.btnUssd);
        btnUssdNew = findViewById(R.id.btnUssdNew);
        btnDelete = findViewById(R.id.btnDelete);
        operLabel = findViewById(R.id.operLabel);
        operList = findViewById(R.id.operList);
        textView = findViewById(R.id.textView);
        mtcNum = findViewById(R.id.numMts);
        beeNum = findViewById(R.id.numBee);
        teleNum = findViewById(R.id.numTele);
        megaNum = findViewById(R.id.numMega);
        mtcSum = findViewById(R.id.sumMts);
        beeSum = findViewById(R.id.sumBee);
        teleSum = findViewById(R.id.sumTele);
        megaSum = findViewById(R.id.sumMega);
        mtcSave = findViewById(R.id.saveMTC);
        beeSave = findViewById(R.id.saveBee);
        teleSave = findViewById(R.id.saveTele);
        megaSave = findViewById(R.id.saveMega);
        textView.setScroller(new Scroller(this));
        textView.setVerticalScrollBarEnabled(true);
        textView.setMovementMethod(new ScrollingMovementMethod());

        btnDiactivate();
        /**INI lib*/
        main = new com.p2plib2.PayLib();
        final CallSmsResult smsResult = new CallSmsResult();
        main.updateData(act, cnt, smsResult);
        com.p2plib2.common.CommonFunctions.permissionCheck(this, this);
        String result = "";
        /**For available sim cards**/
        /**Operator destination chooser and Ussd receiver**/
        final AlertDialog.Builder builderOperator = new AlertDialog.Builder(cnt);
        builderOperator.setTitle("Choose operator for destination ussd");
        LayoutInflater li = LayoutInflater.from(cnt);
        mtcSave.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String newNum = mtcNum.getText().toString();
                String newSum = mtcSum.getText().toString();
                Message msg = new Message();
                if ((newNum != null || newNum != "") && newNum.length() == 10 && newNum.matches(regex)) {
                    nums.put("MTS", newNum);
                    msg.obj = "Новый номер " + newNum;
                } else {
                    msg.obj = "Не корректный формат номера. Пожалуйста, введите десятизначный номер (например 9876543210)";
                }
                Logger.lg("sum " + newSum);
                if (newSum.matches(regex)) {
                    sums.put("MTS", newSum);
                    msg.obj = msg.obj + " новая сумма " + newSum;
                } else if (newSum == "") {
                    sums.put("MTS", null);
                }
                handler.sendMessage(msg);
            }
        });
        beeSave.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String newNum = beeNum.getText().toString();
                String newSum = beeSum.getText().toString();
                Message msg = new Message();
                if ((newNum != null || newNum != "") && newNum.length() == 10 && newNum.matches(regex)) {
                    nums.put("BEELINE", newNum);
                    msg.obj = "Новый номер " + newNum;
                } else {
                    msg.obj = "Не корректный формат номера. Пожалуйста, введите 10-й номер";
                }
                if (newSum.matches(regex)) {
                    sums.put("BEELINE", newSum);
                    msg.obj = msg.obj + " новая сумма " + newSum;
                } else if (newSum == "") {
                    sums.put("BEELINE", null);
                }
                handler.sendMessage(msg);
            }
        });
        teleSave.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String newNum = teleNum.getText().toString();
                String newSum = teleSum.getText().toString();
                Message msg = new Message();
                if ((newNum != null || newNum != "") && newNum.length() == 10 && newNum.matches(regex)) {
                    nums.put("TELE", newNum);
                    msg.obj = "Новый номер " + newNum;
                } else {
                    msg.obj = "Не корректный формат номера. Пожалуйста, введите 10-й номер";
                }
                if (newSum.matches(regex)) {
                    sums.put("TELE", newSum);
                    msg.obj = msg.obj + " новая сумма " + newSum;
                } else if (newSum == "") {
                    sums.put("TELE", null);
                }
                handler.sendMessage(msg);
            }
        });
        megaSave.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String newNum = megaNum.getText().toString();
                String newSum = megaSum.getText().toString();
                Message msg = new Message();
                if ((newNum != null || newNum != "") && newNum.length() == 10 && newNum.matches(regex)) {
                    nums.put("MEGAFON", newNum);
                    msg.obj = "Новый номер " + newNum;
                } else {
                    msg.obj = "Не корректный формат номера. Пожалуйста, введите 10-й номер";

                }
                if (newSum.matches(regex)) {
                    sums.put("MEGAFON", newSum);
                    msg.obj = msg.obj + " новая сумма " + newSum;
                } else if (newSum == "") {
                    sums.put("MEGAFON", null);
                }
                handler.sendMessage(msg);
            }
        });

        builderOperator.setItems(operators, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                operDest = operators[which];
                if (curOper == "ussd") {
                    Logger.lg("Choose " + operDest + " ");
                    String num = null;
                    String sum = null;
                    if (flagogek == true) {
                        if (nums.containsKey(operDest)) {
                            num = nums.get(operDest);
                        }
                        if (sums.containsKey(operDest)) {
                            sum = sums.get(operDest);
                        }
                    }
                    main.operation("ussd", true, act, cnt, operDest, num, sum);
                    operationFlag = true;
                } else {
                    String n = null;
                    String sum = null;
                    if (nums.containsKey(operDest)) {
                        n = nums.get(operDest);
                    }
                    if (sums.containsKey(operDest)) {
                        sum = sums.get(operDest);
                    }
                    main.operation("sms", curSave, act, cnt, operDest, n, sum);
                    operationFlag = true;
                }
                dialog.dismiss();
                dialog.cancel();
            }
        });
        btnUssd.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                flagogek = false;
                //    main.updateData(act, cnt, smsResult);
                curOper = "ussd";
                AlertDialog alertD = builderOperator.create();
                alertD.show();
            }
        });
        btnUssdNew.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                flagogek = true;
                //  main.updateData(act, cnt, smsResult);
                curOper = "ussd";
                AlertDialog alertD = builderOperator.create();
                alertD.show();
            }
        });
        /**SMS*/
        btnSmsSave.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // main.updateData(act, cnt, smsResult);
                main.operation("sms", true, act, cnt, operDest, null, null);
                operationFlag = true;
            }
        });
        btnSmsUnSave.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //   main.updateData(act, cnt, smsResult);
                main.operation("sms", false, act, cnt, operDest, null, null);
                operationFlag = true;
            }
        });

        btnSMSNewSave.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //  main.updateData(act, cnt, smsResult);
                curOper = "sms";
                curSave = true;
                AlertDialog alertD = builderOperator.create();
                alertD.show();
            }
        });
        btnSMSNewUnSave.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // main.updateData(act, cnt, smsResult);
                curOper = "sms";
                curSave = false;
                AlertDialog alertD = builderOperator.create();
                alertD.show();
            }
        });

        /**Button for delete*/
        btnDelete.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                flag = true;
                if(operationFlag) {
                    main.checkSmsDefaultApp(true, code);
                } else {
                    Message msg = new Message();
                    msg.obj = " Пожалуйста, проведите хотя бы одну операцию оплаты";
                    handler.sendMessage(msg);
                }
            }
        });
        /**reinizialization*/
        btnIni.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                btnDiactivate();
                //  main.updateData(act, cnt, smsResult);
                String mass[] = main.operatorChooser(MainActivity.cnt, null, 0);
                String result = null;
                for (int k = 0; k < mass.length; k++) {
                    result = result + " SimCard № " + k + " operator " + mass[k] + " ";
                }
                operList.setText(result);
            }
        });

        showSimSetting.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivityForResult(new Intent(Settings.ACTION_SETTINGS), 1);
            }
        });
        String mass[] = main.operatorChooser(MainActivity.cnt, null, 0);
        for (int k = 0; k < mass.length; k++) {
            result = result + " SimCard № " + k + " operator " + mass[k] + " ";
        }
        operList.setText(result);
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
                String result = "";
                String mass[] = main.operatorChooser(MainActivity.cnt, null, 0);
                for (int k = 0; k < mass.length; k++) {
                    result = result + " SimCard № " + k + " operator " + mass[k] + " ";
                }
                operList.setText(result);
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
        btnSmsSave.setEnabled(false);
        btnSmsUnSave.setEnabled(false);
        btnSMSNewSave.setEnabled(false);
        btnSMSNewUnSave.setEnabled(false);
        btnUssd.setEnabled(false);
        btnUssdNew.setEnabled(false);
        btnDelete.setEnabled(false);
        mtcSave.setEnabled(false);
        beeSave.setEnabled(false);
        teleSave.setEnabled(false);
        megaSave.setEnabled(false);
    }

    public void btnActivated() {
        btnSmsSave.setEnabled(true);
        btnSmsUnSave.setEnabled(true);
        btnSMSNewSave.setEnabled(true);
        btnSMSNewUnSave.setEnabled(true);
        btnUssd.setEnabled(true);
        btnUssdNew.setEnabled(true);
        btnDelete.setEnabled(true);
        mtcSave.setEnabled(true);
        beeSave.setEnabled(true);
        teleSave.setEnabled(true);
        megaSave.setEnabled(true);
    }

}
