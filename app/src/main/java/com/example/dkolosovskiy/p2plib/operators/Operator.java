package com.example.dkolosovskiy.p2plib.operators;


import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;

import com.example.dkolosovskiy.p2plib.Logger;
import com.example.dkolosovskiy.p2plib.PayLib;
import com.example.dkolosovskiy.p2plib.ussd.USSDController;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import static com.example.dkolosovskiy.p2plib.PayLib.flagok;

public class Operator {
    /**
     * operator info
     */
    public String name;
    public String smsNum;
    protected String ussdNum;
    protected String target;
    protected String sum;
    /**
     * Additional settings
     */
    private Boolean sendWithSaveOutput;
    private Boolean sendWithSaveInput;
    private Context cnt;
    static HashMap mapUssd = new HashMap<>();

//    public Operator(String name, String smsNum, String ussdNum, String target, String sum,
//                    Boolean sendWithSaveOutput, Boolean sendWithSaveInput, Context cnt) {
//        this.name = name;
//        this.smsNum = smsNum;
//        this.ussdNum = ussdNum;
//        this.target = target;
//        this.sum = sum
//        this.cnt = cnt;
//    }
    public Operator(String operName, Boolean sendWithSaveOutput, Boolean sendWithSaveInput) {
        this.name = operName;
        this.sendWithSaveOutput = sendWithSaveOutput;
        this.sendWithSaveInput = sendWithSaveInput;
    }


    public void sendSMS(Context cnt) {
        String msgBody = createMsgBody();
        String number = getOperNum();
        SmsManager smsManager = SmsManager.getDefault();
        try {
            if (sendWithSaveOutput) {
                smsManager.sendTextMessage(number, null, msgBody, null, null);
            } else {
                smsManager.sendTextMessageWithoutPersisting(number, null, msgBody, null, null);

            }
        } catch  (Exception e){
            Logger.lg("Something wring! " + e.getMessage() );
        }
    }

    private String getOperNum() {
        String operNum = "";
        switch (name) {
            case "MTS":
                operNum = "+7" + target;
                break;
            case "BEELINE":
                operNum = smsNum;
                break;
        }
        return operNum;
    }

    private String createMsgBody() {
        String msgBody = "";
        switch (name) {
            case "MTS":
                msgBody = "#перевод " + sum;
                break;
            case "BEELINE":
                msgBody = "7" + target + " " + sum;
                break;
        }
        return msgBody;
    }

    public void sendAnswer(String sms_body) {
        if (sms_body.contains("отправьте")) {
            SmsManager smsManager = SmsManager.getDefault();
            PendingIntent sentPI = PendingIntent.getBroadcast(cnt, 0, new Intent(
                    "SMS_SENT"), 0);
            if (sendWithSaveOutput) {
                smsManager.sendTextMessage(smsNum, null, "1", sentPI, null);
            } else {
                smsManager.sendTextMessageWithoutPersisting(smsNum, null, "1", sentPI, null);
            }
            PayLib.feedback.callResult(sentPI.toString());
        } else {
            Logger.lg("Error! " + sms_body);
            PayLib.getSMSResult(sms_body);
        }

    }


