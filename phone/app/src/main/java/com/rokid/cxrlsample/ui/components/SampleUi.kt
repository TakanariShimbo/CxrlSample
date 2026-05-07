package com.rokid.cxrlsample.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.annotation.StringRes
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rokid.cxrlsample.R

/**
 * Default width ratio for primary action buttons.
 */
const val PRIMARY_BUTTON_WIDTH = 0.82f

/**
 * Reusable screen shell component.
 *
 * Provides unified background, title region, and scroll container.
 * Business pages only need to supply their content area.
 */
@Composable
fun SampleScreenShell(
    title: String,
    subtitle: String? = null,
    content: @Composable () -> Unit
) {
    Image(
        painter = painterResource(id = R.drawable.glasses_bg),
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
        contentDescription = null,
        alpha = 0.3f
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        subtitle?.let {
            Text(text = it, style = MaterialTheme.typography.bodyMedium)
        }
        content()
        Spacer(modifier = Modifier.height(8.dp))
    }
}

/**
 * Status information panel.
 *
 * @param title Panel title.
 * @param lines Status texts rendered line-by-line; blank lines are ignored.
 */
@Composable
fun StatusPanel(
    title: String = stringResource(id = R.string.status_panel_title),
    lines: List<String>
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            lines.filter { it.isNotBlank() }.forEach {
                Text(text = it, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/**
 * Section title text.
 */
@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Medium
    )
}

/**
 * Action button group container.
 */
@Composable
fun ActionButtonGroup(
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        content()
    }
}

/**
 * Builds a one-placeholder status line from string resources.
 */
@Composable
fun statusLine(
    @StringRes formatResId: Int,
    value: String
): String = stringResource(id = formatResId, value)

/**
 * Builds a boolean status line where true/false texts are resource-driven.
 */
@Composable
fun booleanStatusLine(
    @StringRes formatResId: Int,
    @StringRes trueResId: Int,
    @StringRes falseResId: Int,
    condition: Boolean
): String = statusLine(
    formatResId = formatResId,
    value = stringResource(id = if (condition) trueResId else falseResId)
)

/**
 * Renders the common "Actions" section title.
 */
@Composable
fun CommonActionsSectionTitle() {
    SectionTitle(text = stringResource(id = R.string.common_actions))
}

/**
 * Renders a hint text and returns false when precondition fails.
 *
 * Useful in action groups to centralize the common
 * "show hint and short-circuit" interaction pattern.
 */
@Composable
fun requireActionPrecondition(
    condition: Boolean,
    message: String
): Boolean {
    if (!condition) {
        Text(message)
        return false
    }
    return true
}

/**
 * Resource-based overload for [requireActionPrecondition].
 */
@Composable
fun requireActionPrecondition(
    condition: Boolean,
    @StringRes messageResId: Int
): Boolean = requireActionPrecondition(
    condition = condition,
    message = stringResource(id = messageResId)
)

/**
 * Builds status line list and filters null/blank values.
 */
fun statusLines(vararg lines: String?): List<String> {
    return lines.filterNotNull().filter { it.isNotBlank() }
}
