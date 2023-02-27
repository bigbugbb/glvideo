package com.binbo.glvideo.sample_app.utils

import android.content.Context
import android.text.TextUtils
import android.util.Log
import java.io.*

object FileUtil {
    var TAG = "FileUtil"

    fun copyAssets(context: Context, filename: String, assetDir: String, destPath: String?, copyFileBack: CopyFileBack?) {
        val workingPath = File(destPath)
        // if this directory does not exists, make one.
        workingPath.mkdirs()
        var input: InputStream? = null
        var output: OutputStream? = null
        try {
            val outFile = File(workingPath, filename)
            if (outFile.exists()) outFile.delete()
            input = if (TextUtils.isEmpty(assetDir)) context.assets.open(filename) else context.assets.open(assetDir + File.separator + filename)
            output = FileOutputStream(outFile)

            // Transfer bytes from in to out
            val buf = ByteArray(8192)
            var len: Int
            while (input.read(buf).also { len = it } > 0) {
                output.write(buf, 0, len)
            }
            if (copyFileBack != null) {
                Log.i(TAG, "copyAssets() copyFileBack.succ path = " + outFile.path)
                copyFileBack.success(outFile.path)
            }
        } catch (e: FileNotFoundException) {
            Log.i(TAG, "copyAssets() FileNotFoundException")
            copyFileBack?.fail()
            e.printStackTrace()
        } catch (e: IOException) {
            Log.i(TAG, "copyAssets() IOException")
            copyFileBack?.fail()
            e.printStackTrace()
        } catch (e: Exception) {
            Log.i(TAG, "copyAssets() Exception")
            copyFileBack?.fail()
        } finally {
            if (input != null) {
                try {
                    input.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            if (output != null) {
                try {
                    output.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }
}

interface CopyFileBack {
    fun success(path: String?)
    fun fail()
}
