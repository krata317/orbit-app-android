package com.krata.orbit.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.krata.orbit.data.local.AppDatabase
import com.krata.orbit.data.model.*
import com.krata.orbit.data.repository.BudgetRepository
import com.krata.orbit.utils.DateUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class BudgetUiState(
    val monthKey: String                  = "",
    val budgetMonth: BudgetMonth?         = null,
    val expenses: List<Expense>           = emptyList(),
    val totalExpenditure: Double          = 0.0,
    val leftover: Double                  = 0.0,
    val expenseByCategory: Map<String, Double> = emptyMap(),
    val expenseByDay: Map<Int, Double>    = emptyMap()
)

class BudgetViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = BudgetRepository(AppDatabase.getInstance(application).budgetDao())
    private val currentMonthKey = DateUtils.currentMonthKey()

    val uiState: StateFlow<BudgetUiState> = combine(
        repo.getBudgetMonth(currentMonthKey),
        repo.getExpenses(currentMonthKey)
    ) { budgetMonth, expenses ->
        val bm = budgetMonth ?: BudgetMonth(
            monthKey     = currentMonthKey,
            budgetAmount = 5000.0,
            categories   = DEFAULT_BUDGET_CATEGORIES
        )
        val total        = expenses.sumOf { it.amount }
        val byCat        = expenses.groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
        val byDay        = expenses.groupBy { it.dayOfMonth }
            .mapValues { (_, list) -> list.sumOf { it.amount } }

        BudgetUiState(
            monthKey          = currentMonthKey,
            budgetMonth       = bm,
            expenses          = expenses,
            totalExpenditure  = total,
            leftover          = bm.budgetAmount - total,
            expenseByCategory = byCat,
            expenseByDay      = byDay
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BudgetUiState())

    fun ensureBudgetMonthExists() {
        viewModelScope.launch {
            val existing = repo.getBudgetMonthOnce(currentMonthKey)
            if (existing == null) {
                repo.saveBudgetMonth(
                    BudgetMonth(
                        monthKey     = currentMonthKey,
                        budgetAmount = 5000.0,
                        categories   = DEFAULT_BUDGET_CATEGORIES
                    )
                )
            }
        }
    }

    fun addExpense(description: String, amount: Double, dayOfMonth: Int, category: String) {
        viewModelScope.launch {
            repo.addExpense(
                Expense(
                    monthKey    = currentMonthKey,
                    description = description,
                    amount      = amount,
                    dayOfMonth  = dayOfMonth,
                    category    = category
                )
            )
        }
    }

    fun updateBudgetMonth(budget: BudgetMonth) {
        viewModelScope.launch { repo.saveBudgetMonth(budget) }
    }
}
