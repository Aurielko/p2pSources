package com.example.dkolosovskiy.p2plib;

import android.util.Log;

public class Logger {
    public static void lg(String s){
        Log.e("DebugCat: ", s);
        System.out.println("DebugCat: " + s);
    }
}
