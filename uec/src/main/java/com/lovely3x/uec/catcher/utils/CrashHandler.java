package com.lovely3x.uec.catcher.utils;


import android.content.Context;

import com.lovely3x.uec.catcher.ExceptionHandler;
import com.lovely3x.uec.catcher.ExceptionSerializable;
import com.lovely3x.uec.catcher.TextExceptionSerializable;

import java.lang.Thread.UncaughtExceptionHandler;

/**
 * UncaughtExceptionHandler：线程未捕获异常控制器是用来处理未捕获异常的。 如果程序出现了未捕获异常默认情况下则会出现强行关闭对话框
 * 实现该接口并注册为程序中的默认未捕获异常处理 这样当未捕获异常发生时，就可以做些异常处理操作 例如：收集异常信息，发送错误报告 等。
 * <p/>
 * UncaughtException处理类,当程序发生Uncaught异常的时候,由该类来接管程序,并记录发送错误报告.
 */
public class CrashHandler implements UncaughtExceptionHandler {
    /**
     * Debug Log Tag
     */
    public static final String TAG = "CrashHandler";

    /**
     * CrashHandler实例
     */
    private static CrashHandler INSTANCE;
    /**
     * 程序的Context对象
     */
    private Context mContext;
    /**
     * 系统默认的UncaughtException处理类
     */
    private UncaughtExceptionHandler mDefaultHandler;

    private ExceptionHandler mExceptionHandler;
    private ExceptionSerializable mDefaultExceptionSerializable;

    /**
     * 保证只有一个CrashHandler实例
     */
    private CrashHandler() {

    }

    /**
     * 获取CrashHandler实例 ,单例模式
     */
    public static CrashHandler getInstance() {
        if (INSTANCE == null)
            INSTANCE = new CrashHandler();
        return INSTANCE;
    }

    /**
     * 初始化,注册Context对象, 获取系统默认的UncaughtException处理器, 设置该CrashHandler为程序的默认处理器
     *
     * @param ctx 初始化用的上下文
     */
    public void init(Context ctx, ExceptionHandler handler) {
        mContext = ctx;
        this.mExceptionHandler = handler;

        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        mDefaultExceptionSerializable = new TextExceptionSerializable();
        Thread.setDefaultUncaughtExceptionHandler(this);

        sendCrashReportsToServer(ctx);
    }

    /**
     * 设置默认的异常序列化器
     *
     * @param exceptionSerializable 序列化器
     */
    public void setDefaultExceptionSerializable(ExceptionSerializable exceptionSerializable) {
        if (exceptionSerializable != null) {
            mDefaultExceptionSerializable = exceptionSerializable;
        }
    }


    /**
     * 当UncaughtException发生时会转入该函数来处理
     */
    @Override
    public void uncaughtException(Thread thread, final Throwable ex) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                handleException(ex);
            }
        }).start();

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        //最后由默认的异常捕获器处理
        if (mDefaultHandler != null) {
            mDefaultHandler.uncaughtException(thread, ex);
        }
    }

    /**
     * 自定义错误处理,收集错误信息 发送错误报告等操作均在此完成. 开发者可以根据自己的情况来自定义异常处理逻辑
     *
     * @param ex 发生的异常对象
     * @return true:如果处理了该异常信息;否则返回false
     */
    private boolean handleException(Throwable ex) {
        if (mExceptionHandler != null) {
            if (mDefaultExceptionSerializable != null) {

                String dirName = mDefaultExceptionSerializable.getCRDir(mContext);
                String fileName = mDefaultExceptionSerializable.getCRFileName(mContext, ex);
                mDefaultExceptionSerializable.serialize(mContext, dirName, fileName, ex);
                String[] files = mDefaultExceptionSerializable.getCRFileNames(mContext);
                if (files != null && files.length > 0) {
                    return mExceptionHandler.handleException(mContext, ex, dirName, files);
                }
            }
        }
        return false;
    }

    /**
     * 把错误报告发送给服务器,包含新产生的和以前没发送的.
     *
     * @param ctx 上下文对象
     */
    private void sendCrashReportsToServer(Context ctx) {
        if (mDefaultExceptionSerializable != null) {
            String dir = mDefaultExceptionSerializable.getCRDir(ctx);
            String[] crFiles = mDefaultExceptionSerializable.getCRFileNames(ctx);
            if (crFiles != null && crFiles.length > 0) {
                if (mExceptionHandler != null) {
                    mExceptionHandler.reportOnLaunch(ctx, dir, crFiles);
                }
            }
        }
    }
}