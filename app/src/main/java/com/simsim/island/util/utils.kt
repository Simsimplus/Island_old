package com.simsim.island.util

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import com.google.gson.Gson
import com.simsim.island.model.*
import java.time.Duration
import java.time.LocalDateTime
import kotlin.math.abs


const val TARGET_THREAD = "thread"
const val TARGET_SECTION = "section"
const val SWIPE_UP = "swipe_up_key"
const val SWIPE_DOWN = "swipe_down_key"
const val SWIPE_LEFT = "swipe_left_key"
const val SWIPE_RIGHT = "swipe_right_key"
val sections = listOf(
    Section(0,"综合线",true,"时间线","https://adnmb3.com/Forum/timeline/id/1",""),
    Section(1,"岛风线",true,"时间线","https://adnmb3.com/Forum/timeline/id/2",""),
    Section(2,"游戏线",true,"时间线","https://adnmb3.com/Forum/timeline/id/3",""),
    Section(3,"亚文化线",true,"时间线","https://adnmb3.com/Forum/timeline/id/4",""),
    Section(4,"综合版1",true,"综合","https://adnmb3.com/f/%E7%BB%BC%E5%90%88%E7%89%881","4"),
    Section(5,"速报2New!",true,"综合","https://adnmb3.com/f/%E9%80%9F%E6%8A%A52","121"),
    Section(6,"欢乐恶搞",true,"综合","https://adnmb3.com/f/%E6%AC%A2%E4%B9%90%E6%81%B6%E6%90%9E","20"),
    Section(7,"询问3",true,"综合","https://adnmb3.com/f/%E8%AF%A2%E9%97%AE3","114"),
    Section(8,"姐妹1(淑女)",true,"综合","https://adnmb3.com/f/%E5%A7%90%E5%A6%B91","98"),
    Section(9,"女装(时尚)",true,"综合","https://adnmb3.com/f/%E5%A5%B3%E8%A3%85","97"),
    Section(10,"买买买(剁手)",true,"综合","https://adnmb3.com/f/%E4%B9%B0%E4%B9%B0%E4%B9%B0","106"),
    Section(11,"数码(装机)",true,"综合","https://adnmb3.com/f/%E6%95%B0%E7%A0%81","75"),
    Section(12,"技术(码农)",true,"综合","https://adnmb3.com/f/%E6%8A%80%E6%9C%AF%E5%AE%85","30"),
    Section(13,"科学(理学)",true,"综合","https://adnmb3.com/f/%E7%A7%91%E5%AD%A6","15"),
    Section(14,"体育",true,"综合","https://adnmb3.com/f/%E4%BD%93%E8%82%B2","33"),
    Section(15,"军武",true,"综合","https://adnmb3.com/f/%E5%86%9B%E6%AD%A6","37"),
    Section(16,"电影/电视",true,"综合","https://adnmb3.com/f/%E5%BD%B1%E8%A7%86","31"),
    Section(17,"日记(树洞)",true,"综合","https://adnmb3.com/f/%E6%97%A5%E8%AE%B0","89"),
    Section(18,"艺人",true,"综合","https://adnmb3.com/f/%E8%89%BA%E4%BA%BA","100"),
    Section(19,"围炉",true,"岛风","https://adnmb3.com/f/%E5%9B%B4%E7%82%89","120"),
    Section(20,"跑团",true,"岛风","https://adnmb3.com/f/%E8%B7%91%E5%9B%A2","111"),
    Section(21,"故事(小说)",true,"岛风","https://adnmb3.com/f/%E5%B0%8F%E8%AF%B4","19"),
    Section(22,"都市怪谈(灵异)",true,"岛风","https://adnmb3.com/f/%E9%83%BD%E5%B8%82%E6%80%AA%E8%B0%88","81"),
    Section(23,"脑洞(推理)",true,"岛风","https://adnmb3.com/f/%E6%8E%A8%E7%90%86","11"),
    Section(24,"占星(卜卦)New!",true,"岛风","https://adnmb3.com/f/%E5%8D%A0%E6%98%9F","126"),
    Section(25,"喵版(主子)",true,"岛风","https://adnmb3.com/f/%E7%8C%AB%E7%89%88","40"),
    Section(26,"料理(美食)",true,"岛风","https://adnmb3.com/f/%E6%96%99%E7%90%86","32"),
    Section(27,"宠物",true,"岛风","https://adnmb3.com/f/%E5%AE%A0%E7%89%A9","118"),
    Section(28,"学业(校园)",true,"岛风","https://adnmb3.com/f/%E8%80%83%E8%AF%95","56"),
    Section(29,"社畜",true,"岛风","https://adnmb3.com/f/%E7%A4%BE%E7%95%9C","110"),
    Section(30,"育儿",true,"岛风","https://adnmb3.com/f/%E8%82%B2%E5%84%BF","113"),
    Section(31,"摄影2",true,"岛风","https://adnmb3.com/f/%E6%91%84%E5%BD%B12","115"),
    Section(32,"旅行New!",true,"岛风","https://adnmb3.com/f/%E6%97%85%E8%A1%8C","125"),
    Section(33,"文学(推书)",true,"岛风","https://adnmb3.com/f/%E6%96%87%E5%AD%A6","103"),
    Section(34,"音乐(推歌)",true,"岛风","https://adnmb3.com/f/%E9%9F%B3%E4%B9%90","35"),
    Section(35,"游戏综合版",true,"游戏","https://adnmb3.com/f/%E6%B8%B8%E6%88%8F","2"),
    Section(36,"手游",true,"游戏","https://adnmb3.com/f/%E6%89%8B%E6%B8%B8","3"),
    Section(37,"Steam",true,"游戏","https://adnmb3.com/f/Steam","107"),
    Section(38,"任天堂NS",true,"游戏","https://adnmb3.com/f/%E4%BB%BB%E5%A4%A9%E5%A0%82","25"),
    Section(39,"LOL",true,"游戏","https://adnmb3.com/f/LOL","22"),
    Section(40,"暴雪游戏",true,"游戏","https://adnmb3.com/f/%E6%9A%B4%E9%9B%AA%E6%B8%B8%E6%88%8F","23"),
    Section(41,"SE(FF14)",true,"游戏","https://adnmb3.com/f/SE","124"),
    Section(42,"DOTA&自走棋",true,"游戏","https://adnmb3.com/f/DOTA","70"),
    Section(43,"DNF",true,"游戏","https://adnmb3.com/f/DNF","72"),
    Section(44,"微软(XBOX)New!",true,"游戏","https://adnmb3.com/f/%E5%BE%AE%E8%BD%AF","108"),
    Section(45,"索尼",true,"游戏","https://adnmb3.com/f/%E7%B4%A2%E5%B0%BC","24"),
    Section(46,"Minecraft",true,"游戏","https://adnmb3.com/f/Minecraft","10"),
    Section(47,"怪物猎人",true,"游戏","https://adnmb3.com/f/%E6%80%AA%E7%89%A9%E7%8C%8E%E4%BA%BA","28"),
    Section(48,"彩虹六号",true,"游戏","https://adnmb3.com/f/%E5%BD%A9%E8%99%B9%E5%85%AD%E5%8F%B7","119"),
    Section(49,"精灵宝可梦",true,"游戏","https://adnmb3.com/f/%E5%8F%A3%E8%A2%8B%E5%A6%96%E6%80%AA","38"),
    Section(50,"EVE(Old!)",true,"游戏","https://adnmb3.com/f/EVE","73"),
    Section(51,"战争游戏(WOT)",true,"游戏","https://adnmb3.com/f/WOT","51"),
    Section(52,"战争雷霆",true,"游戏","https://adnmb3.com/f/%E6%88%98%E4%BA%89%E9%9B%B7%E9%9C%86","86"),
    Section(53,"卡牌桌游",true,"游戏","https://adnmb3.com/f/%E5%8D%A1%E7%89%8C%E6%A1%8C%E6%B8%B8","45"),
    Section(54,"音乐游戏",true,"游戏","https://adnmb3.com/f/MUG","34"),
    Section(55,"AC大逃杀",true,"游戏","https://adnmb3.com/f/AC%E5%A4%A7%E9%80%83%E6%9D%80","29"),
    Section(56,"动画",true,"亚文化","https://adnmb3.com/f/%E5%8A%A8%E7%94%BB","14"),
    Section(57,"漫画",true,"亚文化","https://adnmb3.com/f/%E6%BC%AB%E7%94%BB","12"),
    Section(58,"创意(涂鸦)",true,"亚文化","https://adnmb3.com/f/%E5%88%9B%E6%84%8F","17"),
    Section(59,"主播(UP)",true,"亚文化","https://adnmb3.com/f/%E4%B8%BB%E6%92%AD","116"),
    Section(60,"特摄",true,"亚文化","https://adnmb3.com/f/%E7%89%B9%E6%91%84","9"),
    Section(61,"模型(手办)",true,"亚文化","https://adnmb3.com/f/%E6%A8%A1%E5%9E%8B","39"),
    Section(62,"眼科(Cosplay)",true,"亚文化","https://adnmb3.com/f/COSPLAY","13"),
    Section(63,"声优",true,"亚文化","https://adnmb3.com/f/%E5%A3%B0%E4%BC%98","55"),
    Section(64,"偶像",true,"亚文化","https://adnmb3.com/f/%E5%81%B6%E5%83%8F","16"),
    Section(65,"虚拟偶像(LL)",true,"亚文化","https://adnmb3.com/f/%E8%99%9A%E6%8B%9F%E5%81%B6%E5%83%8F","101"),
    Section(66,"美漫(小马)",true,"亚文化","https://adnmb3.com/f/%E7%BE%8E%E6%BC%AB","90"),
    Section(67,"国漫",true,"亚文化","https://adnmb3.com/f/%E5%9B%BD%E6%BC%AB","99"),
    Section(68,"轻小说",true,"亚文化","https://adnmb3.com/f/%E8%BD%BB%E5%B0%8F%E8%AF%B4","87"),
    Section(69,"东方Project",true,"亚文化","https://adnmb3.com/f/%E4%B8%9C%E6%96%B9Project","5"),
    Section(70,"舰娘",true,"亚文化","https://adnmb3.com/f/%E8%88%B0%E5%A8%98","93"),
    Section(71,"VOCALOID",true,"亚文化","https://adnmb3.com/f/VOCALOID","6"),
    Section(72,"值班室",true,"管理","https://adnmb3.com/f/%E5%80%BC%E7%8F%AD%E5%AE%A4","18"),
    Section(73,"城墙",true,"管理","https://adnmb3.com/f/%E5%9F%8E%E5%A2%99","112"),
    Section(74,"技术支持",true,"管理","https://adnmb3.com/f/%E6%8A%80%E6%9C%AF%E6%94%AF%E6%8C%81","117"),
    Section(75,"圈内(版务讨论)",true,"管理","https://adnmb3.com/f/%E5%9C%88%E5%86%85","96"),

)