    public void sendUssd(final String destOper, Activity act) {
        Logger.lg("Flagok " + flagok);
        if (flagok = true) {
            final USSDController ussdController = USSDController.getInstance(act);
            ussdController.cleanCallbackMessage();
            mapUssd.put("KEY_LOGIN", new HashSet<>(Arrays.asList("подождите", "загрузка")));
            mapUssd.put("KEY_ERROR", new HashSet<>(Arrays.asList("problema", "problem", "ошибка", "null")));
            //*145*9031234567*150#
            final String[] route = {""};
            switch (name) {
                case "MTS":
                    ussdController.callUSSDInvoke(ussdNum, mapUssd, new USSDController.CallbackInvoke() {
                        @Override
                        public void responseInvoke(String message) {
                            //   Logger.lg("Case 1 " + message);
                            if (message.contains("1>Мобильный телефон")) {
                                route[0] = route[0] + " " + message;
                                ussdController.send("1", new USSDController.CallbackMessage() {
                                    @Override
                                    public void responseMessage(String message) {
                                        if (message.contains("1>Оплатить МТС") && destOper == "MTS") {
                                            route[0] = route[0] + " " + message;
                                            ussdController.send("1", new USSDController.CallbackMessage() {
                                                @Override
                                                public void responseMessage(String message) {
                                                }
                                            });
                                        }
                                        if (destOper == "Beeline") {
                                            ussdController.send("2", new USSDController.CallbackMessage() {
                                                @Override
                                                public void responseMessage(String message) {

                                                }
                                            });
                                        }
                                        if (destOper == "Megafon") {
                                            ussdController.send("3", new USSDController.CallbackMessage() {
                                                @Override
                                                public void responseMessage(String message) {

                                                }
                                            });
                                        }
                                        if (destOper == "Tele2") {
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
                                    route[0] = route[0] + " " + message;
                                    ussdController.send("2", new USSDController.CallbackMessage() {
                                        @Override
                                        public void responseMessage(String message) {
                                            if (message.contains("Номер телефона")) {
                                                route[0] = route[0] + " " + message;
                                                ussdController.send(target, new USSDController.CallbackMessage() {
                                                    @Override
                                                    public void responseMessage(String message) {

                                                    }
                                                });
                                            }
                                        }
                                    });
                                }
                                if (message.contains("Сумма платежа")) {
                                    route[0] = route[0] + message;
                                    ussdController.send(sum, new USSDController.CallbackMessage() {
                                        @Override
                                        public void responseMessage(String message) {
                                            if (message.contains("счет МТС")) {
                                                route[0] = route[0] + " " + message;
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
                                    route[0] = route[0] + " " + message;
                                    ussdController.send("1", new USSDController.CallbackMessage() {
                                        @Override
                                        public void responseMessage(String message) {

                                        }
                                    });
                                }
                                if (message.contains("Сумма к оплате")) {
                                    route[0] = route[0] + " " + message;
                                    ussdController.send("1", new USSDController.CallbackMessage() {
                                        @Override
                                        public void responseMessage(String message) {
                                        }
                                    });
                                }

                            }
                            if (destOper == "Beeline") {
                                if (message.contains("Номер телефона")) {
                                    route[0] = route[0] + " " + message;
                                    ussdController.send(target, new USSDController.CallbackMessage() {
                                        @Override
                                        public void responseMessage(String message) {
                                            if (message.contains("Сумма платежа")) {
                                                route[0] = route[0] + " " + message;
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
                                    route[0] = route[0] + " " + message;
                                    ussdController.send("1", new USSDController.CallbackMessage() {
                                        @Override
                                        public void responseMessage(String message) {
                                            if (message.contains("Комиссия") || message.contains("Сумма к оплате")) {
                                                route[0] = route[0] + " " + message;
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
                                    route[0] = route[0] + " " + message;
                                    ussdController.send(target, new USSDController.CallbackMessage() {
                                        @Override
                                        public void responseMessage(String message) {
                                            if (message.contains("Сумма платежа")) {
                                                route[0] = route[0] + " " + message;
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
                                    route[0] = route[0] + " " + message;
                                    ussdController.send("1", new USSDController.CallbackMessage() {
                                        @Override
                                        public void responseMessage(String message) {
                                            if (message.contains("Сумма к")) {
                                                route[0] = route[0] + " " + message;
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
                                    route[0] = route[0] + " " + message;
                                    ussdController.send("1", new USSDController.CallbackMessage() {
                                        @Override
                                        public void responseMessage(String message) {
                                            if (message.contains("Номер телефона")) {
                                                route[0] = route[0] + " " + message;
                                                ussdController.send(target, new USSDController.CallbackMessage() {
                                                    @Override
                                                    public void responseMessage(String message) {
                                                    }
                                                });
                                            }
                                        }
                                    });
                                }
                                if (message.contains("Сумма") && !message.contains("Комиссия")) {
                                    route[0] = route[0] + " " + message;
                                    ussdController.send(sum, new USSDController.CallbackMessage() {
                                        @Override
                                        public void responseMessage(String message) {
                                            if (message.contains("счет МТС")) {
                                                route[0] = route[0] + " " + message;
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
                                    route[0] = route[0] + " " + message;
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
                            // message has the response string data from USSD
                            // response no have input text, NOT SEND ANY DATA
                        }
                    });
                    Logger.lg("Route " + route[0]);
                    break;
                case "BEELINE":
                    String str = ussdNum + target + "*" + sum + "#";
                    ussdController.callUSSDInvoke(str, mapUssd, new USSDController.CallbackInvoke() {
                        @Override
                        public void responseInvoke(String message) {
                            // first option list - select option 1
                            String answ = message.substring(message.indexOf("введите ") + 8, message.indexOf("."));
                            route[0] = route[0] + " " + message;
                            ussdController.send(answ, new USSDController.CallbackMessage() {
                                @Override
                                public void responseMessage(String message) {
                                }
                            });
                        }

                        @Override
                        public void over(String message) {
                            // message has the response string data from USSD
                            // response no have input text, NOT SEND ANY DATA
                        }
                    });
                    Logger.lg("Route " + route[0]);
                    break;
            }
        }
    }

    public void setData(String sms_number, String target, String sum, String ussd) {
        this.smsNum = sms_number;
        this.target = target;
        this.sum = sum;
        this.ussdNum = ussd;
    }

    @Override
    public String toString() {
        return name +
                " smsNum " + smsNum +
                " ussdNum " + ussdNum +
                " target " + target +
                " sum " + sum;
    }
}
