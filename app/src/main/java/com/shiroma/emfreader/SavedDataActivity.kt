package com.shiroma.emfreader

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.shiroma.emfreader.data.AppDatabase
import com.shiroma.emfreader.data.MagneticData
import com.shiroma.emfreader.data.MagneticDataDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class SavedDataActivity: ComponentActivity() {
    private lateinit var magneticDataDao: MagneticDataDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Создаем интерфейс с прокруткой
        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        val textViewContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val savedDataTextView = TextView(this).apply {
            textSize = 16f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(16, 16, 16, 16)
        }

        textViewContainer.addView(savedDataTextView)
        scrollView.addView(textViewContainer)
        setContentView(scrollView)

        // Инициализация базы данных
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "magnetic_database"
        ).build()
        magneticDataDao = db.magneticDataDao()

        // Загрузка данных из базы
        lifecycleScope.launch {
            val savedData = withContext(Dispatchers.IO) {
                magneticDataDao.getAll()
            }
            displaySavedData(savedData, savedDataTextView)
        }
    }

    private fun displaySavedData(data: List<MagneticData>, savedDataTextView: TextView) {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val displayText = data.joinToString("\n") { item ->
            val formattedTime = sdf.format(Date(item.timestamp))
            val formattedMagnitude = "%.2f".format(item.magnitude)

            """
                Time: $formattedTime, Magnitude: $formattedMagnitude
                
            """.trimIndent()
        }
        savedDataTextView.text = displayText
    }
}