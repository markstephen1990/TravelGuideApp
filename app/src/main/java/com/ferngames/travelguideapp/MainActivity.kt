package com.ferngames.travelguideapp

import android.os.Bundle
import android.view.MenuItem
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.homeFragment -> {
                    navController.popBackStack(R.id.homeFragment, false)
                    true
                }
                R.id.exploreFragment -> {
                    if (navController.currentDestination?.id != R.id.exploreFragment) {
                        navController.navigate(R.id.exploreFragment)
                    }
                    true
                }
                R.id.mapFragment -> {
                    if (navController.currentDestination?.id != R.id.mapFragment) {
                        navController.navigate(R.id.mapFragment)
                    }
                    true
                }
                R.id.wishlistFragment -> {
                    if (navController.currentDestination?.id != R.id.wishlistFragment) {
                        navController.navigate(R.id.wishlistFragment)
                    }
                    true
                }
                R.id.moreMenu -> {
                    showMoreMenu(bottomNav)
                    true
                }
                else -> false
            }
        }
    }

    private fun showMoreMenu(anchor: BottomNavigationView) {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        val popup = PopupMenu(this, anchor)
        popup.menuInflater.inflate(R.menu.menu_more, popup.menu)

        popup.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.assistantFragment -> {
                    if (navController.currentDestination?.id != R.id.assistantFragment) {
                        navController.navigate(R.id.assistantFragment)
                    }
                    true
                }
                R.id.plannerFragment -> {
                    if (navController.currentDestination?.id != R.id.plannerFragment) {
                        navController.navigate(R.id.plannerFragment)
                    }
                    true
                }
                R.id.journalFragment -> {
                    if (navController.currentDestination?.id != R.id.journalFragment) {
                        navController.navigate(R.id.journalFragment)
                    }
                    true
                }
                else -> false
            }
        }
        popup.show()
    }
}