package com.sqz.checklist.presentation.task.info.type.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sqz.checklist.R
import com.sqz.checklist.common.AndroidEffectFeedback
import com.sqz.checklist.presentation.task.info.type.InfoDialogScaffold
import com.sqz.checklist.ui.common.unit.pxToDp
import sqz.checklist.common.EffectFeedback
import sqz.checklist.task.api.info.TaskInfo.DetailInfoState

/** This method expected to be called only within this package and its sub-packages. **/
@Composable
internal fun InfoDetailList(
    details: List<DetailInfoState>,
    onSelected: (index: Int) -> Unit,
    onDismissRequest: () -> Unit,
    isSmallScreenSize: Boolean,
    feedback: EffectFeedback,
    modifier: Modifier = Modifier,
) {
    InfoDialogScaffold(
        onDismissRequest = onDismissRequest,
        onDialogBackgroundClick = onDismissRequest,
        isSmallScreenSize = isSmallScreenSize,
        modifier = modifier
    ) {
        ThisTitle()
        Spacer(modifier = Modifier.height(if (isSmallScreenSize) 10.dp else 18.dp))
        ListCard(isSmallScreenSize) {
            itemsIndexed(details) { index, item ->
                CardItem(
                    description = item.detailDescription,
                    type = item.detailType,
                    modifier = Modifier.padding(vertical = (2.5).dp)
                ) {
                    onSelected(index)
                    feedback.onClickEffect()
                }
            }
        }
        Spacer(modifier = Modifier.height(if (isSmallScreenSize) 8.dp else 15.dp))
        CancelButton {
            onDismissRequest().also { feedback.onClickEffect() }
        }
    }
}

@Composable
private fun ColumnScope.CancelButton(onClick: () -> Unit) {
    TextButton(
        modifier = Modifier.align(Alignment.End),
        onClick = onClick
    ) {
        Text(
            text = stringResource(R.string.cancel),
            maxLines = 1,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun CardItem(
    description: String?,
    type: DetailInfoState.DetailType,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val titleStyle = MaterialTheme.typography.titleLarge.copy(lineHeight = TextUnit.Unspecified)
    val labelStype = MaterialTheme.typography.bodyMedium.copy(lineHeight = TextUnit.Unspecified)
    val title: String = description ?: type.toLocalString()
    val label: String? = description.let {
        if (it == null) null else type.toLocalString()
    }
    @Composable
    fun CardText(textModifier: Modifier) = Column(
        modifier = textModifier.let {
            val titleHeight = with(LocalDensity.current) {
                ((titleStyle.fontSize * 2.168f).toPx()).toDp()
            }
            val emptyTextHeight = with(LocalDensity.current) {
                (labelStype.fontSize.toPx()).toDp()
            }
            it.height(titleHeight + (10.dp + emptyTextHeight))
        },
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            modifier = Modifier.let {
                if (label == null) it else it.padding(bottom = 5.dp)
            },
            style = titleStyle,
            maxLines = 2,
            autoSize = TextAutoSize.StepBased(
                minFontSize = 5.sp, maxFontSize = titleStyle.fontSize
            )
        )
        label?.let {
            Text(
                text = it,
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.surfaceContainer,
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 5.dp, vertical = 2.5.dp),
                style = labelStype,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    Card(
        modifier = modifier,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CardText(Modifier.padding(end = 8.dp))
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = stringResource(R.string.detail),
                modifier = Modifier.rotate(270f)
            )
        }
    }
}

@Composable
@ReadOnlyComposable
private fun DetailInfoState.DetailType.toLocalString(): String {
    return when (this) {
        is DetailInfoState.DetailType.Text -> stringResource(R.string.text)
        is DetailInfoState.DetailType.URL -> stringResource(R.string.url)
        is DetailInfoState.DetailType.Application -> stringResource(R.string.application)
        is DetailInfoState.DetailType.Picture -> stringResource(R.string.picture)
        is DetailInfoState.DetailType.Video -> stringResource(R.string.video)
        is DetailInfoState.DetailType.Audio -> stringResource(R.string.audio)
    }
}

@Composable
private fun ListCard(isSmallScreenSize: Boolean, content: LazyListScope.() -> Unit) {
    val windowSize = LocalWindowInfo.current.containerSize
    val heightLimit = windowSize.height.pxToDp().let {
        if (isSmallScreenSize) it else (it / 3f).let { dvd ->
            if (dvd < 200.dp) 210.dp else dvd
        }
    }
    Card(
        modifier = Modifier.heightIn(max = heightLimit),
        colors = CardDefaults.cardColors(
            MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = ShapeDefaults.Medium,
    ) {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 5.dp, vertical = 2.dp),
            content = content,
        )
    }
}

@Composable
private fun ThisTitle() {
    val windowSize = LocalWindowInfo.current.containerSize
    val titleStyle = MaterialTheme.typography.headlineSmall.copy(
        fontWeight = FontWeight.Bold, lineHeight = TextUnit.Unspecified
    )
    Text(
        text = stringResource(R.string.detail),
        style = titleStyle,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.requiredHeightIn(max = (windowSize.height.pxToDp() / 5)),
        maxLines = 1,
        autoSize = TextAutoSize.StepBased(minFontSize = 5.sp, maxFontSize = titleStyle.fontSize)
    )
}

@Preview
@Composable
private fun InfoDetailListPreview() {
    InfoDetailList(
        details = listOf(
            DetailInfoState(
                detailDescription = "Description Test",
                detailType = DetailInfoState.DetailType.Text("Test")
            )
        ),
        onSelected = {},
        onDismissRequest = {},
        isSmallScreenSize = false,
        feedback = AndroidEffectFeedback(androidx.compose.ui.platform.LocalView.current)
    )
}
