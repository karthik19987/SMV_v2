package com.shopkeeper.pro.ui.sales

import android.content.Intent
import android.os.Bundle
import android.text.TextWatcher
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import android.util.Log
import kotlinx.coroutines.Job
import com.shopkeeper.pro.data.database.ShopKeeperDatabase
import com.shopkeeper.pro.data.entity.Item
import com.shopkeeper.pro.data.entity.Sale
import com.shopkeeper.pro.data.entity.SaleItem
import com.shopkeeper.pro.ui.auth.UserPreferences
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.Date
import com.shopkeeper.pro.databinding.FragmentSalesBinding
import com.shopkeeper.pro.databinding.ItemSaleProductBinding
import java.text.NumberFormat
import java.util.*

class SalesFragment : Fragment() {

    private var _binding: FragmentSalesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SalesViewModel by viewModels()

    private lateinit var database: ShopKeeperDatabase
    private lateinit var saleDao: com.shopkeeper.pro.data.dao.SaleDao

    private val productViews = mutableMapOf<String, ProductViewHolder>()
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("hi", "IN")).apply {
        currency = Currency.getInstance("INR")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSalesBinding.inflate(inflater, container, false)
        database = ShopKeeperDatabase.getDatabase(requireContext())
        saleDao = database.saleDao()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        // Observe products - will automatically update when database changes
        observeProducts()
    }

    private fun setupUI() {
        // Action buttons
        binding.btnClearAll.setOnClickListener { clearAllFields() }
        binding.btnCompleteSale.setOnClickListener { completeSale() }
        binding.btnViewAllSales.setOnClickListener {
            startActivity(Intent(requireContext(), ViewAllSalesActivity::class.java))
        }

        // Debug button
        binding.btnDebugCheckProducts.setOnClickListener {
            checkProductsInDatabase()
        }
    }

    private fun observeProducts() {
        // Use viewLifecycleOwner for proper lifecycle management
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.activeProducts.collect { products ->
                Log.d("SalesFragment", "Received ${products.size} products from database")
                products.forEach { product ->
                    Log.d("SalesFragment", "Product in list: ${product.name}")
                }
                updateProductList(products)
            }
        }
    }

    private fun updateProductList(products: List<Item>) {
        // Clear existing views
        binding.containerSaleItems.removeAllViews()
        productViews.clear()

        if (products.isEmpty()) {
            Log.d("SalesFragment", "No products available, showing empty message")
            binding.tvNoProducts.visibility = View.VISIBLE
            binding.containerSaleItems.visibility = View.GONE
            updateTotalAmount()
        } else {
            Log.d("SalesFragment", "Displaying ${products.size} products")
            binding.tvNoProducts.visibility = View.GONE
            binding.containerSaleItems.visibility = View.VISIBLE

            // Add a view for each product
            products.forEach { product ->
                Log.d("SalesFragment", "Adding product: ${product.name} (${product.id})")
                val productBinding = ItemSaleProductBinding.inflate(
                    layoutInflater,
                    binding.containerSaleItems,
                    false
                )

                // Set product name and unit
                productBinding.tvProductName.text = product.name
                productBinding.tvUnit.text = product.unit

                // Set price hint if available
                product.pricePerKg?.let { price ->
                    productBinding.etPrice.hint = price.toString()
                }

                // Create holder to track this product's views
                val holder = ProductViewHolder(
                    product = product,
                    quantityField = productBinding.etQuantity,
                    priceField = productBinding.etPrice,
                    totalField = productBinding.tvTotal
                )
                productViews[product.id] = holder

                // Set up text watchers
                setupItemTextWatchers(
                    productBinding.etQuantity,
                    productBinding.etPrice,
                    productBinding.tvTotal
                )

                // Set up keyboard navigation
                if (productViews.size > 1) {
                    val previousHolder = productViews.values.toList()[productViews.size - 2]
                    setupKeyboardForEditText(previousHolder.priceField, productBinding.etQuantity)
                }
                setupKeyboardForEditText(productBinding.etQuantity, productBinding.etPrice)

                // This will be updated after all products are added
                if (products.last() == product) {
                    setupKeyboardForEditText(productBinding.etPrice, null)
                } else {
                    // Will be updated in the next iteration
                }

                // Add to container
                binding.containerSaleItems.addView(productBinding.root)
            }

            // Update keyboard navigation for the last product of previous iteration
            if (products.size > 1) {
                for (i in 0 until products.size - 1) {
                    val currentHolder = productViews[products[i].id]!!
                    val nextHolder = productViews[products[i + 1].id]!!
                    setupKeyboardForEditText(currentHolder.priceField, nextHolder.quantityField)
                }
            }

            updateTotalAmount()
        }
    }

    private fun setupItemTextWatchers(qtyField: EditText, priceField: EditText, totalField: TextView) {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateItemTotal(qtyField, priceField, totalField)
                updateTotalAmount()
            }
        }

        qtyField.addTextChangedListener(textWatcher)
        priceField.addTextChangedListener(textWatcher)
    }

    private fun setupKeyboardForEditText(editText: EditText, nextEditText: EditText?) {
        editText.setOnClickListener {
            editText.requestFocus()
            showKeyboard(editText)
        }

        editText.setOnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                showKeyboard(view as EditText)
            }
        }

        // Handle Enter key to move to next field
        editText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT || actionId == EditorInfo.IME_ACTION_DONE) {
                if (nextEditText != null) {
                    nextEditText.requestFocus()
                    showKeyboard(nextEditText)
                } else {
                    // If last field, hide keyboard
                    val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(editText.windowToken, 0)
                    editText.clearFocus()
                }
                true
            } else {
                false
            }
        }
    }

    private fun showKeyboard(editText: EditText) {
        editText.postDelayed({
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
        }, 100)
    }

    private fun updateItemTotal(qtyField: EditText, priceField: EditText, totalField: TextView) {
        val price = priceField.text.toString().toDoubleOrNull() ?: 0.0
        val quantity = if (price > 0 && qtyField.text.isEmpty()) {
            1.0 // Default quantity is 1 when price is entered but quantity is empty
        } else {
            qtyField.text.toString().toDoubleOrNull() ?: 0.0
        }
        val total = quantity * price

        totalField.text = if (total > 0) currencyFormatter.format(total) else "₹0"
    }

    private fun updateTotalAmount() {
        var grandTotal = 0.0

        // Add all item totals
        productViews.values.forEach { holder ->
            grandTotal += getItemTotal(holder.quantityField, holder.priceField)
        }

        binding.tvTotalAmount.text = currencyFormatter.format(grandTotal)
    }

    private fun getItemTotal(qtyField: EditText, priceField: EditText): Double {
        val price = priceField.text.toString().toDoubleOrNull() ?: 0.0
        val quantity = if (price > 0 && qtyField.text.isEmpty()) {
            1.0 // Default quantity is 1 when price is entered but quantity is empty
        } else {
            qtyField.text.toString().toDoubleOrNull() ?: 0.0
        }
        return quantity * price
    }

    private fun clearAllFields() {
        if (hasAnyValues()) {
            android.app.AlertDialog.Builder(requireContext())
                .setTitle("Clear All")
                .setMessage("Are you sure you want to clear all fields?")
                .setPositiveButton("Yes") { _, _ ->
                    clearFields()
                }
                .setNegativeButton("No", null)
                .show()
        }
    }

    private fun hasAnyValues(): Boolean {
        return productViews.values.any { holder ->
            holder.quantityField.text.isNotEmpty() || holder.priceField.text.isNotEmpty()
        }
    }

    private fun clearFields() {
        productViews.values.forEach { holder ->
            holder.quantityField.text.clear()
            holder.priceField.text.clear()
            holder.totalField.text = "₹0"
        }

        updateTotalAmount()
        Toast.makeText(requireContext(), "All fields cleared", Toast.LENGTH_SHORT).show()
    }

    private fun completeSale() {
        val grandTotal = productViews.values.sumOf { holder ->
            getItemTotal(holder.quantityField, holder.priceField)
        }

        if (grandTotal <= 0) {
            Toast.makeText(requireContext(), "Please add items to complete the sale", Toast.LENGTH_SHORT).show()
            return
        }

        val saleDetails = buildSaleDetailsMessage()
        val saleItems = buildSaleItems()

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Complete Sale")
            .setMessage("Sale Details:\n$saleDetails\n\nTotal Amount: ${currencyFormatter.format(grandTotal)}\n\nConfirm this sale?")
            .setPositiveButton("Confirm") { _, _ ->
                saveSale(grandTotal, saleItems)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun buildSaleDetailsMessage(): String {
        val details = mutableListOf<String>()

        productViews.forEach { (_, holder) ->
            val total = getItemTotal(holder.quantityField, holder.priceField)
            if (total > 0) {
                val qty = holder.quantityField.text.toString().ifEmpty { "1" }
                val price = holder.priceField.text.toString()
                details.add("• ${holder.product.name}: $qty ${holder.product.unit} @ ₹$price = ${currencyFormatter.format(total)}")
            }
        }

        return details.joinToString("\n")
    }

    private fun buildSaleItems(): List<SaleItem> {
        val items = mutableListOf<SaleItem>()

        productViews.forEach { (_, holder) ->
            val total = getItemTotal(holder.quantityField, holder.priceField)
            if (total > 0) {
                val qty = holder.quantityField.text.toString().toDoubleOrNull() ?: 1.0
                val price = holder.priceField.text.toString().toDoubleOrNull() ?: 0.0
                items.add(SaleItem(
                    itemId = holder.product.id,
                    itemName = holder.product.name,
                    quantity = qty,
                    pricePerUnit = price,
                    totalPrice = total
                ))
            }
        }

        return items
    }

    private fun saveSale(totalAmount: Double, saleItems: List<SaleItem>) {
        lifecycleScope.launch {
            try {
                val userId = UserPreferences.getCurrentUserId(requireContext()) ?: "default"

                // Convert SaleItem list to JSON string
                val itemsJson = JSONArray().apply {
                    saleItems.forEach { item ->
                        put(JSONObject().apply {
                            put("itemId", item.itemId)
                            put("itemName", item.itemName)
                            put("quantity", item.quantity)
                            put("pricePerUnit", item.pricePerUnit)
                            put("totalPrice", item.totalPrice)
                        })
                    }
                }.toString()

                val sale = Sale(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    totalAmount = totalAmount,
                    items = itemsJson,
                    createdAt = Date(),
                    paymentMethod = "cash"
                )

                saleDao.insertSale(sale)
                Toast.makeText(requireContext(), "Sale completed successfully!", Toast.LENGTH_SHORT).show()
                clearFields()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error saving sale: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Force reload products when returning to this screen
        // This ensures new products added in other screens are shown
        Log.d("SalesFragment", "onResume called - forcing refresh of products")
        viewModel.refreshProducts()
    }

    private fun checkProductsInDatabase() {
        lifecycleScope.launch {
            try {
                val itemDao = database.itemDao()
                val allProducts = itemDao.getAllActiveItemsOnce()
                val message = StringBuilder()
                message.append("Products in database: ${allProducts.size}\n\n")

                allProducts.forEach { product ->
                    message.append("• ${product.name} (${product.id})\n")
                    message.append("  Category: ${product.category}\n")
                    message.append("  Unit: ${product.unit}\n")
                    message.append("  Active: ${product.isActive}\n")
                    message.append("  Price: ₹${product.pricePerKg ?: "N/A"}\n\n")
                }

                if (allProducts.isEmpty()) {
                    message.append("No products found in database!\n")
                    message.append("Please add products from Dashboard → Products")
                }

                // Also check what the ViewModel has
                val viewModelProducts = viewModel.activeProducts.value
                message.append("\nViewModel has: ${viewModelProducts.size} products")

                android.app.AlertDialog.Builder(requireContext())
                    .setTitle("Debug: Product Status")
                    .setMessage(message.toString())
                    .setPositiveButton("OK", null)
                    .show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("SalesFragment", "Error checking products", e)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Helper class to hold references to product views
    private data class ProductViewHolder(
        val product: Item,
        val quantityField: EditText,
        val priceField: EditText,
        val totalField: TextView
    )
}