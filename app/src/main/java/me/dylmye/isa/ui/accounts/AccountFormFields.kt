package me.dylmye.isa.ui.accounts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/** A read-only dropdown over [options], showing the label of [selectedId]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptionDropdownField(
  label: String,
  options: List<PickerOption>,
  selectedId: String?,
  onSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  var expanded by remember { mutableStateOf(false) }
  val selectedLabel = options.firstOrNull { it.id == selectedId }?.label

  ExposedDropdownMenuBox(
    expanded = expanded,
    onExpandedChange = { expanded = it },
    modifier = modifier.fillMaxWidth(),
  ) {
    OutlinedTextField(
      value = selectedLabel ?: "",
      onValueChange = {},
      readOnly = true,
      label = { Text(label) },
      trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
      modifier =
        Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
    )
    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      options.forEach { option ->
        DropdownMenuItem(
          text = {
            Column {
              Text(option.label)
              option.description?.let { description ->
                Text(
                  text = description,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          },
          onClick = {
            onSelected(option.id)
            expanded = false
          },
        )
      }
    }
  }
}

/** A disabled field used to show a value that cannot be edited (e.g. provider, opening year). */
@Composable
fun ReadOnlyField(label: String, value: String, modifier: Modifier = Modifier) {
  OutlinedTextField(
    value = value,
    onValueChange = {},
    enabled = false,
    label = { Text(label) },
    modifier = modifier.fillMaxWidth(),
  )
}

/** The "Flexible?" switch with its explanatory note. */
@Composable
fun FlexibleField(
  flexible: Boolean,
  onFlexibleChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
    Column(Modifier.weight(1f)) {
      Text("Flexible", style = MaterialTheme.typography.bodyLarge)
      Text(
        text = "Check with your bank whether this ISA is flexible.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Switch(checked = flexible, onCheckedChange = onFlexibleChange)
  }
}
