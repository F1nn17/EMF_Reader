package com.shiroma.emfreader

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.widget.Button
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.charts.LineChart
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import android.os.Handler
import androidx.room.Room
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import com.shiroma.emfreader.data.AppDatabase
import com.shiroma.emfreader.data.MagneticDataDao
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.pow
import kotlin.math.sqrt
import android.os.Looper
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.formatter.IValueFormatter
import com.shiroma.emfreader.data.MagneticData

class MainActivity : ComponentActivity() {
    private lateinit var sensorManager: SensorManager
    private var magneticSensor: Sensor? = null
    private lateinit var chart: LineChart
    private val dataPoints = mutableListOf<Entry>()
    private lateinit var magneticDataDao: MagneticDataDao
    private var timestampStart = System.currentTimeMillis()
    private val updateIntervalMillis = 5_000L // 15 секунд
    private val handler = Handler(Looper.getMainLooper())
    private val maxPoints = 7 // Максимальное количество точек на графике

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Создаем базовый макет
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        // Инициализация базы данных
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "magnetic_database"
        ).build()
        magneticDataDao = db.magneticDataDao()

        // Инициализация графика
        chart = LineChart(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 15f // Минимальный шаг между метками в секундах
                setLabelCount(6, true)
                setAvoidFirstLastClipping(true)
                textColor = Color.BLACK
                valueFormatter = object : IAxisValueFormatter {
                    @SuppressLint("ConstantLocale")
                    private val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

                    override fun getFormattedValue(value: Float, p1: AxisBase?): String {
                        return sdf.format(Date(timestampStart + value.toLong() * 1000))
                    }

                    override fun getDecimalDigits(): Int {
                        return 2
                    }

                }
            }

            axisLeft.apply {
                textColor = Color.BLACK
                axisMinimum = 0f // Минимальное значение оси Y
                valueFormatter = object : IAxisValueFormatter {
                    override fun getFormattedValue(value: Float, p1: AxisBase?): String {
                        // Показываем значение магнитуды с единицей измерения
                        return "$value мкТл"
                    }

                    override fun getDecimalDigits(): Int {
                        return 2
                    }
                }
            }
            // Описание графика (удаляем общий текст и переносим его на ось Y)
            description.isEnabled = false
            axisLeft.textColor = Color.BLACK
            axisRight.isEnabled = false
            legend.isEnabled = false
        }
        mainLayout.addView(chart)

        if (dataPoints.isEmpty()) {
            dataPoints.add(Entry(0f, 0f))
        }
        // Настраиваем график
        val lineDataSet = LineDataSet(dataPoints, "Магнитное поле (мкТл)").apply {
            color = Color.BLUE
            valueTextSize = 8f
            setDrawCircles(true)
            setDrawCircleHole(false)
            circleRadius = 2f // Радиус точки
        }

        lineDataSet.valueFormatter = IValueFormatter { value, p1, p2, p3 ->
            // Отображаем значение только для каждой второй точки
            if (value.toInt() % 2 == 0) "%.2f".format(value) else ""
        }

        val lineData = LineData(lineDataSet)
        chart.data = lineData

        val viewSavedDataButton = Button(this).apply {
            text = "Посмотреть сохраненные данные"
            setOnClickListener {
                val intent = Intent(this@MainActivity, SavedDataActivity::class.java)
                startActivity(intent)
            }
        }
        mainLayout.addView(viewSavedDataButton)
        val openSettingsButton = Button(this).apply {
            text = "Настройки"
            setOnClickListener {
                val intent = Intent(this@MainActivity, SettingsActivity::class.java)
                startActivity(intent)
            }
        }
        mainLayout.addView(openSettingsButton)
        setContentView(mainLayout)


        // Инициализация сенсора
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        magneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        // Загрузка сохраненных данных в график
        lifecycleScope.launch {
            loadSavedDataToGraph()
        }

        // Старт измерений
        startMagneticFieldMonitoring()
    }

    private fun startMagneticFieldMonitoring() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                magneticSensor?.let {
                    sensorManager.registerListener(
                        object : SensorEventListener {
                            override fun onSensorChanged(event: SensorEvent) {
                                if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                                    val timestamp = System.currentTimeMillis()
                                    val magnitude = sqrt(
                                        event.values[0].pow(2) +
                                                event.values[1].pow(2) +
                                                event.values[2].pow(2)
                                    )

                                    // Получаем верхний предел из SharedPreferences
                                    val sharedPreferences = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
                                    val upperLimit = sharedPreferences.getFloat("upper_limit", 100f) // Дефолтный предел

                                    // Проверяем верхний предел
                                    if (magnitude > upperLimit) {
                                        val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 10)
                                        toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 500)
                                    }

                                    lifecycleScope.launch(Dispatchers.IO) {
                                        saveDataToDatabase(timestamp, magnitude, event.values)
                                        updateGraph(timestamp, magnitude)
                                    }

                                    sensorManager.unregisterListener(this)
                                }
                            }

                            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
                        },
                        it,
                        SensorManager.SENSOR_DELAY_UI
                    )
                }
                handler.postDelayed(this, updateIntervalMillis) // Интервал в 15 секунд
            }
        }, updateIntervalMillis)
    }

    // Обновление графика
    @SuppressLint("DefaultLocale")
    private fun updateGraph(timestamp: Long, magnitude: Float) {
        val roundedMagnitude = String.format("%.2f", magnitude).replace(',', '.').toFloat()
        val timeInSeconds = (timestamp - timestampStart) / 1000f

        // Удаляем старейшие точки, если их больше maxPoints
        if (dataPoints.size >= maxPoints) {
            dataPoints.removeAt(0)
        }

        // Добавление точки на график
        dataPoints.add(Entry(timeInSeconds, roundedMagnitude))

        runOnUiThread {
            val lineDataSet = LineDataSet(dataPoints, "Магнитное поле (мкТл)").apply {
                color = Color.BLUE
                valueTextSize = 12f
                setDrawCircles(false)
            }
            chart.data = LineData(lineDataSet)
            chart.notifyDataSetChanged()
            chart.invalidate()
        }
    }

    // Сохранение данных в базу
    private suspend fun saveDataToDatabase(timestamp: Long, magnitude: Float, values: FloatArray) {
        val magneticData = MagneticData(
            timestamp = timestamp,
            x = values[0],
            y = values[1],
            z = values[2],
            magnitude = magnitude
        )
        magneticDataDao.insert(magneticData)
    }

    @SuppressLint("DefaultLocale")
    private suspend fun loadSavedDataToGraph() {
        val savedData = withContext(Dispatchers.IO) { magneticDataDao.getAll() }
        if (savedData.isNotEmpty()) {
            val currentTime = System.currentTimeMillis()
            dataPoints.clear()
            // Проверяем разрыв времени с последней сохранённой точкой
            val lastDataPoint = savedData.last()
            val timeDifference = currentTime - lastDataPoint.timestamp
            // Если разрыв более 5 минут
            if (timeDifference > 5 * 60 * 1000) {
                val timeInSeconds = (currentTime - timestampStart) / 1000f
                dataPoints.add(Entry(timeInSeconds, 0F))
            } else {
                // Берём последние 7 точек
                val last7Data = savedData.takeLast(maxPoints)
                last7Data.forEach { data ->
                    val timeInSeconds = (data.timestamp - timestampStart) / 1000f
                    val roundedMagnitude = String.format("%.2f", data.magnitude).replace(',', '.').toFloat()
                    dataPoints.add(Entry(timeInSeconds, roundedMagnitude))
                }
            }

            runOnUiThread {
                val lineDataSet = LineDataSet(dataPoints, "Магнитное поле (мкТл)").apply {
                    color = Color.BLUE
                    valueTextSize = 12f
                    setDrawCircles(false)
                }
                chart.data = LineData(lineDataSet)
                chart.notifyDataSetChanged()
                chart.invalidate()
            }
        }
    }

}
