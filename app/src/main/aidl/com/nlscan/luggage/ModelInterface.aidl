// ModelInterface.aidl
package com.nlscan.luggage;

import com.nlscan.luggage.IJudgeCallback;
// Declare any non-default types here with import statements

interface ModelInterface {


    //初始化服务
    int initService();


    //下发数据
    void sendInfo(int dataType, String data);

    //清除数据
    void clearInfo(String conditions);

    //设置回调接口
    void setCallback(IJudgeCallback callback);


    //开启服务
    int startService();

    //结束服务
    int stopService();


}
