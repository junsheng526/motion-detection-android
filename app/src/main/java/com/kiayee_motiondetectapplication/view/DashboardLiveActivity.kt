package com.kiayee_motiondetectapplication.view

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.graphics.Bitmap
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import android.util.Log
import androidx.appcompat.widget.Toolbar
import com.google.gson.Gson
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.Matrix
import android.graphics.BitmapFactory
import android.graphics.PointF
import org.json.JSONObject
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import java.io.IOException
import android.graphics.YuvImage
import android.media.MediaPlayer
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.github.mikephil.charting.components.XAxis
import com.kiayee_motiondetectapplication.R
import com.kiayee_motiondetectapplication.data.api.ApiRoutes
import com.kiayee_motiondetectapplication.data.model.AnalyticRecord
import com.kiayee_motiondetectapplication.data.model.DetectedPerson
import com.kiayee_motiondetectapplication.data.model.SettingsPackage
import com.kiayee_motiondetectapplication.data.model.TransformResult
import com.kiayee_motiondetectapplication.utils.CameraConfig
import com.kiayee_motiondetectapplication.viewmodel.AnalyticViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


class DashboardLiveActivity : AppCompatActivity() {

    private lateinit var dashboardTab: TextView
    private lateinit var liveTab: TextView
    private lateinit var analyticTab: TextView
    private lateinit var tabContainer: FrameLayout
    private lateinit var inflater: LayoutInflater
    private lateinit var backgroundPreviewView: PreviewView //background layout
    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var preview: Preview
    private lateinit var imageAnalyzer: ImageAnalysis
    private val gson = Gson()
    private lateinit var rootLayout: LinearLayout

    private var isPreviewReady = false
    private var isFrameReady = false
    private var isReadyHandled = false

    // Http part
    private val executor = Executors.newSingleThreadExecutor()
    private val client = OkHttpClient()
    private var lastSendTime = 0L
    private val sendIntervalMs = 200L
    private val resultPollIntervalMs = 200L
    private val resultHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val resultPollRunnable = object : Runnable {
        override fun run() {
            getResultFromPC()
            resultHandler.postDelayed(this, resultPollIntervalMs)
        }
    }

    //Receive Info
    private var l_totalNumTextView: TextView? = null
    private var d_totalNumTextView: TextView? = null
    private var l_totalinROITextView: TextView? = null
    private var d_totalinROITextView: TextView? = null
    private var l_crowdstatus: TextView? = null
    private var d_crowdstatus: TextView? = null
    private var l_avgdwelltime: TextView? = null
    private var d_avgdwelltime: TextView? = null
    private var number_enter: TextView? = null
    private var number_out: TextView? = null

    private var lineroiOverlay: DrawLineROIView? = null
    private val inData = mutableListOf<Entry>()
    private val outData = mutableListOf<Entry>()
    private val roiData = mutableListOf<Entry>()
    private lateinit var inOutChart: LineChart
    private lateinit var roiChart: LineChart
    private var startTimeMillis: Long = 0L
    private var mediaPlayer: MediaPlayer? = null
    private var hasPlayedCrowdedAlert = false

    //Send Info
    var lineObj: DefineInoutActivity.InOutLines? =
        null //line can read by python but unable to read by kt
    var roiObj: DefineROIActivity.ROI? = null
    var roiJson: String? = null
    var threshold: Int? = null
    var inLine: List<PointF>? = null
    var outLine: List<PointF>? = null

    var frameToSendWidth: Int = 0
    var frameToSendHeight: Int = 0

    private val vm: AnalyticViewModel by viewModels()
    private val seenIds = mutableSetOf<Int>()
    private var lastSavedHour: String? = null

    // Keep track of all unique detected IDs
    private val uniquePersonIds = mutableSetOf<Int>()

