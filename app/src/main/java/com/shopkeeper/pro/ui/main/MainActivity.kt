package com.shopkeeper.pro.ui.main

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.shopkeeper.pro.R
import com.shopkeeper.pro.databinding.ActivityMainBinding
import com.shopkeeper.pro.ui.auth.UserPreferences

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if user is logged in
        if (!UserPreferences.isLoggedIn(this)) {
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
        setupActionBar()
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_dashboard,
                R.id.nav_sales,
                R.id.nav_expenses,
                R.id.nav_reports
            )
        )

        // Set up bottom navigation with proper handling
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            // Clear back stack and navigate to selected destination
            navController.popBackStack(navController.graph.startDestinationId, false)
            when (item.itemId) {
                R.id.nav_dashboard -> navController.navigate(R.id.nav_dashboard)
                R.id.nav_sales -> navController.navigate(R.id.nav_sales)
                R.id.nav_expenses -> navController.navigate(R.id.nav_expenses)
                R.id.nav_reports -> navController.navigate(R.id.nav_reports)
            }
            true
        }

        // Add reselected listener to handle tap on already selected item
        binding.bottomNavigation.setOnItemReselectedListener {
            // Do nothing to avoid navigation stack issues
        }
    }

    private fun setupActionBar() {
        setSupportActionBar(binding.toolbar)
        setupActionBarWithNavController(navController, appBarConfiguration)
        
        // Simple title only
        supportActionBar?.title = "ShopKeeper Pro"
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                UserPreferences.clearUser(this)
                finish()
                true
            }
            R.id.action_settings -> {
                // TODO: Navigate to settings
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}