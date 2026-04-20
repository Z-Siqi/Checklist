package com.sqz.checklist.presentation.task.list.scene

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.byValue
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.insert
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.ShapeDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sqz.checklist.R

/** This method expected to be called only within this package and its sub-packages. **/
@Composable
internal fun TaskSearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val textFieldState = rememberTextFieldState(initialText = searchQuery)
    OutlinedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 18.dp, top = 12.dp)
            .heightIn(min = 50.dp),
        shape = ShapeDefaults.ExtraLarge
    ) {
        Row(Modifier.heightIn(min = 50.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                modifier = Modifier.padding(start = 10.dp),
                imageVector = Icons.Filled.Search,
                contentDescription = stringResource(id = R.string.search)
            )
            BasicTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 9.dp, end = 9.dp, top = 10.dp, bottom = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                state = textFieldState,
                inputTransformation = InputTransformation.byValue { _, new ->
                    onSearchQueryChange(new.toString())
                    new
                },
                lineLimits = TextFieldLineLimits.SingleLine,
                textStyle = TextStyle(
                    fontSize = 24.sp,
                    textAlign = TextAlign.Start,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }
    }
    LaunchedEffect(Unit) {
        if (searchQuery != textFieldState.text) {
            textFieldState.clearText()
            textFieldState.edit { insert(0, searchQuery) }
        }
    }
}
