package com.p2plib2.operators;


import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.telephony.SmsManager;

import com.p2plib2.common.MapData.OperatorNames;
import com.p2plib2.Logger;
import com.p2plib2.PayLib;
import com.p2plib2.common.Constructors;
import com.p2plib2.common.MapData;
import com.p2plib2.entity.MessageEntity;
import com.p2plib2.ussd.USSDController;
import com.p2plib2.common.Constructors;

import static com.p2plib2.PayLib.serviceActivation;
import static com.p2plib2.PayLib.simCounter;
import static com.p2plib2.common.CommonFunctions.equalsOperators;
import static com.p2plib2.common.Constructors.createMsgBody;
import static com.p2plib2.common.Constructors.getAnswerMsgBody;
import static com.p2plib2.common.Constructors.getNumberForAnswer;
import static com.p2plib2.common.Constructors.getOperatorNumber;
import static com.p2plib2.common.Constructors.getUssdCommand;
import static com.p2plib2.common.MapData.mapUssd;
import static com.p2plib2.sms.SmsMonitor.checkContents;

import com.p2plib2.PayLib.Operation;

/**
 * Class for operator work for specifying operation in devMode
 */
public class Operator {
    /**
     * Parameters:
     *
     * @param id - operator id
     * @param operatorName - operator operatorName
     * @param smsNum - From this number (or numbers in format [num]/[num]/[num]) lib receives sms with payment report, payment verification code and etc.
     * @param ussdNum - From this number (or numbers) lib calls ussd-request for payment
     * @param target -  payment will be provided to this number
     * @param sum - sum will be transferred
     * @param simMun - number of sim-current, chosen for the operation
     * @param operationId - id for one separated payment process
     * @param operatorType - type for advanced or testing mode, sms, ussd or common
     */
    public Long id;
    public static Long operationId;
    public String operatorName;
    public String smsNum;
    public String ussdNum;
    public String target;
    public String sum;
    //    public static Integer simNumSms;
//    public static Integer simNumUssd;
    public Integer simNum;
    Operation operatorType;

    public void setType(Operation type) {
        this.operatorType = type;
    }

    public Operation getType() {
        return operatorType;
    }

    public void setParameters(String target, String sum, Integer simNum) {
        this.target = target;
        this.sum = sum;
        if (simNum != null) {
            this.simNum = simNum;
        }
    }

    /**
     * Additional settings
     *
     * @see Operator#Operator(String operName, Boolean sendWithSaveOutput, Boolean sendWithSaveInput, Context cnt, Long operatorId, Operation type)
     */
    private Boolean sendWithSaveOutput;
    private Boolean sendWithSaveInput;
    private Context cnt;

    /**
     * OperatorNames constructor
     *
     * @param operationId        - id for one separated payment process
     * @param sendWithSaveOutput - true - save output sms; false - unsave/delete output sms
     * @param sendWithSaveInput  - true - save input sms; false - unsave/delete input sms
     */
    public Operator(String operName, Boolean sendWithSaveOutput, Boolean sendWithSaveInput,
                    Context cnt, Long operationId, Operation type) {
        this.operatorName = operName;
        this.cnt = cnt;
        Logger.lg(operName + " curs " + operatorName);
        if (operationId != null) {
            this.operationId = operationId;
        } else {
            this.operationId = System.currentTimeMillis();
        }
        this.operatorType = type;
        this.sendWithSaveOutput = sendWithSaveOutput;
        this.sendWithSaveInput = sendWithSaveInput;
    }

    /**
     * This function provides sending SMS process for sms payment
     *
     * @param number  - target number for sending
     * @param message - message for sending
     */

