package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

enum class PlayerChoice(val title: String, val description: String) {
    INTERNAL("المشغل المدمج المتطور (ExoPlayer)", "يدعم الإيماءات، تغيير الأبعاد، واختيار الصوت والترجمة"),
    VLC("مشغل VLC Player", "تشغيل عبر تطبيق VLC الخارجي"),
    MX_PLAYER("مشغل MX Player", "تشغيل عبر تطبيق MX Player الخارجي"),
    SYSTEM_CHOOSER("تطبيق خارجي آخر", "اختيار المشغل من قائمة التطبيقات المثبتة في الهاتف")
}

object ExternalPlayerHelper {

    fun launchPlayer(
        context: Context,
        streamUrl: String,
        channelName: String,
        playerChoice: PlayerChoice,
        onFallbackToInternal: () -> Unit
    ) {
        if (playerChoice == PlayerChoice.INTERNAL) {
            onFallbackToInternal()
            return
        }

        try {
            val uri = Uri.parse(streamUrl)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "video/*")
                putExtra("title", channelName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            when (playerChoice) {
                PlayerChoice.VLC -> {
                    intent.setPackage("org.videolan.vlc")
                    if (isPackageInstalled(context, "org.videolan.vlc")) {
                        context.startActivity(intent)
                    } else {
                        Toast.makeText(context, "تطبيق VLC غير مثبت، جاري التحويل للمشغل المدمج", Toast.LENGTH_LONG).show()
                        onFallbackToInternal()
                    }
                }
                PlayerChoice.MX_PLAYER -> {
                    val mxFree = "com.mxtech.videoplayer.ad"
                    val mxPro = "com.mxtech.videoplayer.pro"
                    val pkg = if (isPackageInstalled(context, mxPro)) mxPro else mxFree
                    if (isPackageInstalled(context, pkg)) {
                        intent.setPackage(pkg)
                        context.startActivity(intent)
                    } else {
                        Toast.makeText(context, "تطبيق MX Player غير مثبت، جاري التحويل للمشغل المدمج", Toast.LENGTH_LONG).show()
                        onFallbackToInternal()
                    }
                }
                PlayerChoice.SYSTEM_CHOOSER -> {
                    val chooser = Intent.createChooser(intent, "اختر مشغل الفيديو لـ $channelName").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(chooser)
                }
                else -> onFallbackToInternal()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "فشل فتح المشغل الخارجي: ${e.message}", Toast.LENGTH_SHORT).show()
            onFallbackToInternal()
        }
    }

    private fun isPackageInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: Exception) {
            false
        }
    }
}
