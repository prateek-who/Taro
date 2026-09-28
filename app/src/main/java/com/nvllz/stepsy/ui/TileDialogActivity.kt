package com.nvllz.stepsy.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.nvllz.stepsy.ui.components.PauseDialogs
import com.nvllz.stepsy.ui.theme.StepsyTheme
import com.nvllz.stepsy.util.PauseController

class TileDialogActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StepsyTheme {
                PauseDialogs(
                    onPauseFor = { minutes, endTime ->
                        PauseController.pauseFor(this, minutes, endTime)
                        finish()
                    },
                    onPauseIndefinitely = {
                        PauseController.pauseIndefinitely(this)
                        finish()
                    },
                    onDismiss = ::finish,
                )
            }
        }
    }
}
