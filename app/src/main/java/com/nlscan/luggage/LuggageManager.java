package com.nlscan.luggage;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;


public class LuggageManager {


    private static final String ACTION_LUGGAGE_SERVICE = "android.nlscan.intent.action.START_LUGGAGE_SERVICE";
    private static final String SERVICE_PACKAGE = "com.nlscan.luggage";
    private static final String TAG = "LuggageManager";
    private static LuggageManager mInstance;
    private ModelInterface gModelInterface;
    private IJudgeCallback mCallback;
    private boolean ifSetCallback = false;
    private Context mContext;

    private LuggageManager(Context context) {
        this.initObject(context);
    }

    public static LuggageManager getInstance(Context context) {

        if (mInstance == null) {
            mInstance = new LuggageManager(context);
        }

        return mInstance;
    }

    private void initObject(Context context) {
        this.mContext = context;

        mUHFConnHandlerThread = new HandlerThread("UHFConnHandlerThread", android.os.Process.THREAD_PRIORITY_BACKGROUND);
        mUHFConnHandlerThread.start();
        mUHFConnHandler = new UHFConnHandler(mUHFConnHandlerThread.getLooper());


        bindToService();

        threadWait();

    }


    private boolean gBindState = false;//服务是否绑定成功
    private LuggageServiceConnection mConnection;
    private class  LuggageServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG,"luggage service connected ");
            gModelInterface = ModelInterface.Stub.asInterface(service);
            gBindState = true;
            mUHFConnHandler.removeMessages(MSG_CHECK_CONN_ALIVE);
            if (ifSetCallback && mCallback != null)   setCallback(mCallback);

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG,"luggage service disconnected ... ");
            gBindState = false;
            gModelInterface = null;

            //启动服务连接状态监控,如果断开,自动重连
            mUHFConnHandler.removeMessages(MSG_CHECK_CONN_ALIVE);
            mUHFConnHandler.sendEmptyMessage(MSG_CHECK_CONN_ALIVE);


        }
    }


    private HandlerThread mUHFConnHandlerThread ;
    private Handler mUHFConnHandler;
    //检测UHF服务是否断开
    private final static int MSG_CHECK_CONN_ALIVE = 0x01;
    //检测时间间隔(毫秒)
    private final static int CHECK_INTERVAL_TIME_MS = 1000*2;
    //服务重连机制
    private class UHFConnHandler extends Handler {


        public UHFConnHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case MSG_CHECK_CONN_ALIVE:
                    checkServiceConn();
                    sendEmptyMessageDelayed(MSG_CHECK_CONN_ALIVE, CHECK_INTERVAL_TIME_MS);
                    break;
                default:
                    break;
            }
        }

    }



    private void checkServiceConn()
    {
        if(!gBindState  || gModelInterface == null )
        {
            Log.d(TAG, "Luggage service reconnecting ...");
            bindToService();
        }
    }

    private void bindToService(){
        Intent service = new Intent(ACTION_LUGGAGE_SERVICE);
        service.setPackage(SERVICE_PACKAGE);
        mConnection = new LuggageServiceConnection();
        boolean rel = mContext.bindService(service,mConnection, Context.BIND_AUTO_CREATE);
        Log.d(TAG,"binding result is " + rel);
    }





    //-----------------接口方法-------------------------//

    private void threadWait(){
        if (gModelInterface == null){
            final Stack<Integer> timeStack = new Stack<>();

            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    timeStack.add(0);
                }
            },1000);
            while (timeStack.size() == 0 && !gBindState){}
        }
    }




    private IJudgeCallback.Stub mCurrentCallback = new IJudgeCallback.Stub() {
        @Override
        public void onJudgeResult(String s) throws RemoteException {
            Log.d(TAG,"回调数据 ");
            mCallback.onJudgeResult(s);
        }
    };




    //初始化服务
    public int initService(){

        int rel = ResultState.FAIL;


        threadWait();

        try {
            Log.d(TAG, "开始初始化");
            rel = gModelInterface.initService(); //服务初始化
        } catch (Exception e) {
            e.printStackTrace();
        }

        return  rel;
    }

    //下发数据
    public int sendInfo(int dataType, String data){

        int rel = ResultState.FAIL;


        threadWait();

        try {
            Log.d(TAG, "开始下发数据");
            gModelInterface.sendInfo(dataType,data);
            rel = ResultState.SUCCESS;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return rel;
    }



    //清除数据
    public int clearInfo(String conditions){

        int rel = ResultState.FAIL;


        threadWait();

        try {
            Log.d(TAG, "开始清除数据");
            gModelInterface.clearInfo(conditions);
            rel = ResultState.SUCCESS;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return rel;
    }





    //设置回调
    public int setCallback(IJudgeCallback callback){

        int rel = ResultState.FAIL;


        if (gModelInterface == null  || callback == null ) return rel;

        try {
            Log.d(TAG, "开始设置回调");
            mCallback = callback;
            gModelInterface.setCallback(mCurrentCallback);
            ifSetCallback = true;
            rel = ResultState.SUCCESS;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return rel;
    }


    //开启服务
    public int startService(){

        int rel = ResultState.FAIL;


        threadWait();

        try {
            Log.d(TAG, "开启检测");
            rel = gModelInterface.startService();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return rel;
    }


    //结束服务
    public int stopService(){

        int rel = ResultState.FAIL;


        threadWait();

        try {
            Log.d(TAG, "结束检测");
            rel = gModelInterface.stopService();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return rel;
    }











    //-----------------接口方法-------------------------//






}
