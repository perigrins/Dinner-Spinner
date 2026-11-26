package com.example.dinnerspinner

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.dinnerspinner.databinding.FragmentListBinding

class ListFragment : Fragment() {

    private var _binding: FragmentListBinding? = null
    private val binding get() = _binding!!
    private lateinit var mealsList: MutableList<Meal>
    private lateinit var adapter: ArrayAdapter<Meal>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mealsList = MealStorage.getMeals(requireContext())
        
        adapter = object : ArrayAdapter<Meal>(requireContext(), android.R.layout.simple_list_item_2, android.R.id.text1, mealsList) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val itemView = super.getView(position, convertView, parent)
                val text1 = itemView.findViewById<TextView>(android.R.id.text1)
                val text2 = itemView.findViewById<TextView>(android.R.id.text2)

                val meal = mealsList[position]
                text1.text = meal.name
                text2.text = if (meal.ingredients.isEmpty()) {
                    "Brak przypisanych składników"
                } else {
                    "Składniki: ${meal.ingredients.joinToString(", ")}"
                }
                return itemView
            }
        }
        binding.listViewMeals.adapter = adapter

        binding.btnAddMeal.setOnClickListener {
            val name = binding.editTextMealName.text.toString().trim()
            val rawIngredients = binding.editTextIngredients.text.toString()

            if (name.isNotEmpty()) {
                val ingredientsList = parseIngredients(rawIngredients)
                mealsList.add(Meal(name, ingredientsList))
                saveAndRefresh()

                binding.editTextMealName.text.clear()
                binding.editTextIngredients.text.clear()
            } else {
                Toast.makeText(requireContext(), "Wpisz nazwę posiłku", Toast.LENGTH_SHORT).show()
            }
        }

        binding.listViewMeals.setOnItemClickListener { _, _, position, _ ->
            showEditDeleteDialog(position)
        }
    }

    private fun parseIngredients(raw: String): List<String> {
        return raw.split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .distinct()
    }

    private fun showEditDeleteDialog(position: Int) {
        val currentMeal = mealsList[position]

        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 0)
        }

        val inputName = EditText(requireContext()).apply {
            hint = "Nazwa obiadu"
            setText(currentMeal.name)
        }

        val inputIngredients = EditText(requireContext()).apply {
            hint = "Składniki po przecinku"
            setText(currentMeal.ingredients.joinToString(", "))
        }

        container.addView(inputName)
        container.addView(inputIngredients)

        AlertDialog.Builder(requireContext())
            .setTitle("Edytuj lub usuń posiłek")
            .setView(container)
            .setPositiveButton("Zapisz") { _, _ ->
                val updatedName = inputName.text.toString().trim()
                val updatedIngredients = parseIngredients(inputIngredients.text.toString())

                if (updatedName.isNotEmpty()) {
                    mealsList[position] = Meal(updatedName, updatedIngredients)
                    saveAndRefresh()
                }
            }
            .setNegativeButton("Usuń") { _, _ ->
                mealsList.removeAt(position)
                saveAndRefresh()
                Toast.makeText(requireContext(), "Usunięto: ${currentMeal.name}", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("Anuluj", null)
            .show()
    }

    private fun saveAndRefresh() {
        mealsList.sortBy { it.name.lowercase() }
        MealStorage.saveMeals(requireContext(), mealsList)
        adapter.notifyDataSetChanged()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}