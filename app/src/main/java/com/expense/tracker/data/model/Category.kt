package com.expense.tracker.data.model

data class Category(
    val id: String = "",
    val name: String = "",
    val subCategories: List<SubCategory> = emptyList(),
    val isHidden: Boolean = false,
    val order: Int = 0
)

data class SubCategory(
    val name: String = "",
    val isHidden: Boolean = false
)

object CategoryData {
    val categories = listOf(
        Category(
            name = "Accommodation",
            subCategories = listOf("Rent", "Food", "Electronics", "Flat Maintenance", "Other Things", "Light Bill", "Laundry").map { SubCategory(it) }
        ),
        Category(
            name = "Food",
            subCategories = listOf("Tiffin", "Outside Food", "Fast Food", "Eggs", "Swiggy", "Zomato", "Food With Friends", "Soft Drink", "Water bottle", "Packets", "Healthy Food", "Sweet", "Drink").map { SubCategory(it) }
        ),
        Category(
            name = "Personal Care",
            subCategories = listOf("Haircut", "Skin Care Products").map { SubCategory(it) }
        ),
        Category(
            name = "Health",
            subCategories = listOf("Doctor Fees", "Medicine", "Medical Tests").map { SubCategory(it) }
        ),
        Category(
            name = "Transport",
            subCategories = listOf("Fuel", "Maintenance", "Bus Ticket", "Auto rickshaw", "Accessories & Parts", "City Bus", "Train Ticket").map { SubCategory(it) }
        ),
        Category(
            name = "Monthly Bills",
            subCategories = listOf("My Mobile Bill", "Mummy's Mobile Bill", "Subscriptions").map { SubCategory(it) }
        ),
        Category(
            name = "Grocery",
            subCategories = listOf("Home Grocery", "Anand Grocery").map { SubCategory(it) }
        ),
        Category(
            name = "Shoping",
            subCategories = listOf("Amazon", "Flipkart", "Clothing", "Electronics", "Ajio", "Myntra", "Others").map { SubCategory(it) }
        ),
        Category(
            name = "Education & Job",
            subCategories = listOf("Exam Fees", "Course Fees", "PHD Fees", "Stationery", "Print & Xerox", "PHD Related").map { SubCategory(it) }
        ),
        Category(
            name = "Social",
            subCategories = listOf("General").map { SubCategory(it) }
        ),
        Category(
            name = "Other",
            subCategories = listOf("General").map { SubCategory(it) }
        )
    )
}
