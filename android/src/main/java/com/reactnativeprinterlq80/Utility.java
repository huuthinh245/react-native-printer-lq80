package com.reactnativeprinterlq80;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * Created by NO on 2017/9/14.
 */

public class Utility {
    public  static void checkBlueboothPermission(Activity context, String permission, String[] requestPermissions, Callback callback){
        if (Build.VERSION.SDK_INT >= 23) {
            //校验是否已具有模糊定位权限
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(context, requestPermissions, 100);
            } else {
                //具有权限
                callback.permit();
                return;
            }
        } else {
            //系统不高于6.0直接执行
            callback.pass();
        }
    }
    public interface Callback {
        /**
         * API>=23 允许权限
         */
        void permit();


        /**
         * API<23 无需授予权限
         */
        void pass();
    }

    public static void show(final Context context, final String message){
        ((Activity)context).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context,message, Toast.LENGTH_SHORT).show();
            }
        });

    }
}
