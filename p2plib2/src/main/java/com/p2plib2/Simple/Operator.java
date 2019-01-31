package com.p2plib2.Simple;


import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.telephony.SmsManager;

import com.p2plib2.Logger;
import com.p2plib2.Simple.PayLib;
import com.p2plib2.ussd.USSDController;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import static com.p2plib2.Simple.PayLib.flagok;
import static com.p2plib2.Simple.PayLib.simCounter;
import static com.p2plib2.Simple.PayLib.currentMsg;
import static com.p2plib2.Simple.PayLib.curMesage;

/**
 * Class for operator work for specifying operation in testMode
 */

public class Operator {
    /**
     * @param id - operator id
     * @param name - operator name
     * @param smsNum - From this number (or numbers) lib receives sms with payment report, payment verification code and etc.
     * @param ussdNum - From this number (or numbers) lib calls ussd-request for payment
     * @param target -  payment will be provided to this number
     * @param sum - sum will be transferred
     * @param simNumSms - sim number for sms processing
     * @param simNumUssd - sim number for ussd processing
     */
    public Long id;
    public String name;
    public String smsNum;
    public String ussdNum;
    public String target;
    public String sum;
    public static Integer simNumSms;
    public static Integer simNumUssd;
    /**
     * Additional settings
     * @param  mapUssd -  flow fow ussd processing (incoming feature)
     * @see Operator#Operator(String operName, Boolean sendWithSaveOutput, Boolean sendWithSaveInput, Context cnt, Long operationId)
     */
    public Boolean sendWithSaveOutput;
    private Boolean sendWithSaveInput;
    private Context cnt;
    static HashMap mapUssd = new HashMap<>();
    public static Long operationId;

    /**Operator constructor
     * @param operationId - id for one separated payment process
     * @param sendWithSaveOutput - true - save output sms; false - unsave/delete output sms
     * @param sendWithSaveInput - true - save input sms; false - unsave/delete input sms
     **/
    public Operator(String operName, Boolean sendWithSaveOutput, Boolean sendWithSaveInput, Context cnt, Long operationId) {
        this.name = operName;
        this.cnt = cnt;
        Logger.lg(operName + " " + operationId);
        this.operationId = operationId;
        this.sendWithSaveOutput = sendWithSaveOutput;
        this.sendWithSaveInput = sendWithSaveInput;
    }

    /**
     * Enum: operators Name constants
     */
    public enum OperatorNames {
        BEELINE, MTS, TELE, MEGAFON
    }

