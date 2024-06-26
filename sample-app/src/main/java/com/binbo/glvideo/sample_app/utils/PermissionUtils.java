package com.binbo.glvideo.sample_app.utils;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.AppOpsManagerCompat;
import androidx.core.content.ContextCompat;

import com.binbo.glvideo.sample_app.utils.rom.HuaweiUtils;
import com.binbo.glvideo.sample_app.utils.rom.MeizuUtils;
import com.binbo.glvideo.sample_app.utils.rom.MiuiUtils;
import com.binbo.glvideo.sample_app.utils.rom.OppoUtils;
import com.binbo.glvideo.sample_app.utils.rom.QikuUtils;
import com.binbo.glvideo.sample_app.utils.rom.RomUtils;

import java.util.List;


public class PermissionUtils {

    public static final String TAG = "PermissionUtils";

    /**
     * 系统层的权限判断
     *
     * @param context     上下文
     * @param permissions 申请的权限 Manifest.permission.READ_CONTACTS
     * @return 是否有权限 ：其中有一个获取不了就是失败了
     */
    public static boolean hasPermission(@NonNull Context context, @NonNull List<String> permissions) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true;
        for (String permission : permissions) {
            try {
                String op = AppOpsManagerCompat.permissionToOp(permission);
                if (TextUtils.isEmpty(op)) continue;
                int result = AppOpsManagerCompat.noteOp(context, op, android.os.Process.myUid(), context.getPackageName());
                if (result == AppOpsManagerCompat.MODE_IGNORED) return false;
                AppOpsManager appOpsManager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
                String ops = AppOpsManager.permissionToOp(permission);
                int locationOp = appOpsManager.checkOp(ops, Binder.getCallingUid(), context.getPackageName());
                if (locationOp == AppOpsManager.MODE_IGNORED) return false;
                result = ContextCompat.checkSelfPermission(context, permission);
                if (result != PackageManager.PERMISSION_GRANTED) return false;
            } catch (Exception ex) {
                Log.e(TAG, "[hasPermission] error ", ex);
            }
        }
        return true;
    }

    /**
     * 跳转到权限设置界面
     *
     * @param context
     */
    public static void toPermissionSetting(Context context) throws NoSuchFieldException, IllegalAccessException {
        if (Build.VERSION.SDK_INT < 23) {
            if (RomUtils.checkIsMiuiRom()) {
                MiuiUtils.applyMiuiPermission(context);
            } else if (RomUtils.checkIsMeizuRom()) {
                MeizuUtils.applyPermission(context);
            } else if (RomUtils.checkIsHuaweiRom()) {
                HuaweiUtils.applyPermission(context);
            } else if (RomUtils.checkIs360Rom()) {
                QikuUtils.applyPermission(context);
            } else if (RomUtils.checkIsOppoRom()) {
                OppoUtils.applyOppoPermission(context);
            } else {
                RomUtils.getAppDetailSettingIntent(context);
            }
        } else {
            if (RomUtils.checkIsMeizuRom()) {
                MeizuUtils.applyPermission(context);
            } else {
                if (RomUtils.checkIsOppoRom() || RomUtils.checkIsVivoRom()
                        || RomUtils.checkIsHuaweiRom() || RomUtils.checkIsSamsunRom()) {
                    RomUtils.getAppDetailSettingIntent(context);
                } else if (RomUtils.checkIsMiuiRom()) {
                    MiuiUtils.toPermisstionSetting(context);
                } else {
                    RomUtils.commonROMPermissionApplyInternal(context);
                }
            }
        }
    }
}
