package com.nvllz.stepsy.ui

import android.os.Bundle
import android.widget.Toast
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
