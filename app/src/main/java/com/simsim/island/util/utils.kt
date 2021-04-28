package com.simsim.island.util

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.simsim.island.model.BasicThread
import com.simsim.island.model.Cookie
import com.simsim.island.model.PoThread
import java.time.Duration
import java.time.LocalDateTime
import kotlin.math.abs


const val TARGET_THREAD="thread"
const val TARGET_SECTION="section"
const val SWIPE_UP="swipe_up_key"
const val SWIPE_DOWN="swipe_down_key"
const val SWIPE_LEFT="swipe_left_key"
const val SWIPE_RIGHT="swipe_right_key"

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
                            Log.e(LOG_TAG, "swipe right")
                            onSwipeRight()
                        } else {
                            Log.e(LOG_TAG, "swipe left")
                            onSwipeLeft()
                        }
                        result = true
                    }
                } else if (abs(diffY) > swipeThreshold && abs(velocityY) > swipeVelocityThreshold) {
                    if (diffY > 0) {
                        Log.e(LOG_TAG, "swipe down")
                        onSwipeBottom()
                    } else {
                        Log.e(LOG_TAG, "swipe up")
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
class OnSwipeAndDragListener(
    val context: Context,
    val onSwipeRight: () -> Unit,
    val onSwipeLeft: () -> Unit,
    val onSwipeTop: () -> Unit,
    val onSwipeBottom: () -> Unit
) : View.OnTouchListener {
    companion object{
        private const val CLICK_DRAG_TOLERANCE = 10F
    }
    private val gestureDetector = GestureDetector(context, GestureListener())

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        return if (gestureDetector.onTouchEvent(event)){
            return true
        }else{
            Log.e(LOG_TAG,"detect fab is dragged or not")
            onDrag(v,event)
        }
    }
    private fun onDrag(view: View, event: MotionEvent): Boolean{
        var downRawX=0F
        var downRawY=0F
        var dX: Float=0F
        var dY: Float=0F
        val layoutParams=view.layoutParams as ViewGroup.MarginLayoutParams
        return when(event.action){
            MotionEvent.ACTION_DOWN -> {
                Log.e(LOG_TAG,"fab touch down")
                downRawX=event.rawX
                downRawY=event.rawY
                dX=view.x-downRawX
                dY=view.y-downRawY
                true
            }
            MotionEvent.ACTION_MOVE->{
                Log.e(LOG_TAG,"fab touch move")
                val viewHeight=view.height
                val viewWidth=view.width
                val viewParent=view.parent as View
                val parentHeight=viewParent.height
                val parentWidth=viewParent.width
                val newX=(event.x+dX)
                    .coerceIn((layoutParams.leftMargin).toFloat()..(parentWidth - viewWidth - layoutParams.rightMargin).toFloat())
                val newY=(event.y+dY)
                    .coerceIn((layoutParams.topMargin).toFloat()..(parentHeight - viewHeight - layoutParams.bottomMargin).toFloat())
                view.animate().x(newX).y(newY).setDuration(0).start()
                true
            }
            MotionEvent.ACTION_UP->{
                Log.e(LOG_TAG,"fab touch up")
                val upRawX=event.rawX
                val upRawY=event.rawY
                val upDX=upRawX-downRawX
                val upDY=upRawY-downRawY
                !(abs(upDX)< CLICK_DRAG_TOLERANCE &&abs(upDY)< CLICK_DRAG_TOLERANCE)
            }

            else->false
        }
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
                            Log.e(LOG_TAG, "swipe right")
                            onSwipeRight()
                        } else {
                            Log.e(LOG_TAG, "swipe left")
                            onSwipeLeft()
                        }
                        result = true
                    }
                } else if (abs(diffY) > swipeThreshold && abs(velocityY) > swipeVelocityThreshold) {
                    if (diffY > 0) {
                        Log.e(LOG_TAG, "swipe down")
                        onSwipeBottom()
                    } else {
                        Log.e(LOG_TAG, "swipe up")
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

class MovableFloatingActionButton : FloatingActionButton,View.OnTouchListener {
    constructor(context: Context):super(context)
    constructor(context: Context,attrs: AttributeSet):super(context,attrs)
    constructor(context: Context,attrs: AttributeSet,defStyleAttr:Int):super(context,attrs,defStyleAttr)
    companion object{
        private const val CLICK_DRAG_TOLERANCE = 10F
    }
    init {
        setOnTouchListener(this)
    }
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(view: View, event: MotionEvent): Boolean {
        var downRawX=0F
        var downRawY=0F
        var dX: Float=0F
        var dY: Float=0F
        val layoutParams=view.layoutParams as ViewGroup.MarginLayoutParams
        return when(event.action){
            MotionEvent.ACTION_DOWN -> {
                Log.e(LOG_TAG,"fab touch down")
                downRawX=event.rawX
                downRawY=event.rawY
                dX=view.x-downRawX
                dY=view.y-downRawY
                true
            }
            MotionEvent.ACTION_MOVE->{
                Log.e(LOG_TAG,"fab touch move")
                val viewHeight=view.height
                val viewWidth=view.width
                val viewParent=view.parent as View
                val parentHeight=viewParent.height
                val parentWidth=viewParent.width
                val newX=(event.x+dX).coerceIn((parentWidth - viewWidth - layoutParams.rightMargin).toFloat()..(layoutParams.leftMargin).toFloat())
                val newY=(event.y+dY).coerceIn((parentHeight - viewHeight - layoutParams.bottomMargin).toFloat()..(layoutParams.topMargin).toFloat())
                view.animate().x(newX).y(newY).setDuration(0).start()
                true
            }
            MotionEvent.ACTION_UP->{
                Log.e(LOG_TAG,"fab touch up")
                val upRawX=event.rawX
                val upRawY=event.rawY
                val upDX=upRawX-downRawX
                val upDY=upRawY-downRawY
                if(abs(upDX)<CLICK_DRAG_TOLERANCE &&abs(upDY)<CLICK_DRAG_TOLERANCE){
                    performClick()
                }else{
                    true
                }
            }

            else->super.onTouchEvent(event)
        }
    }



}
fun View.toggleVisibility(){
    when(this.visibility){
        View.VISIBLE -> {
            this.visibility = View.GONE
        }
        View.GONE -> {
            this.visibility = View.VISIBLE
        }
        else->{

        }
    }
}
fun String.extractCookie():String?=try{
    Gson().fromJson(this, Cookie::class.java).cookie
}catch (e: Exception){
    Log.e(LOG_TAG, "parse json exception:${e.stackTraceToString()}")
    null
}
fun String.ellipsis(remainLength: Int = 20)=if (this.length<=remainLength) this else
    this.replaceRange((remainLength until this.length), "…")
fun <T> Array<out T>.indexOfOrFirst(element: T): Int{
    val index=this.indexOf(element)
    return if (index!=-1) index else 0
}