    /**This function provides sending SMS process for requesting sms payment.
     * Using following methods @see #createMsgBody(), #getOperNum() for construction output sms-request for specifying operator
     * */
    public void sendSMS(Boolean sendWithSaveOutput, Context cnt) {
        String msgBody = createMsgBody();
        String number = getOperNum();
        SmsManager smsManager = SmsManager.getDefault();
        this.sendWithSaveOutput = sendWithSaveOutput;
        PendingIntent piSent = PendingIntent.getBroadcast(cnt, 0, new Intent("SMS_SENT"), 0);
        try {
            Logger.lg(name + " num " + number + " " + sendWithSaveInput + " " + msgBody + " " + simCounter);
            /**For monitoring current outside message sms and its status
             * @param currentMsg and curMesage*/
            currentMsg = number + "[]" + msgBody;
            curMesage.add(number + "[]" + msgBody);
            if (ActivityCompat.checkSelfPermission(cnt, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
                if (simCounter == 1) {
                    if (!sendWithSaveOutput) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            smsManager.sendTextMessageWithoutPersisting(number, null, msgBody, piSent, null);
                        } else {
                            smsManager.sendTextMessage(number, null, msgBody, piSent, null);
                            PayLib.codeFeedback(" Code P2P-006: пожалуйста, удалите смс с помощью кнопки \"Удалить все СМС, связанные с транзакциями\"", operationId);
                        }
                    } else {
                        smsManager.sendTextMessage(number, null, msgBody, piSent, null);
                    }
                } else {
                    if (sendWithSaveOutput) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                            Logger.lg(" fgr " + currentMsg + "  "  + curMesage);
                            SmsManager.getSmsManagerForSubscriptionId(simNumSms).sendTextMessage(number, null, msgBody, piSent, null);
                        } else {
                            PayLib.codeFeedback(" Code P2P-011: текущая вверсия системы не поддерживает dual sim", operationId);
                        }
                    } else {
                        Logger.lg("Build.VERSION.SDK_INT  " + Build.VERSION.SDK_INT + " " + Build.VERSION_CODES.P);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            SmsManager.getSmsManagerForSubscriptionId(simNumSms).sendTextMessageWithoutPersisting(number, null, msgBody, piSent, null);
                        } else {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                                SmsManager.getSmsManagerForSubscriptionId(simNumSms).sendTextMessage(number, null, msgBody, piSent, null);
                            } else {
                                PayLib.codeFeedback(" Code P2P-011: текущая вверсия системы не поддерживает dual sim", operationId);
                            }
                            PayLib.codeFeedback(" Code P2P-006: пожалуйста, удалите смс с помощью кнопки \"Удалить все СМС, связанные с транзакциями\"", operationId);
                        }
                    }
                }
            }
            flagok = false;
        } catch (Exception e) {
            Logger.lg("Code P2P-008: " + e.getMessage());
        }
    }

    /**@return - format telephone number for sms sending */
    private String getOperNum() {
        String operNum = "";
        if (equalsOperators(name, OperatorNames.MTS)) {
            operNum = "+7" + target;
        } else if (equalsOperators(name, OperatorNames.BEELINE)) {
            operNum = smsNum;
        } else if (equalsOperators(name, OperatorNames.MEGAFON)) {
            operNum = "+7" + target;
        }
        return operNum;
    }

    /**@return msg for sms payment request for specifying operator */
    private String createMsgBody() {
        String msgBody = "";
        if (equalsOperators(name, OperatorNames.MTS)) {
            msgBody = "#перевод " + sum;
        } else if (equalsOperators(name, OperatorNames.BEELINE)) {
            msgBody = "7" + target + " " + sum;
        } else if (equalsOperators(name, OperatorNames.MEGAFON)) {
            msgBody = sum + " перевод";
        }
        return msgBody;
    }

    /**Function for sending of sms answer for specifying operator
     * Check message body for following cases:
     *  - sms_body.contains("отправь") or sms_body.contains("ответь") or  sms_body.contains("подтверд") - the sms for code answer
     *  - sms_body.contains("кодом ") and sms_body.contains(" в ответном") - format number for answer
     *  Also, this method get code from sms-body for answer or send default answer for this operator
     *  */
    public void sendAnswer(String smsBody, String smsSender) {
        String sms_body = smsBody.toLowerCase();
        Logger.lg("SendAnswer " + sms_body + " smsSender " + smsSender + "  " + flagok);
        flagok = true;
        if (sms_body.contains("отправь") || sms_body.contains("ответь") || sms_body.contains("подтверд")) {
            SmsManager smsManager = SmsManager.getDefault();
            String answ = "";
            // Подтвердите платёж кодом 6 в ответном SMS
            if (sms_body.toLowerCase().contains("подтвер")) {
                if (sms_body.contains("кодом ") && sms_body.contains(" в ответном")) {
                    answ = sms_body.substring(sms_body.indexOf("кодом ") + 6, sms_body.indexOf(" в ответном") + 1);
                } else {
                    answ = "1";
                }
            } else {
                answ = "1";
            }
            if (sms_body.contains("ответ") && sms_body.contains("на номер")) {
                smsNum = sms_body.substring(sms_body.indexOf("на номер") + 8).replaceAll("[^0-9]", "");
            } else if (sms_body.contains("ответном") || sms_body.contains("ответное") || sms_body.contains("в ответ")) {
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
                            if (ActivityCompat.checkSelfPermission(cnt, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
                                smsManager.sendTextMessageWithoutPersisting(smsNum, null, answ, piSent, null);
                            }
                        } else {
                            smsManager.sendTextMessage(smsNum, null, answ, piSent, null);
                            PayLib.codeFeedback(" Code P2P-006: пожалуйста, удалите смс с помощью кнопки \"Удалить все СМС, связанные с транзакциями\"", operationId);
                        }
                    } else {
                        smsManager.sendTextMessage(smsNum, null, answ, piSent, null);
                    }
                } else {
                    if (sendWithSaveOutput) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                            SmsManager.getSmsManagerForSubscriptionId(simNumSms).sendTextMessage(smsNum, null, answ, piSent, null);
                        } else {
                            PayLib.codeFeedback(" Code P2P-011: текущая версия системы не поддерживает dual-sim", operationId);
                        }
                    } else {
                        Logger.lg("Build.VERSION.SDK_INT  " + Build.VERSION.SDK_INT + " " + Build.VERSION_CODES.P);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            SmsManager.getSmsManagerForSubscriptionId(simNumSms).sendTextMessageWithoutPersisting(smsNum, null, answ, piSent, null);
                        } else {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                                SmsManager.getSmsManagerForSubscriptionId(simNumSms).sendTextMessage(smsNum, null, answ, piSent, null);
                            } else {
                                PayLib.codeFeedback(" Code P2P-011: текущая версия системы не поддерживает dual-sim", operationId);
                            }
                            PayLib.codeFeedback(" Code P2P-006:пожалуйста, удалите смс с помощью кнопки \"Удалить все СМС, связанные с транзакциями\"", operationId);
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

    /**Compare operator name with enums @see #OperatorNames*/
    private Boolean equalsOperators(String name, OperatorNames constantName) {
        Logger.lg(name + " " + constantName + " " + (name.equals(constantName.toString())));
        return name.equals(constantName.toString());
    }

    /**Function for sending ussd-request and provides passing flow for ussd payment
     * @@param desOper - destination operator. It is required for some operators, which flow in dependence on destination operator
     * @see USSDController#callbackInvoke
     * */
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
            Logger.lg("Send ussd " + name + " dor destination " + destOper + " target " + target
                    + "  " + equalsOperators(name, OperatorNames.MEGAFON));
            if (equalsOperators(name, OperatorNames.MTS)) {
                ussdController.callUSSDInvoke(ussdNum, mapUssd, new USSDController.CallbackInvoke() {
                    @Override
                    public void responseInvoke(String message) {
                        //   Logger.lg("Case 1 " + message);
                        if (message.contains("1>Мобильный телефон")) {
                            ussdController.send("1", new USSDController.CallbackMessage() {
                                @Override
                                public void responseMessage(String message) {
                                    if (message.contains("1>Оплатить МТС") && equalsOperators(destOper, OperatorNames.MTS)) {
//                                        Logger.lg(message);
                                        ussdController.send("1", new USSDController.CallbackMessage() {
                                            @Override
                                            public void responseMessage(String message) {
                                            }
                                        });
                                    }
                                    if (equalsOperators(destOper, OperatorNames.BEELINE)) {
                                        ussdController.send("2", new USSDController.CallbackMessage() {
                                            @Override
                                            public void responseMessage(String message) {

                                            }
                                        });
                                    }
                                    if (equalsOperators(destOper, OperatorNames.MEGAFON)) {
                                        ussdController.send("3", new USSDController.CallbackMessage() {
                                            @Override
                                            public void responseMessage(String message) {

                                            }
                                        });
                                    }
                                    if (equalsOperators(destOper, OperatorNames.TELE)) {
                                        ussdController.send("4", new USSDController.CallbackMessage() {
                                            @Override
                                            public void responseMessage(String message) {

                                            }
                                        });
                                    }
                                }
                            });
                        }
                        if (equalsOperators(destOper, OperatorNames.MTS)) {
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
                        if (equalsOperators(destOper, OperatorNames.BEELINE)) {
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
                        if (equalsOperators(destOper, OperatorNames.MEGAFON)) {
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
                        if (equalsOperators(destOper, OperatorNames.TELE)) {
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
                        PayLib.codeFeedback(" Code P2P-004: " + message, operationId);
                        // message has the response string data from USSD
                        // response no have input text, NOT SEND ANY DATA
                    }
                });
            }
            if (equalsOperators(name, OperatorNames.BEELINE)) {
                str = ussdNum + target + "*" + sum + "#";
                Logger.lg("bee " + str);
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
                        PayLib.codeFeedback(" Code P2P-004: " + message, operationId);
                        // message has the response string data from USSD
                        // response no have input text, NOT SEND ANY DATA
                    }
                });
            }
            if (equalsOperators(name, OperatorNames.MEGAFON)) {
                str = ussdNum + sum + "*" + target + "#";
                Logger.lg("Megafon ussd " + str);
                ussdController.callUSSDInvoke(str, mapUssd, new USSDController.CallbackInvoke() {
                    @Override
                    public void responseInvoke(String message) {
                        // first option list - select option 1
                    }

                    @Override
                    public void over(String message) {
                        Logger.lg("message " + message);
                        PayLib.flagok = false;
                        PayLib.codeFeedback(" Code P2P-004: " + message, operationId);
                        // message has the response string data from USSD
                        // response no have input text, NOT SEND ANY DATA
                    }
                });
            }
            if (equalsOperators(name, OperatorNames.TELE)) {
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
                        PayLib.codeFeedback(" Code P2P-004: " + message, operationId);
                        // message has the response string data from USSD
                        // response no have input text, NOT SEND ANY DATA
                    }
                });
            }
        }
    }

    /**Set operators data*/
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

}
