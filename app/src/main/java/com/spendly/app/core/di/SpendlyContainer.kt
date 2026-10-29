package com.spendly.app.core.di

import android.content.Context
import com.spendly.app.core.preferences.UserPreferences
import com.spendly.app.data.local.SpendlyDatabase
import com.spendly.app.data.repository.SpendlyRepository
import com.spendly.app.data.repository.SpendlyRepositoryImpl

class SpendlyContainer(context: Context) {
    val database: SpendlyDatabase = SpendlyDatabase.getInstance(context)
    val userPreferences: UserPreferences = UserPreferences(context)
    val repository: SpendlyRepository = SpendlyRepositoryImpl(database)
}
