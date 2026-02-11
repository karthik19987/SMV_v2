package com.shopkeeper.pro.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.shopkeeper.pro.R
import com.shopkeeper.pro.data.database.ShopKeeperDatabase
import com.shopkeeper.pro.data.entity.Item
import com.shopkeeper.pro.data.entity.ItemCategory
import com.shopkeeper.pro.databinding.FragmentDashboardBinding
import com.shopkeeper.pro.ui.auth.UserPreferences
import com.shopkeeper.pro.ui.products.ViewAllProductsActivity
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DashboardViewModel by viewModels()

    private val rupeeFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        // Reload data when returning to dashboard
        loadData()
    }

    private fun setupUI() {
        val displayName = UserPreferences.getCurrentUserDisplayName(requireContext())
        binding.tvWelcome.text = "Welcome, $displayName"

        binding.cardSales.setOnClickListener {
            findNavController().navigate(R.id.nav_sales)
        }

        binding.cardExpenses.setOnClickListener {
            findNavController().navigate(R.id.nav_expenses)
        }

        binding.cardReports.setOnClickListener {
            findNavController().navigate(R.id.nav_reports)
        }

        binding.cardInventory.setOnClickListener {
            startActivity(Intent(requireContext(), ViewAllProductsActivity::class.java))
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.dashboardData.collect { data ->
                binding.tvTodaySales.text = rupeeFormat.format(data.todaySales)
                binding.tvTodayExpenses.text = rupeeFormat.format(data.todayExpenses)
                binding.tvProfit.text = rupeeFormat.format(data.todaySales - data.todayExpenses)
                binding.tvTotalItems.text = data.totalItems.toString()
            }
        }
    }

    private fun loadData() {
        viewModel.loadDashboardData()
    }

    private fun showAddProductDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_product, null)

        val etProductName = dialogView.findViewById<TextInputEditText>(R.id.et_product_name)
        val spinnerCategory = dialogView.findViewById<Spinner>(R.id.spinner_category)
        val tilPriceHint = dialogView.findViewById<TextInputLayout>(R.id.til_price_hint)
        val etPriceHint = dialogView.findViewById<TextInputEditText>(R.id.et_price_hint)
        val spinnerUnit = dialogView.findViewById<Spinner>(R.id.spinner_unit)
        val tvUnitLabel = dialogView.findViewById<TextView>(R.id.tv_unit_label)

        // Setup category spinner
        val categories = arrayOf("Product", "Service")
        spinnerCategory.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, categories)

        // Setup unit spinner
        val units = arrayOf("kg", "piece", "liter", "meter", "hour")
        spinnerUnit.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, units)

        // Show/hide price hint based on category
        spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                when (categories[position]) {
                    "Product" -> {
                        tilPriceHint.visibility = View.VISIBLE
                        spinnerUnit.visibility = View.VISIBLE
                        tvUnitLabel.visibility = View.VISIBLE
                    }
                    "Service" -> {
                        tilPriceHint.visibility = View.GONE
                        spinnerUnit.visibility = View.GONE
                        tvUnitLabel.visibility = View.GONE
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Add New Product/Service")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val productName = etProductName.text.toString().trim()
                val category = spinnerCategory.selectedItem.toString()
                val priceHint = etPriceHint.text.toString().toDoubleOrNull()
                val unit = if (category == "Product") spinnerUnit.selectedItem.toString() else ""

                if (productName.isNotEmpty()) {
                    saveNewProduct(productName, category, priceHint, unit)
                } else {
                    Toast.makeText(requireContext(), "Please enter product name", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveNewProduct(name: String, category: String, priceHint: Double?, unit: String) {
        lifecycleScope.launch {
            try {
                val database = ShopKeeperDatabase.getDatabase(requireContext())
                val itemDao = database.itemDao()
                val userId = UserPreferences.getCurrentUserId(requireContext()) ?: "default"

                val itemCategory = when (category) {
                    "Product" -> ItemCategory.PRODUCT
                    "Service" -> ItemCategory.SERVICE
                    else -> ItemCategory.PRODUCT
                }

                val item = Item(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    category = itemCategory,
                    pricePerKg = priceHint, // Note: field name is pricePerKg, not priceHint
                    unit = if (itemCategory == ItemCategory.PRODUCT) unit else "",
                    isActive = true,
                    createdAt = Date(),
                    createdBy = userId
                )

                itemDao.insertItem(item)
                Toast.makeText(requireContext(), "$name added successfully!", Toast.LENGTH_SHORT).show()

                // Refresh the dashboard to update item count
                loadData()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error adding product: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}