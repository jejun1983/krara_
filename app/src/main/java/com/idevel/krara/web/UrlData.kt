package com.idevel.krara.web

import android.content.Context
import com.idevel.krara.BuildConfig
import com.idevel.krara.utils.DLog
import com.idevel.krara.utils.SharedPreferencesUtil.getString
import com.idevel.krara.utils.SharedPreferencesUtil

/**
 * The UrlData Class.
 *
 * @author : jjbae
 */
object UrlData {
    const val PERMISSIONS_URL = "https://app.krara.co.kr/app/infoAuth.php"
    const val NORMAL_SERVER_URL = "https://app.krara.co.kr/main/index.php"

    fun getMainUrl(context: Context?): String? {
        var url: String? = ""
        url = if (BuildConfig.DEBUG) {
            getString(context, SharedPreferencesUtil.Cmd.SETTING_URL)
        } else {
            NORMAL_SERVER_URL
        }

        if (url.isNullOrEmpty()) {
            url = NORMAL_SERVER_URL
        }

        DLog.e("bjj UrlData :: getUploadUrl $url")

        return url
    }

    fun getUploadUrl(context: Context?): String {
        var url = ""

//        if (BuildConfig.DEBUG) {
//            url = SharedPreferencesUtil.getString(context, SharedPreferencesUtil.Cmd.SETTING_URL);
//        } else {
//            // 상용
        url = "https://dev-fo.atomyn.kr/atomy/"
        //        }
        DLog.e("bjj UrlData :: getUploadUrl $url")

        return url
    }
}