package com.shiroma.emfreader

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.view.setPadding
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.shiroma.emfreader.data.AppDatabase
import com.shiroma.emfreader.data.MagneticDataDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsActivity : ComponentActivity() {
    private lateinit var magneticDataDao: MagneticDataDao
    private lateinit var upperLimitField: EditText
    private lateinit var sharedPreferences: SharedPreferences
    private val defaultLimit = 100f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Инициализация SharedPreferences
        sharedPreferences = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val savedLimit = sharedPreferences.getFloat("upper_limit", defaultLimit)

        // Инициализация базы данных
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "magnetic_database"
        ).build()
        magneticDataDao = db.magneticDataDao()

        // Создаем интерфейс
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            setPadding(16)
        }

        // Кнопка очистки базы данных и графика
        val clearDatabaseButton = Button(this).apply {
            text = "Очистить базу данных и график"
            setOnClickListener {
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        magneticDataDao.deleteAll()
                    }
                    // Очищаем график в MainActivity
                    MainActivity.clearGraph()
                    Toast.makeText(this@SettingsActivity, "База данных очищена", Toast.LENGTH_SHORT).show()
                }
            }
        }



        // Кнопка очистки базы данных
        val clearGraphics = Button(this).apply {
            text = "Очистить график"
            setOnClickListener {
                lifecycleScope.launch {
                    // Очищаем график в MainActivity
                    MainActivity.clearGraph()
                    Toast.makeText(this@SettingsActivity, "График очищен", Toast.LENGTH_SHORT).show()
                }
            }
        }


        // Поле ввода верхнего предела
        upperLimitField = EditText(this).apply {
            hint = "Установить верхний предел (мкТл)"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(savedLimit.toString())
        }
        layout.addView(upperLimitField)

        // Кнопка для сохранения верхнего предела
        val saveLimitButton = Button(this).apply {
            text = "Сохранить предел"
            setOnClickListener {
                val input = upperLimitField.text.toString()
                if (input.isNotBlank()) {
                    val limit = input.toFloat()
                    sharedPreferences.edit().putFloat("upper_limit", limit).apply()
                    Toast.makeText(
                        this@SettingsActivity,
                        "Верхний предел сохранен: $limit мкТл",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(this@SettingsActivity, "Введите корректное значение", Toast.LENGTH_SHORT).show()
                }
            }
        }
        layout.addView(saveLimitButton)
        layout.addView(clearGraphics)
        layout.addView(clearDatabaseButton)

        setContentView(layout)
    }
}