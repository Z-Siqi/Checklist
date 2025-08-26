package com.sqz.checklist.ui.common

import android.content.Intent
import android.net.Uri
import android.view.SoundEffectConstants
import android.view.View
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit

@Composable
fun UrlText(
    url: String,
    view: View,
    modifier: Modifier = Modifier,
    text: String? = null,
    color: Color = MaterialTheme.colorScheme.primary,
    fontSize: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = TextDecoration.Underline,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    fontStyle: FontStyle? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Visible,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    style: TextStyle = LocalTextStyle.current,
) {
    val formatURL = when {
        !url.startsWith("http") -> url.replaceFirst(url[0].toString(), "http://" + url[0])
        else -> url
    }
    Text(
        text = text ?: url,
        fontSize = fontSize,
        color = color,
        modifier = modifier.clickable {
            view.playSoundEffect(SoundEffectConstants.CLICK)
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(formatURL)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            view.context.startActivity(intent)
        },
        textDecoration = textDecoration,
        fontWeight = fontWeight,
        textAlign = textAlign,
        fontStyle = fontStyle,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        onTextLayout = onTextLayout,
        style = style
    )
}

@Preview(showBackground = true)
@Composable
private fun Preview() {
    Column {
        UrlText("https://www.github.com/", view = LocalView.current)
        UrlText("https://www.github.com/", text = "Github", view = LocalView.current)
    }
}
