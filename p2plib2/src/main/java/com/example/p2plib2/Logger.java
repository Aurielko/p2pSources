package com.example.p2plib2;

import android.util.Log;

public class Logger {
    public static void lg(String s){
        Log.e("DebugCat: ", s);
        System.out.println("DebugCat: " + s);
    }
}
