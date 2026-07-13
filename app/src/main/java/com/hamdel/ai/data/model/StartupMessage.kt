package com.hamdel.ai.data.model

data class StartupMessage(
    val version: Int = 1,
    val title: String = "به همدل خوش آمدید",
    val message: String = "همدل یک دستیار هوشمند برای شناخت بهتر رابطه است. این برنامه تصمیم قطعی نمی‌گیرد و جایگزین مشاور انسانی نیست؛ فقط با رضایت دوطرفه، گفتگوها و داده‌های واردشده را تحلیل می‌کند.",
    val primaryAction: String = "متوجه شدم",
    val secondaryAction: String? = null,
    val showEveryLaunch: Boolean = true
)