    // Track cumulative totals
    private var cumulativeInCount = 0
    private var cumulativeOutCount = 0
    private var cumulativeTotalPeople = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard_live)

        dashboardTab = findViewById(R.id.dashboardTab)
        liveTab = findViewById(R.id.liveTab)
        analyticTab = findViewById(R.id.analyticTab)
        tabContainer = findViewById(R.id.tabContainer)
        inflater = LayoutInflater.from(this)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "User"

        dashboardTab.setOnClickListener { activateTab("DASHBOARD") }
        liveTab.setOnClickListener { activateTab("LIVE") }
        analyticTab.setOnClickListener { activateTab("ANALYTICS") }
        mediaPlayer = MediaPlayer.create(this, R.raw.alert)
        mediaPlayer?.setOnCompletionListener {
            hasPlayedCrowdedAlert = false  // Allow next play
        }

        rootLayout = findViewById<LinearLayout>(R.id.rootLayout)
        backgroundPreviewView = findViewById(R.id.backgroundCameraPreview)
        if (backgroundPreviewView.parent == null) {
            rootLayout.addView(backgroundPreviewView)
        }
        initializeCameraProvider()

        activateTab("DASHBOARD") // Show dashboard by default
    }

    override fun onSupportNavigateUp(): Boolean {
        cameraProvider?.unbindAll()
        finish()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        resultHandler.removeCallbacks(resultPollRunnable)
        mediaPlayer?.release()
        mediaPlayer = null

    }

    private fun activateTab(type: String) {
        if (type == "DASHBOARD") {
            dashboardTab.setBackgroundResource(R.drawable.toggle_selected)
            dashboardTab.setTextColor(Color.WHITE)
            liveTab.setBackgroundResource(R.drawable.toggle_unselected)
            liveTab.setTextColor(Color.WHITE)
            analyticTab.setBackgroundResource(R.drawable.toggle_unselected)
            analyticTab.setTextColor(Color.WHITE)
            loadTabLayout(R.layout.layout_dashboard)
        } else if (type === "LIVE") {
            liveTab.setBackgroundResource(R.drawable.toggle_selected)
            liveTab.setTextColor(Color.WHITE)
            dashboardTab.setBackgroundResource(R.drawable.toggle_unselected)
            dashboardTab.setTextColor(Color.WHITE)
            analyticTab.setBackgroundResource(R.drawable.toggle_unselected)
            analyticTab.setTextColor(Color.WHITE)
            loadTabLayout(R.layout.layout_live)
        } else {
            analyticTab.setBackgroundResource(R.drawable.toggle_selected)
            analyticTab.setTextColor(Color.WHITE)
            dashboardTab.setBackgroundResource(R.drawable.toggle_unselected)
            dashboardTab.setTextColor(Color.WHITE)
            liveTab.setBackgroundResource(R.drawable.toggle_unselected)
            liveTab.setTextColor(Color.WHITE)
            loadTabLayout(R.layout.layout_analytics)
        }
    }

    private fun loadTabLayout(layoutId: Int) {
        // ✅ Remove existing fragment if any
        val existingFragment = supportFragmentManager.findFragmentById(R.id.tabContainer)
        if (existingFragment != null) {
            supportFragmentManager.beginTransaction()
                .remove(existingFragment)
                .commitNowAllowingStateLoss()
        }

        // ✅ Clear any existing views
        tabContainer.removeAllViews()

        when (layoutId) {
            R.layout.layout_live -> {
                val view = inflater.inflate(layoutId, tabContainer, false)
                tabContainer.addView(view)
                setupLiveTab(view)
            }

            R.layout.layout_dashboard -> {
                val view = inflater.inflate(layoutId, tabContainer, false)
                tabContainer.addView(view)
                setupDashboardTab(view)
            }

            R.layout.layout_analytics -> {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.tabContainer, AnalyticFragment())
                    .commitNowAllowingStateLoss()
            }
        }
    }