    public void send(Context cnt, String number, String message) {

        /**For monitoring current outside messageEntity sms and its status
         *@param currentMsg and @currentMessages*/
        SmsManager smsManager = SmsManager.getDefault();
        MessageEntity messageEntity = new MessageEntity(number, message, "currentOutputMsg");
        PayLib.currentMessages.add(messageEntity);
        PayLib.currentMsg = number + "[]" + message + "operationId" + operationId;
        if (ActivityCompat.checkSelfPermission(cnt, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            if (simCounter == 1) {
                if (!sendWithSaveOutput) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && sendWithSaveOutput) {
                        smsManager.sendTextMessageWithoutPersisting(number, null, message, null, null);
                    } else {
                        /**Function send without sending is not support if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)*/
                        smsManager.sendTextMessage(number, null, message, null, null);
                        PayLib.feedback.callResult("Process " + operationId + " Code P2P-006: пожалуйста, удалите смс с помощью кнопки \"Удалить все СМС, связанные с транзакциями\"");
                    }
                } else {
                    smsManager.sendTextMessage(number, null, message, null, null);
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                    Logger.lg("SEND! ");
                    if (sendWithSaveOutput) {
                        Logger.lg("SEND! for sim num " + simNum);
                        try {
                            SmsManager.getSmsManagerForSubscriptionId(simNum).sendTextMessage(number, null, message, null, null);
                        } catch (Exception e){
                            PayLib.feedback.callResult("Process error "  + e.toString() );
                        }
                    } else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            SmsManager.getSmsManagerForSubscriptionId(simNum).sendTextMessageWithoutPersisting(number, null, message, null, null);
                        } else {
                            SmsManager.getSmsManagerForSubscriptionId(simNum).sendTextMessage(number, null, message, null, null);
                            PayLib.feedback.callResult("Process " + operationId + " Code P2P-006: пожалуйста, удалите смс с помощью кнопки \"Удалить все СМС, связанные с транзакциями\"");
                        }
                    }
                } else {
                    PayLib.feedback.callResult("Process " + operationId + " Code P2P-011: текущая вверсия системы не поддерживает dual sim");
                }

            }
        } else {
            PayLib.feedback.callResult("Process " + operationId + " Code P2P-015: отсутствует разрешение на отправку СМС \n");
        }
        /**After checking window with confirmation SMS sending turn off Access Service functionality*/
        serviceActivation = false;
