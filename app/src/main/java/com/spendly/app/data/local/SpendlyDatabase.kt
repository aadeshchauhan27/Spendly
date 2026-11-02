package com.spendly.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.spendly.app.data.local.dao.BudgetDao
import com.spendly.app.data.local.dao.CategoryDao
import com.spendly.app.data.local.dao.DeletedTransactionDao
import com.spendly.app.data.local.dao.GoalContributionDao
import com.spendly.app.data.local.dao.ImportSessionDao
import com.spendly.app.data.local.dao.SavingsGoalDao
import com.spendly.app.data.local.dao.TransactionDao
import com.spendly.app.data.local.entity.BudgetEntity
import com.spendly.app.data.local.entity.CategoryEntity
import com.spendly.app.data.local.entity.DeletedTransactionEntity
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
        ImportSessionEntity::class,
        DeletedTransactionEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class SpendlyDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun savingsGoalDao(): SavingsGoalDao
    abstract fun goalContributionDao(): GoalContributionDao
    abstract fun importSessionDao(): ImportSessionDao
    abstract fun deletedTransactionDao(): DeletedTransactionDao

    companion object {
        @Volatile
        private var INSTANCE: SpendlyDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `deleted_transactions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `bank_reference_num` TEXT,
                        `raw_identifier` TEXT,
                        `amount_paise` INTEGER NOT NULL,
                        `timestamp_ms` INTEGER NOT NULL,
                        `counterparty_name` TEXT NOT NULL,
                        `sms_body` TEXT,
                        `deleted_at_ms` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_deleted_transactions_bank_reference_num` ON `deleted_transactions` (`bank_reference_num`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_deleted_transactions_raw_identifier` ON `deleted_transactions` (`raw_identifier`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_deleted_transactions_timestamp_ms` ON `deleted_transactions` (`timestamp_ms`)")
            }
        }

        fun getInstance(context: Context): SpendlyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SpendlyDatabase::class.java,
                    "spendly_database.db"
                )
                    .addMigrations(MIGRATION_1_2)
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
            CategoryEntity(name = "Food & Dining", type = "EXPENSE", colorHex = "#FF7A00", iconName = "food_dining", isSystem = true, sortOrder = 1),
            CategoryEntity(name = "Groceries", type = "EXPENSE", colorHex = "#2EC4B6", iconName = "groceries", isSystem = true, sortOrder = 2),
            CategoryEntity(name = "Shopping", type = "EXPENSE", colorHex = "#7C5CFC", iconName = "shopping", isSystem = true, sortOrder = 3),
            CategoryEntity(name = "Fuel", type = "EXPENSE", colorHex = "#FFAA00", iconName = "fuel", isSystem = true, sortOrder = 4),
            CategoryEntity(name = "Transport", type = "EXPENSE", colorHex = "#8C7CFF", iconName = "transport", isSystem = true, sortOrder = 5),
            CategoryEntity(name = "Travel", type = "EXPENSE", colorHex = "#2979FF", iconName = "travel", isSystem = true, sortOrder = 6),
            CategoryEntity(name = "Bills & Utilities", type = "EXPENSE", colorHex = "#00C853", iconName = "bills_utilities", isSystem = true, sortOrder = 7),
            CategoryEntity(name = "Loans & EMI", type = "EXPENSE", colorHex = "#4F46E5", iconName = "loans", isSystem = true, sortOrder = 8),
            CategoryEntity(name = "Househelp", type = "EXPENSE", colorHex = "#F43F5E", iconName = "househelp", isSystem = true, sortOrder = 9),
            CategoryEntity(name = "Healthcare", type = "EXPENSE", colorHex = "#FF5964", iconName = "healthcare", isSystem = true, sortOrder = 10),
            CategoryEntity(name = "Entertainment", type = "EXPENSE", colorHex = "#E040FB", iconName = "entertainment", isSystem = true, sortOrder = 11),
            CategoryEntity(name = "Home", type = "EXPENSE", colorHex = "#2ECC71", iconName = "home", isSystem = true, sortOrder = 12),
            CategoryEntity(name = "Education", type = "EXPENSE", colorHex = "#2B8CEE", iconName = "education", isSystem = true, sortOrder = 13),
            CategoryEntity(name = "Investment", type = "EXPENSE", colorHex = "#00B0FF", iconName = "investment", isSystem = true, sortOrder = 14),
            CategoryEntity(name = "Insurance", type = "EXPENSE", colorHex = "#0D9488", iconName = "insurance", isSystem = true, sortOrder = 15),
            CategoryEntity(name = "Personal Care", type = "EXPENSE", colorHex = "#EC4899", iconName = "personal_care", isSystem = true, sortOrder = 16),
            CategoryEntity(name = "Salary", type = "INCOME", colorHex = "#00E676", iconName = "salary", isSystem = true, sortOrder = 17),
            CategoryEntity(name = "Transfer", type = "EXPENSE", colorHex = "#6366F1", iconName = "transfer", isSystem = true, sortOrder = 18),
            CategoryEntity(name = "Others", type = "EXPENSE", colorHex = "#546E7A", iconName = "others", isSystem = true, sortOrder = 19),
            CategoryEntity(name = "Spam", type = "SPAM", colorHex = "#78909C", iconName = "block", isSystem = true, sortOrder = 20)
        )
        val categoryDao = categoryDao()
        for (cat in defaultCategories) {
            val existing = categoryDao.getCategoryByName(cat.name)
            if (existing == null) {
                val alias = when (cat.name) {
                    "Groceries" -> categoryDao.getCategoryByName("Grocery")
                    "Loans & EMI" -> categoryDao.getCategoryByName("Loans") ?: categoryDao.getCategoryByName("Loan")
                    "Home" -> categoryDao.getCategoryByName("Home & Rent")
                    else -> null
                }
                if (alias != null) {
                    categoryDao.update(alias.copy(iconName = cat.iconName, colorHex = cat.colorHex))
                } else {
                    categoryDao.insert(cat)
                }
            } else {
                if (existing.iconName != cat.iconName || existing.colorHex != cat.colorHex) {
                    categoryDao.update(existing.copy(iconName = cat.iconName, colorHex = cat.colorHex))
                }
            }
        }
    }
}
