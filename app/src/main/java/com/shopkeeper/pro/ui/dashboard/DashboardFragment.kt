package com.shopkeeper.pro.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.shopkeeper.pro.R
import com.shopkeeper.pro.databinding.FragmentDashboardBinding
import com.shopkeeper.pro.ui.auth.UserPreferences
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
            // TODO: Navigate to inventory management
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}