const val islandUrl = "https://adnmb3.com"
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

fun PoThread.toBasicThread(): ReplyThread = ReplyThread(
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
    timelineActualSection = this.timelineActualSection
)

fun PoThread.toSavedPoThread(): SavedPoThread = SavedPoThread(
    threadId = this.threadId,
    pageIndex = this.pageIndex,
    isManager = this.isManager,
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
    collectTime = this.collectTime,
    timelineActualSection = this.timelineActualSection
)

fun ReplyThread.toSavedReplyThread(): SavedReplyThread = SavedReplyThread(
    replyThreadId = this.replyThreadId,
    poThreadId = this.poThreadId,
    title = this.title,
    name = this.name,
    link = this.link,
    time = this.time,
    uid = this.uid,
    imageUrl = this.imageUrl,
    content = this.content,
    isManager = this.isManager,
    isPo = this.isPo,
    commentsNumber = this.commentsNumber,
    section = this.section,
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
            Log.e(LOG_TAG,"[OnSwipeListener] on touch consumed:$result")
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
    companion object {
        private const val CLICK_DRAG_TOLERANCE = 10F
    }

    private val gestureDetector = GestureDetector(context, GestureListener())

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        return if (gestureDetector.onTouchEvent(event)) {
            return true
        } else {
            Log.e(LOG_TAG, "detect fab is dragged or not")
            onDrag(v, event)
        }
    }

    private fun onDrag(view: View, event: MotionEvent): Boolean {
        var downRawX = 0F
        var downRawY = 0F
        var dX: Float = 0F
        var dY: Float = 0F
        val layoutParams = view.layoutParams as ViewGroup.MarginLayoutParams
        return when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                Log.e(LOG_TAG, "fab touch down")
                downRawX = event.rawX
                downRawY = event.rawY
                dX = view.x - downRawX
                dY = view.y - downRawY
                true
            }
            MotionEvent.ACTION_MOVE -> {
                Log.e(LOG_TAG, "fab touch move")
                val viewHeight = view.height
                val viewWidth = view.width
                val viewParent = view.parent as View
                val parentHeight = viewParent.height
                val parentWidth = viewParent.width
                val newX = (event.x + dX)
                    .coerceIn((layoutParams.leftMargin).toFloat()..(parentWidth - viewWidth - layoutParams.rightMargin).toFloat())
                val newY = (event.y + dY)
                    .coerceIn((layoutParams.topMargin).toFloat()..(parentHeight - viewHeight - layoutParams.bottomMargin).toFloat())
                view.animate().x(newX).y(newY).setDuration(0).start()
                true
            }
            MotionEvent.ACTION_UP -> {
                Log.e(LOG_TAG, "fab touch up")
                val upRawX = event.rawX
                val upRawY = event.rawY
                val upDX = upRawX - downRawX
                val upDY = upRawY - downRawY
                !(abs(upDX) < CLICK_DRAG_TOLERANCE && abs(upDY) < CLICK_DRAG_TOLERANCE)
            }

            else -> false
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


fun View.toggleVisibility() {
    when (this.visibility) {
        View.VISIBLE -> {
            this.visibility = View.GONE
        }
        View.GONE -> {
            this.visibility = View.VISIBLE
        }
        else -> {

        }
    }
}

fun String.extractCookie(): String? = try {
    Gson().fromJson(this, Cookie::class.java).cookie
} catch (e: Exception) {
    Log.e(LOG_TAG, "parse json exception:${e.stackTraceToString()}")
    null
}

fun String.ellipsis(remainLength: Int = 20) = if (this.length <= remainLength) this else
    this.replaceRange((remainLength until this.length), "…")

fun <T> Array<out T>.indexOfOrFirst(element: T): Int {
    val index = this.indexOf(element)
    return if (index != -1) index else 0
}