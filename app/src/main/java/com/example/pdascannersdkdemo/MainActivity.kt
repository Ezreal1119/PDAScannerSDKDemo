package com.example.pdascannersdkdemo

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.device.ScanManager
import android.device.ScanManager.ACTION_DECODE
import android.device.ScanManager.BARCODE_LENGTH_TAG
import android.device.ScanManager.BARCODE_STRING_TAG
import android.device.ScanManager.BARCODE_TYPE_TAG
import android.device.ScanManager.DECODE_DATA_TAG
import android.device.scanner.configuration.PropertyID.LABEL_PREFIX
import android.device.scanner.configuration.PropertyID.LABEL_SUFFIX
import android.device.scanner.configuration.PropertyID.SEND_LABEL_PREFIX_SUFFIX
import android.device.scanner.configuration.Triggering
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition


import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions

private const val TAG = "Patrick_MainActivity"
private const val ACTION_CAPTURE_IMAGE_REQUEST = "action.scanner_capture_image"
private const val ACTION_CAPTURE_IMAGE_RESULT = "scanner_capture_image_result"
private const val BITMAP_BYTES_TAG = "bitmapBytes"
private const val LEFT_SCAN_KEYCODE = 521
private const val RIGHT_SCAN_KEYCODE = 520
class MainActivity : AppCompatActivity() {

    private val btnOpenScanner by lazy { findViewById<Button>(R.id.btnOpenScanner) }
    private val btnCloseScanner by lazy { findViewById<Button>(R.id.btnCloseScanner) }
    private val btnGetMode by lazy { findViewById<Button>(R.id.btnGetMode) }
    private val btnToIntent by lazy { findViewById<Button>(R.id.btnToIntent) }
    private val btnToKeyboard by lazy { findViewById<Button>(R.id.btnToKeyboard) }
    private val btnLock by lazy { findViewById<Button>(R.id.btnLock) }
    private val btnLockState by lazy { findViewById<Button>(R.id.btnLockState) }
    private val btnUnlock by lazy { findViewById<Button>(R.id.btnUnlock)}
    private val btnResetScannerParameters by lazy { findViewById<Button>(R.id.btnResetScannerParameters) }
    private val btnSetTriggerMode by lazy { findViewById<Button>(R.id.btnSetTriggerMode) }
    private val btnSetPreSuf by lazy { findViewById<Button>(R.id.btnSetPreSuf) }
    private val btnClearPreSuf by lazy { findViewById<Button>(R.id.btnClearPreSuf) }
    private val btnClearConsole by lazy { findViewById<Button>(R.id.btnClearConsole) }
    private val tvScannerOn by lazy { findViewById<TextView>(R.id.tvScannerOn) }
    private val tvScannerOff by lazy { findViewById<TextView>(R.id.tvScannerOff) }
    private val tvOCR by lazy { findViewById<TextView>(R.id.tvOCR) }
    private val tvResult by lazy { findViewById<TextView>(R.id.tvResult) }
    private val etTextBox by lazy { findViewById<EditText>(R.id.etTextBox)}
    private val spTriggerMode by lazy { findViewById<Spinner>(R.id.spTriggerMode) }
    private val ivScanImage by lazy { findViewById<ImageView>(R.id.ivScanImage) }
    private val triggerModeList = listOf(Triggering.HOST, Triggering.CONTINUOUS, Triggering.PULSE)

