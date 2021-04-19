package com.simsim.island.util

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.View
import com.simsim.island.model.BasicThread
import com.simsim.island.model.PoThread
import java.time.Duration
import java.time.LocalDateTime
import kotlin.math.abs

const val islandUrl="https://adnmb3.com"
val threadIdPattern = "\\d+".toRegex()
const val referenceStringSpliterator = "\n"
val LOG_TAG = "Simsim "
fun handleThreadId(id: String): String {
    val regex = "I?D?:?([\\s0-9a-zA-Z]+)".toRegex()
    return regex.matchEntire(id)?.groupValues?.get(1)?.trim() ?: ""
}

fun handleThreadTime(time: LocalDateTime): String {
    val now = LocalDateTime.now()
    val duration = Duration.between(time, now)
    val minutes = duration.toMinutes()
    when {
        minutes in 0..59 -> {
            return "${duration.toMinutes()}分前"
        }
        minutes in 60 until 60 * 24 -> {
            return "${duration.toHours()}时前"
        }
        minutes in 60 * 24 until 60 * 24 * 2 -> {
            return "昨天"
        }
        minutes in 60 * 24 * 2 until 60 * 24 * 3 -> {
            return "前天"
        }
        minutes in 60 * 24 * 3 until 60 * 24 * 4 -> {
            return "大前天"
        }
        minutes < 0 -> {
            return "未来"
        }
        else -> {
            return if (time.year == LocalDateTime.now().year) "%d-%d".format(
                time.month.value,
                time.dayOfMonth
            ) else "%d-%d-%d".format(time.year, time.month.value, time.dayOfMonth)
        }
    }
}

fun String.firstNumber(): Int =
    ((".*?(\\d+).*?".toRegex().matchEntire(this)?.groupValues?.get(1)?.toInt() ?: 0) + 0)

fun String.findPageNumber(): String =
    ".*page=(\\d+).*".toRegex().matchEntire(this)?.groupValues?.get(1) ?: "99"

fun String.removeQueryTail(): String = this.replace("\\?r=\\d+".toRegex(), "")

fun PoThread.toBasicThread(): BasicThread = BasicThread(
    isManager = this.isManager,
    replyThreadId = this.threadId,
    poThreadId = this.threadId,
    title = this.title,
    name = this.name,
    link = this.link,
    time = this.time,
    uid = this.uid,
    imageUrl = this.imageUrl,
    content = this.content,
    isPo = this.isPo,
    commentsNumber = this.commentsNumber,
    section = this.section,
    references = this.references,
    timelineActualSection = this.timelineActualSection
)

class OnSwipeListener(
    val context: Context,
    val onSwipeRight: () -> Unit,
    val onSwipeLeft: () -> Unit,
    val onSwipeTop: () -> Unit,
    val onSwipeBottom: () -> Unit
) : View.OnTouchListener {
    private val gestureDetector = GestureDetector(context, GestureListener())

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        return gestureDetector.onTouchEvent(event)
    }

    inner class GestureListener : SimpleOnGestureListener() {
        private val swipeThreshold = 50
        private val swipeVelocityThreshold = 50
        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent?,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            var result = false
            try {
                val diffY = e2!!.y - e1!!.y
                val diffX = e2.x - e1.x
                if (abs(diffX) > abs(diffY)) {
                    if (abs(diffX) > swipeThreshold && abs(velocityX) > swipeVelocityThreshold) {
                        if (diffX > 0) {
                            Log.e(LOG_TAG,"swipe right")
                            onSwipeRight()
                        } else {
                            Log.e(LOG_TAG,"swipe left")
                            onSwipeLeft()
                        }
                        result = true
                    }
                } else if (abs(diffY) > swipeThreshold && abs(velocityY) > swipeVelocityThreshold) {
                    if (diffY > 0) {
                        onSwipeBottom()
                    } else {
                        onSwipeTop()
                    }
                    result = true
                }
            } catch (exception: Exception) {
                exception.printStackTrace()
            }
            return result
        }

    }


}
