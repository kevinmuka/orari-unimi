package app.orariunimi

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AppSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(18.dp),
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        trailingIcon = if (value.isNotEmpty()) {{
            IconButton(onClick = { onValueChange("") }) {
                Icon(Icons.Outlined.Close, contentDescription = "Cancella ricerca")
            }
        }} else null
    )
}

@Composable
fun AppLoadingState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

data class AppNavigationItem(val label: String, val icon: ImageVector)

@Composable
fun AppBottomNavigation(
    items: List<AppNavigationItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    Column(Modifier.background(MaterialTheme.colorScheme.surfaceContainerLow)) {
        androidx.compose.material3.HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        )
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                val selected = index == selectedIndex
                val tint by animateColorAsState(
                    if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    label = "nav-tint-$index"
                )
                val capsuleColor by animateColorAsState(
                    if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                    label = "nav-capsule-$index"
                )
                Column(
                    Modifier.weight(1f).heightIn(min = 58.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onSelect(index) }
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                ) {
                    Box(
                        Modifier.size(width = 52.dp, height = 32.dp)
                            .background(capsuleColor, RoundedCornerShape(13.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(item.icon, contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else tint)
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(item.label, style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = tint)
                }
            }
        }
    }
}

@Composable
fun AppScreenHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    secondaryActionIcon: ImageVector? = null,
    secondaryActionDescription: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
    actionIcon: ImageVector? = null,
    actionDescription: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, Modifier.padding(top = 5.dp), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (secondaryActionIcon != null && onSecondaryAction != null) {
            Spacer(Modifier.width(8.dp))
            AppSurfaceIconButton(
                icon = secondaryActionIcon,
                contentDescription = secondaryActionDescription,
                onClick = onSecondaryAction
            )
        }
        if (actionIcon != null && onAction != null) {
            Spacer(Modifier.width(8.dp))
            AppSurfaceIconButton(
                icon = actionIcon,
                contentDescription = actionDescription,
                onClick = onAction
            )
        }
    }
}

@Composable
fun AppSurfaceIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        IconButton(onClick = onClick, modifier = Modifier.size(44.dp)) {
            Icon(icon, contentDescription = contentDescription)
        }
    }
}

@Composable
fun AppSectionHeader(
    title: String,
    count: Int,
    actionIcon: ImageVector,
    actionDescription: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(8.dp))
        AppCountBadge(count)
        Spacer(Modifier.weight(1f))
        trailing?.invoke()
        AppSurfaceIconButton(
            icon = actionIcon,
            contentDescription = actionDescription,
            onClick = onAction
        )
    }
}

@Composable
fun AppCountBadge(count: Int) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainer) {
        Text(count.toString(), Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun AppSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    icons: List<ImageVector> = emptyList()
) {
    if (options.isEmpty()) return
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        BoxWithConstraints(Modifier.padding(4.dp)) {
            val safeIndex = selectedIndex.coerceIn(options.indices)
            val segmentWidth = maxWidth / options.size
            val indicatorOffset by animateDpAsState(
                targetValue = segmentWidth * safeIndex,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "segmented-control-indicator"
            )
            Box(
                Modifier.offset { IntOffset(indicatorOffset.roundToPx(), 0) }
                    .width(segmentWidth)
                    .height(42.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(14.dp))
            )
            Row(Modifier.fillMaxWidth()) {
                options.forEachIndexed { index, label ->
                    AppSegmentOption(
                        label = label,
                        icon = icons.getOrNull(index),
                        selected = index == safeIndex,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelected(index) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.AppSegmentOption(
    label: String,
    icon: ImageVector?,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(180),
        label = "segmented-control-label"
    )
    Box(
        modifier.height(42.dp).clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        ),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(17.dp), tint = contentColor)
                Spacer(Modifier.width(6.dp))
            }
            Text(label, style = MaterialTheme.typography.labelLarge, color = contentColor,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
        }
    }
}

@Composable
fun CalendarItemIndicator(count: Int, color: Color, modifier: Modifier = Modifier) {
    if (count <= 0) return
    if (count >= 4) {
        Surface(modifier = modifier, shape = RoundedCornerShape(6.dp),
            color = color.copy(alpha = 0.16f)) {
            Text(count.toString(), Modifier.padding(horizontal = 3.dp, vertical = 1.dp),
                color = color, fontSize = 10.sp, lineHeight = 10.sp,
                fontWeight = FontWeight.Bold)
        }
    } else {
        Row(modifier, horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(2.dp)) {
            repeat(count) { Box(Modifier.size(4.dp).background(color, CircleShape)) }
        }
    }
}

@Composable
fun AppEmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: (() -> Unit)? = null,
    icon: ImageVector = Icons.Outlined.Inbox
) {
    Column(
        modifier.fillMaxWidth().padding(vertical = 36.dp, horizontal = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape,
            modifier = Modifier.size(52.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }
        Text(title, Modifier.fillMaxWidth().padding(top = 12.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Text(message, Modifier.fillMaxWidth().padding(top = 5.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        if (action != null && onAction != null) {
            FilledTonalButton(onClick = onAction, modifier = Modifier.padding(top = 14.dp)) {
                Text(action)
            }
        }
    }
}
