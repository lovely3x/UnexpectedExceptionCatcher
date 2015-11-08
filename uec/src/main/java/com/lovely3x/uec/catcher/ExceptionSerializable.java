package com.lovely3x.uec.catcher;

import android.content.Context;

/**
 * 异常序列化接口
 * Created by lovely3x on 15-11-8.
 */
public interface ExceptionSerializable {

    /**
     * 获取崩溃文日志存放文件夹
     *
     * @param context   上下文对象
     * @return 崩溃文日志存放文件夹
     */
    String getCRDir(Context context);

    /**
     * 生成崩溃日志文件名
     *
     * @param context   上下文对象
     * @param throwable 异常对象
     * @return 崩溃日志文件名
     */
    String getCRFileName(Context context, Throwable throwable);


    /**
     * 序列化异常到文件中
     *
     * @param context   上下文对象
     * @param dir       存放异常文件的文件夹名
     * @param fileName  写入异常的文件名
     * @param throwable 需要写入的异常对象
     * @return 是否序列化成功
     */
    boolean serialize(Context context, String dir, String fileName, Throwable throwable);


    /**
     * 获取当前所有的异常文件名
     *
     * @param context 上下文对象
     * @return 当前所有的异常文件
     */
    String[] getCRFileNames(Context context);
}
