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

    private val qrLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) handleScannedCode(result.contents!!)
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startQRScanner() else
                toast(getString(R.string.camera_permission_required))
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
            setPrompt(getString(R.string.scan_prompt))
            setBeepEnabled(true)
            setOrientationLocked(true)
        }
        qrLauncher.launch(opt)
    }

    private fun handleScannedCode(code: String) {
        // 参加店舗のQRかを照合し、保存するのは店舗IDのみ
        val store = StampRules.findStoreByQr(StoreRepository.getStores(this), code)
        if (store == null) {
            toast(getString(R.string.qr_store_not_found))
            return
        }

        val already = stampStore.has(store.id)
        if (!already) stampStore.add(store.id)
        updateStampUI()

        val count = stampStore.count()
        toast(
            when {
                already -> getString(R.string.stamp_already, store.name)
                count == StampRules.MAX_STAMPS -> getString(R.string.stamp_completed, count)
                else -> getString(R.string.stamp_added, store.name)
            }
        )
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    private fun updateStampUI() {
        val count = stampStore.count()
        stampStatusText.text = getString(R.string.stamp_status, count, StampRules.MAX_STAMPS)
        findViewById<ProgressBar>(R.id.stampProgress).apply {
            max = StampRules.MAX_STAMPS
            progress = count.coerceAtMost(StampRules.MAX_STAMPS)
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
