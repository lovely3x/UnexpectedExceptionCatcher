package com.lovely3x.uec.catcher;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Process;
import android.util.Log;

import com.lovely3x.uec.catcher.utils.Email;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Email异常处理器
 * 用于异常发生时通过电子邮件的方式发送异常
 * 该方式一般推荐用于 在测试中使用
 * 在正式环境中建议不要使用该方式
 */
public class EmailExceptionHandler implements ExceptionHandler {

    private static final String TAG = "EmailExceptionHandler";
    private final String mUsername;
    private final String mPassword;
    private final String mTo;

    /**
     *
     * @param user 发送人账号
     * @param password 发送人密码
     * @param to 接收人账号
     */
    public EmailExceptionHandler(String user, String password, String to) {
        this.mUsername = user;
        mPassword = password;
        mTo = to;
    }

    @Override
    public boolean handleException(final Context context, Throwable e, String dir, final String[] crFiles) {
        for (String p : crFiles) {
            sendFile(context, new File(dir, p));
        }
        return true;
    }

    public void reportOnLaunch(Context context, String dir, String[] crFiles) {
        sendFileOnNewWorkThread(context, dir, crFiles);
    }

    @Override
    public void handleSuccessful(Context context) {
        Log.e(TAG, "handleSuccessful");
        System.exit(1);
        Process.killProcess(Process.myPid());
    }

    /**
     * 创建一个新的线程,并发送崩溃日志
     *
     * @param context 上下文
     * @param dir
     * @param crFiles 崩溃日志
     */
    protected void sendFileOnNewWorkThread(final Context context, final String dir, final String[] crFiles) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (String p : crFiles) {
                    File file = new File(dir, p);
                    try {
                        sendFile(context, file);
                    } catch (RuntimeException e1) {
                        e1.printStackTrace();
                        Log.e(TAG, String.format("send file %s failure", file));
                    }
                }
            }
        }).start();

    }


    /**
     * 通过电子邮件发送文件
     *
     * @param context 上下文对象
     * @param file    文件对象
     */
    public void sendFile(Context context, File file) {
        if (file.exists()) {
            boolean result = sendEmail(context, file.getAbsolutePath());
            Log.e(TAG, "send email result " + file + " " + result);

            if (result) {
                boolean deleteResult = file.delete();
                Log.e(TAG, "delete file " + file + " " + deleteResult);
            }
        }
    }


    /**
     * 发送邮件
     *
     * @return 是否发送成功
     */
    private synchronized boolean sendEmail(Context context, String crFilePath) throws RuntimeException {

        final String username = mUsername;
        final String password = mPassword;
        final String to = mTo;

        String host = "smtp." + username.split("@")[1];

        Email email = new Email();
        email.setUserName(username);
        email.setPassword(password);
        email.setIfAuth(true);
        email.setTo(to);
        email.setSubject(getAppName(context) + new SimpleDateFormat("yyyy-MM-dd HH:mmss", Locale.getDefault()).format(new Date()) + "异常反馈");
        email.setDisplayName(getAppName(context));
        email.setFrom(username);
        email.setContent(getAppName(context) + "出现异常情况");
        email.setSmtpServer(host);
        email.addAttachfile(crFilePath);
        return email.send();
    }

    /**
     * 获取 程序名
     *
     * @param context
     * @return
     */
    public static final String getAppName(Context context) {
        PackageManager packageManager = null;
        ApplicationInfo applicationInfo = null;
        try {
            packageManager = context.getApplicationContext().getPackageManager();
            applicationInfo = packageManager.getApplicationInfo(context.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            applicationInfo = null;
        }
        String applicationName = (String) packageManager.getApplicationLabel(applicationInfo);
        return applicationName;
    }
}