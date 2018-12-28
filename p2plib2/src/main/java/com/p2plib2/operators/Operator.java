package com.p2plib2.operators;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.telephony.SmsManager;

import com.p2plib2.Logger;
import com.p2plib2.PayLib;
import com.p2plib2.ussd.USSDController;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import static com.p2plib2.PayLib.flagok;
import static com.p2plib2.PayLib.simCounter;

public class Operator {
    /**
     * operatorSMS info
     */
    public String name;
    public String smsNum;
    public String ussdNum;
    public String target;
    public String sum;
    public static Integer simNumSms;
    public static Integer simNumUssd;
    /**
     * Additional settings
     */
    public Boolean sendWithSaveOutput;
    private Boolean sendWithSaveInput;
    private Context cnt;
    static HashMap mapUssd = new HashMap<>();

    public Operator(String operName, Boolean sendWithSaveOutput, Boolean sendWithSaveInput, Context cnt) {
        this.name = operName;
        this.cnt = cnt;
        this.sendWithSaveOutput = sendWithSaveOutput;
        this.sendWithSaveInput = sendWithSaveInput;
    }


    @SuppressLint("MissingPermission")
    public void sendSMS(Boolean sendWithSaveOutput, Context cnt) {
        String msgBody = createMsgBody();
        String number = getOperNum();
        SmsManager smsManager = SmsManager.getDefault();
        this.sendWithSaveOutput = sendWithSaveOutput;
        PendingIntent piSent = PendingIntent.getBroadcast(cnt, 0, new Intent("SMS_SENT"), 0);
        try {
            Logger.lg(name + " num " + number + " " + sendWithSaveInput + " " + msgBody + " " + simCounter);
            PayLib.currentMsg = number + "[]" + msgBody;
            PayLib.curMesage.add(number + "[]" + msgBody);
            if (simCounter == 1) {
                if (!sendWithSaveOutput) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        smsManager.sendTextMessageWithoutPersisting(number, null, msgBody, piSent, null);
                    } else {
                        smsManager.sendTextMessage(number, null, msgBody, piSent, null);
                        PayLib.feedback.callResult("Code P2P-006: пожалуйста, удалите смс с помощью кнопки \"Удалить все СМС, связанные с транзакциями\"");
                    }
                } else {
                    smsManager.sendTextMessage(number, null, msgBody, piSent, null);
                }
            } else {
                if (sendWithSaveOutput) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                        SmsManager.getSmsManagerForSubscriptionId(simNumSms).sendTextMessage(number, null, msgBody, piSent, null);
                    } else {
                        PayLib.feedback.callResult("Code P2P-011: текущая вверсия системы не поддерживает dual sim");
                    }
                } else {
                    Logger.lg("Build.VERSION.SDK_INT  " + Build.VERSION.SDK_INT + " " + Build.VERSION_CODES.P);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        SmsManager.getSmsManagerForSubscriptionId(simNumSms).sendTextMessageWithoutPersisting(number, null, msgBody, piSent, null);
                    } else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                            SmsManager.getSmsManagerForSubscriptionId(simNumSms).sendTextMessage(number, null, msgBody, piSent, null);
                        } else {
                            PayLib.feedback.callResult("Code P2P-011: текущая вверсия системы не поддерживает dual sim");
                        }
                        PayLib.feedback.callResult("Code P2P-006: пожалуйста, удалите смс с помощью кнопки \"Удалить все СМС, связанные с транзакциями\"");
                    }
                }
            }
        } catch (Exception e) {
            Logger.lg("Code P2P-008: " + e.getMessage());
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

    @SuppressLint("MissingPermission")
    public void sendAnswer(String smsBody, String smsSender) {
        String sms_body = smsBody.toLowerCase();
        Logger.lg("SendAnswer " + sms_body);
        if (sms_body.contains("отправьте") || sms_body.contains("ответьте") || sms_body.contains("подтвердите")) {
            SmsManager smsManager = SmsManager.getDefault();
            String answ = "";
            if (sms_body.toLowerCase().contains("подтвердите")) {
                answ = sms_body.substring(sms_body.indexOf("кодом ") + 6, sms_body.indexOf(" в ответном") + 1);
            } else {
                answ = "1";
            }
            if (sms_body.contains("ответьте") || sms_body.contains("на номер")) {
                smsNum = sms_body.substring(sms_body.indexOf("на номер") + 8).replaceAll("[^0-9]", "");
            } else if (sms_body.contains("ответном")) {
                smsNum = smsSender;
            }
            Logger.lg("Answer  " + answ + " smsNum " + smsNum);
            PendingIntent piSent = PendingIntent.getBroadcast(cnt, 0, new Intent("SMS_SENT"), 0);
            try {
                  PayLib.currentMsg = smsNum + "[]" + answ;
                  PayLib.curMesage.add(smsNum + "[]" + answ);
                if (simCounter == 1) {
                    if (!sendWithSaveOutput) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            smsManager.sendTextMessageWithoutPersisting(smsNum, null, answ, piSent, null);
                        } else {
                            smsManager.sendTextMessage(smsNum, null, answ, piSent, null);
                            PayLib.feedback.callResult("Code P2P-006: пожалуйста, удалите смс с помощью кнопки \"Удалить все СМС, связанные с транзакциями\"");
                        }
                    } else {
                        smsManager.sendTextMessage(smsNum, null, answ, piSent, null);
                    }
                } else {
                    if (sendWithSaveOutput) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                            SmsManager.getSmsManagerForSubscriptionId(simNumSms).sendTextMessage(smsNum, null, answ, piSent, null);
                        } else {
                            PayLib.feedback.callResult("Code P2P-011: текущая версия системы не поддерживает dual-sim");
                        }
                    } else {
                        Logger.lg("Build.VERSION.SDK_INT  " + Build.VERSION.SDK_INT + " " + Build.VERSION_CODES.P);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            SmsManager.getSmsManagerForSubscriptionId(simNumSms).sendTextMessageWithoutPersisting(smsNum, null, answ, piSent, null);
                        } else {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                                SmsManager.getSmsManagerForSubscriptionId(simNumSms).sendTextMessage(smsNum, null, answ, piSent, null);
                            } else {
                                PayLib.feedback.callResult("Code P2P-011: текущая версия системы не поддерживает dual-sim");
                            }
                            PayLib.feedback.callResult("Code P2P-006:пожалуйста, удалите смс с помощью кнопки \"Удалить все СМС, связанные с транзакциями\"");
                        }
                    }
                }
            } catch (Exception e) {
                Logger.lg("Code P2P-008: " + e.getMessage());
            }

        } else {
            Logger.lg("Error! " + sms_body);
            PayLib.getSMSResult("Code : " + sms_body);
        }
    }




    public void sendUssd(final String desOper, Activity act) {
        Logger.lg("Flagok " + flagok);
        if (flagok = true) {
            final String destOper = desOper.toUpperCase();
            final USSDController ussdController = USSDController.getInstance(act);
            ussdController.cleanCallbackMessage();
            mapUssd.put("KEY_LOGIN", new HashSet<>(Arrays.asList("подождите", "загрузка")));
            mapUssd.put("KEY_ERROR", new HashSet<>(Arrays.asList("problema", "problem", "ошибка", "null")));
            //*145*9031234567*150#
            String str = "";
            Logger.lg("Send ussd " + name + " dor destination " + destOper + " target " + target);
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
                                        Logger.lg(destOper + " "  );
                                        if (message.contains("1>Оплатить МТС") && destOper == "MTS") {
                                            Logger.lg(message);
                                            ussdController.send("1", new USSDController.CallbackMessage() {
                                                @Override
                                                public void responseMessage(String message) {
                                                }
                                            });
                                        }
                                        if (destOper == "BEELINE") {
                                            ussdController.send("2", new USSDController.CallbackMessage() {
                                                @Override
                                                public void responseMessage(String message) {

                                                }
                                            });
                                        }
                                        if (destOper == "MEGAFON") {
                                            ussdController.send("3", new USSDController.CallbackMessage() {
                                                @Override
                                                public void responseMessage(String message) {

                                                }
                                            });
                                        }
                                        if (destOper == "TELE") {
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
                            if (destOper == "BEELINE") {
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
                            if (destOper == "MEGAFON") {
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
                            if (destOper == "TELE") {
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
                            PayLib.feedback.callResult("Code P2P-004: " + message);
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
                            PayLib.feedback.callResult("Code P2P-004: " + message);
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
                            PayLib.feedback.callResult("Code P2P-004: " + message);
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
                            PayLib.feedback.callResult("Code P2P-004: " + message);
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
        Logger.lg(name + " " + smsNum + " " + target + " " + sum + " " + ussdNum);
    }

    @Override
    public String toString() {
        return name +
                " smsNum " + smsNum +
                " ussdNum " + ussdNum +
                " target " + target +
                " sum " + sum;
    }

    public enum OperatorNames {
        BEELINE, MTS, MEGAFON, TELE
    }

}
