package com.reactnativeprinterlq80;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.Message;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.BaseActivityEventListener;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.module.annotations.ReactModule;

import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import tspl.HPRTPrinterHelper;
import tspl.Print;
import tspl.PublicFunction;

import static android.app.Activity.RESULT_CANCELED;
import static com.facebook.react.bridge.UiThreadUtil.runOnUiThread;

@ReactModule(name = PrinterLq80Module.NAME)
public class PrinterLq80Module extends ReactContextBaseJavaModule implements LifecycleEventListener {
  public static final String NAME = "PrinterLq80";
  private UsbManager mUsbManager = null;
  private UsbDevice device = null;
  private ReactApplicationContext context;
  private PendingIntent mPermissionIntent=null;
  private Print print;
  private ExecutorService executorService = Executors.newSingleThreadExecutor();
  private String mStatusStr="";
  private static final String ACTION_USB_PERMISSION = "com.HPRTSDK";
  private PublicFunction PFun=null;
  private PublicAction PAct=null;
  Handler handler;
  private static String[] PERMISSIONS_STORAGE = {
    "android.permission.READ_EXTERNAL_STORAGE",
    "android.permission.WRITE_EXTERNAL_STORAGE" };
  public PrinterLq80Module(ReactApplicationContext reactContext) {
    super(reactContext);
    context = reactContext;
    context.addActivityEventListener(mActivityEventListener);
  }

  @Override
  @NonNull
  public String getName() {
    return NAME;
  }

  @ReactMethod
  public void init() {
    print = new Print();
    PFun=new PublicFunction(context);
    PAct=new PublicAction(context);
    initSetting();
  }

  private void initSetting(){
    String SettingValue="";
    SettingValue=PFun.ReadSharedPreferencesData("Codepage");
    if(SettingValue.equals(""))
      PFun.WriteSharedPreferencesData("Codepage", "0,PC437(USA:Standard Europe)");

    SettingValue=PFun.ReadSharedPreferencesData("Cut");
    if(SettingValue.equals(""))
      PFun.WriteSharedPreferencesData("Cut", "0");

    SettingValue=PFun.ReadSharedPreferencesData("Cashdrawer");
    if(SettingValue.equals(""))
      PFun.WriteSharedPreferencesData("Cashdrawer", "0");

    SettingValue=PFun.ReadSharedPreferencesData("Buzzer");
    if(SettingValue.equals(""))
      PFun.WriteSharedPreferencesData("Buzzer", "0");

    SettingValue=PFun.ReadSharedPreferencesData("Feeds");
    if(SettingValue.equals(""))
      PFun.WriteSharedPreferencesData("Feeds", "0");
  }

  @ReactMethod
  public void connectUSB(Promise promise) {
    Log.d("CONNECT-USB", "CONNECT");
    mPermissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);
    IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
    filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
    context.registerReceiver(mUsbReceiver, filter);

    IntentFilter intent = new IntentFilter();
    intent.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
    context.registerReceiver(mReceiver, intent);
    handler = new Handler(){
      @Override
      public void handleMessage(Message msg) {
        // TODO Auto-generated method stub
        super.handleMessage(msg);
        if (msg.what==1) {
          Toast.makeText(context, "succeed", Toast.LENGTH_SHORT).show();
        }else {
          Toast.makeText(context, "failure",Toast.LENGTH_SHORT).show();
        }
      }
    };


    mUsbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
    HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
    Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
    boolean HavePrinter = false;
    while (deviceIterator.hasNext()) {
      device = deviceIterator.next();
      int count = device.getInterfaceCount();
      for (int i = 0; i < count; i++) {
        UsbInterface intf = device.getInterface(i);
        if (intf.getInterfaceClass() == 7) {
          HavePrinter = true;
          mUsbManager.requestPermission(device, mPermissionIntent);
        }
      }
    }
    promise.resolve(HavePrinter);
    if(!HavePrinter) {
      Toast.makeText(context, "Please connect usb printer",Toast.LENGTH_SHORT).show();
    }
  }
  @ReactMethod
  public void printImage(String filePath) {
    new Thread(){
      public void run() {
        try {
          int size = 639;
          byte[] decodedString = Base64.decode(filePath, Base64.DEFAULT);
          Bitmap bmp = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
//          Bitmap bmp= BitmapFactory.decodeFile(filePath);
          bmp = resizeImage(bmp,size,size, "cover",true);
          if(bmp == null) {
            Toast.makeText(context, "bitmap null",Toast.LENGTH_LONG).show();
            return;
          }
          int height = bmp.getHeight()/8;
          if(HPRTPrinterHelper.printAreaSize("100",""+ height)==-1){
            Toast.makeText(context, "disconnect",Toast.LENGTH_LONG).show();
            return;
          }
          HPRTPrinterHelper.CLS();
          int a=HPRTPrinterHelper.printImage("0","0",bmp,true);
          HPRTPrinterHelper.Gap("5","5");
          HPRTPrinterHelper.Print("1", "1");
          if (a>0) {
            handler.sendEmptyMessage(1);
          }else {
            handler.sendEmptyMessage(0);
          }
        }catch (Exception e) {
          Log.e("error", e.getMessage());
          handler.sendEmptyMessage(0);
        }
      }
    }.start();
  }

  private BroadcastReceiver mUsbReceiver = new BroadcastReceiver()
  {
    public void onReceive(Context context, Intent intent)
    {
      try{
        String action = intent.getAction();
        if (ACTION_USB_PERMISSION.equals(action)){
          synchronized (this){
            device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)){
              if(HPRTPrinterHelper.PortOpen(context,device)!=0){
//				        		HPRTPrinter=null;

                return;
              }

            }else{
              return;
            }
          }
        }
        if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)){
          device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
          if (device != null){
            int count = device.getInterfaceCount();
            for (int i = 0; i < count; i++){
              UsbInterface intf = device.getInterface(i);
              //Class ID 7代表打印机
              if (intf.getInterfaceClass() == 7){
                HPRTPrinterHelper.PortClose();
              }
            }
          }
        }
      }catch (Exception e){
        Log.e("HPRTSDKSample", (new StringBuilder("Activity_Main --> mUsbReceiver ")).append(e.getMessage()).toString());
      }
    }
  };

  public final BroadcastReceiver mReceiver = new BroadcastReceiver() {

    @Override
    public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();
      if(BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)){
        try {
          HPRTPrinterHelper.PortClose();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
  };

  @Override
  public void onHostResume() {

  }

  @Override
  public void onHostPause() {

  }

  @Override
  public void onHostDestroy() {
    try {
      HPRTPrinterHelper.PortClose();
      if (mUsbReceiver!=null){
        context.unregisterReceiver(mUsbReceiver);
      }
      if (mReceiver!=null){
        context.unregisterReceiver(mReceiver);
      }
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  private static Bitmap resizeImage(Bitmap image, int newWidth, int newHeight,
                                    String mode, boolean onlyScaleDown) {
    Bitmap newImage = null;
    if (image == null) {
      return null; // Can't load the image from the given path.
    }

    int width = image.getWidth();
    int height = image.getHeight();

    if (newHeight > 0 && newWidth > 0) {
      int finalWidth;
      int finalHeight;

      if (mode.equals("stretch")) {
        // Distort aspect ratio
        finalWidth = newWidth;
        finalHeight = newHeight;

        if (onlyScaleDown) {
          finalWidth = Math.min(width, finalWidth);
          finalHeight = Math.min(height, finalHeight);
        }
      } else {
        // "contain" (default) or "cover": keep its aspect ratio
        float widthRatio = (float) newWidth / width;
        float heightRatio = (float) newHeight / height;

        float ratio = mode.equals("cover") ?
          Math.max(widthRatio, heightRatio) :
          Math.min(widthRatio, heightRatio);

        if (onlyScaleDown) ratio = Math.min(ratio, 1);

        finalWidth = (int) Math.round(width * ratio);
        finalHeight = (int) Math.round(height * ratio);
      }

      try {
        newImage = Bitmap.createScaledBitmap(image, finalWidth, finalHeight, true);
      } catch (OutOfMemoryError e) {
        return null;
      }
    }

    return newImage;
  }
  @ReactMethod
  private void getPrintStatus() {
    executorService.execute(new Runnable() {
      @Override
      public void run() {
        try{
          int printerStatus = HPRTPrinterHelper.getPrinterStatus();
          switch(printerStatus){
            case HPRTPrinterHelper.STATUS_DISCONNECT:
              mStatusStr=context.getString(R.string.status_disconnect);
              break;
            case HPRTPrinterHelper.STATUS_TIMEOUT:
              mStatusStr=context.getString(R.string.status_timeout);
              break;
            case HPRTPrinterHelper.STATUS_OK:
              mStatusStr=context.getString(R.string.status_ok);
              break;
            case HPRTPrinterHelper.STATUS_COVER_OPENED:
              mStatusStr=context.getString(R.string.status_cover_opened);
              break;
            case HPRTPrinterHelper.STATUS_NOPAPER:
              mStatusStr=context.getString(R.string.status_nopaper);
              break;
            case HPRTPrinterHelper.STATUS_OVER_HEATING:
              mStatusStr=context.getString(R.string.status_over_heating);
              break;
            case HPRTPrinterHelper.STATUS_PRINTING:
              mStatusStr=context.getString(R.string.status_printing);
              break;
            default:
              break;
          }
          runOnUiThread(new Runnable() {
            @Override
            public void run() {
              Toast.makeText(context,mStatusStr,Toast.LENGTH_SHORT).show();
            }
          });
        }catch (Exception e){}
      }
    });
  }
  private final ActivityEventListener mActivityEventListener = new BaseActivityEventListener() {

    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent intent) {
      {
        switch(resultCode)
        {
          case HPRTPrinterHelper.ACTIVITY_IMAGE_FILE:
            String strImageFile=intent.getExtras().getString("FilePath");
            Log.d("IMAGE", strImageFile);
            return;

        }
      }
    }
  };
}
