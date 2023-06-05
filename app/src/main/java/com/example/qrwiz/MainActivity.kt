package com.example.qrwiz

import android.Manifest
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.MutableLiveData
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.PagerState
import com.google.accompanist.pager.rememberPagerState
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.client.android.BeepManager
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {
    private lateinit var barcodeView: DecoratedBarcodeView

    private val requestPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                barcodeView.resume()
            }
        }

    override fun onResume() {
        super.onResume()
        requestPermission.launch(Manifest.permission.CAMERA)
    }

    override fun onPause() {
        super.onPause()
        barcodeView.pause()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val beepManager = BeepManager(this)
        val cameraSurfaceRoot = layoutInflater.inflate(R.layout.camera_surface, null)
        barcodeView = cameraSurfaceRoot.findViewById(R.id.barcode_scanner)
        val formats = listOf(BarcodeFormat.QR_CODE, BarcodeFormat.QR_CODE)
        barcodeView.barcodeView.decoderFactory = DefaultDecoderFactory(formats)
        barcodeView.initializeFromIntent(intent)

        val text = MutableLiveData("")
        val callback = object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult) {
                if (result.text == null || result.text == text.value) {
                    return
                }
                text.value = result.text
                beepManager.playBeepSoundAndVibrate()
            }
        }
        barcodeView.decodeContinuous(callback)

        setContent {
            HorizontalPagerScreen(
                items = listOf("My QR", "Scan"),
                root = cameraSurfaceRoot,
                scannedTextState = text.observeAsState()
            )
        }
    }
}

@Composable
fun ZxingDemo(root: View, value: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        AndroidView(modifier = Modifier.fillMaxSize(),
            factory = {
                root
            })
        if (value.isNotBlank()) {
            Text(
                modifier = Modifier.padding(16.dp),
                text = value,
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium
            )
        }
    }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun HorizontalPagerScreen(
    items: List<String>,
    root: View,
    scannedTextState: State<String?>,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(30.dp)
    ) {
        val pagerState = rememberPagerState()
        val coroutineScope = rememberCoroutineScope()

        HorizontalTabs(
            items = items,
            pagerState = pagerState,
            scope = coroutineScope
        )

        HorizontalPager(
            count = items.size,
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { currentPage ->
            if (currentPage == 0) {
                getMyQrCode()?.let {
                    Image(
                        bitmap = it,
                        contentDescription = "My QR Code",
                    )
                }
            } else {
                scannedTextState.value?.let {
                    Log.d("Value: ", it)
                    ZxingDemo(root, it)
                }
            }
        }
    }
}

private fun getMyQrCode(): ImageBitmap? {
    val text = "https://devfinity.io"
    val multiFormatWriter = MultiFormatWriter()
    return try {
        val bitMatrix =
            multiFormatWriter.encode(text, BarcodeFormat.QR_CODE, 400, 400)
        val barcodeEncoder = BarcodeEncoder()
        val bitmap = barcodeEncoder.createBitmap(bitMatrix)
        bitmap.asImageBitmap()
    } catch (e: WriterException) {
        e.printStackTrace()
        null
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun HorizontalTabs(
    items: List<String>,
    pagerState: PagerState,
    scope: CoroutineScope
) {
    TabRow(
        selectedTabIndex = pagerState.currentPage,
        indicator = { tabPositions ->
            TabRowDefaults.Indicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage])
            )
        }
    ) {
        items.forEachIndexed { index, item ->
            Tab(
                selected = pagerState.currentPage == index,
                onClick = {
                    scope.launch {
                        pagerState.animateScrollToPage(page = index)
                    }
                }
            ) {
                Text(text = item)
            }
        }
    }
}
