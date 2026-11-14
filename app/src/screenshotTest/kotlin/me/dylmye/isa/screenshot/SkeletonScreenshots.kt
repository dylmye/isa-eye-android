package me.dylmye.isa.screenshot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import me.dylmye.isa.theme.ISAEyeTheme
import me.dylmye.isa.ui.components.SkeletonBox
import me.dylmye.isa.ui.components.SkeletonCard
import me.dylmye.isa.ui.components.SkeletonGroup
import me.dylmye.isa.ui.components.SkeletonLine

/** The shared skeleton pieces: a headline card, a progress card and a list row. */
@PreviewTest
@Preview(name = "Skeleton 400x600", widthDp = 400, heightDp = 600)
@Composable
fun SkeletonComponentsScreenshot() {
  ISAEyeTheme(dynamicColor = false) {
    Surface(color = MaterialTheme.colorScheme.background) {
      SkeletonGroup(label = "Loading content", modifier = Modifier.fillMaxSize()) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          SkeletonCard {
            SkeletonLine(modifier = Modifier.width(160.dp), height = 48.dp)
            SkeletonLine(modifier = Modifier.width(120.dp), height = 20.dp)
          }
          SkeletonCard {
            SkeletonLine(modifier = Modifier.width(140.dp), height = 20.dp)
            SkeletonLine(modifier = Modifier.fillMaxWidth(), height = 8.dp)
            SkeletonLine(modifier = Modifier.width(220.dp), height = 20.dp)
            SkeletonLine(modifier = Modifier.fillMaxWidth(0.8f), height = 16.dp)
          }
          SkeletonCard {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(16.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              SkeletonBox(modifier = Modifier.size(40.dp), shape = CircleShape)
              Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                SkeletonLine(modifier = Modifier.fillMaxWidth(0.6f), height = 16.dp)
                SkeletonLine(modifier = Modifier.fillMaxWidth(0.35f), height = 12.dp)
              }
              Spacer(modifier = Modifier.width(4.dp))
              SkeletonLine(modifier = Modifier.width(72.dp), height = 20.dp)
            }
          }
        }
      }
    }
  }
}
