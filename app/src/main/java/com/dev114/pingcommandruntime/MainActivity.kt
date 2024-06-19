package com.dev114.pingcommandruntime

import android.content.ClipData
import android.content.ClipboardManager
import android.os.AsyncTask
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {

    private lateinit var etHost: EditText
    private lateinit var etPingLimit: EditText
    private lateinit var btnPing: Button
    private lateinit var btnPaste: Button
    private lateinit var btnClear: Button
    private lateinit var btnCopy: Button
    private lateinit var btnStop: Button
    private lateinit var tvPingResult: TextView
    private lateinit var tvStatistics: TextView
    private var pingTask: PingTask? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etHost = findViewById(R.id.etHost)
        etPingLimit = findViewById(R.id.etPingLimit)
        btnPing = findViewById(R.id.btnPing)
        btnPaste = findViewById(R.id.btnPaste)
        btnClear = findViewById(R.id.btnClear)
        btnCopy = findViewById(R.id.btnCopy)
        btnStop = findViewById(R.id.btnStop)
        tvPingResult = findViewById(R.id.tvPingResult)
        tvStatistics = findViewById(R.id.tvStatistics)

        btnPing.setOnClickListener {
            val host = etHost.text.toString()
            val pingLimit = if (etPingLimit.text.toString().isEmpty()) "4" else etPingLimit.text.toString()
            if (host.isNotEmpty()) {
                pingTask = PingTask().apply { execute(host, pingLimit) }
            } else {
                Toast.makeText(this, "Host cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }

        btnPaste.setOnClickListener {
            val clipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = clipboardManager.primaryClip
            if (clipData != null && clipData.itemCount > 0) {
                val text = clipData.getItemAt(0).text.toString()
                etHost.setText(text)
            }
        }

        btnClear.setOnClickListener {
            etHost.setText("")
            etPingLimit.setText("")
            tvPingResult.text = ""
            tvStatistics.text = ""
        }

        btnCopy.setOnClickListener {
            val clipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Ping Result", tvPingResult.text.toString())
            clipboardManager.setPrimaryClip(clip)
        }

        btnStop.setOnClickListener {
            pingTask?.cancel(true)
            Toast.makeText(this, "Ping stopped", Toast.LENGTH_SHORT).show()
        }
    }

    private inner class PingTask : AsyncTask<String, String, Pair<String, String>>() {
        override fun doInBackground(vararg params: String): Pair<String, String> {
            val host = params[0]
            val pingLimit = params[1]
            var pingResult = ""
            var statistics = ""

            return try {
                val process = Runtime.getRuntime().exec("/system/bin/ping -c $pingLimit $host")
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                var line: String?

                while (reader.readLine().also { line = it } != null) {
                    if (isCancelled) {
                        process.destroy()
                        return Pair(pingResult, "Ping cancelled")
                    }
                    pingResult += line + "\n"
                    if (line!!.contains("packets transmitted")) {
                        statistics = line!!
                    }
                    publishProgress(pingResult, statistics)
                }

                if (pingResult.isEmpty()) {
                    throw Exception("Host not reachable")
                }

                Pair(pingResult, statistics)
            } catch (e: Exception) {
                Pair(pingResult, e.message ?: "Error")
            }
        }

        override fun onProgressUpdate(vararg values: String) {
            tvPingResult.text = values[0]
            tvStatistics.text = values[1]
        }

        override fun onPostExecute(result: Pair<String, String>) {
            tvPingResult.text = result.first
            tvStatistics.text = result.second
            if (result.first.isEmpty()) {
                Toast.makeText(this@MainActivity, "Ping host is not reachable", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onCancelled(result: Pair<String, String>?) {
            Toast.makeText(this@MainActivity, "Ping stopped", Toast.LENGTH_SHORT).show()
            tvPingResult.text = result?.first ?: ""
            tvStatistics.text = result?.second ?: "Ping cancelled"
        }
    }
}



