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
import com.shopkeeper.pro.data.sync.SyncWorker
import com.shopkeeper.pro.ui.auth.UserPreferences
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.Date
import com.shopkeeper.pro.databinding.FragmentSalesBinding
import com.shopkeeper.pro.databinding.ItemSaleProductBinding
import java.text.NumberFormat
import java.util.Locale
import android.widget.ImageButton
import android.widget.LinearLayout
import com.google.android.material.button.MaterialButton
import com.shopkeeper.pro.data.dao.SaleDao
import kotlinx.coroutines.flow.collectLatest
import com.shopkeeper.pro.data.firebase.FirebaseConfig
import com.shopkeeper.pro.data.firebase.FirebaseSyncRepository

class SalesFragment : Fragment() {

    private var _binding: FragmentSalesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SalesViewModel by viewModels()
    private val fragmentViewModel: SalesFragmentViewModel by viewModels()
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    private val database by lazy { ShopKeeperDatabase.getDatabase(requireContext()) }
    private val saleDao by lazy { database.saleDao() }
    private var observeJob: Job? = null
    private val firebaseSyncRepository = FirebaseSyncRepository()

    // Map to track product views for quick access
    private val productViews = mutableMapOf<String, ProductViewHolder>()

    data class ProductViewHolder(
        val product: Item,
        val quantityField: EditText,
        val priceField: EditText,
        val totalField: TextView,
        val binding: ItemSaleProductBinding
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSalesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.d("SalesFragment", "onViewCreated called")

        // Add debug button temporarily
        addDebugButton()

        observeTodaySales()
        setupButtons()
        observeProducts()

        // Force immediate refresh
        viewModel.refreshProducts()

        // Setup customer info listeners
        setupCustomerInfoListeners()
    }

    private fun addDebugButton() {
        val debugButton = MaterialButton(requireContext()).apply {
            text = "Check Products & Firebase"
            setOnClickListener {
                checkProductsInDatabase()
                checkFirebaseConnection()
            }
        }
        // Add to the LinearLayout inside the ScrollView, not the ScrollView itself
        val container = binding.root.getChildAt(0) as? LinearLayout
        container?.addView(debugButton, 0)
    }

