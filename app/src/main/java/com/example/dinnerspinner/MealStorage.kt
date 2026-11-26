package com.example.dinnerspinner

import android.content.Context
import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.reflect.TypeToken
import com.google.gson.Gson

object MealStorage {
    private const val PREFS_NAME = "meal_prefs_v2"
    private const val KEY_MEALS = "meals_json"
    private val gson = Gson()

    fun getMeals(context: Context): MutableList<Meal> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_MEALS, null) ?: return mutableListOf()

        val type = object : TypeToken<MutableList<Meal>>() {}.type
        val meals: MutableList<Meal> = gson.fromJson(json, type)
        meals.sortBy { it.name.lowercase() }
        return meals
    }

    fun saveMeals(context: Context, meals: List<Meal>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = gson.toJson(meals)
        prefs.edit().putString(KEY_MEALS, json).apply()
    }

    fun getAllUniqueIngredients(context: Context): List<String> {
        return getMeals(context)
            .flatMap { it.ingredients }
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .distinct()
            .sorted()
    }
}