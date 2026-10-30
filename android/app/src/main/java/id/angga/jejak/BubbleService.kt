package id.angga.jejak

import android.animation.ValueAnimator
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.Outline
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.TextView
import kotlin.math.abs
import kotlin.math.min

/**
 * Bubble bulat yang melayang di atas aplikasi lain (mirip bubble Shazam).
 * Ketuk  -> kartu kecil untuk mencatat langkah.
 * Tahan  -> buka peta lengkap di aplikasi.
 * Geser  -> pindahkan bubble; lepas di dekat tanda ✕ di bawah untuk mematikan.
 */
class BubbleService : Service() {

    companion object {
        @Volatile
        var running = false
        private const val CHANNEL = "bubble"
        private const val ACTION_STOP = "id.angga.jejak.STOP"
        private const val ACCENT = 0xFF0E6B60.toInt()
        private const val MARK = 0xFFE2B23A.toInt()
    }

    private lateinit var wm: WindowManager
    private lateinit var bubble: FrameLayout
    private lateinit var bubbleParams: WindowManager.LayoutParams
    private var card: FrameLayout? = null
    private var cardWeb: WebView? = null
    private var cardShown = false
    private var trash: TextView? = null
    private var pulse: ValueAnimator? = null
    private val main = Handler(Looper.getMainLooper())
    @Volatile private var bubbleDownAt = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        running = true
        startAsForeground()
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        addBubble()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        running = false
        pulse?.cancel()
        safeRemove(bubble)
        card?.let { safeRemove(it) }
        trash?.let { safeRemove(it) }
        cardWeb?.destroy()
        super.onDestroy()
    }

    // ---------- notifikasi wajib untuk layanan latar depan ----------
    private fun startAsForeground() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Bubble Jejak Langkah", NotificationManager.IMPORTANCE_MIN)
        )
        val stopPi = PendingIntent.getService(
            this, 0, Intent(this, BubbleService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE
        )
        val openPi = PendingIntent.getActivity(
            this, 1, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val n: Notification = Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle("Bubble Jejak Langkah aktif")
            .setContentText("Ketuk bubble untuk mencatat langkah")
            .setContentIntent(openPi)
            .setOngoing(true)
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(this, R.drawable.ic_stat), "Matikan bubble", stopPi
                ).build()
            )
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, n)
        }
    }

    // ---------- bubble ----------
    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
    private fun screenW(): Int = resources.displayMetrics.widthPixels
    private fun screenH(): Int = resources.displayMetrics.heightPixels

    private fun overlayParams(w: Int, h: Int, focusable: Boolean): WindowManager.LayoutParams {
        var flags = WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        flags = if (focusable) {
            flags or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        } else {
            flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        }
        return WindowManager.LayoutParams(
            w, h, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, flags, PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START }
    }

    private fun addBubble() {
        val size = dp(76)
        val core = dp(56)
        bubble = FrameLayout(this)

        // Lingkaran luar yang berdenyut, seperti saat Shazam sedang mendengarkan.
        val halo = View(this).apply {
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(ACCENT) }
            alpha = 0.35f
        }
        bubble.addView(halo, FrameLayout.LayoutParams(core, core, Gravity.CENTER))

        val dot = TextView(this).apply {
            text = "⑂"
            textSize = 24f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(ACCENT)
                setStroke(dp(3), MARK)
            }
            elevation = dp(6).toFloat()
        }
        bubble.addView(dot, FrameLayout.LayoutParams(core, core, Gravity.CENTER))

        pulse = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1600
            repeatCount = ValueAnimator.INFINITE
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                val f = it.animatedValue as Float
                halo.scaleX = 1f + 0.33f * f
                halo.scaleY = 1f + 0.33f * f
                halo.alpha = 0.45f * (1f - f)
            }
            start()
        }

        bubbleParams = overlayParams(size, size, false).apply {
            x = screenW() - size
            y = dp(220)
        }
        wm.addView(bubble, bubbleParams)
        bubble.setOnTouchListener(DragListener())
    }

    private inner class DragListener : View.OnTouchListener {
        private var downX = 0f
        private var downY = 0f
        private var startX = 0
        private var startY = 0
        private var downAt = 0L
        private var dragging = false
        private val longPress = Runnable {
            if (!dragging) {
                downAt = -1L // tanda: tahan sudah ditangani
                openApp()
            }
        }

        override fun onTouch(v: View, e: MotionEvent): Boolean {
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = e.rawX; downY = e.rawY
                    startX = bubbleParams.x; startY = bubbleParams.y
                    downAt = System.currentTimeMillis()
                    bubbleDownAt = downAt
                    dragging = false
                    main.postDelayed(longPress, 550)
                    v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(90).start()
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - downX
                    val dy = e.rawY - downY
                    if (!dragging && (abs(dx) > dp(8) || abs(dy) > dp(8))) {
                        dragging = true
                        main.removeCallbacks(longPress)
                        if (cardShown) hideCard()
                        showTrash()
                    }
                    if (dragging) {
                        bubbleParams.x = startX + dx.toInt()
                        bubbleParams.y = startY + dy.toInt()
                        safeUpdate(bubble, bubbleParams)
                        trash?.alpha = if (nearTrash()) 1f else 0.6f
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    main.removeCallbacks(longPress)
                    v.animate().scaleX(1f).scaleY(1f).setDuration(90).start()
                    if (dragging) {
                        val kill = nearTrash()
                        hideTrash()
                        if (kill) {
                            stopSelf(); return true
                        }
                        // Tempelkan ke tepi layar terdekat.
                        val w = bubble.width.takeIf { it > 0 } ?: dp(76)
                        bubbleParams.x = if (bubbleParams.x + w / 2 < screenW() / 2) 0 else screenW() - w
                        bubbleParams.y = bubbleParams.y.coerceIn(dp(24), screenH() - w - dp(24))
                        safeUpdate(bubble, bubbleParams)
                    } else if (downAt > 0 && e.actionMasked == MotionEvent.ACTION_UP) {
                        toggleCard()
                    }
                }
            }
            return true
        }
    }

    // ---------- tempat sampah untuk mematikan bubble ----------
    private fun showTrash() {
        if (trash != null) return
        val t = TextView(this).apply {
            text = "✕"
            textSize = 22f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(0xCC333333.toInt()) }
            alpha = 0.6f
        }
        val s = dp(60)
        val p = overlayParams(s, s, false).apply {
            x = (screenW() - s) / 2
            y = screenH() - s - dp(64)
        }
        wm.addView(t, p)
        trash = t
    }

    private fun hideTrash() {
        trash?.let { safeRemove(it) }
        trash = null
    }

    private fun nearTrash(): Boolean {
        val w = bubble.width.takeIf { it > 0 } ?: dp(76)
        val cx = bubbleParams.x + w / 2
        val cy = bubbleParams.y + w / 2
        val tx = screenW() / 2
        val ty = screenH() - dp(64) - dp(30)
        return abs(cx - tx) < dp(70) && abs(cy - ty) < dp(70)
    }

    // ---------- kartu kecil untuk mencatat ----------
    private fun toggleCard() = if (cardShown) hideCard() else showCard()

    private fun buildCard() {
        val web = WebView(this)
        Web.setup(web, this)
        web.addJavascriptInterface(CardBridge(), "Android")
        web.setBackgroundColor(Color.TRANSPARENT)
        web.loadUrl(BuildConfig.APP_URL + "?mode=bubble")

        val frame = object : FrameLayout(this) {
            override fun dispatchKeyEvent(event: KeyEvent): Boolean {
                if (event.keyCode == KeyEvent.KEYCODE_BACK) {
                    if (event.action == KeyEvent.ACTION_UP) hideCard()
                    return true
                }
                return super.dispatchKeyEvent(event)
            }
        }
        frame.background = GradientDrawable().apply {
            cornerRadius = dp(20).toFloat()
            setColor(0xFFF8FAF7.toInt())
        }
        frame.elevation = dp(12).toFloat()
        frame.clipToOutline = true
        frame.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, dp(20).toFloat())
            }
        }
        frame.addView(web, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        frame.setOnTouchListener { _, e ->
            if (e.actionMasked == MotionEvent.ACTION_OUTSIDE) {
                // Sentuhan di luar kartu menutupnya, kecuali sentuhan itu ke bubble sendiri.
                main.postDelayed({
                    if (System.currentTimeMillis() - bubbleDownAt > 400) hideCard()
                }, 120)
            }
            false
        }
        card = frame
        cardWeb = web
    }

    private fun showCard() {
        if (card == null) buildCard()
        val c = card ?: return
        val w = screenW() - dp(24)
        val h = min(dp(460), (screenH() * 0.62).toInt())
        val p = overlayParams(w, h, true).apply {
            x = dp(12)
            y = dp(72)
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }
        wm.addView(c, p)
        cardShown = true
        // Bubble tetap di atas kartu supaya bisa diketuk untuk menutup.
        safeRemove(bubble)
        wm.addView(bubble, bubbleParams)
        cardWeb?.evaluateJavascript("window.onBubbleShow&&window.onBubbleShow()", null)
    }

    private fun hideCard() {
        if (!cardShown) return
        card?.let { safeRemove(it) }
        cardShown = false
    }

    private fun openApp() {
        hideCard()
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        )
    }

    inner class CardBridge {
        @JavascriptInterface
        fun collapse() = main.post { hideCard() }.let { }

        @JavascriptInterface
        fun openApp() = main.post { this@BubbleService.openApp() }.let { }

        @JavascriptInterface
        fun stopBubble() = main.post { stopSelf() }.let { }
    }

    private fun safeRemove(v: View) {
        try {
            if (v.isAttachedToWindow) wm.removeViewImmediate(v)
        } catch (_: Exception) {
        }
    }

    private fun safeUpdate(v: View, p: WindowManager.LayoutParams) {
        try {
            wm.updateViewLayout(v, p)
        } catch (_: Exception) {
        }
    }
}
