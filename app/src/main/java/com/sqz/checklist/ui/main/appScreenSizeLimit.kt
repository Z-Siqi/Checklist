package com.sqz.checklist.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sqz.checklist.R
import com.sqz.checklist.ui.theme.unit.pxToDp
import com.sqz.checklist.ui.theme.unit.pxToDpInt

const val screenWidthLimitDpInt = 335

const val screenHeightLimitDpInt = 338

@Composable
fun appScreenSizeLimit(override: Boolean = false): Boolean {
    val config = LocalWindowInfo.current.containerSize
    val heightLimit = config.height.pxToDpInt() < screenHeightLimitDpInt
    val widthLimit = config.width.pxToDpInt() < screenWidthLimitDpInt
    val limit = heightLimit || widthLimit
    if (!override && limit) Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                stringResource(R.string.unsupported_screen_size),
                color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold
            )
            Text(
                stringResource(R.string.unsupported_screen_size_describe),
                textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = 5.dp)
            )
            Text("Request dp: W >= 335, H >= 338", color = MaterialTheme.colorScheme.outline)
            Text(
                "Currently dp: W = ${config.width.pxToDp()}, H = ${config.height.pxToDp()}",
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
    return if (override) false else limit
}

@Preview(widthDp = 334, heightDp = 337)
@Composable
private fun Preview() {
    appScreenSizeLimit()
}
