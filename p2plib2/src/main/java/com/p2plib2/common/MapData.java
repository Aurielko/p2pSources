package com.p2plib2.common;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

public class MapData {
    public static HashMap mapUssd = new HashMap<>();
    public static HashMap<OperatorNames, HashSet<String>> paymentGranted = new HashMap();
    public static HashMap<OperatorNames, HashSet<String>> paymentDenied = new HashMap();
    public static HashMap<OperatorNames, HashSet<String>> codeRequest = new HashMap();
    public static HashMap<OperatorNames, HashSet<String>> answerSenderCatch = new HashMap();
    public static HashMap<OperatorNames, HashSet<String>> answerCodeConstructor = new HashMap();

    public static enum OperatorNames {
        BEELINE, MTS, TELE, MEGAFON, ALL
    }

    public static void ini() {
        /**Template for payment granted*/
        HashSet grantedSet = new HashSet();
        grantedSet.add("успеш");
        grantedSet.add("выпол");
        grantedSet.add("латеж ");
        grantedSet.add("осуществ");
        paymentGranted.put(OperatorNames.ALL, grantedSet);
        /**Template for payment denied*/
        HashSet deniedSet = new HashSet();
        deniedSet.add("отказан");
        deniedSet.add("не осущ");
        deniedSet.add("не выполн");
        deniedSet.add("не про");
        deniedSet.add("отказано");
        deniedSet.add("не можете");
        paymentDenied.put(OperatorNames.ALL, deniedSet);
        /**Template for code request*/
        HashSet requestSet = new HashSet();
        requestSet.add("отправь");
        requestSet.add("ответь");
        requestSet.add("ответн");
        requestSet.add("подтверд");
        requestSet.add("подтвержде");
        codeRequest.put(OperatorNames.ALL, requestSet);

        /**Template for catch sender for answer code*/
        HashSet catchSenderSet = new HashSet();
        catchSenderSet.add("ответном");
        catchSenderSet.add("ответное");
        catchSenderSet.add("в ответ");
        answerSenderCatch.put(OperatorNames.ALL, catchSenderSet);
        /**Templates for ussd working*/
        mapUssd.put("KEY_LOGIN", new HashSet<>(Arrays.asList("подождите", "загрузка")));
        mapUssd.put("KEY_ERROR", new HashSet<>(Arrays.asList("problema", "problem", "ошибка", "null")));
        /**Templates for sms-message structure*/

        /**Templates for construct answer sms with code*/
        HashSet answerSenderSet = new HashSet();
        answerSenderSet.add("ответном");
        answerCodeConstructor.put(OperatorNames.ALL, answerSenderSet);

    }
}
