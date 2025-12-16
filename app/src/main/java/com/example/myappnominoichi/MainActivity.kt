package com.example.myappnominoichi

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import android.widget.LinearLayout
import android.view.ViewGroup
import android.util.TypedValue

class MainActivity : AppCompatActivity() {

    private lateinit var stampStatusText: TextView
    private lateinit var openStoresButton: Button
    private lateinit var scanButton: Button
    private lateinit var stampStore: StampStore

    private val MAX_STAMPS = 3

    private val qrLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) handleScannedCode(result.contents!!)
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startQRScanner() else
                Toast.makeText(this, "カメラ権限が必要です", Toast.LENGTH_SHORT).show()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        stampStatusText = findViewById(R.id.stampStatusText)
        openStoresButton = findViewById(R.id.openStoresButton)
        scanButton = findViewById(R.id.scanButton)

        stampStore = StampStore(this)

        openStoresButton.setOnClickListener {
            startActivity(Intent(this, StoreListActivity::class.java))
        }

        scanButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
                startQRScanner()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        updateStampUI()
    }

    override fun onResume() {
        super.onResume()
        updateStampUI()
    }

    private fun startQRScanner() {
        val opt = ScanOptions().apply {
            setPrompt("QRコードを読み取ってください")
            setBeepEnabled(true)
            setOrientationLocked(true)
        }
        qrLauncher.launch(opt)
    }

    private fun handleScannedCode(code: String) {
        val scanned = code.trim()
        if (scanned.isBlank()) {
            Toast.makeText(this, "無効なQRコードです", Toast.LENGTH_SHORT).show()
            return
        }
        val already = stampStore.has(scanned)
        stampStore.add(scanned)
        updateStampUI()

        Toast.makeText(
            this,
            if (already) "この店舗は既にスタンプ済みです" else "スタンプ追加！（$scanned）",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun updateStampUI() {
        val count = stampStore.getAll().size
        stampStatusText.text = "スタンプ：$count / $MAX_STAMPS"
        findViewById<ProgressBar>(R.id.stampProgress).apply {
            max = MAX_STAMPS
            progress = count.coerceAtMost(MAX_STAMPS)
        }
        renderStampIcons(count)
    }

    /** スタンプアイコンを count 個だけ並べる（🍶アイコンで実装） */
    private fun renderStampIcons(count: Int) {
        val container = findViewById<LinearLayout>(R.id.stampIconsContainer)
        container.removeAllViews()

        val dp8 = (8 * resources.displayMetrics.density).toInt()
        val lp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(dp8 / 2, 0, dp8 / 2, 0) }

        repeat(count) {
            val tv = TextView(this).apply {
                text = "🍶"                 // ← お好みで「⭐」「●」「🍺」などに変更可
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
            }
            container.addView(tv, lp)
        }
    }

}
