package com.idevel.krara.web

import android.content.Context
import android.webkit.JavascriptInterface
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.idevel.krara.utils.DLog
import com.idevel.krara.web.constdata.*
import com.idevel.krara.web.interfaces.IWebBridgeApi

/**
 * web 과의 연동을 위한 interface Class.
 *
 */
class MyWebInterface(private val mContext: Context, private val api: IWebBridgeApi) {
    companion object {
        const val webInvoker = "NativeInvoker"
        const val NAME = "idevel_app"
    }

    private fun gson(): Gson {
        return GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create()
    }

    @JavascriptInterface
    fun removeSplash() {
        DLog.e("bjj data: removeSplash ")
        api.removeSplash()
    }

    @JavascriptInterface
    fun getPushRegId() {
        DLog.e("bjj data: getPushRegId ")
        api.getPushRegId()
    }

    @JavascriptInterface
    fun restartApp() {
        DLog.e("bjj data: restartApp ")
        api.restartApp()
    }

    @JavascriptInterface
    fun finishApp() {
        DLog.e("bjj data: finishApp ")
        api.finishApp()
    }

    @JavascriptInterface
    fun getAppVersion() {
        DLog.e("bjj data: getAppVersion ")
        api.getAppVersion()
    }

    @JavascriptInterface
    fun requestCallPhone(jsonData: String) {
        val data = gson().fromJson(jsonData, RequestCallPhoneInfo::class.java)
        DLog.e("bjj data: requestCallPhone " + data)
        api.requestCallPhone(data)
    }

    @JavascriptInterface
    fun requestExternalWeb(jsonData: String) {
        val data = gson().fromJson(jsonData, RequestExternalWebInfo::class.java)
        DLog.e("bjj data: requestExternalWeb " + data)
        api.requestExternalWeb(data)
    }

    @JavascriptInterface
    fun openSharePopup(url: String) {
//        val data = gson().fromJson(jsonData, OpenSharePopupInfo::class.java)
        DLog.e("bjj data: openSharePopup " + url)
        api.openSharePopup(url)
    }

    @JavascriptInterface
    fun pageClearHistory() {
        DLog.e("bjj data: pageClearHistory ")
        api.pageClearHistory()
    }

    @JavascriptInterface
    fun getGpsInfo() {
        DLog.e("bjj data: getLocation ")
        api.getGpsInfo()
    }

    @JavascriptInterface
    fun readyOneStoreBilling() {
        DLog.e("bjj data: readyOneStoreBilling ")
        api.readyOneStoreBilling()
    }

    @JavascriptInterface
    fun requestBuyProduct(jsonData: String) {
        val data = gson().fromJson(jsonData, RequestBuyProductInfo::class.java)
        DLog.e("bjj data: requestBuyProduct " + jsonData)
        api.requestBuyProduct(data)
    }

    @JavascriptInterface
    fun openCamera(type: String, param: String) {
        DLog.e("bjj data: openCamera " + type + " ^ " + param)
        api.openCamera(type, param)
    }

    @JavascriptInterface
    fun openGallery(type: String, param: String) {
        DLog.e("bjj data: openGallery " + type + " ^ " + param)
        api.openGallery(type, param)
    }

    @JavascriptInterface
    fun setPushVibrate(isBool: Boolean) {
        DLog.e("bjj data: setPushVibrate ")
        api.setPushVibrate(isBool)
    }

    @JavascriptInterface
    fun setPushBeep(isBool: Boolean) {
        DLog.e("bjj data: setPushBeep ")
        api.setPushBeep(isBool)
    }


    @JavascriptInterface
    fun setAutoLogin(isAuto: Boolean, token: String) {
        DLog.e("bjj data: setAutoLogin " + isAuto)
        api.setAutoLogin(isAuto, token)
    }

    @JavascriptInterface
    fun getAutoLogin() {
        DLog.e("bjj data: getAutoLogin ")
        api.getAutoLogin()
    }

    @JavascriptInterface
    fun setAccount(id: String, pw: String) {
        DLog.e("bjj data: setAccount " + id + " ^ " + pw)
        api.setAccount(id, pw)
    }

    @JavascriptInterface
    fun getAccount() {
        DLog.e("bjj data: getAccount ")
        api.getAccount()
    }

    @JavascriptInterface
    fun downloadFile(fileURL: String, fileName: String, type: String) {
        DLog.e("bjj data: downloadFile " + fileURL + " ^ " + fileName)
        api.downloadFile(fileURL, fileName, type)
    }

    @JavascriptInterface
    fun callBiometrics() {
        DLog.e("bjj data: callBiometrics ")
        api.callBiometrics()
    }

    @JavascriptInterface
    fun callStt(locale: String) {
        DLog.e("bjj data: callStt " + locale)
        api.callStt(locale)
    }

    @JavascriptInterface
    fun openQR() {
        DLog.e("bjj data: call openQR ")
        api.openQR()
    }

    @JavascriptInterface
    fun isApp() {
        DLog.e("bjj data: call isApp ")
        api.isApp()
    }
}