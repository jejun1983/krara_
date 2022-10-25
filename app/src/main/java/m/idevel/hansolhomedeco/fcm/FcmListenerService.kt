package m.idevel.hansolhomedeco.fcm

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import m.idevel.hansolhomedeco.activity.MainActivity
import m.idevel.hansolhomedeco.activity.PushPopupActivity
import m.idevel.hansolhomedeco.fcm.PushPreferences.PUSH_DATA
import m.idevel.hansolhomedeco.notification.PushNotification
import m.idevel.hansolhomedeco.utils.DLog
import m.idevel.hansolhomedeco.utils.SharedPreferencesUtil
import m.idevel.hansolhomedeco.utils.isAppRunning
import m.idevel.hansolhomedeco.utils.isForeground

/**
 * Created by jjbae on 2016-02-25.
 */
class FcmListenerService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)

        DLog.d("bjj FcmListenerService onNewToken token: $token")

        SharedPreferencesUtil.setString(this, SharedPreferencesUtil.Cmd.PUSH_REG_ID, token)
    }

    @Override
    override fun onMessageReceived(message: RemoteMessage) {
        val from = message.from

        DLog.v("bjj FcmListenerService push from : "
                + message + " ^ "
                + from)

        // intent로 넘길 push 데이터
        val data = message.data
        val bundle = Bundle()

        for (entry in data) {
            bundle.putString(entry.key, entry.value)
            DLog.v("bjj FcmListenerService push key : " + entry.key + " : " + entry.value)
        }

        DLog.e(
                "bjj FcmListenerService push : "
                        + isAppRunning(applicationContext) + " ^ "
                        + isForeground(applicationContext) + " ^ "
                        + applicationContext + " ^ "
                        + bundle
        )

        if (!isAppRunning(applicationContext)) {
            val myData = bundle.get("noti")
            val pushDataNotiParser: PushDataNotiParser = gson().fromJson(myData.toString(), PushDataNotiParser::class.java)

            if (pushDataNotiParser.noti_display == "show") {
                setNotification(pushDataNotiParser)
            }
        } else {
//            if (!isForeground(applicationContext)) {


            val myData = bundle.get("noti")
            val pushDataNotiParser: PushDataNotiParser = gson().fromJson(myData.toString(), PushDataNotiParser::class.java)

            if (pushDataNotiParser.noti_display == "show") {
                setNotification(pushDataNotiParser)
            }


//            } else {
////                Handler(Looper.getMainLooper()).postDelayed({
////                    Toast.makeText(this@FcmListenerService, bundle.get("popup").toString(), Toast.LENGTH_SHORT).show()
////                }, 0L)
//
//                // popup
//                val i = Intent(applicationContext, PushPopupActivity::class.java)
//                i.putExtra(PUSH_DATA, bundle)
//                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//
//                if (!isAppRunning(applicationContext)) {
//                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
//                }
//
//                startActivity(i)
//            }
        }
    }

    /**
     * Notification 설정
     */
    private fun setNotification(pushDataNotiParser: PushDataNotiParser) {
        val notiIntent = Intent(this, MainActivity::class.java)
        notiIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

//        {
//            "noti_target":"_webview",
//            "noti_link":"https:\/\/hansolhomedeco.wtest.biz\/mypage\/",
//            "noti_display":"show",
//            "noti_showtime":"2000",
//            "noti_message":"0708푸시 내용",
//            "noti_title":"0708 푸시 제목"
//        }

        DLog.e("bjj FcmListenerService notiIntent noti_target : " + pushDataNotiParser.noti_target)
        DLog.e("bjj FcmListenerService notiIntent noti_link : " + pushDataNotiParser.noti_link)
        DLog.e("bjj FcmListenerService notiIntent noti_display : " + pushDataNotiParser.noti_display)
        DLog.e("bjj FcmListenerService notiIntent noti_showtime : " + pushDataNotiParser.noti_showtime)
        DLog.e("bjj FcmListenerService notiIntent noti_message : " + pushDataNotiParser.noti_message)
        DLog.e("bjj FcmListenerService notiIntent noti_title : " + pushDataNotiParser.noti_title)
        DLog.e("bjj FcmListenerService notiIntent noti_img : " + pushDataNotiParser.noti_img)

        notiIntent.putExtra(PushPreferences.IS_NOTI, 0)

        notiIntent.putExtra(PushPreferences.PUSH_DATA_TITLE, pushDataNotiParser.noti_title)
        notiIntent.putExtra(PushPreferences.PUSH_DATA_MESSAGE, pushDataNotiParser.noti_message)
        notiIntent.putExtra(PushPreferences.PUSH_DATA_LINK_URL, pushDataNotiParser.noti_link)
        notiIntent.putExtra(PushPreferences.PUSH_DATA_LINK_TYPE, pushDataNotiParser.noti_target)
        notiIntent.putExtra(PushPreferences.PUSH_DATA_IMAGE, pushDataNotiParser.noti_img)

        Glide.with(this)
                .asBitmap()
                .load(pushDataNotiParser.noti_img)
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        DLog.e("bjj FcmListenerService setNotification ImageUrl : onResourceReady")
                        PushNotification.sendNotification(applicationContext, notiIntent,
                                pushDataNotiParser.noti_title, pushDataNotiParser.noti_message, resource)
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                        DLog.e("bjj FcmListenerService setNotification ImageUrl : onLoadCleared")
                    }

                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        DLog.e("bjj FcmListenerService setNotification ImageUrl : onLoadFailed")

                        PushNotification.sendNotification(applicationContext, notiIntent,
                                pushDataNotiParser.noti_title, pushDataNotiParser.noti_message)
                    }
                })
    }

    private fun gson(): Gson {
        return GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create()
    }
}