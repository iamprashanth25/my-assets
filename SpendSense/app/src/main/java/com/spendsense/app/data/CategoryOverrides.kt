package com.spendsense.app.data

import android.content.Context

/** Remembers "this merchant is always category X" choices the user makes. */
class CategoryOverrides(context: Context) {
    private val prefs = context.getSharedPreferences("category_overrides", Context.MODE_PRIVATE)

    fun all(): Map<String, Category> = prefs.all.mapNotNull { (key, value) ->
        val cat = (value as? String)?.let { runCatching { Category.valueOf(it) }.getOrNull() }
        cat?.let { key to it }
    }.toMap()

    fun set(merchant: String, category: Category) {
        prefs.edit().putString(Categorizer.overrideKey(merchant), category.name).apply()
    }
}
