package com.sqz.checklist.presentation.history.task.list

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sqz.checklist.R
import com.sqz.checklist.ui.main.task.CardHeight
import kotlinx.datetime.LocalDate
import sqz.checklist.common.KmpLocalDatePatternFormatter
import sqz.checklist.common.TimestampHelper

/** This method expected to be called only within this package and its sub-packages. **/
@Composable
internal fun TaskHistoryCard( //TODO: improve the card UI
    historyLong: Long,
    taskDescription: String,
    createDate: LocalDate,
    onLongClick: () -> Unit,
    onClick: () -> Unit,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    val selectedBorder = isSelected.let {
        if (it) { // is selected state border
            return@let BorderStroke(3.dp, MaterialTheme.colorScheme.tertiary)
        }
        // default border
        return@let BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceDim)
    }
    OutlinedCard(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainer),
        border = selectedBorder,
        shape = ShapeDefaults.ExtraLarge
    ) {
        Column(
            modifier = Modifier
                .heightIn(min = (CardHeight + 8).dp)
                .combinedClickable(
                    onLongClick = onLongClick, onClick = onClick
                )
                .padding(bottom = 8.dp, top = 12.dp, start = 12.dp, end = 11.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Top
        ) {
            TaskDescription(description = taskDescription)
            Spacer(modifier = Modifier.weight(1f))
            if (historyLong > TimestampHelper.TWENTY_FIRST_CENTURY) {
                val toTime = TimestampHelper.toLocalDate(historyLong)
                Text(
                    text = getTaskHistoryDateText(localDate = toTime),
                    modifier = Modifier.align(Alignment.End),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp / LocalConfiguration.current.fontScale,
                    lineHeight = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.weight(1f))
            }
            Text(
                text = getTaskCreateDateText(localDate = createDate),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp / LocalConfiguration.current.fontScale,
                lineHeight = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TaskDescription(
    description: String,
    density: Density = LocalDensity.current
) = Column(
    modifier = Modifier.fillMaxWidth(0.75f),
) {
    val twoLinesHeightDp = with(density) {
        ((21.sp).toPx() * 2.5f).toDp()
    }
    Box(modifier = Modifier.height(twoLinesHeightDp)) {
        Text(
            text = description,
            modifier = Modifier.fillMaxSize(),
            fontSize = 21.sp,
            lineHeight = 21.sp,
            overflow = TextOverflow.Ellipsis,
            maxLines = 2,
            fontWeight = FontWeight.Normal,
        )
    }
}

@Composable
@ReadOnlyComposable
private fun getTaskCreateDateText(localDate: LocalDate): String {
    val dateFormat = KmpLocalDatePatternFormatter.format(
        localDate, stringResource(R.string.task_date_format),
    )
    return stringResource(R.string.task_creation_time, dateFormat)
}

@Composable
@ReadOnlyComposable
private fun getTaskHistoryDateText(localDate: LocalDate): String {
    val dateFormat = KmpLocalDatePatternFormatter.format(
        localDate, stringResource(R.string.task_date_format),
    )
    return stringResource(R.string.task_finish_time, dateFormat)
}