// -------------------- Helper setups --------------------

    private fun setupLiveTab(view: View) {
        lineroiOverlay = view.findViewById(R.id.lineroiview)
        l_totalNumTextView = view.findViewById(R.id.totalNumber)
        l_avgdwelltime = view.findViewById(R.id.dwellTime)
        val l_roiareaname = view.findViewById<TextView>(R.id.visitorInArea)
        l_totalinROITextView = view.findViewById(R.id.numberVisitorInArea)
        l_crowdstatus = view.findViewById(R.id.statusText)

        // Attach camera preview
        val previewFrame = view.findViewById<FrameLayout>(R.id.previewFrame)
        val parent = backgroundPreviewView.parent as? ViewGroup
        if (parent != null && parent != previewFrame) {
            parent.removeView(backgroundPreviewView)
        }
        previewFrame.addView(backgroundPreviewView)
        backgroundPreviewView.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        backgroundPreviewView.visibility = View.VISIBLE

        // Overlay
        val overlay = lineroiOverlay ?: return
        (overlay.parent as? ViewGroup)?.removeView(overlay)
        previewFrame.addView(lineroiOverlay)

        // ROI + Line setup
        l_roiareaname.text = "Visitor in ${roiObj?.name ?: ""} area"
        lineObj = loadInOutLines()
        inLine = lineObj?.inLine?.map { PointF(it.x, it.y) }
        outLine = lineObj?.outLine?.map { PointF(it.x, it.y) }
        threshold = loadThreshold()
        backgroundPreviewView.post {
            isPreviewReady = true
            checkIfReady()
        }
        lineroiOverlay?.setadminsetting(roiObj, inLine, outLine)
    }

    private fun setupDashboardTab(view: View) {
        // Camera hidden (keeps running in background)
        val parent = backgroundPreviewView.parent as? ViewGroup
        if (parent != null && parent != rootLayout) {
            parent.removeView(backgroundPreviewView)
            rootLayout.addView(backgroundPreviewView)
            backgroundPreviewView.layoutParams = FrameLayout.LayoutParams(1, 1)
            backgroundPreviewView.visibility = View.GONE
        }
        backgroundPreviewView.visibility = View.GONE

        // Bind views
        val d_roiareaname = view.findViewById<TextView>(R.id.d_visitorInArea)
        d_roiareaname.text = "Visitor in ${roiObj?.name ?: ""} area"
        d_crowdstatus = view.findViewById(R.id.d_statusText)
        d_avgdwelltime = view.findViewById(R.id.d_dwellTimeText)
        d_totalNumTextView = view.findViewById(R.id.d_totalNumber)
        d_totalinROITextView = view.findViewById(R.id.d_numberVisitorInArea)
        number_enter = view.findViewById(R.id.d_numberEntered)
        number_out = view.findViewById(R.id.d_numberLeft)

        inOutChart = view.findViewById(R.id.inOutChart)
        roiChart = view.findViewById(R.id.roiChart)

        setupChart(inOutChart, "People In/Out Over Time")
        setupChart(roiChart, "People in ROI Over Time")
    }

    private fun initializeCameraProvider() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindPreviewAndStartSending()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindPreviewAndStartSending() {
        if (cameraProvider == null) return
        preview = Preview.Builder()
            .setTargetResolution(CameraConfig.DEFAULT_RESOLUTION)
            .build()
            .also {
                it.setSurfaceProvider(backgroundPreviewView.surfaceProvider)
            }
        imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(executor) { imageProxy ->
                    val bitmap = imageProxyToBitmap(imageProxy)
                    imageProxy.close()
                    val now = System.currentTimeMillis()
                    if (now - lastSendTime > sendIntervalMs) {
                        sendFrameToPC(bitmap)
                        lastSendTime = now
                    }
                    isFrameReady = true
                    checkIfReady()
                }
            }

        try {
            cameraProvider?.unbindAll() // Optional: rebind safely
            cameraProvider?.bindToLifecycle(
                this,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageAnalyzer
            )
        } catch (e: Exception) {
            Log.e("CameraX", "Use case binding failed", e)
        }
    }

    private fun loadThreshold(): Int? {
        val prefs = getSharedPreferences("threshold_prefs", MODE_PRIVATE)
        return if (prefs.contains("threshold")) prefs.getInt("threshold", 0) else null
    }

    private fun loadInOutLines(): DefineInoutActivity.InOutLines? {
        val prefs = getSharedPreferences("inout_prefs", MODE_PRIVATE)
        val json = prefs.getString("lines", null)
        if (json != null) {
            val gson = Gson()
            val type =
                object : com.google.gson.reflect.TypeToken<DefineInoutActivity.InOutLines>() {}.type
            return gson.fromJson<DefineInoutActivity.InOutLines>(json, type)
        }
        return null
    }

    private fun sendFrameToPC(bitmap: Bitmap) {
        var frameToSend = bitmap
        // Always create a Matrix instance
        val matrix = Matrix()
        if (resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT) {
            matrix.postRotate(90f)
            frameToSend =
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }
        frameToSendWidth = frameToSend.width
        frameToSendHeight = frameToSend.height
        isFrameReady = true
        checkIfReady()
        val stream = ByteArrayOutputStream()
        frameToSend.compress(Bitmap.CompressFormat.JPEG, 80, stream)
        val byteArray = stream.toByteArray()
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "frame", "frame.jpg",
                byteArray.toRequestBody("image/jpeg".toMediaTypeOrNull())
            )
            .build()

        val request = Request.Builder()
            .url(ApiRoutes.SEND_FRAME)
            .post(requestBody)
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("HTTP", "Failed to send: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                Log.d("HTTP", "Settings sent: ${response.body?.string()}")
                Log.d(
                    "FrameSend",
                    "Frame sent at ${System.currentTimeMillis()}, size=${byteArray.size} bytes"
                )

            }
        })
    }

    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap {
        val yBuffer = imageProxy.planes[0].buffer
        val uBuffer = imageProxy.planes[1].buffer
        val vBuffer = imageProxy.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, imageProxy.width, imageProxy.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, imageProxy.width, imageProxy.height), 80, out)
        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    private fun calculateImageToViewTransform(): TransformResult {

        val imageWidth = frameToSendWidth.toFloat()
        val imageHeight = frameToSendHeight.toFloat()
        val previewWidth = backgroundPreviewView.width.toFloat()
        val previewHeight = backgroundPreviewView.height.toFloat()

        val imageAspect = imageWidth / imageHeight
        val viewAspect = previewWidth / previewHeight
        val scale: Float
        val dx: Float
        val dy: Float
        if (imageAspect > viewAspect) {
            scale = previewWidth / imageWidth
            dx = 0f
            dy = (previewHeight - imageHeight * scale) / 2f
        } else {
            scale = previewHeight / imageHeight
            dx = (previewWidth - imageWidth * scale) / 2f
            dy = 0f
        }
        return TransformResult(
            imageWidth,
            imageHeight,
            previewWidth,
            previewHeight,
            scale,
            dx,
            dy
        )

    } //end calculate image to view transform function

    private fun checkIfReady() {
        if (isPreviewReady && isFrameReady && !isReadyHandled) {
            onPreviewAndFrameReady()
            isReadyHandled = true
            resultHandler.post(resultPollRunnable)
        }
    }

    private fun onPreviewAndFrameReady() {
        backgroundPreviewView.post {
            val maptoPC = calculateImageToViewTransform()
            val scaledroiPoints: List<PointF>? = roiObj?.points?.map { pt ->
                val x = ((pt.x - maptoPC.dx) / maptoPC.scale).coerceIn(0f, maptoPC.imageWidth - 1)
                val y = ((pt.y - maptoPC.dy) / maptoPC.scale).coerceIn(0f, maptoPC.imageHeight - 1)
                PointF(x, y) // Create a new PointF with the mapped coordinates
            }
            val scaledRoi = roiObj?.let { roi ->
                DefineROIActivity.ROI(
                    name = roi.name,
                    points = scaledroiPoints ?: emptyList()
                )
            }
            val scaledLinePoints: DefineInoutActivity.InOutLines? = lineObj?.let { obj ->
                DefineInoutActivity.InOutLines(
                    inLine = obj.inLine.map { pt ->
                        // pt is PointF if lineObj is PointF
                        val x = ((pt.x - maptoPC.dx) / maptoPC.scale).coerceIn(
                            0f,
                            maptoPC.imageWidth - 1
                        )
                        val y = ((pt.y - maptoPC.dy) / maptoPC.scale).coerceIn(
                            0f,
                            maptoPC.imageHeight - 1
                        )
                        // Convert to Points, not PointF
                        DefineInoutActivity.Points(x, y)
                    },
                    outLine = obj.outLine.map { pt ->
                        val x = ((pt.x - maptoPC.dx) / maptoPC.scale).coerceIn(
                            0f,
                            maptoPC.imageWidth - 1
                        )
                        val y = ((pt.y - maptoPC.dy) / maptoPC.scale).coerceIn(
                            0f,
                            maptoPC.imageHeight - 1
                        )
                        DefineInoutActivity.Points(x, y)
                    }
                )
            }
            val settingsPackage = SettingsPackage(
                roi = scaledRoi,
                threshold = threshold,
                lines = scaledLinePoints
            )
            sendsettingtoPC(settingsPackage)
            Log.d("SENDSETTINGS", "ROI=$scaledRoi, Lines=$scaledLinePoints, threshold=$threshold")
        }
    }

    private fun sendsettingtoPC(settings: SettingsPackage?) {
        val settingsJson = gson.toJson(settings)
        Log.d("SENDSETTINGS", "Setting send in JSON: $settingsJson")
        val requestBody = settingsJson.toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder()
            .url(ApiRoutes.SEND_SETTING)
            .post(requestBody)
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.d("SendtoPC", "Failed to send: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                Log.d("SendtoPC", "Settings sent: ${response.body?.string()}")
            }
        })
    }

    private fun resolveDayDateHour(): Triple<String, String, String> {
        // First pick mock or now
        val cal = vm.mockDateTime.value ?: Calendar.getInstance()

        // Try to resolve day, date, hour
        val day = cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault())
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
        val hour = cal.get(Calendar.HOUR_OF_DAY).toString()

        // If day is null or "Unknown", fallback to system calendar
        return if (day.isNullOrEmpty() || day == "Unknown") {
            val now = Calendar.getInstance()
            val fallbackDay = now.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault()) ?: "Unknown"
            val fallbackDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now.time)
            val fallbackHour = now.get(Calendar.HOUR_OF_DAY).toString()
            Triple(fallbackDay, fallbackDate, fallbackHour)
        } else {
            Triple(day, date, hour)
        }
    }

    private fun getResultFromPC() {
        val request = Request.Builder()
            .url(ApiRoutes.RECEIVE_RESULT)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("HTTP", "Failed to receive: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.d("GetFromPC", "Total People in area: $responseBody")
                try {
                    val json = JSONObject(responseBody ?: "{}")
                    val totalPeople = json.optInt("people_count", 0)
                    val totaiInROI = json.optInt("count_in_roi", 0)
                    val avgdwell = json.optInt("average_dwell")
                    val peopleArray = json.optJSONArray("latest_detections")
                    val inlinecount = json.optInt("in_count", 0)
                    val outlinecount = json.optInt("out_count", 0)

                    val peopleList = mutableListOf<DetectedPerson>()
                    if (peopleArray != null) {
                        for (i in 0 until peopleArray.length()) {
                            val personObj = peopleArray.getJSONObject(i)
                            val id = personObj.optInt("id", -1)
                            val boxArray = personObj.optJSONArray("bbox")
                            val circleArray = personObj.optJSONArray("center")

                            val box = if (boxArray != null && boxArray.length() == 4)
                                IntArray(4) { boxArray.getInt(it) } else IntArray(4)
                            val circle = if (circleArray != null && circleArray.length() == 2)
                                IntArray(2) { circleArray.getInt(it) } else IntArray(2)

                            peopleList.add(DetectedPerson(id, box, circle))

                            // ✅ Count only new unique IDs
                            if (id != -1 && !uniquePersonIds.contains(id)) {
                                uniquePersonIds.add(id)

                                // Update cumulative counts
                                cumulativeTotalPeople += 1
                            }
                        }
                    }

                    runOnUiThread {
                        val boolstart: Boolean = json.optBoolean("bool_start")
                        if (boolstart) {
                            //total people
                            val ttl_ppl = uniquePersonIds.size.toString()
                            l_totalNumTextView?.text = ttl_ppl
                            d_totalNumTextView?.text = ttl_ppl

                            //total people (roi)
                            val ttl_roi = totaiInROI.toString()
                            l_totalinROITextView?.text = ttl_roi
                            d_totalinROITextView?.text = ttl_roi
                            //dwell time
                            val dwellText = formatDwellTime(avgdwell)
                            l_avgdwelltime?.text = dwellText
                            d_avgdwelltime?.text = dwellText

                            //CrowdStatus
                            val isNormal = totaiInROI <= (threshold ?: Int.MAX_VALUE)
                            val statusText = if (isNormal) "Normal" else "Crowded"
                            val statusColor = if (isNormal)
                                ContextCompat.getColor(
                                    this@DashboardLiveActivity,
                                    R.color.Normal_text
                                )
                            else
                                Color.RED
                            if (!isNormal && !hasPlayedCrowdedAlert) {
                                mediaPlayer?.start()
                                hasPlayedCrowdedAlert = true
                            }
                            if (isNormal) {
                                hasPlayedCrowdedAlert =
                                    false  // Reset flag when it returns to normal
                            }

                            d_crowdstatus?.text = statusText
                            d_crowdstatus?.setTextColor(statusColor)
                            l_crowdstatus?.text = statusText
                            l_crowdstatus?.setTextColor(statusColor)

                            // Add to totals instead of replacing
                            cumulativeInCount += inlinecount
                            cumulativeOutCount += outlinecount

                            // Update UI with cumulative values
                            number_enter?.text = cumulativeInCount.toString()
                            number_out?.text = cumulativeOutCount.toString()

                            val transform = calculateImageToViewTransform()
                            val mappedPeopleList = peopleList.map { person ->
                                val (id, box, center) = person
                                val x1 = box[0] * transform.scale + transform.dx
                                val y1 = box[1] * transform.scale + transform.dy
                                val x2 = box[2] * transform.scale + transform.dx
                                val y2 = box[3] * transform.scale + transform.dy
                                val cx = center[0] * transform.scale + transform.dx
                                val cy = center[1] * transform.scale + transform.dy
                                DetectedPerson(
                                    id,
                                    intArrayOf(x1.toInt(), y1.toInt(), x2.toInt(), y2.toInt()),
                                    intArrayOf(cx.toInt(), cy.toInt())
                                )
                            }
                            lineroiOverlay?.updatePeople(mappedPeopleList)

                            val now = System.currentTimeMillis() / 100f // seconds
                            // Get relative time (X-axis)
                            if (startTimeMillis == 0L) {
                                startTimeMillis = System.currentTimeMillis()
                            }
                            val elapsedSeconds =
                                (System.currentTimeMillis() - startTimeMillis) / 1000f
                            // ✅ Add data at every response (not limited to once per second)
                            inData.add(Entry(elapsedSeconds, inlinecount.toFloat()))
                            outData.add(Entry(elapsedSeconds, outlinecount.toFloat()))
                            roiData.add(Entry(elapsedSeconds, totaiInROI.toFloat()))

                            val isDashboardVisible =
                                dashboardTab.background.constantState == resources.getDrawable(
                                    R.drawable.toggle_selected
                                ).constantState

                            if (isDashboardVisible) {
                                updateChart(inOutChart, inData, outData, "In", "Out")
                                updateChart(roiChart, roiData, null, "In ROI", null)
                            }

                            val record = AnalyticRecord(
                                peopleCount = totalPeople,
                                countInRoi = totaiInROI,
                                inCount = inlinecount,
                                outCount = outlinecount,
                                averageDwell = avgdwell
                            )

                            val (day, date, hour) = resolveDayDateHour()

                            // Track unique IDs
                            for (person in peopleList) {
                                if (person.id != -1) {
                                    seenIds.add(person.id)
                                }
                            }

                            // Reset if new hour starts
                            if (lastSavedHour != hour) {
                                seenIds.clear()
                                lastSavedHour = hour
                            }

                            // Save record with stable unique count
                            val recordWithUnique = record.copy(
                                peopleCount = seenIds.size // replace fluctuating count
                            )

                            lifecycleScope.launch {
                                vm.set(day, date, hour, recordWithUnique)
                            }


                        }//end if start receive info
                    }// end run on UI
                } catch (e: Exception) {
                    Log.d("GetFromPC", "Error parsing JSON: ${e.message}")
                }
            }
        })
    }

    private fun setupChart(chart: LineChart, title: String) {
        chart.description.text = title
        chart.setTouchEnabled(true)
        chart.setPinchZoom(true)
        chart.setDrawGridBackground(false)
        chart.xAxis.labelRotationAngle = -45f
    }

    private fun updateChart(
        chart: LineChart?,
        line1: List<Entry>,
        line2: List<Entry>?,
        label1: String,
        label2: String?
    ) {
        if (chart == null) return

        val dataSets = ArrayList<ILineDataSet>()
        val dataSet1 = LineDataSet(line1, label1).apply {
            color = Color.GREEN
            setCircleColor(Color.GREEN)
            lineWidth = 2f
            setDrawValues(false)
            mode = LineDataSet.Mode.LINEAR  // angular lines like your matplotlib
        }
        dataSets.add(dataSet1)

        if (line2 != null && label2 != null) {
            val dataSet2 = LineDataSet(line2, label2).apply {
                color = Color.RED
                setCircleColor(Color.RED)
                lineWidth = 2f
                setDrawValues(false)
                mode = LineDataSet.Mode.LINEAR
            }
            dataSets.add(dataSet2)
        }

        chart.data = LineData(dataSets)

        // Get the latest X value (time in seconds)
        val maxX = line1.lastOrNull()?.x ?: 0f

        // Setup X axis to match your Python matplotlib behavior
        chart.xAxis.apply {
            axisMinimum = 0f
            axisMaximum = maxX
            setLabelCount(6, true)              // Always 6 ticks (like Python plot)
            granularity = if (maxX >= 5f) maxX / 5f else 1f
            setGranularityEnabled(true)
            position = XAxis.XAxisPosition.BOTTOM
            textColor = Color.BLACK
        }

        chart.axisLeft.apply {
            axisMinimum = 0f
            textColor = Color.BLACK
        }

        chart.axisRight.isEnabled = false
        chart.legend.textColor = Color.BLACK
        chart.description.isEnabled = false
        chart.invalidate() // Redraw chart
    }

    private fun formatDwellTime(seconds: Int): String {
        val totalSeconds = seconds.toInt()
        val minutes = totalSeconds / 60
        val remainingSeconds = totalSeconds % 60

        return if (minutes > 0) {
            "$minutes minute${if (minutes > 1) "s" else ""} $remainingSeconds second${if (remainingSeconds != 1) "s" else ""}"
        } else {
            "$remainingSeconds second${if (remainingSeconds != 1) "s" else ""}"
        }
    }
}
