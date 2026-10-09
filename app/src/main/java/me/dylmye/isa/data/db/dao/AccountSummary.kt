package me.dylmye.isa.data.db.dao

/** Flattened product + provider row backing the accounts list. */
data class AccountSummary(
  val productId: String,
  val friendlyName: String,
  val productTypeId: String,
  val providerId: String,
  val providerName: String,
  val providerColour: String,
)
