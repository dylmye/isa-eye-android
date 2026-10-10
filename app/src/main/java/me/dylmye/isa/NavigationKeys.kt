package me.dylmye.isa

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Accounts : NavKey

@Serializable data class AccountDetail(val productId: String) : NavKey

@Serializable data object Insights : NavKey

@Serializable data object Help : NavKey

@Serializable data object AddAccount : NavKey

@Serializable data class EditAccount(val productId: String) : NavKey
