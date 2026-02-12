package com.shopkeeper.pro.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.shopkeeper.pro.databinding.ActivityLoginBinding
import com.shopkeeper.pro.ui.main.MainActivity
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        observeViewModel()

        // Debug: Check existing users
        debugCheckUsers()
    }

    private fun setupUI() {
        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (username.isEmpty()) {
                binding.etUsername.error = "Username is required"
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                binding.etPassword.error = "Password is required"
                return@setOnClickListener
            }

            // Hide keyboard
            currentFocus?.let { view ->
                val imm = getSystemService(INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
                imm?.hideSoftInputFromWindow(view.windowToken, 0)
            }

            viewModel.login(username, password)
        }

        binding.btnCreateUser.setOnClickListener {
            // For now, create a demo user
            viewModel.createDemoUser()
        }

        binding.btnResetPassword.setOnClickListener {
            viewModel.resetAdminPassword()
            Toast.makeText(this, "Admin password reset to 'admin123'", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.loginState.collect { state ->
                if (!isFinishing && !isDestroyed) {
                    when (state) {
                        is LoginState.Loading -> {
                            binding.btnLogin.isEnabled = false
                            binding.btnCreateUser.isEnabled = false
                            binding.btnResetPassword.isEnabled = false
                        }
                        is LoginState.Success -> {
                            val intent = Intent(this@LoginActivity, MainActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        }
                        is LoginState.Error -> {
                            binding.btnLogin.isEnabled = true
                            binding.btnCreateUser.isEnabled = true
                            binding.btnResetPassword.isEnabled = true
                            Toast.makeText(this@LoginActivity, state.message, Toast.LENGTH_LONG).show()
                        }
                        is LoginState.Idle -> {
                            binding.btnLogin.isEnabled = true
                            binding.btnCreateUser.isEnabled = true
                            binding.btnResetPassword.isEnabled = true
                        }
                    }
                }
            }
        }
    }

    private fun debugCheckUsers() {
        lifecycleScope.launch {
            viewModel.debugGetAllUsers()
        }
    }
}