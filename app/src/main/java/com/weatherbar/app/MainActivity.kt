package com.weatherbar.app

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences

    companion object {
        const val PREFS_NAME = "WeatherBarPrefs"
        const val KEY_CITY = "city"
        const val KEY_API_TYPE = "api_type"
        const val KEY_API_KEY = "api_key"
        const val KEY_UNIT = "unit"
        const val KEY_INTERVAL = "interval"
        const val KEY_ICON_PACK = "icon_pack"
        const val API_OPEN_METEO = 0
        const val API_OPENWEATHER = 1
        const val API_WEATHERAPI = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        setupUI()
        requestPermissions()
    }

    override fun onResume() {
        super.onResume()
        refreshPreview()
    }

    private fun setupUI() {
        val etCity = findViewById<EditText>(R.id.etCity)
        val spinnerApi = findViewById<Spinner>(R.id.spinnerApi)
        val etApiKey = findViewById<EditText>(R.id.etApiKey)
        val rgUnit = findViewById<RadioGroup>(R.id.rgUnit)
        val spinnerInterval = findViewById<Spinner>(R.id.spinnerInterval)
        val spinnerIconPack = findViewById<Spinner>(R.id.spinnerIconPack)
        val btnSave = findViewById<Button>(R.id.btnSave)
        val btnStart = findViewById<Button>(R.id.btnStart)
        val btnStop = findViewById<Button>(R.id.btnStop)
        val tvStatus = findViewById<TextView>(R.id.tvStatus)
        val layoutApiKey = findViewById<LinearLayout>(R.id.layoutApiKey)

        // Setup spinners
        ArrayAdapter(this, R.layout.spinner_item, resources.getStringArray(R.array.api_types))
            .also { it.setDropDownViewResource(R.layout.spinner_item); spinnerApi.adapter = it }

        ArrayAdapter(this, R.layout.spinner_item, resources.getStringArray(R.array.interval_labels))
            .also { it.setDropDownViewResource(R.layout.spinner_item); spinnerInterval.adapter = it }

        ArrayAdapter(this, R.layout.spinner_item, resources.getStringArray(R.array.icon_packs))
            .also { it.setDropDownViewResource(R.layout.spinner_item); spinnerIconPack.adapter = it }

        // Load saved values
        etCity.setText(prefs.getString(KEY_CITY, "Istanbul"))
        val apiType = prefs.getInt(KEY_API_TYPE, API_OPEN_METEO)
        spinnerApi.setSelection(apiType)
        layoutApiKey.visibility = if (apiType == API_OPEN_METEO) android.view.View.GONE else android.view.View.VISIBLE
        etApiKey.setText(prefs.getString(KEY_API_KEY, ""))

        if (prefs.getString(KEY_UNIT, "C") == "C") rgUnit.check(R.id.rbCelsius)
        else rgUnit.check(R.id.rbFahrenheit)

        val intervalValues = resources.getIntArray(R.array.interval_values)
        val savedInterval = prefs.getInt(KEY_INTERVAL, 30)
        spinnerInterval.setSelection(intervalValues.indexOfFirst { it == savedInterval }.takeIf { it >= 0 } ?: 1)
        spinnerIconPack.setSelection(prefs.getInt(KEY_ICON_PACK, 0))

        spinnerApi.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: android.view.View?, pos: Int, id: Long) {
                layoutApiKey.visibility = if (pos == API_OPEN_METEO) android.view.View.GONE else android.view.View.VISIBLE
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        spinnerIconPack.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: android.view.View?, pos: Int, id: Long) { refreshPreview() }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        rgUnit.setOnCheckedChangeListener { _, _ -> refreshPreview() }

        btnSave.setOnClickListener {
            val city = etCity.text.toString().trim()
            if (city.isEmpty()) { Toast.makeText(this, "Lütfen şehir girin", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            val ivals = resources.getIntArray(R.array.interval_values)
            prefs.edit().apply {
                putString(KEY_CITY, city)
                putInt(KEY_API_TYPE, spinnerApi.selectedItemPosition)
                putString(KEY_API_KEY, etApiKey.text.toString().trim())
                putString(KEY_UNIT, if (rgUnit.checkedRadioButtonId == R.id.rbCelsius) "C" else "F")
                putInt(KEY_INTERVAL, ivals.getOrElse(spinnerInterval.selectedItemPosition) { 30 })
                putInt(KEY_ICON_PACK, spinnerIconPack.selectedItemPosition)
                apply()
            }
            Toast.makeText(this, "✅ Ayarlar kaydedildi", Toast.LENGTH_SHORT).show()
            restartService()
            tvStatus.text = "Durum: Çalışıyor ✅"
        }

        btnStart.setOnClickListener {
            restartService()
            tvStatus.text = "Durum: Çalışıyor ✅"
        }

        btnStop.setOnClickListener {
            stopService(Intent(this, WeatherService::class.java))
            prefs.edit().putBoolean("service_was_running", false).apply()
            tvStatus.text = "Durum: Durduruldu ⛔"
        }
    }

    private fun restartService() {
        stopService(Intent(this, WeatherService::class.java))
        val i = Intent(this, WeatherService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(i)
        else startService(i)
        prefs.edit().putBoolean("service_was_running", true).apply()
    }

    private fun refreshPreview() {
        val tvPreview = findViewById<TextView>(R.id.tvPreview) ?: return
        val lastText = prefs.getString("last_weather_text", null)
        if (lastText != null) {
            tvPreview.text = lastText
        } else {
            val iconPack = prefs.getInt(KEY_ICON_PACK, 0)
            val (_, icon) = WeatherFetcher.getWeatherInfo(2, iconPack)
            val unit = prefs.getString(KEY_UNIT, "C") ?: "C"
            tvPreview.text = "$icon 23°$unit"
        }
    }

    private fun requestPermissions() {
        val perms = mutableListOf(Manifest.permission.INTERNET, Manifest.permission.ACCESS_NETWORK_STATE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) perms.add(Manifest.permission.POST_NOTIFICATIONS)
        val toReq = perms.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (toReq.isNotEmpty()) ActivityCompat.requestPermissions(this, toReq.toTypedArray(), 100)
    }
}
