package com.lovely3x.uec.catcher;

import android.content.Context;

/**
 * 异常捕获后回调处理器接口(实现该接口表示可以处理异常)
 */
public interface ExceptionHandler {

    /**
     * 处理异常
     *
     * @param context 上下文
     * @param e       异常对象
     * @param dir     文件夹
     * @param crFiles 当前设备中已经存在的异常文件名数组
     * @return 是否成功处理该异常
     */
    public boolean handleException(Context context, Throwable e, String dir, String[] crFiles);

    /**
     * 在监听器注册后,如果发现设备中存在崩溃日志则会调用
     *
     * @param context 上下文对象
     * @param crFiles 崩溃日志文件名数组
     */
    public void reportOnLaunch(Context context, String dir, String[] crFiles);

    /**
     * 处理完成
     */
    public void handleSuccessful(Context context);
}