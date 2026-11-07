package me.dylmye.isa.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Shows [text] in a plain tooltip beside [content] on hover or long press. Pointer users get the
 * same label that the content's accessibility description gives a screen reader.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Tooltip(
  text: String,
  modifier: Modifier = Modifier,
  position: TooltipPosition = TooltipPosition.Above,
  content: @Composable () -> Unit,
) {
  val anchorPosition =
    when (position) {
      TooltipPosition.Above -> TooltipAnchorPosition.Above
      TooltipPosition.Below -> TooltipAnchorPosition.Below
      TooltipPosition.Start -> TooltipAnchorPosition.Start
      TooltipPosition.End -> TooltipAnchorPosition.End
    }
  TooltipBox(
    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(anchorPosition),
    tooltip = { PlainTooltip { Text(text) } },
    state = rememberTooltipState(),
    modifier = modifier,
    content = content,
  )
}

/** An icon-only [IconButton] that also shows its label as a tooltip on hover or long press. */
@Composable
fun TooltipIconButton(
  icon: ImageVector,
  contentDescription: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  position: TooltipPosition = TooltipPosition.Above,
) {
  Tooltip(text = contentDescription, position = position) {
    IconButton(onClick = onClick, modifier = modifier, enabled = enabled) {
      Icon(icon, contentDescription = contentDescription)
    }
  }
}
