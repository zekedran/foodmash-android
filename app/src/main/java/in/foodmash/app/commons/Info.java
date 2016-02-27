package in.foodmash.app.commons;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by Zeke on Sep 18 2015.
 */
public class Info {

    public static int getPackagingCentreId(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("cache",0);
        return sharedPreferences.getInt("packaging_centre_id",-1);
    }

    public static String getAreaName(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("cache", 0);
        return sharedPreferences.getString("area_name", null);
    }

    public static String getCityName(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("cache", 0);
        return sharedPreferences.getString("city_name", null);
    }

    public static String getCityJsonArrayString(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("cache", 0);
        return sharedPreferences.getString("city_list", null);
    }

    public static String getComboJsonArrayString(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("cache", 0);
        return sharedPreferences.getString("combo_list", null);
    }

    public static boolean isLoggedIn(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("session",0);
        return sharedPreferences.getBoolean("logged_in",false);
    }

    public static String getEmail(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("cache", 0);
        return sharedPreferences.getString("email", null);
    }

    public static String getPhone(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("cache", 0);
        return sharedPreferences.getString("phone",null);
    }

    public static String getFirstName(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("cache", 0);
        return sharedPreferences.getString("firstName",null);
    }

}
