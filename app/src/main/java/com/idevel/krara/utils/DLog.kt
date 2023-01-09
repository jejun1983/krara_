package com.idevel.krara.utils

import android.util.Log

/**
 * The MLogger
 * @company : medialog
 * @author  : jjbae
 */
object DLog {
    private const val TAG = "jjbae_AOS"

    private fun getLogMsg(msg: Any): String {
        val stack = Throwable().fillInStackTrace()
        val trace = stack.stackTrace

        return "[${trace[2].fileName}>${trace[2].methodName}():${trace[2].lineNumber}]: $msg"
    }

    @JvmStatic
    fun i() {
        if (com.idevel.krara.BuildConfig.DEBUG || com.idevel.krara.MyApplication.instance.usReleaseApkDebug) {
            Log.i(TAG, getLogMsg(""))
        }
    }

    @JvmStatic
    fun i(msg: Any) {
        if (com.idevel.krara.BuildConfig.DEBUG || com.idevel.krara.MyApplication.instance.usReleaseApkDebug) {
            Log.i(TAG, getLogMsg(msg))
        }
    }

    @JvmStatic
    fun d() {
        if (com.idevel.krara.BuildConfig.DEBUG || com.idevel.krara.MyApplication.instance.usReleaseApkDebug) {
            Log.d(TAG, getLogMsg(""))
        }
    }

    @JvmStatic
    fun d(msg: Any) {
        if (com.idevel.krara.BuildConfig.DEBUG || com.idevel.krara.MyApplication.instance.usReleaseApkDebug) {
            Log.d(TAG, getLogMsg(msg))
        }
    }

    @JvmStatic
    fun w() {
        if (com.idevel.krara.BuildConfig.DEBUG || com.idevel.krara.MyApplication.instance.usReleaseApkDebug) {
            Log.w(TAG, getLogMsg(""))
        }
    }

    @JvmStatic
    fun w(msg: Any) {
        if (com.idevel.krara.BuildConfig.DEBUG || com.idevel.krara.MyApplication.instance.usReleaseApkDebug) {
            Log.w(TAG, getLogMsg(msg))
        }
    }

    @JvmStatic
    fun e() {
        if (com.idevel.krara.BuildConfig.DEBUG || com.idevel.krara.MyApplication.instance.usReleaseApkDebug) {
            Log.e(TAG, getLogMsg(""))
        }
    }

    @JvmStatic
    fun e(msg: Any) {
        if (com.idevel.krara.BuildConfig.DEBUG || com.idevel.krara.MyApplication.instance.usReleaseApkDebug) {
            Log.e(TAG, getLogMsg(msg))
        }
    }

    @JvmStatic
    fun d(tag: String, msg: Any) {
        if (com.idevel.krara.BuildConfig.DEBUG || com.idevel.krara.MyApplication.instance.usReleaseApkDebug) {
            Log.d(tag, getLogMsg(msg))
        }
    }

    @JvmStatic
    fun i(tag: String, msg: Any) {
        if (com.idevel.krara.BuildConfig.DEBUG || com.idevel.krara.MyApplication.instance.usReleaseApkDebug) {
            Log.i(tag, getLogMsg(msg))
        }
    }

    @JvmStatic
    fun w(tag: String, msg: Any) {
        if (com.idevel.krara.BuildConfig.DEBUG || com.idevel.krara.MyApplication.instance.usReleaseApkDebug) {
            Log.w(tag, getLogMsg(msg))
        }
    }

    @JvmStatic
    fun e(tag: String, msg: Any) {
        if (com.idevel.krara.BuildConfig.DEBUG || com.idevel.krara.MyApplication.instance.usReleaseApkDebug) {
            Log.e(tag, getLogMsg(msg))
        }
    }

    @JvmStatic
    fun v(msg: String) {
        if (com.idevel.krara.BuildConfig.DEBUG || com.idevel.krara.MyApplication.instance.usReleaseApkDebug) {
            Log.v(TAG, getLogMsg(msg))

        }
    }

    @JvmStatic
    fun v(TAG: String, msg: String) {
        if (com.idevel.krara.BuildConfig.DEBUG || com.idevel.krara.MyApplication.instance.usReleaseApkDebug) {
            Log.v(TAG, getLogMsg(msg))
        }
    }
}