    private val mScanManager = ScanManager()
    private val receiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_DECODE -> {
                    val barcodeBytes = intent.getByteArrayExtra(DECODE_DATA_TAG)
                    val barcodeString = intent.getStringExtra(BARCODE_STRING_TAG)
                    val barcodeLen = intent.getIntExtra(BARCODE_LENGTH_TAG, 0)
                    val barcodeType = intent.getByteExtra(BARCODE_TYPE_TAG, 0)
                    Log.e(TAG, "barcodeString: $barcodeString")
                    Log.e(TAG, "barcodeLen: $barcodeLen")
                    Log.e(TAG, "barcodeType: $barcodeType")
                    val text = buildString {
                        append("This is an Intent received by Patrick's receiver:\n")
                        append("barcodeString: $barcodeString\n")
                        append("barcodeLen: $barcodeLen\n")
                        append("barcodeType: $barcodeType")
                    }
                    tvResult.text = text
                }
                ACTION_CAPTURE_IMAGE_RESULT -> {
                    Log.e(TAG, "onReceive: ACTION_CAPTURE_IMAGE successfully 1")
                    val imageData = intent.getByteArrayExtra(BITMAP_BYTES_TAG)
                    if (imageData != null && imageData.isNotEmpty()) {
                        val bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
                        if (bitmap != null) {
                            ivScanImage.setImageBitmap(bitmap)
                            Log.e(TAG, "onReceive: ACTION_CAPTURE_IMAGE successfully")
                            runOcr(bitmap)
                        } else {
                            Toast.makeText(this@MainActivity, "bitmap = 0", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@MainActivity, "imageData = 0", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun runOcr(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
        recognizer.process(image)
            .addOnSuccessListener { result ->
                val text = result.text
                tvOCR.text = text
                Log.e(TAG, "OCR result:\n$text")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "OCR failed", e)
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnOpenScanner.setOnClickListener { onOpenScannerButtonClicked() }
        btnCloseScanner.setOnClickListener { onCloseScannerButtonClicked() }
        btnGetMode.setOnClickListener { onGetModeButtonClicked() }
        btnToIntent.setOnClickListener { onToIntentButtonClicked() }
        btnToKeyboard.setOnClickListener { onToKeyboardButtonClicked() }
        btnLock.setOnClickListener { onLockButtonClicked() }
        btnLockState.setOnClickListener { onLockStateButtonClicked() }
        btnUnlock.setOnClickListener { onUnlockButtonClicked() }
        btnResetScannerParameters.setOnClickListener { onResetScannerParametersButtonClicked() }
        btnSetTriggerMode.setOnClickListener { onSetTriggerModeButtonClicked() }
        btnSetPreSuf.setOnClickListener { onSetPreSufButtonClicked() }
        btnClearPreSuf.setOnClickListener { onClearPreSufButtonClicked() }
        btnClearConsole.setOnClickListener { onClearConsoleButtonClicked() }

        spTriggerMode.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, triggerModeList).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onStart() {
        super.onStart()
        init()
        val filter = IntentFilter().apply {
            addAction(ACTION_DECODE)
            addAction(ACTION_CAPTURE_IMAGE_RESULT)
        }
        registerReceiver(receiver, filter)
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(receiver)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode != LEFT_SCAN_KEYCODE && keyCode != RIGHT_SCAN_KEYCODE) {
            return super.onKeyUp(keyCode, event)
        }
        if (event != null && event.repeatCount > 0) return true
        Log.e(TAG, "onKeyDown: 1", )
        if (!mScanManager.scannerState) {
            Toast.makeText(this, "Please turn on the scanner first", Toast.LENGTH_SHORT).show()
            return true
        }
        if (mScanManager.triggerLockState) {
            Toast.makeText(this, "Please unlock the scanner first", Toast.LENGTH_SHORT).show()
            return super.onKeyUp(keyCode, event)
        }
        val ret = mScanManager.startDecode()
        when (ret) {
            true -> if (mScanManager.outputMode == 1) etTextBox.requestFocus()
            false -> Toast.makeText(this, "Start scan failed", Toast.LENGTH_SHORT).show()
        }
        return true
    }


    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode != LEFT_SCAN_KEYCODE && keyCode != RIGHT_SCAN_KEYCODE) {
            return super.onKeyDown(keyCode, event)
        }
        if (!mScanManager.scannerState) {
            Toast.makeText(this, "Please turn on the scanner first", Toast.LENGTH_SHORT).show()
            return super.onKeyDown(keyCode, event)
        }
        if (mScanManager.triggerLockState) {
            Toast.makeText(this, "Please unlock the scanner first", Toast.LENGTH_SHORT).show()
            return super.onKeyDown(keyCode, event)
        }
        sendBroadcast(Intent(ACTION_CAPTURE_IMAGE_REQUEST))
        val ret = mScanManager.stopDecode()
        if (!ret) Toast.makeText(this, "Stop scan failed", Toast.LENGTH_SHORT).show()
        return true
    }



    private fun init() {
        when (mScanManager.scannerState) {
            true -> {
                tvScannerOn.visibility = View.VISIBLE
                tvScannerOff.visibility = View.INVISIBLE
                btnOpenScanner.isEnabled = false
                btnCloseScanner.isEnabled = true
            }
            false -> {
                tvScannerOn.visibility = View.INVISIBLE
                tvScannerOff.visibility = View.VISIBLE
                btnOpenScanner.isEnabled = true
                btnCloseScanner.isEnabled = false
            }
        }

        when (mScanManager.outputMode) {
            0 -> {
                btnToIntent.isEnabled = false
                btnToKeyboard.isEnabled = true
            }
            1 -> {
                btnToIntent.isEnabled = true
                btnToKeyboard.isEnabled = false
            }
        }

        when (mScanManager.triggerLockState) {
            true -> {
                btnLock.isEnabled = false
                btnUnlock.isEnabled = true
            }
            false -> {
                btnLock.isEnabled = true
                btnUnlock.isEnabled = false
            }
        }

        spTriggerMode.setSelection(triggerModeList.indexOf(mScanManager.triggerMode))
    }

    private fun onOpenScannerButtonClicked() {
        // To power on the scanner
        val ret = mScanManager.openScanner()
        when (ret) {
            true -> {
                Toast.makeText(this, "Scanner opened successfully", Toast.LENGTH_SHORT).show()
                tvScannerOn.visibility = View.VISIBLE
                tvScannerOff.visibility = View.INVISIBLE
                btnOpenScanner.isEnabled = false
                btnCloseScanner.isEnabled = true
            }
            false -> Toast.makeText(this, "Scanner opened failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun onCloseScannerButtonClicked() {
        // To power off the scanner
        mScanManager.closeScanner()
        Toast.makeText(this, "Scanner closed successfully", Toast.LENGTH_SHORT).show()
        tvScannerOn.visibility = View.INVISIBLE
        tvScannerOff.visibility = View.VISIBLE
        btnOpenScanner.isEnabled = true
        btnCloseScanner.isEnabled = false
    }

    private fun onGetModeButtonClicked() {
        // To get the output mode of the scanner
        val outputMode = mScanManager.outputMode
        when (outputMode) {
            0 -> Toast.makeText(this, "Intent Output", Toast.LENGTH_SHORT).show()
            1 -> Toast.makeText(this, "Keyboard Output", Toast.LENGTH_SHORT).show()
        }
    }

    private fun onToIntentButtonClicked() {
        // To switch output mode to Intent mode
        if (!mScanManager.scannerState) {
            Toast.makeText(this, "Please turn on the scanner first", Toast.LENGTH_SHORT).show()
            return
        }
        val ret = mScanManager.switchOutputMode(0)
        when (ret) {
            true -> {
                Toast.makeText(this, "Switch to Intent successfully", Toast.LENGTH_SHORT).show()
                btnToIntent.isEnabled = false
                btnToKeyboard.isEnabled = true
            }
            false -> Toast.makeText(this, "Switch to Intent failed", Toast.LENGTH_SHORT).show()
        }
    }


    private fun onToKeyboardButtonClicked() {
        // To switch output mode to Keyboard mode
        if (!mScanManager.scannerState) {
            Toast.makeText(this, "Please turn on the scanner first", Toast.LENGTH_SHORT).show()
        } else {
            val ret = mScanManager.switchOutputMode(1)
            when (ret) {
                true -> {
                    Toast.makeText(this, "Switch to Keyboard successfully", Toast.LENGTH_SHORT).show()
                    btnToIntent.isEnabled = true
                    btnToKeyboard.isEnabled = false
                }
                false -> Toast.makeText(this, "Switch to Keyboard failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun onLockButtonClicked() {
        // To disable scanner
        if (!mScanManager.scannerState) {
            Toast.makeText(this, "Please turn on the scanner first", Toast.LENGTH_SHORT).show()
            return
        }
        val ret = mScanManager.lockTrigger()
        when (ret) {
            true -> {
                Toast.makeText(this, "Disable Scan button successfully", Toast.LENGTH_SHORT).show()
                btnLock.isEnabled = false
                btnUnlock.isEnabled = true
            }
            false -> Toast.makeText(this, "Disable Scan button failed", Toast.LENGTH_SHORT).show()
        }
    }


    private fun onLockStateButtonClicked() {
        // To check if the scanner has been disabled or not
        if (!mScanManager.scannerState) {
            Toast.makeText(this, "Please turn on the scanner first", Toast.LENGTH_SHORT).show()
            return
        }
        when (mScanManager.triggerLockState) {
            true -> Toast.makeText(this, "Scan Button is not Active", Toast.LENGTH_SHORT).show()
            false -> Toast.makeText(this, "Scan Button is Active", Toast.LENGTH_SHORT).show()
        }
    }

    private fun onUnlockButtonClicked() {
        // To enable scanner
        if (!mScanManager.scannerState) {
            Toast.makeText(this, "Please turn on the scanner first", Toast.LENGTH_SHORT).show()
            return
        }
        val ret = mScanManager.unlockTrigger()
        when (ret) {
            true -> {
                Toast.makeText(this, "Enable Scan button successfully", Toast.LENGTH_SHORT).show()
                btnLock.isEnabled = true
                btnUnlock.isEnabled = false
            }
            false -> Toast.makeText(this, "Enable Scan button failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun onResetScannerParametersButtonClicked() {
        // To reset the scanner config
        if (!mScanManager.scannerState) {
            Toast.makeText(this, "Please turn on the scanner first", Toast.LENGTH_SHORT).show()
            return
        }
        val ret = mScanManager.resetScannerParameters()
        when (ret) {
            true -> {
                Toast.makeText(this, "Reset Scanner parameters successfully", Toast.LENGTH_SHORT).show()
                init()
            }
            false -> Toast.makeText(this, "Reset Scanner parameters failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun onSetTriggerModeButtonClicked() {
        // To set the scanning mode
        if (!mScanManager.scannerState) {
            Toast.makeText(this, "Please turn on the scanner first", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            mScanManager.triggerMode = spTriggerMode.selectedItem as Triggering
            val triggerModeAsString = when (spTriggerMode.selectedItem) {
                Triggering.HOST -> "HOST"
                Triggering.CONTINUOUS -> "CONTINUOUS"
                Triggering.PULSE -> "PULSE"
                else -> null
            }
            Toast.makeText(
                this,
                "Set Trigger Mode successfully: $triggerModeAsString",
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Set Trigger Mode failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun onSetPreSufButtonClicked() {
        mScanManager.setParameterString(intArrayOf(LABEL_PREFIX, LABEL_SUFFIX), arrayOf("Pre_", "_Suf"))
        mScanManager.setParameterInts(intArrayOf(SEND_LABEL_PREFIX_SUFFIX), intArrayOf(3))
        Toast.makeText(this, "Set Prefix&Suffix successfully", Toast.LENGTH_SHORT).show()
    }

    private fun onClearPreSufButtonClicked() {
        mScanManager.setParameterInts(intArrayOf(SEND_LABEL_PREFIX_SUFFIX), intArrayOf(0))
        Toast.makeText(this, "Clear Prefix&Suffix successfully", Toast.LENGTH_SHORT).show()
    }

    private fun onClearConsoleButtonClicked() {
        tvResult.text = ""
        tvOCR.text = ""
        etTextBox.setText("")
        ivScanImage.setImageBitmap(null)
    }
}