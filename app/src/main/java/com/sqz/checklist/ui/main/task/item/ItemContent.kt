package com.sqz.checklist.ui.main.task.item

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sqz.checklist.R
import com.sqz.checklist.ui.main.InfoAlertDialog
import com.sqz.checklist.ui.main.task.cardBackgoundColor

@Composable
internal fun ItemContent(
    description: String,
    createDate: String,
    reminderOnClick: () -> Unit,
    editOnClick: () -> Unit,
    timerIconState: Boolean,
    pinOnClick: () -> Unit,
    pinIconState: Boolean,
    modifier: Modifier = Modifier
) {
    Column {
        ContentTop(
            description = description,
            pinOnClick = pinOnClick,
            pinIcon = if (pinIconState) {
                R.drawable.pinned
            } else {
                R.drawable.pin
            }
        )
        Spacer(modifier = modifier.weight(1f))
        ContentBottom(
            createDate = createDate,
            reminderOnClick = reminderOnClick,
            editOnClick = editOnClick,
            timerIcon = if (timerIconState) {
                R.drawable.timer_on
            } else R.drawable.timer
        )
    }
}

@Composable
private fun ContentTop(
    description: String,
    pinOnClick: () -> Unit,
    pinIcon: Int,
    modifier: Modifier = Modifier
) {
    Row {
        Column(
            modifier = modifier
                .fillMaxWidth(0.75f)
                .height(58.dp),
            horizontalAlignment = Alignment.Start
        ) {
            var overflowState by rememberSaveable { mutableStateOf(false) }
            var overflowInfo by rememberSaveable { mutableStateOf(false) }
            Card(colors = cardBackgoundColor(), modifier = modifier.padding(top = 2.dp)) {
                Text(
                    text = description,
                    modifier = modifier
                        .padding(start = 3.dp)
                        .fillMaxSize()
                        .clickable(overflowState) { overflowInfo = true },
                    fontSize = 21.sp,
                    overflow = TextOverflow.Ellipsis,
                    onTextLayout = { textLayoutResult: TextLayoutResult ->
                        overflowState = textLayoutResult.hasVisualOverflow
                    },
                    maxLines = 2,
                    fontWeight = FontWeight.Normal,
                )
            }
            if (overflowInfo) {
                InfoAlertDialog(
                    onDismissRequest = { overflowInfo = false },
                    text = description
                )
            }
        }
        Spacer(modifier = modifier.weight(1f))
        IconButton(
            modifier = modifier.rotate(40f),
            onClick = pinOnClick
        ) {
            Icon(
                painter = painterResource(pinIcon),
                contentDescription = stringResource(R.string.pin)
            )
        }
    }
}

@Composable
private fun ContentBottom(
    createDate: String,
    reminderOnClick: () -> Unit,
    editOnClick: () -> Unit,
    timerIcon: Int,
    modifier: Modifier = Modifier
) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = createDate,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.padding(start = 2.dp)
        )
        Spacer(modifier = modifier
            .weight(1f)
            .widthIn(min = 10.dp))
        Row(modifier = modifier.widthIn(min = 40.dp, max = 100.dp)) {
            IconButton(modifier = modifier.size(30.dp), onClick = reminderOnClick) {
                Icon(
                    painter = painterResource(id = timerIcon),
                    contentDescription = stringResource(R.string.reminder)
                )
            }
            Spacer(modifier = modifier.weight(0.2f))
            IconButton(modifier = modifier.size(30.dp), onClick = editOnClick) {
                Icon(
                    painter = painterResource(id = R.drawable.edit),
                    contentDescription = stringResource(R.string.edit)
                )
            }
            Spacer(modifier = modifier.weight(0.2f))
        }
    }
}

@Preview
@Composable
private fun Preview() {
    Surface(modifier = Modifier.size(500.dp, 120.dp)){
        ItemContent(
            description = "description",
            createDate = "createDate",
            reminderOnClick = {},
            editOnClick = {},
            timerIconState = false,
            pinOnClick = {},
            pinIconState = false
        )
    }
}