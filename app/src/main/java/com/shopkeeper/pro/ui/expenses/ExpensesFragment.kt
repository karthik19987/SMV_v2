package com.shopkeeper.pro.ui.expenses

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.shopkeeper.pro.R
import com.shopkeeper.pro.data.entity.ExpenseCategory
import com.shopkeeper.pro.databinding.FragmentExpensesBinding
import com.shopkeeper.pro.databinding.DialogAddExpenseBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ExpensesFragment : Fragment() {

    private var _binding: FragmentExpensesBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: ExpenseViewModel

    private var currentPhotoUri: Uri? = null
    private var currentPhotoPath: String? = null
    private var currentExpenseDialog: AlertDialog? = null

    companion object {
        const val REQUEST_CAMERA_PERMISSION = 100
    }

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            // Photo captured successfully
            currentPhotoUri?.let { uri ->
                val dialogBinding = currentExpenseDialog?.findViewById<View>(android.R.id.content)?.tag as? DialogAddExpenseBinding
                dialogBinding?.let {
                    it.ivBillPreview.setImageURI(uri)
                    it.ivBillPreview.visibility = View.VISIBLE
                    it.btnRemoveBill.visibility = View.VISIBLE
                    it.btnCaptureBill.text = "Retake"
                }
            }
        } else {
            // Delete the empty file if capture was cancelled
            currentPhotoPath?.let { File(it).delete() }
            currentPhotoUri = null
            currentPhotoPath = null
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExpensesBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[ExpenseViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers()
        setupClickListeners()
        viewModel.loadTodayExpenses()
    }

    private fun setupObservers() {
        // Observe today's expenses
        viewModel.todayExpenses.observe(viewLifecycleOwner) { total ->
            binding.tvTodayExpenses.text = String.format("₹ %.2f", total)
        }

        // Observe save state
        viewModel.saveState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is SaveState.Success -> {
                    Toast.makeText(requireContext(), "Expense added successfully", Toast.LENGTH_SHORT).show()
                }
                is SaveState.Error -> {
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                }
                else -> {}
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnPurchase.setOnClickListener {
            showAddExpenseDialog(ExpenseCategory.PURCHASE, "Purchase")
        }

        binding.btnDailyWages.setOnClickListener {
            showAddExpenseDialog(ExpenseCategory.DAILY_WAGES, "Daily Wages")
        }

        binding.btnBills.setOnClickListener {
            showAddExpenseDialog(ExpenseCategory.BILLS, "Bills")
        }

        binding.btnRent.setOnClickListener {
            showAddExpenseDialog(ExpenseCategory.RENT, "Rent")
        }

        binding.btnElectricity.setOnClickListener {
            showAddExpenseDialog(ExpenseCategory.ELECTRICITY, "Electricity")
        }

        binding.btnTransport.setOnClickListener {
            showAddExpenseDialog(ExpenseCategory.TRANSPORT, "Transport")
        }

        binding.btnOther.setOnClickListener {
            showAddExpenseDialog(ExpenseCategory.OTHER, "Other")
        }

        binding.btnViewAll.setOnClickListener {
            // Navigate to View All Expenses Activity
            val intent = Intent(requireContext(), ViewAllExpensesActivity::class.java)
            startActivity(intent)
        }

        binding.btnPurchaseHistory.setOnClickListener {
            // Navigate to Purchase History Activity (only purchase expenses)
            val intent = Intent(requireContext(), ViewAllExpensesActivity::class.java)
            intent.putExtra("filter_category", ExpenseCategory.PURCHASE.name)
            startActivity(intent)
        }
    }

    private fun showAddExpenseDialog(category: ExpenseCategory, categoryName: String) {
        val dialogBinding = DialogAddExpenseBinding.inflate(layoutInflater)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialogBinding.tvTitle.text = "Add $categoryName Expense"

        // Show bill capture option only for PURCHASE category
        if (category == ExpenseCategory.PURCHASE) {
            dialogBinding.layoutPurchaseBill.visibility = View.VISIBLE

            dialogBinding.btnCaptureBill.setOnClickListener {
                currentExpenseDialog = dialog
                dialog.window?.decorView?.findViewById<View>(android.R.id.content)?.tag = dialogBinding
                capturePhoto()
            }

            dialogBinding.btnRemoveBill.setOnClickListener {
                currentPhotoUri = null
                currentPhotoPath = null
                dialogBinding.ivBillPreview.setImageURI(null)
                dialogBinding.ivBillPreview.visibility = View.GONE
                dialogBinding.btnRemoveBill.visibility = View.GONE
                dialogBinding.btnCaptureBill.text = "Capture Bill"
            }
        }

        dialogBinding.btnCancel.setOnClickListener {
            // Clean up photo if dialog is cancelled
            currentPhotoPath?.let { File(it).delete() }
            currentPhotoUri = null
            currentPhotoPath = null
            dialog.dismiss()
        }

        dialogBinding.btnSave.setOnClickListener {
            val description = dialogBinding.etDescription.text.toString().trim()
            val amountText = dialogBinding.etAmount.text.toString().trim()

            when {
                amountText.isEmpty() -> {
                    dialogBinding.etAmount.error = "Please enter amount"
                }
                else -> {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (amount <= 0) {
                        dialogBinding.etAmount.error = "Please enter valid amount"
                    } else {
                        // Use category name if description is empty
                        val finalDescription = if (description.isEmpty()) {
                            category.toString().lowercase().replaceFirstChar { it.uppercase() }
                        } else {
                            description
                        }
                        viewModel.addExpense(finalDescription, amount, category, currentPhotoUri?.toString())
                        currentPhotoUri = null
                        currentPhotoPath = null
                        dialog.dismiss()
                    }
                }
            }
        }

        dialog.show()
    }

    override fun onResume() {
        super.onResume()
        // Refresh expenses when returning from View All
        viewModel.loadTodayExpenses()
    }

    private fun capturePhoto() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
        } else {
            launchCamera()
        }
    }

    private fun launchCamera() {
        val photoFile = createImageFile()
        photoFile?.also {
            currentPhotoUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                it
            )
            currentPhotoPath = it.absolutePath
            takePictureLauncher.launch(currentPhotoUri)
        }
    }

    private fun createImageFile(): File? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            File.createTempFile(
                "PURCHASE_${timeStamp}_",
                ".jpg",
                storageDir
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Error creating image file", Toast.LENGTH_SHORT).show()
            null
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchCamera()
            } else {
                Toast.makeText(requireContext(), "Camera permission required to capture bills", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}