package m.idevel.hansolhomedeco.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import m.idevel.hansolhomedeco.interfaces.IDataSaverListener
import m.idevel.hansolhomedeco.utils.DLog

/**
 * DataSaverChangeReceiver
 * @company : medialog
 * @author : jjbae
 * datasaver 변경상태를 브로드캐스트 리시버로 받는다.
 **/

class DataSaverChangeReceiver : BroadcastReceiver() {

    var mListener: IDataSaverListener? = null

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) return

        if (ConnectivityManager.ACTION_RESTRICT_BACKGROUND_CHANGED == intent?.action) {
            mListener?.onDataSaverChanged()
        }

        DLog.e("bjj onReceive DataSaverChangeReceiver " + intent?.action)
    }

    fun setListener(listener: IDataSaverListener) {
        mListener = listener
    }
}