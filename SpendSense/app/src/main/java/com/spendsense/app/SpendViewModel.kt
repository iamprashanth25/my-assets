package com.spendsense.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.spendsense.app.data.Categorizer
import com.spendsense.app.data.Category
import com.spendsense.app.data.CategoryOverrides
import com.spendsense.app.data.SmsRepository
import com.spendsense.app.data.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

data class UiState(
    val loading: Boolean = false,
    val transactions: List<Transaction> = emptyList(),
    val month: YearMonth = YearMonth.now(),
    val categoryFilter: Category? = null,
    val error: String? = null,
)

class SpendViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = SmsRepository(app)
    private val overrides = CategoryOverrides(app)

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            val since = LocalDate.now().minusMonths(12).withDayOfMonth(1)
                .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val result = runCatching {
                withContext(Dispatchers.IO) { repo.loadTransactions(since, overrides.all()) }
            }
            _state.update { s ->
                result.fold(
                    onSuccess = { s.copy(loading = false, transactions = it) },
                    onFailure = { s.copy(loading = false, error = it.message ?: "Could not read SMS") },
                )
            }
        }
    }

    fun previousMonth() = _state.update { it.copy(month = it.month.minusMonths(1), categoryFilter = null) }

    fun nextMonth() = _state.update {
        if (it.month < YearMonth.now()) it.copy(month = it.month.plusMonths(1), categoryFilter = null) else it
    }

    fun toggleFilter(category: Category) = _state.update {
        it.copy(categoryFilter = if (it.categoryFilter == category) null else category)
    }

    /** Re-categorizes every transaction from this merchant, now and in future loads. */
    fun recategorize(merchant: String, category: Category) {
        overrides.set(merchant, category)
        val key = Categorizer.overrideKey(merchant)
        _state.update { s ->
            s.copy(transactions = s.transactions.map {
                if (Categorizer.overrideKey(it.merchant) == key) it.copy(category = category) else it
            })
        }
    }
}
