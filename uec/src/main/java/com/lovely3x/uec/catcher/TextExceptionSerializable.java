package com.lovely3x.uec.catcher;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Field;

/**
 * 普通的文本异常序列化
 * Created by lovely3x on 15-11-8.
 */
public class TextExceptionSerializable implements ExceptionSerializable {

    private static final String TAG = "TES";

    /**
     * 崩溃日志文件夹名字
     */
    public static final String CR_DIR_NAME = "crs";

    /**
     * 奔溃日志 后缀名
     */
    public static final String CR_FILE_SUFFIX = ".cr.txt";
    /**
     * 版本名
     */
    private static final String VERSION_NAME = "versionName";

    /**
     * 版本号
     */
    private static final String VERSION_CODE = "versionCode";

    /**
     * 堆栈跟踪信息
     */
    private static final String STACK_TRACE = "STACK_TRACE";

    /**
     * 键值对分隔符号
     */
    private static final String PAIR_SEPARATOR = ":";


    @Override
    public String getCRDir(Context context) {
        String path = String.format("%s%s%s", context.getFilesDir(), File.separator, CR_DIR_NAME);
        File file = new File(path);
        if(!file.exists())file.mkdirs();
        return path;
    }

    @Override
    public String getCRFileName(Context context, Throwable throwable) {
        return  String.format("%s%s",System.currentTimeMillis(),CR_FILE_SUFFIX);
    }

    @Override
    public boolean serialize(Context context, String dir, String fileName, Throwable throwable) {
        boolean result = true;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        writeCrashDeviceInfo(baos, context);
        writeCrashInfo(baos, throwable);
        File serializeFile = new File(dir, fileName);
        if (!serializeFile.exists()) {
            try {
                serializeFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                result = false;
            }
        }

        if (serializeFile.exists()) {
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(serializeFile);
                fos.write(baos.toByteArray());
            } catch (IOException e) {
                e.printStackTrace();
                result = false;
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e) {
                    }
                }
            }
        }

        try {
            baos.close();
        } catch (IOException e) {
        }

        return result;
    }


    @Override
    public String[] getCRFileNames(Context context) {
        File filesDir = new File(getCRDir(context));

        // 实现FilenameFilter接口的类实例可用于过滤器文件名
        FilenameFilter filter = new FilenameFilter() {
            // accept(File dir, String name)
            // 测试指定文件是否应该包含在某一文件列表中。
            public boolean accept(File dir, String name) {
                return name.endsWith(CR_FILE_SUFFIX);
            }
        };
        // 返回一个字符串数组，这些字符串指定此抽象路径名表示的目录中满足指定过滤器的文件和目录
        return filesDir.list(filter);
    }


    /**
     * 收集程序崩溃的设备信息
     *
     * @param ctx 搜集异常要用到的 上下文对象
     */
    private void writeCrashDeviceInfo(OutputStream os, Context ctx) {
        try {
            // Class for retrieving various kinds of information related to the
            // application packages that are currently installed on the device.
            // You can find this class through getPackageManager().
            PackageManager pm = ctx.getPackageManager();
            // getPackageInfo(String packageName, int flags)
            // Retrieve overall information about an application package that is
            // installed on the system.
            // public static final int GET_ACTIVITIES
            // Since: API Level 1 PackageInfo flag: return information about
            // activities in the package in activities.
            PackageInfo pi = pm.getPackageInfo(ctx.getPackageName(), PackageManager.GET_ACTIVITIES);
            if (pi != null) {
                // public String versionName The version name of this package,
                // as specified by the <manifest> tag's versionName attribute.
                put(os, VERSION_NAME, pi.versionName == null ? "not set" : pi.versionName);
                // public int versionCode The version number of this package,
                // as specified by the <manifest> tag's versionCode attribute.
                put(os, VERSION_CODE, String.valueOf(pi.versionCode));
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Error while collect package info", e);
        }
        // 使用反射来收集设备信息.在Build类中包含各种设备信息,
        // 例如: 系统版本号,设备生产商 等帮助调试程序的有用信息
        // 返回 Field 对象的一个数组，这些对象反映此 Class 对象所表示的类或接口所声明的所有字段
        Field[] fields = Build.class.getDeclaredFields();
        for (Field field : fields) {
            try {
                // setAccessible(boolean flag)
                // 将此对象的 accessible 标志设置为指示的布尔值。
                // 通过设置Accessible属性为true,才能对私有变量进行访问，不然会得到一个IllegalAccessException的异常
                field.setAccessible(true);
                put(os, field.getName(), field.get(null).toString());
            } catch (Exception e) {
                Log.e(TAG, "Error while collect crash info", e);
            }
        }
    }

    /**
     * 写入错误信息到
     *
     * @param ex 需要保存的错误信息
     */
    private void writeCrashInfo(OutputStream os, Throwable ex) {
        Writer info = new StringWriter();
        PrintWriter printWriter = new PrintWriter(info);
        // printStackTrace(PrintWriter s)
        // 将此 throwable 及其追踪输出到指定的 PrintWriter
        ex.printStackTrace(printWriter);

        // getCause() 返回此 throwable 的 cause；如果 cause 不存在或未知，则返回 null。
        Throwable cause = ex.getCause();
        while (cause != null) {
            cause.printStackTrace(printWriter);
            cause = cause.getCause();
        }

        // toString() 以字符串的形式返回该缓冲区的当前值。
        String result = info.toString();
        printWriter.close();
        put(os, STACK_TRACE, result);
    }


    /**
     * 写入key-value 到文件中
     *
     * @param os    输出流
     * @param key   键
     * @param value 值
     */
    protected void put(OutputStream os, String key, String value) {
        try {
            os.write(String.format("%s%s%s\n", key, PAIR_SEPARATOR, value).getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
