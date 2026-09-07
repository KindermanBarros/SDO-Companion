package com.kinderman.sdo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.kinderman.sdo.presentation.SdoApp
import com.kinderman.sdo.ui.SdoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SdoTheme { SdoApp(this) } }
    }
}
