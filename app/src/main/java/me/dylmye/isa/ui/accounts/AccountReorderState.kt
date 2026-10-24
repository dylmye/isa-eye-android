package me.dylmye.isa.ui.accounts

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt

/**
 * Long-press drag reordering for the accounts list.
 *
 * The order lives here while a drag is in flight, so rows move out of the way under the finger; the
 * finished order is handed back through `onOrderChange`. Each row gets its gesture from
 * [dragModifier]. [move] is the same operation for callers that cannot drag, such as TalkBack's
 * custom actions.
 */
@Stable
internal class AccountReorderState(
  private val listState: LazyListState,
  initialItems: List<AccountListItemUiState>,
  private val onOrderChange: (List<String>) -> Unit,
) {
  /** The list as currently shown, which follows the finger until the drag ends. */
  var items: List<AccountListItemUiState> by mutableStateOf(initialItems)
    private set

  private var draggedKey: String? by mutableStateOf<String?>(null)
  private var pointerY by mutableFloatStateOf(0f)
  private var grabOffset = 0f
  private var moved = false

  fun isDragging(key: String): Boolean = draggedKey == key

  /** Adopts the saved [items], unless a drag is in flight and its local order should win. */
  fun updateItems(items: List<AccountListItemUiState>) {
    if (draggedKey == null) this.items = items
  }

  /** Moves one account, then saves the order. A no-op move saves nothing. */
  fun move(from: Int, to: Int) {
    val reordered = items.move(from, to)
    if (reordered === items) return
    items = reordered
    onOrderChange(reordered.map { it.productId })
  }

  /**
   * Lifts one row out of the list on a long press and drops it back where the finger is. The row is
   * moved with a layout offset rather than a graphics layer, so its place in the list and its
   * position on the screen always agree, which is what the swap logic reads.
   */
  fun dragModifier(key: String): Modifier = Modifier
    .zIndex(if (isDragging(key)) 1f else 0f)
    .offset { dragOffset(key) }
    .pointerInput(key) {
      detectDragGesturesAfterLongPress(
        onDragStart = { position -> start(key, position.y) },
        onDrag = { change, amount ->
          change.consume()
          dragBy(key, amount.y)
        },
        onDragEnd = { finishDrag() },
        onDragCancel = { finishDrag() },
      )
    }

  private fun dragOffset(key: String): IntOffset {
    if (draggedKey != key) return IntOffset.Zero
    val slot = slotOf(key)
    val top = pointerY - grabOffset
    return IntOffset(0, (top - (slot?.offset ?: 0)).roundToInt())
  }

  private fun start(key: String, localY: Float) {
    val slot = slotOf(key) ?: return
    draggedKey = key
    grabOffset = localY
    pointerY = slot.offset + localY
    moved = false
  }

  /** Swaps the dragged row with whichever row its centre is now over. */
  private fun dragBy(key: String, deltaY: Float) {
    if (draggedKey != key) return
    pointerY += deltaY
    val dragged = slotOf(key) ?: return
    val centre = pointerY - grabOffset + dragged.size / 2f
    val target = slotAt(centre, excluding = key)
    if (target != null && target.index != dragged.index) {
      items = items.move(dragged.index, target.index)
      moved = true
    }
  }

  private fun finishDrag() {
    if (moved) onOrderChange(items.map { it.productId })
    draggedKey = null
    moved = false
  }

  private fun slotOf(key: String) =
    listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key }

  private fun slotAt(offset: Float, excluding: String) =
    listState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
      item.key != excluding && offset >= item.offset && offset < item.offset + item.size
    }
}

/** A copy of the list with the item at [from] moved to [to], or this list when nothing moves. */
internal fun <T> List<T>.move(from: Int, to: Int): List<T> {
  if (from == to || from !in indices || to !in indices) return this
  return toMutableList().apply { add(to, removeAt(from)) }
}
