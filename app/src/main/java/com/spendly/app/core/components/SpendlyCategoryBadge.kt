package com.spendly.app.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.spendly.app.core.theme.CategoryBlueTravel
import com.spendly.app.core.theme.CategoryGreenBills
import com.spendly.app.core.theme.CategoryOrangeFood
import com.spendly.app.core.theme.CategoryPinkEntertainment
import com.spendly.app.core.theme.CategoryPurpleShopping
import com.spendly.app.core.theme.CategorySlateOthers
import java.util.Locale

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

val AVAILABLE_CATEGORY_ICONS = listOf(
    "restaurant" to Icons.Default.Restaurant,
    "shopping_bag" to Icons.Default.ShoppingBag,
    "shopping_cart" to Icons.Default.ShoppingCart,
    "flight" to Icons.Default.Flight,
    "directions_car" to Icons.Default.DirectionsCar,
    "receipt_long" to Icons.Default.ReceiptLong,
    "sports_esports" to Icons.Default.SportsEsports,
    "local_hospital" to Icons.Default.LocalHospital,
    "attach_money" to Icons.Default.AttachMoney,
    "savings" to Icons.Default.Savings,
    "home" to Icons.Default.Home,
    "school" to Icons.Default.School,
    "fitness_center" to Icons.Default.FitnessCenter,
    "person" to Icons.Default.Person,
    "account_balance" to Icons.Default.AccountBalance,
    "block" to Icons.Default.Block,
    "category" to Icons.Default.Category
)

val AVAILABLE_CATEGORY_COLORS = listOf(
    "#FF7A00", // Orange (Food)
    "#7C5CFC", // Purple (Shopping)
    "#2979FF", // Blue (Travel)
    "#00C853", // Green (Bills)
    "#E040FB", // Pink (Entertainment)
    "#FF5252", // Red (Medical)
    "#00B0FF", // Sky Blue (Investment)
    "#C8FF5A", // Electric Lime (Salary/Savings)
    "#FFAB00", // Amber (Education)
    "#546E7A"  // Slate (Others)
)

fun getCategoryIcon(iconName: String?, categoryName: String? = null): ImageVector {
    val key = ((iconName ?: "") + " " + (categoryName ?: "")).lowercase(Locale.ENGLISH)
    return when {
        key.contains("spam") || key.contains("block") || key.contains("ignored") -> Icons.Default.Block
        key.contains("restaurant") || key.contains("food") || key.contains("cafe") || key.contains("dining") || key.contains("swiggy") || key.contains("zomato") -> Icons.Default.Restaurant
        key.contains("shopping_bag") || key.contains("shopping") || key.contains("myntra") || key.contains("flipkart") || key.contains("amazon") -> Icons.Default.ShoppingBag
        key.contains("shopping_cart") || key.contains("grocery") || key.contains("groceries") || key.contains("blinkit") || key.contains("zepto") || key.contains("instamart") -> Icons.Default.ShoppingCart
        key.contains("flight") || key.contains("travel") || key.contains("flight") -> Icons.Default.Flight
        key.contains("car") || key.contains("directions_car") || key.contains("fuel") || key.contains("petrol") || key.contains("uber") || key.contains("ola") -> Icons.Default.DirectionsCar
        key.contains("receipt") || key.contains("bill") || key.contains("utility") || key.contains("electricity") || key.contains("jio") || key.contains("recharge") -> Icons.Default.ReceiptLong
        key.contains("esports") || key.contains("movie") || key.contains("entertainment") || key.contains("netflix") || key.contains("game") -> Icons.Default.SportsEsports
        key.contains("hospital") || key.contains("medical") || key.contains("health") || key.contains("pharmacy") -> Icons.Default.LocalHospital
        key.contains("attach_money") || key.contains("salary") || key.contains("income") || key.contains("work") -> Icons.Default.AttachMoney
        key.contains("savings") || key.contains("investment") || key.contains("stock") -> Icons.Default.Savings
        key.contains("home") || key.contains("rent") || key.contains("house") -> Icons.Default.Home
        key.contains("school") || key.contains("education") || key.contains("book") -> Icons.Default.School
        key.contains("fitness") || key.contains("gym") || key.contains("sport") -> Icons.Default.FitnessCenter
        key.contains("person") || key.contains("family") || key.contains("transfer") -> Icons.Default.Person
        key.contains("account_balance") || key.contains("bank") -> Icons.Default.AccountBalance
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
