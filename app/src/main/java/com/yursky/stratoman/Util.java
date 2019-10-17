package com.yursky.stratoman;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

class Util {

    static void savePreferences(String key, String value, Context context){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.apply();
    }

    static String loadPreferences(String key, Context context){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(key,"");
    }
}
