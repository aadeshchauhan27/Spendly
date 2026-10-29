package com.spendly.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.spendly.app.data.local.dao.BudgetDao
import com.spendly.app.data.local.dao.CategoryDao
import com.spendly.app.data.local.dao.GoalContributionDao
import com.spendly.app.data.local.dao.ImportSessionDao
import com.spendly.app.data.local.dao.SavingsGoalDao
import com.spendly.app.data.local.dao.TransactionDao
import com.spendly.app.data.local.entity.BudgetEntity
import com.spendly.app.data.local.entity.CategoryEntity
import com.spendly.app.data.local.entity.GoalContributionEntity
import com.spendly.app.data.local.entity.ImportSessionEntity
import com.spendly.app.data.local.entity.SavingsGoalEntity
import com.spendly.app.data.local.entity.TransactionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        BudgetEntity::class,
        SavingsGoalEntity::class,
        GoalContributionEntity::class,
        ImportSessionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SpendlyDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun savingsGoalDao(): SavingsGoalDao
    abstract fun goalContributionDao(): GoalContributionDao
    abstract fun importSessionDao(): ImportSessionDao

    companion object {
        @Volatile
        private var INSTANCE: SpendlyDatabase? = null

        fun getInstance(context: Context): SpendlyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SpendlyDatabase::class.java,
                    "spendly_database.db"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Populate default system categories on database creation
                            CoroutineScope(Dispatchers.IO).launch {
                                getInstance(context).seedDefaultCategories()
                            }
                        }
                    })
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    suspend fun seedDefaultCategories() {
        val defaultCategories = listOf(
            CategoryEntity(id = 1, name = "Food & Dining", type = "EXPENSE", colorHex = "#FF7A00", iconName = "restaurant", isSystem = true, sortOrder = 1),
            CategoryEntity(id = 2, name = "Shopping", type = "EXPENSE", colorHex = "#7C5CFC", iconName = "shopping_bag", isSystem = true, sortOrder = 2),
            CategoryEntity(id = 3, name = "Travel", type = "EXPENSE", colorHex = "#2979FF", iconName = "flight", isSystem = true, sortOrder = 3),
            CategoryEntity(id = 4, name = "Bills & Utilities", type = "EXPENSE", colorHex = "#00C853", iconName = "receipt_long", isSystem = true, sortOrder = 4),
            CategoryEntity(id = 5, name = "Entertainment", type = "EXPENSE", colorHex = "#E040FB", iconName = "sports_esports", isSystem = true, sortOrder = 5),
            CategoryEntity(id = 6, name = "Investment", type = "EXPENSE", colorHex = "#00B0FF", iconName = "savings", isSystem = true, sortOrder = 6),
            CategoryEntity(id = 7, name = "Salary", type = "INCOME", colorHex = "#00E676", iconName = "work", isSystem = true, sortOrder = 7),
            CategoryEntity(id = 8, name = "Others", type = "EXPENSE", colorHex = "#546E7A", iconName = "more_horiz", isSystem = true, sortOrder = 8),
            CategoryEntity(id = 9, name = "Spam", type = "SPAM", colorHex = "#78909C", iconName = "block", isSystem = true, sortOrder = 9)
        )
        categoryDao().insertAll(defaultCategories)
    }
}
