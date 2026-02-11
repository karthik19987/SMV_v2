package com.shopkeeper.pro.ui.sales

import android.app.DatePickerDialog
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import com.shopkeeper.pro.R
import com.shopkeeper.pro.data.entity.Sale
import com.shopkeeper.pro.data.entity.SaleItem
import com.shopkeeper.pro.databinding.ActivityViewAllSalesBinding
import com.shopkeeper.pro.databinding.DialogEditSaleBinding
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class ViewAllSalesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityViewAllSalesBinding
    private lateinit var viewModel: SalesViewModel
    private lateinit var adapter: SalesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewAllSalesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[SalesViewModel::class.java]

        setupToolbar()
        setupRecyclerView()
        observeSales()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "All Sales"

        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(0, 1, 0, "Clear All")
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
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
        adapter = SalesAdapter(
            onDeleteClick = { sale ->
                showDeleteConfirmationDialog(sale)
            },
            onItemClick = { sale ->
                showEditSaleDialog(sale)
            }
        )

        binding.rvSales.apply {
            layoutManager = LinearLayoutManager(this@ViewAllSalesActivity)
            adapter = this@ViewAllSalesActivity.adapter
        }

        // Add swipe to delete
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            private val deleteIcon = ContextCompat.getDrawable(this@ViewAllSalesActivity, android.R.drawable.ic_menu_delete)
            private val background = ColorDrawable(Color.parseColor("#F44336"))

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val sale = adapter.getSaleAt(position)
                showDeleteConfirmationDialog(sale)
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

        itemTouchHelper.attachToRecyclerView(binding.rvSales)
    }

    private fun observeSales() {
        lifecycleScope.launch {
            viewModel.allSales.collect { sales ->
                if (sales.isEmpty()) {
                    binding.tvNoSales.visibility = View.VISIBLE
                    binding.rvSales.visibility = View.GONE
                } else {
                    binding.tvNoSales.visibility = View.GONE
                    binding.rvSales.visibility = View.VISIBLE
                    adapter.submitList(sales)
                }

                // Calculate total
                val total = sales.sumOf { it.totalAmount }
                binding.tvTotalAmount.text = String.format("Total: ₹%.2f", total)
            }
        }
    }

    private fun showDeleteConfirmationDialog(sale: Sale) {
        AlertDialog.Builder(this)
            .setTitle("Delete Sale")
            .setMessage("Are you sure you want to delete this sale?")
            .setPositiveButton("Yes") { _, _ ->
                viewModel.deleteSale(sale)
                Toast.makeText(this, "Sale deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun showClearAllConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Clear All Sales")
            .setMessage("Are you sure you want to delete ALL sales? This action cannot be undone.")
            .setPositiveButton("Delete All") { _, _ ->
                viewModel.deleteAllSales()
                Toast.makeText(this, "All sales deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditSaleDialog(sale: Sale) {
        val binding = DialogEditSaleBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setView(binding.root)
            .create()

        // Parse sale items
        val saleItems = mutableListOf<SaleItem>()
        try {
            val itemsArray = JSONArray(sale.items)
            for (i in 0 until itemsArray.length()) {
                val itemJson = itemsArray.getJSONObject(i)
                saleItems.add(SaleItem(
                    itemId = itemJson.getString("itemId"),
                    itemName = itemJson.getString("itemName"),
                    quantity = itemJson.getDouble("quantity"),
                    pricePerUnit = itemJson.getDouble("pricePerUnit"),
                    totalPrice = itemJson.getDouble("totalPrice")
                ))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Setup date
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        var selectedDate = sale.createdAt
        binding.tvSelectedDate.text = dateFormat.format(selectedDate)

        // Date picker
        binding.btnPickDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            calendar.time = selectedDate

            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    // Preserve time
                    val oldCalendar = Calendar.getInstance()
                    oldCalendar.time = selectedDate
                    calendar.set(Calendar.HOUR_OF_DAY, oldCalendar.get(Calendar.HOUR_OF_DAY))
                    calendar.set(Calendar.MINUTE, oldCalendar.get(Calendar.MINUTE))
                    calendar.set(Calendar.SECOND, oldCalendar.get(Calendar.SECOND))

                    selectedDate = calendar.time
                    binding.tvSelectedDate.text = dateFormat.format(selectedDate)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // Add sale items to the container
        val itemViews = mutableListOf<Triple<SaleItem, EditText, EditText>>()

        // Function to update total amount
        val updateTotalAmount = {
            var total = 0.0
            itemViews.forEach { (_, qtyEdit, priceEdit) ->
                val qty = qtyEdit.text.toString().toDoubleOrNull() ?: 0.0
                val price = priceEdit.text.toString().toDoubleOrNull() ?: 0.0
                total += qty * price
            }
            binding.tvTotalAmount.text = String.format("₹%.2f", total)
        }

        saleItems.forEach { item ->
            val itemView = layoutInflater.inflate(R.layout.item_edit_sale_item, binding.containerItems, false)

            val tvItemName = itemView.findViewById<TextView>(R.id.tv_item_name)
            val etQuantity = itemView.findViewById<EditText>(R.id.et_quantity)
            val etPrice = itemView.findViewById<EditText>(R.id.et_price)
            val tvItemTotal = itemView.findViewById<TextView>(R.id.tv_item_total)
            val tvUnit = itemView.findViewById<TextView>(R.id.tv_unit)

            tvItemName.text = item.itemName
            etQuantity.setText(item.quantity.toString())
            etPrice.setText(item.pricePerUnit.toString())

            // Set unit based on item
            tvUnit.text = if (item.itemName == "Unda") "pc @" else "kg @"

            // Update item total
            val updateItemTotal = {
                val qty = etQuantity.text.toString().toDoubleOrNull() ?: 0.0
                val price = etPrice.text.toString().toDoubleOrNull() ?: 0.0
                val total = qty * price
                tvItemTotal.text = String.format("₹%.2f", total)
                updateTotalAmount()
            }

            etQuantity.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) { updateItemTotal() }
            })

            etPrice.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) { updateItemTotal() }
            })

            binding.containerItems.addView(itemView)
            itemViews.add(Triple(item, etQuantity, etPrice))
            updateItemTotal()
        }

        // Save button
        binding.btnSave.setOnClickListener {
            // Collect updated items
            val updatedItems = mutableListOf<SaleItem>()
            var totalAmount = 0.0

            itemViews.forEach { (originalItem, qtyEdit, priceEdit) ->
                val qty = qtyEdit.text.toString().toDoubleOrNull()
                val price = priceEdit.text.toString().toDoubleOrNull()

                if (qty != null && qty > 0 && price != null && price > 0) {
                    val itemTotal = qty * price
                    totalAmount += itemTotal

                    updatedItems.add(SaleItem(
                        itemId = originalItem.itemId,
                        itemName = originalItem.itemName,
                        quantity = qty,
                        pricePerUnit = price,
                        totalPrice = itemTotal
                    ))
                }
            }

            if (updatedItems.isEmpty()) {
                Toast.makeText(this, "Sale must have at least one item", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Convert items to JSON
            val itemsJson = JSONArray().apply {
                updatedItems.forEach { item ->
                    put(JSONObject().apply {
                        put("itemId", item.itemId)
                        put("itemName", item.itemName)
                        put("quantity", item.quantity)
                        put("pricePerUnit", item.pricePerUnit)
                        put("totalPrice", item.totalPrice)
                    })
                }
            }.toString()

            // Update sale
            val updatedSale = sale.copy(
                items = itemsJson,
                totalAmount = totalAmount,
                createdAt = selectedDate
            )

            viewModel.updateSale(updatedSale)
            Toast.makeText(this, "Sale updated", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        // Cancel button
        binding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}

// Sales Adapter
class SalesAdapter(
    private val onDeleteClick: (Sale) -> Unit,
    private val onItemClick: (Sale) -> Unit
) : RecyclerView.Adapter<SalesAdapter.SaleViewHolder>() {

    private var sales = listOf<Sale>()
    private val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    fun submitList(list: List<Sale>) {
        sales = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SaleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sale, parent, false)
        return SaleViewHolder(view)
    }

    override fun onBindViewHolder(holder: SaleViewHolder, position: Int) {
        holder.bind(sales[position])
    }

    override fun getItemCount() = sales.size

    fun getSaleAt(position: Int): Sale = sales[position]

    inner class SaleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvSaleId: TextView = itemView.findViewById(R.id.tv_sale_id)
        private val tvItems: TextView = itemView.findViewById(R.id.tv_items)
        private val tvAmount: TextView = itemView.findViewById(R.id.tv_amount)
        private val tvDateTime: TextView = itemView.findViewById(R.id.tv_date_time)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btn_delete)

        fun bind(sale: Sale) {
            tvSaleId.text = "Sale #${sale.id.takeLast(6)}"
            // Parse items JSON to count items
            val itemCount = try {
                JSONArray(sale.items).length()
            } catch (e: Exception) {
                0
            }
            tvItems.text = "$itemCount items"
            tvAmount.text = String.format("₹%.2f", sale.totalAmount)
            tvDateTime.text = dateFormat.format(sale.createdAt)

            btnDelete.setOnClickListener {
                onDeleteClick(sale)
            }

            itemView.setOnClickListener {
                onItemClick(sale)
            }

            itemView.setOnLongClickListener {
                onDeleteClick(sale)
                true
            }
        }
    }
}