package com.example.pdfocr

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.pdfocr.ui.theme.PdfOcrReaderTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PdfReaderRoot()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun PdfReaderRoot() {
        PdfOcrReaderTheme {
            Scaffold(
                topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.MenuBook, contentDescription = null)
                    Spacer(Modifier.height(16.dp))
                    Text("Choose a PDF to read with OCR and tap-to-translate")
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = { openViewer() }) {
                        Text("Open PDF")
                    }
                }
            }
        }
    }

    private fun openViewer() {
        startActivity(
            Intent(this, PdfViewerActivity::class.java).apply {
                // The viewer opens its own SAF picker.
            }
        )
    }
}



