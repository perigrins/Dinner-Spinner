package com.example.dinnerspinner

import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.dinnerspinner.databinding.FragmentDrawBinding
import com.google.android.material.chip.Chip
import android.content.res.ColorStateList
import androidx.core.content.ContextCompat
import android.widget.FrameLayout

class DrawFragment : Fragment() {

    private var _binding: FragmentDrawBinding? = null
    private val binding get() = _binding!!
    private val handler = Handler(Looper.getMainLooper())
    private val selectedIngredients = mutableSetOf<String>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDrawBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.textSwitcherMeal.setFactory {
            TextView(requireContext()).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                gravity = Gravity.CENTER
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                textSize = 22f
                setTypeface(null, Typeface.BOLD)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
            }
        }

        binding.textSwitcherMeal.inAnimation = AnimationUtils.loadAnimation(requireContext(), android.R.anim.slide_in_left)
        binding.textSwitcherMeal.outAnimation = AnimationUtils.loadAnimation(requireContext(), android.R.anim.slide_out_right)
        binding.textSwitcherMeal.setCurrentText("Dodaj obiady i losuj!")

        setupIngredientChips()

        binding.btnDraw.setOnClickListener {
            startRouletteAnimation()
        }
    }

    // Odświeża kafelki składników po powrocie z zakładki edycji listy
    override fun onResume() {
        super.onResume()
        setupIngredientChips()
    }

    private fun setupIngredientChips() {
        val allIngredients = MealStorage.getAllUniqueIngredients(requireContext())
        binding.chipGroupIngredients.removeAllViews()

        // Usuń z zaznaczonych te składniki, które mogły zostać usunięte podczas edycji obiadów
        selectedIngredients.retainAll(allIngredients.toSet())

        for (ingredient in allIngredients) {
            val chip = Chip(requireContext()).apply {
                text = ingredient
                textSize = 13f
                chipMinHeight = 30f * resources.displayMetrics.density
                setEnsureMinTouchTargetSize(false)
                isCheckable = true
                isChecked = selectedIngredients.contains(ingredient)

                // Usuwa fioletowe tło: biały gdy niezaznaczony, jasny morski gdy zaznaczony
                chipBackgroundColor = android.content.res.ColorStateList(
                    arrayOf(
                        intArrayOf(android.R.attr.state_checked),
                        intArrayOf(-android.R.attr.state_checked)
                    ),
                    intArrayOf(
                        ContextCompat.getColor(requireContext(), R.color.teal_light_container),
                        ContextCompat.getColor(requireContext(), R.color.card_surface)
                    )
                )
                chipStrokeColor = android.content.res.ColorStateList.valueOf(
                    androidx.core.content.ContextCompat.getColor(requireContext(), R.color.teal_main)
                )
                chipStrokeWidth = 1f * resources.displayMetrics.density

                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedIngredients.add(ingredient)
                    } else {
                        selectedIngredients.remove(ingredient)
                    }
                    updatePoolCounter()
                }
            }
            binding.chipGroupIngredients.addView(chip)
        }
        updatePoolCounter()
    }

    private fun getFilteredMeals(): List<Meal> {
        val allMeals = MealStorage.getMeals(requireContext())
        if (selectedIngredients.isEmpty()) return allMeals

        // Zwraca tylko te obiady, które zawierają WSZYSTKIE zaznaczone składniki
        return allMeals.filter { meal ->
            meal.ingredients.containsAll(selectedIngredients)
        }
    }

    private fun updatePoolCounter() {
        val count = getFilteredMeals().size
        binding.textPoolCount.text = "Pasujące obiady w puli: $count"
    }

    private fun startRouletteAnimation() {
        val matchingMeals = getFilteredMeals()
        if (matchingMeals.isEmpty()) {
            val msg = if (MealStorage.getMeals(requireContext()).isEmpty()) {
                "Lista jest pusta! Dodaj pierwszy obiad w zakładce obok."
            } else {
                "Żaden obiad nie pasuje do wybranych składników."
            }
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnDraw.isEnabled = false
        binding.textResultIngredients.text = ""
        val totalSpins = 18
        var currentSpin = 0

        val runnable = object : Runnable {
            override fun run() {
                if (currentSpin < totalSpins) {
                    val tempMeal = matchingMeals.random()
                    binding.textSwitcherMeal.setText(tempMeal.name)
                    currentSpin++

                    val delay = 50L + (currentSpin * 12L)
                    handler.postDelayed(this, delay)
                } else {
                    val finalMeal = matchingMeals.random()
                    binding.textSwitcherMeal.setText("${finalMeal.name}")

                    if (finalMeal.ingredients.isNotEmpty()) {
                        binding.textResultIngredients.text = "Potrzebne składniki:\n${finalMeal.ingredients.joinToString(", ")}"
                    }
                    binding.btnDraw.isEnabled = true
                }
            }
        }
        handler.post(runnable)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null)
        _binding = null
    }
}