    private fun checkFirebaseConnection() {
        lifecycleScope.launch {
            try {
                val userId = FirebaseConfig.getCurrentUserId()
                val storeId = FirebaseConfig.getCurrentStoreId()

                Log.d("SalesFragment", "Firebase User ID: $userId")
                Log.d("SalesFragment", "Firebase Store ID: $storeId")

                if (userId != null) {
                    Toast.makeText(context, "Firebase Connected! User: $userId", Toast.LENGTH_LONG).show()

                    // Try to sync a test sale
                    val testSale = Sale(
                        id = "test_${System.currentTimeMillis()}",
                        userId = userId,
                        totalAmount = 100.0,
                        items = "Test Item,1,100,100,kg",
                        createdAt = Date(),
                        paymentMethod = "cash",
                        syncStatus = "pending"
                    )

                    val result = firebaseSyncRepository.syncSaleToFirestore(testSale)
                    if (result.isSuccess) {
                        Toast.makeText(context, "✅ Firebase sync successful! Check Firestore console.", Toast.LENGTH_LONG).show()
                        Log.d("SalesFragment", "Test sale synced to Firebase successfully")
                    } else {
                        Toast.makeText(context, "❌ Firebase sync failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                        Log.e("SalesFragment", "Firebase sync failed", result.exceptionOrNull())
                    }
                } else {
                    Toast.makeText(context, "❌ Not logged in to Firebase!", Toast.LENGTH_LONG).show()
                    Log.e("SalesFragment", "No Firebase user ID - not authenticated")
                }
            } catch (e: Exception) {
                Log.e("SalesFragment", "Firebase connection check failed", e)
                Toast.makeText(context, "Firebase error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupCustomerInfoListeners() {
        // Add text change listeners for customer info if needed
    }

    private fun observeTodaySales() {
        // Today's sales is displayed in a different fragment
        fragmentViewModel.todaySales.observe(viewLifecycleOwner) { total ->
            // Update total display if needed
        }
    }

    private fun observeProducts() {
        Log.d("SalesFragment", "Starting to observe products")

        // Cancel any existing observation
        observeJob?.cancel()

        observeJob = viewLifecycleOwner.lifecycleScope.launch {
            viewModel.activeProducts.collect { products ->
                Log.d("SalesFragment", "Received ${products.size} products from database")
                updateProductList(products)
            }
        }
    }

    private fun updateProductList(products: List<Item>) {
        Log.d("SalesFragment", "Updating product list with ${products.size} products")

        // Clear existing views
        binding.containerSaleItems.removeAllViews()
        productViews.clear()

        if (products.isEmpty()) {
            Log.w("SalesFragment", "No products to display")
            // Add a message view
            val emptyView = TextView(requireContext()).apply {
                text = "No products available. Please add products first."
                setPadding(16, 16, 16, 16)
            }
            binding.containerSaleItems.addView(emptyView)
            return
        }

        // Add product views
        products.forEach { product ->
            Log.d("SalesFragment", "Adding product view for: ${product.name}")
            addProductView(product)
        }

        // Update UI state
        binding.btnCompleteSale.isEnabled = true
    }

    private fun addProductView(product: Item) {
        val itemBinding = ItemSaleProductBinding.inflate(layoutInflater, binding.containerSaleItems, false)

        itemBinding.tvProductName.text = product.name
        itemBinding.tvUnit.text = "per ${product.unit}"

        // Set default price if available
        product.pricePerKg?.let { price ->
            itemBinding.etPrice.setText(price.toString())
        }

        val viewHolder = ProductViewHolder(
            product = product,
            quantityField = itemBinding.etQuantity,
            priceField = itemBinding.etPrice,
            totalField = itemBinding.tvTotal,
            binding = itemBinding
        )

        productViews[product.id] = viewHolder

        // Setup text watchers
        setupTextWatcher(itemBinding.etQuantity, itemBinding.etPrice, itemBinding.tvTotal)
        setupTextWatcher(itemBinding.etPrice, itemBinding.etQuantity, itemBinding.tvTotal)

        // Setup enter key navigation
        setupEnterKeyNavigation(itemBinding.etQuantity, itemBinding.etPrice)
        val nextIndex = productViews.size
        if (nextIndex < viewModel.activeProducts.value.size) {
            // Will be connected to next product's quantity field when it's added
        }

        binding.containerSaleItems.addView(itemBinding.root)
    }

    private fun setupEnterKeyNavigation(editText: EditText, nextEditText: EditText?) {
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
            1.0
        } else {
            qtyField.text.toString().toDoubleOrNull() ?: 0.0
        }
        return quantity * price
    }

    private fun setupTextWatcher(triggerField: EditText, otherField: EditText, totalField: TextView) {
        triggerField.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                updateItemTotal(
                    if (triggerField.hint.toString().contains("Qty")) triggerField else otherField,
                    if (triggerField.hint.toString().contains("Price")) triggerField else otherField,
                    totalField
                )
                updateTotalAmount()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun setupButtons() {
        binding.btnCompleteSale.setOnClickListener { showConfirmationDialog() }
        binding.btnClearAll.setOnClickListener { clearFields() }
        binding.btnViewAllSales.setOnClickListener {
            startActivity(Intent(requireContext(), ViewAllSalesActivity::class.java))
        }
    }

    private fun clearFields() {
        productViews.values.forEach { holder ->
            holder.quantityField.text.clear()
            holder.priceField.text.clear()
            holder.totalField.text = "₹0"
        }
        updateTotalAmount()
    }

    private fun showConfirmationDialog() {
        val grandTotal = productViews.values.sumOf { holder ->
            getItemTotal(holder.quantityField, holder.priceField)
        }

        if (grandTotal == 0.0) {
            Toast.makeText(requireContext(), "Please add at least one item", Toast.LENGTH_SHORT).show()
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

                // Trigger sync to Firebase
                SyncWorker.triggerImmediateSync(requireContext())
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
                message.append("Found ${allProducts.size} products in database:\n")
                allProducts.forEach { product ->
                    message.append("- ${product.name} (${product.id})\n")
                }
                Log.d("SalesFragment", message.toString())
                Toast.makeText(requireContext(), message.toString(), Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Log.e("SalesFragment", "Error checking products", e)
                Toast.makeText(requireContext(), "Error checking products: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        observeJob?.cancel()
        _binding = null
    }
}