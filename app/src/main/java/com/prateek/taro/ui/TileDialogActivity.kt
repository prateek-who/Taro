package com.prateek.taro.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.prateek.taro.ui.components.PauseDialogs
import com.prateek.taro.ui.theme.TaroTheme
import com.prateek.taro.util.PauseController

class TileDialogActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TaroTheme {
                PauseDialogs(
                    onPauseFor = { minutes, endTime ->
                        Toast.makeText(this, PauseController.pauseFor(this, minutes, endTime), Toast.LENGTH_LONG).show()
                        finish()
                    },
                    onPauseIndefinitely = {
                        Toast.makeText(this, PauseController.pauseIndefinitely(this), Toast.LENGTH_SHORT).show()
                        finish()
                    },
                    onDismiss = ::finish,
                )
            }
        }
    }
}
