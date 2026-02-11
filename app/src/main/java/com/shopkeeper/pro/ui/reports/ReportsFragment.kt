package com.shopkeeper.pro.ui.reports

import android.app.DatePickerDialog
import android.content.ContentValues
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.button.MaterialButton
import com.shopkeeper.pro.R
import com.shopkeeper.pro.databinding.FragmentReportsBinding
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class ReportsFragment : Fragment() {

    private var _binding: FragmentReportsBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ReportsViewModel
    private val currencyFormat = java.text.NumberFormat.getCurrencyInstance(Locale("hi", "IN")).apply {
        currency = Currency.getInstance("INR")
    }

    private var selectedPeriodButton: MaterialButton? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[ReportsViewModel::class.java]

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        // Set up period selection buttons
        binding.btnToday.setOnClickListener {
            selectPeriodButton(binding.btnToday)
            viewModel.selectToday()
        }

        binding.btnWeek.setOnClickListener {
            selectPeriodButton(binding.btnWeek)
            viewModel.selectThisWeek()
        }

        binding.btnMonth.setOnClickListener {
            selectPeriodButton(binding.btnMonth)
            viewModel.selectThisMonth()
        }

        binding.btnYear.setOnClickListener {
            selectPeriodButton(binding.btnYear)
            viewModel.selectThisYear()
        }

        binding.btnCustom.setOnClickListener {
            selectPeriodButton(binding.btnCustom)
            showCustomDateRangePicker()
        }

        // Export button
        binding.btnExport.setOnClickListener {
            exportReportToCSV()
        }

        // Select today by default
        selectPeriodButton(binding.btnToday)
    }

    private fun selectPeriodButton(button: MaterialButton) {
        // Reset previous selection
        selectedPeriodButton?.apply {
            strokeColor = ContextCompat.getColorStateList(requireContext(), R.color.grey_500)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_500))
        }

        // Highlight new selection
        button.apply {
            strokeColor = ContextCompat.getColorStateList(requireContext(), R.color.purple_500)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.purple_500))
        }

        selectedPeriodButton = button
    }

    private fun showCustomDateRangePicker() {
        val calendar = Calendar.getInstance()
        var startDate: Date? = null
        var endDate: Date? = null

        // Pick start date
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val startCal = Calendar.getInstance()
                startCal.set(year, month, dayOfMonth, 0, 0, 0)
                startDate = startCal.time

                // Pick end date
                DatePickerDialog(
                    requireContext(),
                    { _, endYear, endMonth, endDayOfMonth ->
                        val endCal = Calendar.getInstance()
                        endCal.set(endYear, endMonth, endDayOfMonth, 23, 59, 59)
                        endDate = endCal.time

                        // Validate dates
                        if (endDate!! >= startDate!!) {
                            viewModel.selectCustomRange(startDate!!, endDate!!)
                        } else {
                            Toast.makeText(requireContext(), "End date must be after start date", Toast.LENGTH_SHORT).show()
                        }
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).apply {
                    datePicker.minDate = startCal.timeInMillis
                    setTitle("Select End Date")
                }.show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            setTitle("Select Start Date")
        }.show()
    }

    private fun observeViewModel() {
        viewModel.periodLabel.observe(viewLifecycleOwner) { label ->
            binding.tvDateRange.text = label
        }

        viewModel.reportData.observe(viewLifecycleOwner) { report ->
            updateUI(report)
        }
    }

    private fun updateUI(report: ReportData) {
        // Update summary cards
        binding.tvTotalSales.text = currencyFormat.format(report.totalSales)
        binding.tvSalesCount.text = "${report.salesCount} transactions"

        binding.tvTotalExpenses.text = currencyFormat.format(report.totalExpenses)
        binding.tvExpensesCount.text = "${report.expensesCount} expenses"

        // Update profit with color based on positive/negative
        binding.tvNetProfit.text = currencyFormat.format(report.netProfit)
        binding.tvNetProfit.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                when {
                    report.netProfit > 0 -> R.color.green_500
                    report.netProfit < 0 -> R.color.red_500
                    else -> R.color.blue_500
                }
            )
        )

        // Update sales breakdown
        binding.containerSalesItems.removeAllViews()
        if (report.salesByItem.isEmpty()) {
            binding.tvNoSales.visibility = View.VISIBLE
            binding.containerSalesItems.visibility = View.GONE
        } else {
            binding.tvNoSales.visibility = View.GONE
            binding.containerSalesItems.visibility = View.VISIBLE

            report.salesByItem.forEach { item ->
                val itemView = layoutInflater.inflate(R.layout.item_report_breakdown, binding.containerSalesItems, false)
                itemView.findViewById<TextView>(R.id.tv_item_name).text = item.name
                itemView.findViewById<TextView>(R.id.tv_item_quantity).text = "${item.quantity} ${if (item.name == "Unda") "pc" else "kg"}"
                itemView.findViewById<TextView>(R.id.tv_item_amount).text = currencyFormat.format(item.amount)
                binding.containerSalesItems.addView(itemView)
            }
        }

        // Update expenses breakdown
        binding.containerExpenseCategories.removeAllViews()
        if (report.expensesByCategory.isEmpty()) {
            binding.tvNoExpenses.visibility = View.VISIBLE
            binding.containerExpenseCategories.visibility = View.GONE
        } else {
            binding.tvNoExpenses.visibility = View.GONE
            binding.containerExpenseCategories.visibility = View.VISIBLE

            report.expensesByCategory.forEach { category ->
                val categoryView = layoutInflater.inflate(R.layout.item_report_breakdown, binding.containerExpenseCategories, false)
                categoryView.findViewById<TextView>(R.id.tv_item_name).text = category.category.name.replace("_", " ")
                categoryView.findViewById<TextView>(R.id.tv_item_quantity).text = "${category.count} items"
                categoryView.findViewById<TextView>(R.id.tv_item_amount).text = currencyFormat.format(category.amount)
                binding.containerExpenseCategories.addView(categoryView)
            }
        }
    }

    private fun exportReportToCSV() {
        try {
            val csvContent = viewModel.generateCSVContent()
            if (csvContent.isEmpty()) {
                Toast.makeText(requireContext(), "No data to export", Toast.LENGTH_SHORT).show()
                return
            }

            val fileName = "ShopKeeper_Report_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.csv"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Use MediaStore for Android 10+
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }

                val resolver = requireContext().contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)

                uri?.let {
                    resolver.openOutputStream(it)?.use { outputStream ->
                        outputStream.write(csvContent.toByteArray())
                    }

                    values.clear()
                    values.put(MediaStore.Downloads.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)

                    Toast.makeText(requireContext(), "Report saved to Downloads/$fileName", Toast.LENGTH_LONG).show()
                }
            } else {
                // Use traditional method for older versions
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadsDir, fileName)

                FileOutputStream(file).use { outputStream ->
                    outputStream.write(csvContent.toByteArray())
                }

                Toast.makeText(requireContext(), "Report saved to Downloads/$fileName", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Error exporting report: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}