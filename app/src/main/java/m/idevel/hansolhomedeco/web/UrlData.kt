package m.idevel.hansolhomedeco.web

import android.content.Context
import m.idevel.hansolhomedeco.BuildConfig
import m.idevel.hansolhomedeco.utils.DLog
import m.idevel.hansolhomedeco.utils.SharedPreferencesUtil.getString
import m.idevel.hansolhomedeco.utils.SharedPreferencesUtil

/**
 * The UrlData Class.
 *
 * @author : jjbae
 */
object UrlData {
    const val PERMISSIONS_URL = "https://www.hansolhomedecolasola.com/app/"
    const val NORMAL_SERVER_URL = "https://www.hansolhomedecolasola.com/"

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