//        } catch (Exception e) {
//            Logger.lg("Code P2P-008: " + e.getMessage());
//        }
    }


    /**
     * This function provides sending SMS process for requesting sms payment.
     * Using following methods
     *
     * @see Constructors#createMsgBody(String operatorName, String sum, String target)
     * for constructor output sms-request for specifying operator and
     * @see Constructors#getOperatorNumber(String operatorName, String  smsNum, String target)
     * for constructor number sms-request for specifying
     */
    public void sendSMS(Context cnt) {
        String msgBody = createMsgBody(operatorName, sum, target);
        String number = getOperatorNumber(operatorName, smsNum, target);
        /**For monitoring current outside messageEntity sms and its status
         *@param currentMsg and @currentMessages*/
        Logger.lg("msgBody " + msgBody + " " + number);
        MessageEntity messageEntity = new MessageEntity(number, msgBody, "currentOutputMsg");
        PayLib.currentMessages.add(messageEntity);
        PayLib.currentMsg = number + "[]" + msgBody;
        send(cnt, number, msgBody);
//        catch (Exception e) {
//            Logger.lg("Code P2P-008: " + e.getMessage());
//        }
    }

    /**
     * Function for sending of sms answer for specifying operator
     * Check message body for following cases:
     *
     * @see Constructors#getAnswerMsgBody(String, String) (String operatorName, String sum, String target)
     * for constructor output sms-answer for specifying operator and
     * @see Constructors#getNumberForAnswer(String, String, String) (String operatorName, String  smsNum, String target)
     * for constructor target number for answer
     * Also, this method get code from sms-body for answer or send default answer for this operator
     */
    public void sendAnswer(String smsBody, String smsSender) {
        serviceActivation = true;
        if (checkContents(smsBody, MapData.codeRequest.get(OperatorNames.ALL))) {
            String answer = getAnswerMsgBody(operatorName, smsBody.toLowerCase());
            String numberForAnswer = getNumberForAnswer(operatorName, smsBody.toLowerCase(), smsSender);
            Logger.lg("SendAnswer for " + smsBody + " from smsSender " + smsSender + " answer  " + answer + " to number " + smsNum);
            // try {
            MessageEntity messageEntity = new MessageEntity(numberForAnswer, answer, "currentOutputMsg");
            PayLib.currentMessages.add(messageEntity);
            PayLib.currentMsg = numberForAnswer + "[]" + answer;
            send(cnt, numberForAnswer, answer);
//            } catch (Exception e) {
//                Logger.lg("Code P2P-008: " + e.getMessage());
//            }
        } else {
            Logger.lg("Error for smsBody: " + smsBody);
            PayLib.getSMSResult("Code P2P-008: " + smsBody);
        }
    }

    /**
     * Function for sending ussd-request and provides passing flow for ussd payment
     *
     * @@param desOper - destination operator. It is required for some operators, which flow in dependence on destination operator
     * @see USSDController#callbackInvoke
     */
    public void sendUssd(final String desOper, Activity act) {
        Logger.lg("Service activation status " + serviceActivation);
        if (serviceActivation) {
            final String destOper = desOper.toUpperCase();
            final USSDController ussdController = USSDController.getInstance(act);
            ussdController.cleanCallbackMessage();
            final String ussdCommand = getUssdCommand(operatorName, sum, target, ussdNum);
            //*145*9031234567*150#
            Logger.lg("Send ussd from " + operatorName + " for destination " + destOper + " to target " + target + " and sum " + sum);
            if (equalsOperators(operatorName, MapData.OperatorNames.MTS)) {
                ussdController.callUSSDInvoke(ussdNum, mapUssd, simNum, new USSDController.CallbackInvoke() {
                    @Override
                    public void responseInvoke(String message) {
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
                        PayLib.serviceActivation = false;
                        PayLib.feedback.callResult("Process " + operationId + " Code P2P-004: " + message);
                        // message has the response string data from USSD
                        // response no have input text, NOT SEND ANY DATA
                    }
                });
            }
            if (equalsOperators(operatorName, OperatorNames.BEELINE)) {
                ussdController.callUSSDInvoke(ussdCommand, mapUssd, simNum, new USSDController.CallbackInvoke() {
                    @Override
                    public void responseInvoke(String message) {
                        // first option list - select option 1
                        String answ = message.substring(message.indexOf("введите ") + 8, message.indexOf("."));
                        Logger.lg("Cur message " + message + " and code " + answ);
                        ussdController.send(answ, new USSDController.CallbackMessage() {
                            @Override
                            public void responseMessage(String message) {
                            }
                        });
                    }

                    @Override
                    public void over(String message) {
                        PayLib.serviceActivation = false;
                        PayLib.feedback.callResult("Process " + operationId + " Code P2P-004: " + message);
                        // message has the response string data from USSD and response no have input text, NOT SEND ANY DATA
                    }
                });
            }
            if (equalsOperators(operatorName, OperatorNames.MEGAFON)) {
                ussdController.callUSSDInvoke(ussdCommand, mapUssd, simNum, new USSDController.CallbackInvoke() {
                    @Override
                    public void responseInvoke(String message) {
                        // first option list - select option 1
                    }

                    @Override
                    public void over(String message) {
                        PayLib.serviceActivation = false;
                        PayLib.feedback.callResult("Process " + operationId + " Code P2P-004: " + message);
                    }
                });
            }
            if (equalsOperators(operatorName, OperatorNames.TELE)) {
                ussdController.callUSSDInvoke(ussdCommand, mapUssd, simNum, new USSDController.CallbackInvoke() {
                    @Override
                    public void responseInvoke(String message) {
                        // first option list - select option 1
                        ussdController.send("1", new USSDController.CallbackMessage() {
                            @Override
                            public void responseMessage(String message) {
                            }
                        });
                    }

                    @Override
                    public void over(String message) {
                        PayLib.serviceActivation = false;
                        PayLib.feedback.callResult("Process " + operationId + " Code P2P-004: " + message);

                    }
                });
            }
        }
    }


    /**
     * Set operators data
     */
    public void updateData(String sms_number, String ussd) {
        this.smsNum = sms_number;
        this.ussdNum = ussd;
        Logger.lg("Set data: operatorName " + operatorName + "; smsNum " + smsNum + "; target " + target + "; sum " + sum + ";ussdNum " + ussdNum);
    }

    @Override
    public String toString() {
        return "Name " + operatorName +
                " smsNum " + smsNum +
                " ussdNum " + ussdNum +
                " target " + target +
                " sum " + sum;
    }

    public void setFlagSave(Boolean sendWithSaveOutput, Boolean sendWithSaveInput) {
        this.sendWithSaveOutput = sendWithSaveOutput;
        this.sendWithSaveInput = sendWithSaveInput;
    }
}
