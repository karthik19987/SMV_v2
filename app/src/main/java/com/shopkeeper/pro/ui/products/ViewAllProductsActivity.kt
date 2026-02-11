package com.shopkeeper.pro.ui.products

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
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.shopkeeper.pro.R
import com.shopkeeper.pro.data.entity.Item
import com.shopkeeper.pro.data.entity.ItemCategory
import com.shopkeeper.pro.databinding.ActivityViewAllProductsBinding
import com.shopkeeper.pro.databinding.DialogAddEditProductBinding
import com.shopkeeper.pro.ui.auth.UserPreferences
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ViewAllProductsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityViewAllProductsBinding
    private lateinit var viewModel: ProductsViewModel
    private lateinit var adapter: ProductsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewAllProductsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[ProductsViewModel::class.java]

        setupToolbar()
        setupRecyclerView()
        setupFab()
        observeProducts()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Manage Products"

        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Only show Add Default Items option for admins
        if (UserPreferences.isAdmin(this)) {
            menu.add(0, 1, 0, "Add Default Items")
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            1 -> {
                showAddDefaultItemsDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupRecyclerView() {
        val isAdmin = UserPreferences.isAdmin(this)

        adapter = ProductsAdapter(
            onDeleteClick = { product ->
                if (isAdmin) {
                    showDeleteConfirmationDialog(product)
                } else {
                    Toast.makeText(this, "Only admins can delete products", Toast.LENGTH_SHORT).show()
                }
            },
            onItemClick = { product ->
                if (isAdmin) {
                    showEditProductDialog(product)
                } else {
                    Toast.makeText(this, "Only admins can edit products", Toast.LENGTH_SHORT).show()
                }
            },
            onToggleActiveClick = { product ->
                if (isAdmin) {
                    viewModel.toggleProductActive(product)
                } else {
                    Toast.makeText(this, "Only admins can activate/deactivate products", Toast.LENGTH_SHORT).show()
                }
            },
            showActionButtons = isAdmin
        )

        binding.rvProducts.apply {
            layoutManager = LinearLayoutManager(this@ViewAllProductsActivity)
            adapter = this@ViewAllProductsActivity.adapter
        }

        // Add swipe to delete only for admins
        if (isAdmin) {
            val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            private val deleteIcon = ContextCompat.getDrawable(this@ViewAllProductsActivity, android.R.drawable.ic_menu_delete)
            private val background = ColorDrawable(Color.parseColor("#F44336"))

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val product = adapter.getProductAt(position)
                showDeleteConfirmationDialog(product)
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

            itemTouchHelper.attachToRecyclerView(binding.rvProducts)
        }
    }

    private fun setupFab() {
        val isAdmin = UserPreferences.isAdmin(this)

        if (isAdmin) {
            binding.fabAddProduct.visibility = View.VISIBLE
            binding.fabAddProduct.setOnClickListener {
                showAddProductDialog()
            }
        } else {
            binding.fabAddProduct.visibility = View.GONE
        }
    }

    private fun observeProducts() {
        lifecycleScope.launch {
            viewModel.allProducts.collect { products ->
                if (products.isEmpty()) {
                    binding.tvNoProducts.visibility = View.VISIBLE
                    binding.rvProducts.visibility = View.GONE
                } else {
                    binding.tvNoProducts.visibility = View.GONE
                    binding.rvProducts.visibility = View.VISIBLE
                    adapter.submitList(products)
                }

                // Update count
                binding.tvProductCount.text = "${products.size} products (${products.count { it.isActive }} active)"
            }
        }
    }

    private fun showAddProductDialog() {
        val binding = DialogAddEditProductBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setView(binding.root)
            .create()

        binding.tvTitle.text = "Add New Product"
        binding.etDefaultPrice.setText("0")

        // Set up unit spinner
        val units = arrayOf("kg", "g", "pc", "dozen", "liter", "ml", "meter", "pack")
        val unitAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, units)
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerUnit.adapter = unitAdapter

        // Save button
        binding.btnSave.setOnClickListener {
            val name = binding.etProductName.text.toString().trim()
            val priceStr = binding.etDefaultPrice.text.toString().trim()

            if (name.isBlank()) {
                Toast.makeText(this, "Please enter product name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val price = priceStr.toDoubleOrNull() ?: 0.0

            val newProduct = Item(
                id = UUID.randomUUID().toString(),
                name = name,
                category = ItemCategory.PRODUCT,
                pricePerKg = price,
                unit = units[binding.spinnerUnit.selectedItemPosition],
                isActive = true,
                createdAt = Date(),
                createdBy = "user_1"
            )

            viewModel.addProduct(newProduct)
            Toast.makeText(this, "Product added", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        // Cancel button
        binding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showEditProductDialog(product: Item) {
        val binding = DialogAddEditProductBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setView(binding.root)
            .create()

        binding.tvTitle.text = "Edit Product"
        binding.etProductName.setText(product.name)
        binding.etDefaultPrice.setText((product.pricePerKg ?: 0.0).toString())

        // Set up unit spinner
        val units = arrayOf("kg", "g", "pc", "dozen", "liter", "ml", "meter", "pack")
        val unitAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, units)
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerUnit.adapter = unitAdapter

        // Select current unit
        val unitIndex = units.indexOf(product.unit)
        if (unitIndex >= 0) {
            binding.spinnerUnit.setSelection(unitIndex)
        }

        // Save button
        binding.btnSave.setOnClickListener {
            val name = binding.etProductName.text.toString().trim()
            val priceStr = binding.etDefaultPrice.text.toString().trim()

            if (name.isBlank()) {
                Toast.makeText(this, "Please enter product name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val price = priceStr.toDoubleOrNull() ?: 0.0

            val updatedProduct = product.copy(
                name = name,
                pricePerKg = price,
                unit = units[binding.spinnerUnit.selectedItemPosition]
            )

            viewModel.updateProduct(updatedProduct)
            Toast.makeText(this, "Product updated", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        // Cancel button
        binding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showDeleteConfirmationDialog(product: Item) {
        AlertDialog.Builder(this)
            .setTitle("Delete Product")
            .setMessage("Are you sure you want to delete \"${product.name}\"?")
            .setPositiveButton("Yes") { _, _ ->
                viewModel.deleteProduct(product)
                Toast.makeText(this, "Product deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun showAddDefaultItemsDialog() {
        AlertDialog.Builder(this)
            .setTitle("Add Default Items")
            .setMessage("This will add the default items (Podipp, Chilly, Malli, Unda) if they don't already exist. Continue?")
            .setPositiveButton("Yes") { _, _ ->
                viewModel.addDefaultItems()
                Toast.makeText(this, "Default items added", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("No", null)
            .show()
    }
}

// Products Adapter
class ProductsAdapter(
    private val onDeleteClick: (Item) -> Unit,
    private val onItemClick: (Item) -> Unit,
    private val onToggleActiveClick: (Item) -> Unit,
    private val showActionButtons: Boolean = true
) : RecyclerView.Adapter<ProductsAdapter.ProductViewHolder>() {

    private var products = listOf<Item>()
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    fun submitList(list: List<Item>) {
        products = list.sortedBy { it.name }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(products[position])
    }

    override fun getItemCount() = products.size

    fun getProductAt(position: Int): Item = products[position]

    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvProductName: TextView = itemView.findViewById(R.id.tv_product_name)
        private val tvPrice: TextView = itemView.findViewById(R.id.tv_price)
        private val tvUnit: TextView = itemView.findViewById(R.id.tv_unit)
        private val switchActive: Switch = itemView.findViewById(R.id.switch_active)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btn_delete)

        fun bind(product: Item) {
            tvProductName.text = product.name
            tvPrice.text = "₹${product.pricePerKg ?: 0.0}"
            tvUnit.text = "per ${product.unit}"
            switchActive.isChecked = product.isActive

            // Set text color based on active state
            val textColor = if (product.isActive) Color.BLACK else Color.GRAY
            tvProductName.setTextColor(textColor)
            tvPrice.setTextColor(textColor)
            tvUnit.setTextColor(textColor)

            // Show/hide action buttons based on user role
            btnDelete.visibility = if (showActionButtons) View.VISIBLE else View.GONE
            switchActive.visibility = if (showActionButtons) View.VISIBLE else View.GONE

            switchActive.setOnCheckedChangeListener { _, _ ->
                onToggleActiveClick(product)
            }

            btnDelete.setOnClickListener {
                onDeleteClick(product)
            }

            itemView.setOnClickListener {
                onItemClick(product)
            }
        }
    }
}