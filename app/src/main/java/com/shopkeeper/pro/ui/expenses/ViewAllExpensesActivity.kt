package com.shopkeeper.pro.ui.expenses

import android.app.DatePickerDialog
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.TextInputEditText
import com.shopkeeper.pro.R
import com.shopkeeper.pro.data.entity.Expense
import com.shopkeeper.pro.data.entity.ExpenseCategory
import com.shopkeeper.pro.databinding.ActivityViewAllExpensesBinding
import com.shopkeeper.pro.databinding.DialogEditExpenseBinding
import com.shopkeeper.pro.ui.auth.UserPreferences
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ViewAllExpensesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityViewAllExpensesBinding
    private lateinit var viewModel: ExpenseViewModel
    private lateinit var adapter: ExpenseAdapter
    private var currentFilter: ExpenseCategory? = null
    private var allExpenses: List<Expense> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewAllExpensesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[ExpenseViewModel::class.java]

        setupToolbar()
        setupFilterChips()
        setupRecyclerView()
        observeExpenses()

        // Check if we need to apply a filter from intent
        val filterCategory = intent.getStringExtra("filter_category")
        if (filterCategory != null) {
            try {
                currentFilter = ExpenseCategory.valueOf(filterCategory)
                updateChipSelection(currentFilter)

                // Update title if filtering for purchases
                if (currentFilter == ExpenseCategory.PURCHASE) {
                    supportActionBar?.title = "Purchase History"
                }
            } catch (e: Exception) {
                // Invalid category, ignore
            }
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "All Expenses"

        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun setupFilterChips() {
        // Add "All" chip
        val allChip = Chip(this).apply {
            text = "All"
            isCheckable = true
            isChecked = true
            setChipBackgroundColorResource(R.color.purple_500)
            setTextColor(Color.WHITE)
        }
        allChip.setOnClickListener {
            currentFilter = null
            filterExpenses()
            updateChipSelection(null)
        }
        binding.chipGroupFilter.addView(allChip)

        // Add chips for each category
        ExpenseCategory.values().forEach { category ->
            val chip = Chip(this).apply {
                text = when(category) {
                    ExpenseCategory.PURCHASE -> "Purchase"
                    ExpenseCategory.DAILY_WAGES -> "Salary"
                    ExpenseCategory.BILLS -> "Bills"
                    ExpenseCategory.RENT -> "Rent"
                    ExpenseCategory.ELECTRICITY -> "Electricity"
                    ExpenseCategory.TRANSPORT -> "Transport"
                    ExpenseCategory.OTHER -> "Other"
                }
                isCheckable = true
                setChipBackgroundColorResource(R.color.purple_500)
            }
            chip.setOnClickListener {
                currentFilter = category
                filterExpenses()
                updateChipSelection(category)
            }
            binding.chipGroupFilter.addView(chip)
        }
    }

    private fun updateChipSelection(selectedCategory: ExpenseCategory?) {
        for (i in 0 until binding.chipGroupFilter.childCount) {
            val chip = binding.chipGroupFilter.getChildAt(i) as Chip
            if (selectedCategory == null) {
                chip.isChecked = i == 0 // "All" chip
            } else {
                chip.isChecked = chip.text == when(selectedCategory) {
                    ExpenseCategory.PURCHASE -> "Purchase"
                    ExpenseCategory.DAILY_WAGES -> "Salary"
                    ExpenseCategory.BILLS -> "Bills"
                    ExpenseCategory.RENT -> "Rent"
                    ExpenseCategory.ELECTRICITY -> "Electricity"
                    ExpenseCategory.TRANSPORT -> "Transport"
                    ExpenseCategory.OTHER -> "Other"
                }
            }
        }
    }

    private fun filterExpenses() {
        val filteredList = if (currentFilter == null) {
            allExpenses
        } else {
            allExpenses.filter { it.category == currentFilter }
        }

        adapter.submitList(filteredList)

        // Update UI based on filtered results
        if (filteredList.isEmpty()) {
            binding.tvNoExpenses.visibility = View.VISIBLE
            binding.rvExpenses.visibility = View.GONE
        } else {
            binding.tvNoExpenses.visibility = View.GONE
            binding.rvExpenses.visibility = View.VISIBLE
        }

        // Update total
        val total = filteredList.sumOf { it.amount }
        binding.tvTotalAmount.text = String.format("Total: ₹%.2f", total)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Only show Clear All option for admins
        if (UserPreferences.isAdmin(this)) {
            menu.add(0, 1, 0, "Clear All")
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            1 -> {
                showClearAllConfirmationDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupRecyclerView() {
        val isAdmin = UserPreferences.isAdmin(this)

        adapter = ExpenseAdapter(
            onDeleteClick = { expense ->
                if (isAdmin) {
                    showDeleteConfirmationDialog(expense)
                } else {
                    Toast.makeText(this, "Only admins can delete expenses", Toast.LENGTH_SHORT).show()
                }
            },
            onItemClick = { expense ->
                if (isAdmin) {
                    showEditExpenseDialog(expense)
                } else {
                    Toast.makeText(this, "Only admins can edit expenses", Toast.LENGTH_SHORT).show()
                }
            },
            showDeleteButton = isAdmin
        )

        binding.rvExpenses.apply {
            layoutManager = LinearLayoutManager(this@ViewAllExpensesActivity)
            adapter = this@ViewAllExpensesActivity.adapter
        }

        // Add swipe to delete only for admins
        if (isAdmin) {
            val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            private val deleteIcon = ContextCompat.getDrawable(this@ViewAllExpensesActivity, android.R.drawable.ic_menu_delete)
            private val background = ColorDrawable(Color.parseColor("#F44336"))

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val expense = adapter.getExpenseAt(position)
                showDeleteConfirmationDialog(expense)
                // Reset the swipe (in case user cancels)
                adapter.notifyItemChanged(position)
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                val iconMargin = (itemView.height - deleteIcon!!.intrinsicHeight) / 2

                if (dX < 0) { // Swiping to the left
                    background.setBounds(
                        itemView.right + dX.toInt(),
                        itemView.top,
                        itemView.right,
                        itemView.bottom
                    )
                    background.draw(c)

                    val iconTop = itemView.top + iconMargin
                    val iconLeft = itemView.right - iconMargin - deleteIcon.intrinsicWidth
                    val iconRight = itemView.right - iconMargin
                    val iconBottom = iconTop + deleteIcon.intrinsicHeight

                    deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                    deleteIcon.setTint(Color.WHITE)
                    deleteIcon.draw(c)
                }

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        })

            itemTouchHelper.attachToRecyclerView(binding.rvExpenses)
        }
    }

    private fun observeExpenses() {
        lifecycleScope.launch {
            viewModel.allExpenses.collect { expenses ->
                allExpenses = expenses
                filterExpenses()
            }
        }
    }

    private fun showDeleteConfirmationDialog(expense: Expense) {
        AlertDialog.Builder(this)
            .setTitle("Delete Expense")
            .setMessage("Are you sure you want to delete this expense?")
            .setPositiveButton("Yes") { _, _ ->
                viewModel.deleteExpense(expense)
                Toast.makeText(this, "Expense deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun showClearAllConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Clear All Expenses")
            .setMessage("Are you sure you want to delete ALL expenses? This action cannot be undone.")
            .setPositiveButton("Delete All") { _, _ ->
                viewModel.deleteAllExpenses()
                Toast.makeText(this, "All expenses deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditExpenseDialog(expense: Expense) {
        val binding = DialogEditExpenseBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setView(binding.root)
            .create()

        // Initialize fields with current values
        binding.etDescription.setText(expense.description)
        binding.etAmount.setText(expense.amount.toString())

        // Setup category spinner
        val categories = ExpenseCategory.values().map { it.name.replace("_", " ") }
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = categoryAdapter
        binding.spinnerCategory.setSelection(expense.category.ordinal)

        // Setup date
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        var selectedDate = expense.createdAt
        binding.tvSelectedDate.text = dateFormat.format(selectedDate)

        // Date picker
        binding.btnPickDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            calendar.time = selectedDate

            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    selectedDate = calendar.time
                    binding.tvSelectedDate.text = dateFormat.format(selectedDate)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // Save button
        binding.btnSave.setOnClickListener {
            val description = binding.etDescription.text.toString()
            val amountStr = binding.etAmount.text.toString()

            if (description.isBlank() || amountStr.isBlank()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val amount = amountStr.toDoubleOrNull()
            if (amount == null || amount <= 0) {
                Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedCategory = ExpenseCategory.values()[binding.spinnerCategory.selectedItemPosition]

            // Update expense
            val updatedExpense = expense.copy(
                description = description,
                amount = amount,
                category = selectedCategory,
                createdAt = selectedDate
            )

            viewModel.updateExpense(updatedExpense)
            Toast.makeText(this, "Expense updated", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        // Cancel button
        binding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}

// Expense Adapter
class ExpenseAdapter(
    private val onDeleteClick: (Expense) -> Unit,
    private val onItemClick: (Expense) -> Unit,
    private val showDeleteButton: Boolean = true
) : RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder>() {

    private var expenses = listOf<Expense>()
    private val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    fun submitList(list: List<Expense>) {
        expenses = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_expense, parent, false)
        return ExpenseViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        holder.bind(expenses[position])
    }

    override fun getItemCount() = expenses.size

    fun getExpenseAt(position: Int): Expense = expenses[position]

    inner class ExpenseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDescription: TextView = itemView.findViewById(R.id.tv_description)
        private val tvCategory: TextView = itemView.findViewById(R.id.tv_category)
        private val tvAmount: TextView = itemView.findViewById(R.id.tv_amount)
        private val tvDateTime: TextView = itemView.findViewById(R.id.tv_date_time)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btn_delete)

        fun bind(expense: Expense) {
            tvDescription.text = expense.description
            tvCategory.text = expense.category.name.replace("_", " ")
            tvAmount.text = String.format("₹%.2f", expense.amount)
            tvDateTime.text = dateFormat.format(expense.createdAt)

            // Show/hide delete button based on user role
            btnDelete.visibility = if (showDeleteButton) View.VISIBLE else View.GONE

            btnDelete.setOnClickListener {
                onDeleteClick(expense)
            }

            itemView.setOnClickListener {
                onItemClick(expense)
            }

            itemView.setOnLongClickListener {
                if (showDeleteButton) {
                    onDeleteClick(expense)
                    true
                } else {
                    false
                }
            }
        }
    }
}