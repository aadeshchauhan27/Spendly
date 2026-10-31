package com.spendly.app.core.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.spendly.app.R
import com.spendly.app.core.theme.CategoryBlueTravel
import com.spendly.app.core.theme.CategoryGreenBills
import com.spendly.app.core.theme.CategoryOrangeFood
import com.spendly.app.core.theme.CategoryPinkEntertainment
import com.spendly.app.core.theme.CategoryPurpleShopping
import com.spendly.app.core.theme.CategorySlateOthers
import java.util.Locale

@Composable
fun SpendlyCategoryBadge(
    categoryName: String? = null,
    iconName: String? = null,
    icon: ImageVector? = null,
    backgroundColor: Color = Color.Transparent,
    modifier: Modifier = Modifier,
    iconTint: Color = Color.White,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp
) {
    if (!categoryName.isNullOrBlank() || !iconName.isNullOrBlank()) {
        val drawableRes = getCategoryDrawable(iconName, categoryName)
        Image(
            painter = painterResource(id = drawableRes),
            contentDescription = categoryName ?: iconName,
            modifier = modifier
                .size(size)
                .clip(CircleShape)
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon ?: Icons.Default.Category,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

// Backward-compatible overload for explicit ImageVector + Color
@Composable
fun SpendlyCategoryBadge(
    icon: ImageVector,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    iconTint: Color = Color.White,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(iconSize)
        )
    }
}

data class CategoryIconItem(
    val key: String,
    val label: String,
    val drawableResId: Int,
    val vector: ImageVector
)

val AVAILABLE_CATEGORY_ICONS = listOf(
    "food_dining" to Icons.Default.Restaurant,
    "groceries" to Icons.Default.ShoppingCart,
    "shopping" to Icons.Default.ShoppingBag,
    "travel" to Icons.Default.Flight,
    "transport" to Icons.Default.DirectionsCar,
    "fuel" to Icons.Default.LocalGasStation,
    "bills_utilities" to Icons.Default.ReceiptLong,
    "healthcare" to Icons.Default.LocalHospital,
    "entertainment" to Icons.Default.SportsEsports,
    "home" to Icons.Default.Home,
    "education" to Icons.Default.School,
    "investment" to Icons.Default.Savings,
    "salary" to Icons.Default.AttachMoney,
    "transfer" to Icons.AutoMirrored.Filled.CompareArrows,
    "others" to Icons.Default.MoreHoriz,
    "fitness" to Icons.Default.FitnessCenter,
    "bank" to Icons.Default.AccountBalance,
    "block" to Icons.Default.Block
)

val CATEGORY_ICON_ITEMS = listOf(
    CategoryIconItem("food_dining", "Food & Dining", R.drawable.ic_cat_food_dining, Icons.Default.Restaurant),
    CategoryIconItem("groceries", "Groceries", R.drawable.ic_cat_groceries, Icons.Default.ShoppingCart),
    CategoryIconItem("shopping", "Shopping", R.drawable.ic_cat_shopping, Icons.Default.ShoppingBag),
    CategoryIconItem("travel", "Travel", R.drawable.ic_cat_travel, Icons.Default.Flight),
    CategoryIconItem("transport", "Transport", R.drawable.ic_cat_transport, Icons.Default.DirectionsCar),
    CategoryIconItem("fuel", "Fuel", R.drawable.ic_cat_fuel, Icons.Default.LocalGasStation),
    CategoryIconItem("bills_utilities", "Bills & Utilities", R.drawable.ic_cat_bills_utilities, Icons.Default.ReceiptLong),
    CategoryIconItem("healthcare", "Healthcare", R.drawable.ic_cat_healthcare, Icons.Default.LocalHospital),
    CategoryIconItem("entertainment", "Entertainment", R.drawable.ic_cat_entertainment, Icons.Default.SportsEsports),
    CategoryIconItem("home", "Home", R.drawable.ic_cat_home, Icons.Default.Home),
    CategoryIconItem("education", "Education", R.drawable.ic_cat_education, Icons.Default.School),
    CategoryIconItem("investment", "Investment", R.drawable.ic_cat_investment, Icons.Default.Savings),
    CategoryIconItem("salary", "Salary / Income", R.drawable.ic_cat_salary, Icons.Default.AttachMoney),
    CategoryIconItem("transfer", "Transfer", R.drawable.ic_cat_transfer, Icons.AutoMirrored.Filled.CompareArrows),
    CategoryIconItem("others", "Others", R.drawable.ic_cat_others, Icons.Default.MoreHoriz)
)

val AVAILABLE_CATEGORY_COLORS = listOf(
    "#FF8A3D", // Warm Orange (Food & Dining)
    "#FF6584", // Coral Pink (Shopping)
    "#38B6FF", // Cyan Blue (Travel)
    "#8C7CFF", // Lavender (Transport)
    "#FFAA00", // Amber Gold (Fuel)
    "#4DA8DA", // Sky Blue (Bills & Utilities)
    "#2EC4B6", // Mint Turquoise (Groceries)
    "#FF5964", // Rose Red (Healthcare)
    "#7952DE", // Deep Purple (Entertainment)
    "#2ECC71", // Emerald Green (Home)
    "#2B8CEE", // Cobalt Blue (Education)
    "#FFB703", // Sun Yellow (Investment)
    "#10B981", // Forest Mint (Salary)
    "#6366F1", // Indigo (Transfer)
    "#94A3B8", // Slate (Others)
    "#C8FF5A"  // Electric Lime (Accent)
)

fun getCategoryDrawable(iconName: String?, categoryName: String? = null): Int {
    val trimmedIcon = iconName?.trim()?.lowercase(Locale.ENGLISH) ?: ""
    if (trimmedIcon.isNotEmpty()) {
        when {
            trimmedIcon.contains("food") || trimmedIcon.contains("restaurant") || trimmedIcon.contains("dining") -> return R.drawable.ic_cat_food_dining
            trimmedIcon.contains("grocer") || trimmedIcon.contains("shopping_cart") || trimmedIcon.contains("cart") -> return R.drawable.ic_cat_groceries
            trimmedIcon.contains("shopping") || trimmedIcon.contains("bag") -> return R.drawable.ic_cat_shopping
            trimmedIcon.contains("travel") || trimmedIcon.contains("flight") || trimmedIcon.contains("plane") -> return R.drawable.ic_cat_travel
            trimmedIcon.contains("transport") || trimmedIcon.contains("car") || trimmedIcon.contains("directions_car") -> return R.drawable.ic_cat_transport
            trimmedIcon.contains("fuel") || trimmedIcon.contains("gas") || trimmedIcon.contains("petrol") -> return R.drawable.ic_cat_fuel
            trimmedIcon.contains("bill") || trimmedIcon.contains("utilit") || trimmedIcon.contains("receipt") -> return R.drawable.ic_cat_bills_utilities
            trimmedIcon.contains("health") || trimmedIcon.contains("hospital") || trimmedIcon.contains("medic") -> return R.drawable.ic_cat_healthcare
            trimmedIcon.contains("entertain") || trimmedIcon.contains("movie") || trimmedIcon.contains("game") || trimmedIcon.contains("esports") -> return R.drawable.ic_cat_entertainment
            trimmedIcon.contains("home") || trimmedIcon.contains("house") -> return R.drawable.ic_cat_home
            trimmedIcon.contains("educat") || trimmedIcon.contains("school") -> return R.drawable.ic_cat_education
            trimmedIcon.contains("invest") || trimmedIcon.contains("savings") -> return R.drawable.ic_cat_investment
            trimmedIcon.contains("salary") || trimmedIcon.contains("income") || trimmedIcon.contains("money") -> return R.drawable.ic_cat_salary
            trimmedIcon.contains("transfer") || trimmedIcon.contains("arrow") -> return R.drawable.ic_cat_transfer
            trimmedIcon.contains("other") || trimmedIcon.contains("more") -> return R.drawable.ic_cat_others
        }
    }

    val nameKey = (categoryName ?: "").trim().lowercase(Locale.ENGLISH)
    return when {
        nameKey.contains("food") || nameKey.contains("restaurant") || nameKey.contains("dining") || nameKey.contains("swiggy") || nameKey.contains("zomato") || nameKey.contains("cafe") -> R.drawable.ic_cat_food_dining
        nameKey.contains("grocer") || nameKey.contains("blinkit") || nameKey.contains("zepto") || nameKey.contains("instamart") -> R.drawable.ic_cat_groceries
        nameKey.contains("shopping") || nameKey.contains("amazon") || nameKey.contains("flipkart") || nameKey.contains("myntra") -> R.drawable.ic_cat_shopping
        nameKey.contains("travel") || nameKey.contains("flight") || nameKey.contains("irctc") || nameKey.contains("ticket") -> R.drawable.ic_cat_travel
        nameKey.contains("fuel") || nameKey.contains("petrol") || nameKey.contains("diesel") || nameKey.contains("cng") -> R.drawable.ic_cat_fuel
        nameKey.contains("transport") || nameKey.contains("car") || nameKey.contains("uber") || nameKey.contains("ola") || nameKey.contains("auto") || nameKey.contains("taxi") -> R.drawable.ic_cat_transport
        nameKey.contains("bill") || nameKey.contains("utilit") || nameKey.contains("electricity") || nameKey.contains("recharge") || nameKey.contains("jio") || nameKey.contains("airtel") -> R.drawable.ic_cat_bills_utilities
        nameKey.contains("health") || nameKey.contains("hospital") || nameKey.contains("medic") || nameKey.contains("pharmacy") || nameKey.contains("doctor") -> R.drawable.ic_cat_healthcare
        nameKey.contains("entertain") || nameKey.contains("movie") || nameKey.contains("cinema") || nameKey.contains("netflix") || nameKey.contains("spotify") || nameKey.contains("hotstar") || nameKey.contains("prime") || nameKey.contains("game") -> R.drawable.ic_cat_entertainment
        nameKey.contains("home") || nameKey.contains("house") || nameKey.contains("rent") || nameKey.contains("maintenance") -> R.drawable.ic_cat_home
        nameKey.contains("educat") || nameKey.contains("school") || nameKey.contains("college") || nameKey.contains("course") || nameKey.contains("fees") || nameKey.contains("book") -> R.drawable.ic_cat_education
        nameKey.contains("invest") || nameKey.contains("stock") || nameKey.contains("mutual") || nameKey.contains("groww") || nameKey.contains("zerodha") || nameKey.contains("sip") -> R.drawable.ic_cat_investment
        nameKey.contains("salary") || nameKey.contains("income") || nameKey.contains("payroll") -> R.drawable.ic_cat_salary
        nameKey.contains("transfer") || nameKey.contains("self") -> R.drawable.ic_cat_transfer
        else -> R.drawable.ic_cat_others
    }
}

fun getCategoryIcon(iconName: String?, categoryName: String? = null): ImageVector {
    val trimmedIcon = iconName?.trim()?.lowercase(Locale.ENGLISH) ?: ""
    if (trimmedIcon.isNotEmpty()) {
        when {
            trimmedIcon.contains("food") || trimmedIcon.contains("restaurant") || trimmedIcon.contains("dining") -> return Icons.Default.Restaurant
            trimmedIcon.contains("grocer") || trimmedIcon.contains("shopping_cart") || trimmedIcon.contains("cart") -> return Icons.Default.ShoppingCart
            trimmedIcon.contains("shopping") || trimmedIcon.contains("bag") -> return Icons.Default.ShoppingBag
            trimmedIcon.contains("flight") || trimmedIcon.contains("travel") || trimmedIcon.contains("plane") -> return Icons.Default.Flight
            trimmedIcon.contains("transport") || trimmedIcon.contains("car") || trimmedIcon.contains("directions_car") -> return Icons.Default.DirectionsCar
            trimmedIcon.contains("fuel") || trimmedIcon.contains("gas") || trimmedIcon.contains("petrol") -> return Icons.Default.LocalGasStation
            trimmedIcon.contains("receipt") || trimmedIcon.contains("bill") || trimmedIcon.contains("utility") -> return Icons.Default.ReceiptLong
            trimmedIcon.contains("esports") || trimmedIcon.contains("entertainment") || trimmedIcon.contains("movie") || trimmedIcon.contains("game") -> return Icons.Default.SportsEsports
            trimmedIcon.contains("hospital") || trimmedIcon.contains("health") || trimmedIcon.contains("medical") -> return Icons.Default.LocalHospital
            trimmedIcon.contains("home") || trimmedIcon.contains("house") -> return Icons.Default.Home
            trimmedIcon.contains("school") || trimmedIcon.contains("education") -> return Icons.Default.School
            trimmedIcon.contains("savings") || trimmedIcon.contains("investment") -> return Icons.Default.Savings
            trimmedIcon.contains("attach_money") || trimmedIcon.contains("salary") || trimmedIcon.contains("income") -> return Icons.Default.AttachMoney
            trimmedIcon.contains("transfer") || trimmedIcon.contains("arrow") -> return Icons.AutoMirrored.Filled.CompareArrows
            trimmedIcon.contains("fitness") || trimmedIcon.contains("gym") -> return Icons.Default.FitnessCenter
            trimmedIcon.contains("person") -> return Icons.Default.Person
            trimmedIcon.contains("bank") || trimmedIcon.contains("account_balance") -> return Icons.Default.AccountBalance
            trimmedIcon.contains("spam") || trimmedIcon.contains("block") -> return Icons.Default.Block
            trimmedIcon.contains("more") || trimmedIcon.contains("other") -> return Icons.Default.MoreHoriz
        }
    }

    val nameKey = (categoryName ?: "").trim().lowercase(Locale.ENGLISH)
    return when {
        nameKey.contains("spam") || nameKey.contains("block") || nameKey.contains("ignored") -> Icons.Default.Block
        nameKey.contains("food") || nameKey.contains("restaurant") || nameKey.contains("dining") || nameKey.contains("swiggy") || nameKey.contains("zomato") -> Icons.Default.Restaurant
        nameKey.contains("grocer") || nameKey.contains("blinkit") || nameKey.contains("zepto") || nameKey.contains("instamart") -> Icons.Default.ShoppingCart
        nameKey.contains("shopping") || nameKey.contains("myntra") || nameKey.contains("flipkart") || nameKey.contains("amazon") -> Icons.Default.ShoppingBag
        nameKey.contains("travel") || nameKey.contains("flight") || nameKey.contains("irctc") -> Icons.Default.Flight
        nameKey.contains("fuel") || nameKey.contains("petrol") || nameKey.contains("gas") -> Icons.Default.LocalGasStation
        nameKey.contains("car") || nameKey.contains("transport") || nameKey.contains("uber") || nameKey.contains("ola") -> Icons.Default.DirectionsCar
        nameKey.contains("receipt") || nameKey.contains("bill") || nameKey.contains("utility") || nameKey.contains("electricity") || nameKey.contains("jio") -> Icons.Default.ReceiptLong
        nameKey.contains("esports") || nameKey.contains("entertainment") || nameKey.contains("movie") || nameKey.contains("netflix") || nameKey.contains("game") -> Icons.Default.SportsEsports
        nameKey.contains("hospital") || nameKey.contains("medical") || nameKey.contains("health") || nameKey.contains("pharmacy") -> Icons.Default.LocalHospital
        nameKey.contains("attach_money") || nameKey.contains("salary") || nameKey.contains("income") -> Icons.Default.AttachMoney
        nameKey.contains("savings") || nameKey.contains("investment") || nameKey.contains("stock") -> Icons.Default.Savings
        nameKey.contains("home") || nameKey.contains("rent") || nameKey.contains("house") -> Icons.Default.Home
        nameKey.contains("school") || nameKey.contains("education") || nameKey.contains("book") -> Icons.Default.School
        nameKey.contains("fitness") || nameKey.contains("gym") || nameKey.contains("sport") -> Icons.Default.FitnessCenter
        nameKey.contains("transfer") -> Icons.AutoMirrored.Filled.CompareArrows
        nameKey.contains("person") || nameKey.contains("family") -> Icons.Default.Person
        nameKey.contains("account_balance") || nameKey.contains("bank") -> Icons.Default.AccountBalance
        else -> Icons.Default.Category
    }
}

fun getCategoryIconAndColor(name: String, iconName: String? = null): Pair<ImageVector, Color> {
    val icon = getCategoryIcon(iconName, name)
    val color = when (name.lowercase(Locale.ENGLISH)) {
        "spam", "ignored" -> Color(0xFF78909C)
        "food", "food & dining", "swiggy", "zomato" -> CategoryOrangeFood
        "shopping", "amazon", "flipkart", "myntra" -> CategoryPurpleShopping
        "travel", "uber", "ola", "flight", "irctc" -> CategoryBlueTravel
        "bills", "bills & utilities", "jio", "electricity", "recharge" -> CategoryGreenBills
        "entertainment", "netflix", "movies", "spotify" -> CategoryPinkEntertainment
        "salary", "income", "freelance" -> CategoryGreenBills
        "investment", "stocks", "mutual funds" -> CategoryBlueTravel
        else -> CategorySlateOthers
    }
    return Pair(icon, color)
}
