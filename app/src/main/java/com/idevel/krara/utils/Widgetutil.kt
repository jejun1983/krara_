package com.idevel.krara.utils

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.idevel.krara.activity.MainActivity


fun getIconBadgeIntent(context: Context, notiCnt: Int): PendingIntent {
    val badgeIntent = Intent("android.intent.action.BADGE_COUNT_UPDATE")
    badgeIntent.putExtra("badge_count", notiCnt)
    badgeIntent.putExtra("badge_count_package_name", context.packageName)
    badgeIntent.putExtra("badge_count_class_name", MainActivity::class.java.name)

    val intentFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT
    } else {
        PendingIntent.FLAG_UPDATE_CURRENT
    }

    return PendingIntent.getBroadcast(context, 0 /* Request code */, badgeIntent, intentFlag)
}

fun setIconBadge(context: Context, notiCnt: Int) {
    val badgeIntent = Intent("android.intent.action.BADGE_COUNT_UPDATE")
    badgeIntent.putExtra("badge_count", notiCnt)
    badgeIntent.putExtra("badge_count_package_name", context.packageName)
    badgeIntent.putExtra("badge_count_class_name", MainActivity::class.java.name)

    context.sendBroadcast(badgeIntent)
}