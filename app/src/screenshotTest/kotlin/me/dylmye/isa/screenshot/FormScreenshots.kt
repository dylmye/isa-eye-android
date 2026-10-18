package me.dylmye.isa.screenshot

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import me.dylmye.isa.theme.ISAEyeTheme
import me.dylmye.isa.ui.accounts.AddAccountContent
import me.dylmye.isa.ui.accounts.AddAccountUiState
import me.dylmye.isa.ui.accounts.EditAccountContent
import me.dylmye.isa.ui.accounts.EditAccountUiState
import me.dylmye.isa.ui.accounts.PickerOption
import me.dylmye.isa.ui.accounts.STILL_OPEN_ID
import me.dylmye.isa.ui.accounts.UpdateContributionsContent
import me.dylmye.isa.ui.accounts.UpdateContributionsUiState

/*
 * The account forms. `isCompact` is a parameter rather than something each form works out for
 * itself, so the presentation has to be stated here: below 600dp the app renders them full-screen
 * and passes true, and from 600dp it floats them as a card and passes false. Each form therefore
 * gets a preview per presentation, against the widths that match it — [CompactFormPreviews] and
 * [ExpandedFormPreviews].
 */

/** The add-account details step, where the provider and the opening tax year are chosen. */
@PreviewTest
@CompactFormPreviews
@Composable
fun AddAccountScreenshot() {
  ISAEyeTheme(dynamicColor = false) {
    AddAccountContent(
      uiState = PreviewAddAccount,
      onNicknameChange = {},
      onProviderSelected = {},
      onRulesetSelected = {},
      onProductTypeSelected = {},
      onFlexibleChange = {},
      onNext = {},
      onBack = {},
      onSave = {},
      onClose = {},
      isCompact = true,
    )
  }
}

@PreviewTest
@ExpandedFormPreviews
@Composable
fun AddAccountExpandedScreenshot() {
  ISAEyeTheme(dynamicColor = false) {
    AddAccountContent(
      uiState = PreviewAddAccount,
      onNicknameChange = {},
      onProviderSelected = {},
      onRulesetSelected = {},
      onProductTypeSelected = {},
      onFlexibleChange = {},
      onNext = {},
      onBack = {},
      onSave = {},
      onClose = {},
      isCompact = false,
    )
  }
}

/** The edit form, with a closing year picked. */
@PreviewTest
@CompactFormPreviews
@Composable
fun EditAccountScreenshot() {
  ISAEyeTheme(dynamicColor = false) {
    EditAccountContent(
      uiState = PreviewEditAccount,
      onNicknameChange = {},
      onProductTypeSelected = {},
      onFlexibleChange = {},
      onClosingYearSelected = {},
      onSave = {},
      onClose = {},
      isCompact = true,
    )
  }
}

@PreviewTest
@ExpandedFormPreviews
@Composable
fun EditAccountExpandedScreenshot() {
  ISAEyeTheme(dynamicColor = false) {
    EditAccountContent(
      uiState = PreviewEditAccount,
      onNicknameChange = {},
      onProductTypeSelected = {},
      onFlexibleChange = {},
      onClosingYearSelected = {},
      onSave = {},
      onClose = {},
      isCompact = false,
    )
  }
}

/** The contributions form, on a flexible account so the withdrawal copy is shown too. */
@PreviewTest
@CompactFormPreviews
@Composable
fun UpdateContributionsScreenshot() {
  ISAEyeTheme(dynamicColor = false) {
    UpdateContributionsContent(
      uiState = PreviewUpdateContributions,
      onYearSelected = {},
      onAmountChange = {},
      onSave = {},
      onClose = {},
      isCompact = true,
    )
  }
}

@PreviewTest
@ExpandedFormPreviews
@Composable
fun UpdateContributionsExpandedScreenshot() {
  ISAEyeTheme(dynamicColor = false) {
    UpdateContributionsContent(
      uiState = PreviewUpdateContributions,
      onYearSelected = {},
      onAmountChange = {},
      onSave = {},
      onClose = {},
      isCompact = false,
    )
  }
}

private val PreviewAddAccount =
  AddAccountUiState(
    providerOptions =
      listOf(
        PickerOption("lloyds", "Lloyds Bank"),
        PickerOption("vanguard", "Vanguard"),
        PickerOption("nationwide", "Nationwide"),
      ),
    selectedProviderId = "lloyds",
    rulesetOptions =
      listOf(
        PickerOption("2025/2026", "2025/2026"),
        PickerOption("2026/2027", "2026/2027"),
      ),
    selectedRulesetId = "2026/2027",
  )

private val PreviewEditAccount =
  EditAccountUiState(
    nickname = "My ISA",
    providerName = "Lloyds Bank",
    startTaxYear = "2025/2026",
    productTypeOptions = listOf(PickerOption("CASH", "Cash ISA")),
    selectedProductTypeId = "CASH",
    closingYearOptions = listOf(PickerOption(STILL_OPEN_ID, "Still open")),
    selectedClosingYearId = STILL_OPEN_ID,
    isLoading = false,
  )

private val PreviewUpdateContributions =
  UpdateContributionsUiState(
    accountName = "Stocks & Shares ISA",
    isFlexible = true,
    yearOptions =
      listOf(
        PickerOption("2025/2026", "2025/2026"),
        PickerOption("2026/2027", "2026/2027"),
      ),
    selectedYearId = "2026/2027",
    amountInput = "4320.50",
    recordedPence = 432_050,
    isLoading = false,
  )
