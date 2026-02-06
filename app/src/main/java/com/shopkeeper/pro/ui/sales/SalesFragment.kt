package com.shopkeeper.pro.ui.sales

import android.os.Bundle
import android.text.TextWatcher
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.shopkeeper.pro.databinding.FragmentSalesBinding
import java.text.NumberFormat
import java.util.*

class SalesFragment : Fragment() {

    private var _binding: FragmentSalesBinding? = null
    private val binding get() = _binding!!
    
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("hi", "IN")).apply {
        currency = Currency.getInstance("INR")
    }

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
        setupUI()
        updateTotalAmount()
    }

    private fun setupUI() {
        // Set up text watchers for all quantity and price fields
        setupItemTextWatchers(
            binding.etPodippQty, binding.etPodippPrice, binding.tvPodippTotal
        )
        setupItemTextWatchers(
            binding.etChillyQty, binding.etChillyPrice, binding.tvChillyTotal
        )
        setupItemTextWatchers(
            binding.etMalliQty, binding.etMalliPrice, binding.tvMalliTotal
        )
        setupItemTextWatchers(
            binding.etUndaQty, binding.etUndaPrice, binding.tvUndaTotal
        )
        
        // Action buttons
        binding.btnClearAll.setOnClickListener { clearAllFields() }
        binding.btnCompleteSale.setOnClickListener { completeSale() }
        binding.btnAddNewItem.setOnClickListener { 
            Toast.makeText(requireContext(), "Add new item feature coming soon!", Toast.LENGTH_SHORT).show()
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

    private fun updateItemTotal(qtyField: EditText, priceField: EditText, totalField: TextView) {
        val quantity = qtyField.text.toString().toDoubleOrNull() ?: 0.0
        val price = priceField.text.toString().toDoubleOrNull() ?: 0.0
        val total = quantity * price
        
        totalField.text = if (total > 0) currencyFormatter.format(total) else "₹0"
    }

    private fun updateTotalAmount() {
        var grandTotal = 0.0
        
        // Add all item totals
        grandTotal += getItemTotal(binding.etPodippQty, binding.etPodippPrice)
        grandTotal += getItemTotal(binding.etChillyQty, binding.etChillyPrice)
        grandTotal += getItemTotal(binding.etMalliQty, binding.etMalliPrice)
        grandTotal += getItemTotal(binding.etUndaQty, binding.etUndaPrice)
        
        binding.tvTotalAmount.text = currencyFormatter.format(grandTotal)
    }

    private fun getItemTotal(qtyField: EditText, priceField: EditText): Double {
        val quantity = qtyField.text.toString().toDoubleOrNull() ?: 0.0
        val price = priceField.text.toString().toDoubleOrNull() ?: 0.0
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
        return binding.etPodippQty.text.isNotEmpty() ||
               binding.etPodippPrice.text.isNotEmpty() ||
               binding.etChillyQty.text.isNotEmpty() ||
               binding.etChillyPrice.text.isNotEmpty() ||
               binding.etMalliQty.text.isNotEmpty() ||
               binding.etMalliPrice.text.isNotEmpty() ||
               binding.etUndaQty.text.isNotEmpty() ||
               binding.etUndaPrice.text.isNotEmpty()
    }

    private fun clearFields() {
        binding.etPodippQty.text.clear()
        binding.etPodippPrice.text.clear()
        binding.etChillyQty.text.clear()
        binding.etChillyPrice.text.clear()
        binding.etMalliQty.text.clear()
        binding.etMalliPrice.text.clear()
        binding.etUndaQty.text.clear()
        binding.etUndaPrice.text.clear()
        
        // Clear totals
        binding.tvPodippTotal.text = "₹0"
        binding.tvChillyTotal.text = "₹0"
        binding.tvMalliTotal.text = "₹0"
        binding.tvUndaTotal.text = "₹0"
        
        updateTotalAmount()
        Toast.makeText(requireContext(), "All fields cleared", Toast.LENGTH_SHORT).show()
    }

    private fun completeSale() {
        val grandTotal = getItemTotal(binding.etPodippQty, binding.etPodippPrice) +
                        getItemTotal(binding.etChillyQty, binding.etChillyPrice) +
                        getItemTotal(binding.etMalliQty, binding.etMalliPrice) +
                        getItemTotal(binding.etUndaQty, binding.etUndaPrice)

        if (grandTotal <= 0) {
            Toast.makeText(requireContext(), "Please add items to complete the sale", Toast.LENGTH_SHORT).show()
            return
        }

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Complete Sale")
            .setMessage("Total Amount: ${currencyFormatter.format(grandTotal)}\n\nConfirm this sale?")
            .setPositiveButton("Confirm") { _, _ ->
                // TODO: Save sale to database
                Toast.makeText(requireContext(), "Sale completed successfully!", Toast.LENGTH_SHORT).show()
                clearFields()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}