package com.example.cam

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import coil.load
import coil.transform.CircleCropTransformation
import com.example.cam.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initial fragment
        loadFragment(LoadingFragment())

        // Bottom Navigation handlers
        binding.navHome.setOnClickListener {
            loadFragment(HomeFragment())
        }

        binding.navAdd.setOnClickListener {
            loadFragment(AddFruitFragment())
        }

        binding.navHistory.setOnClickListener {
            loadFragment(FruitListFragment())
        }

        // Profile click handler
        val profileClickListener = View.OnClickListener {
            loadFragment(ProfileFragment())
        }
        binding.profileHeaderContainer.setOnClickListener(profileClickListener)
        binding.btnTopProfile.setOnClickListener(profileClickListener)

        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Listen for back stack changes to sync UI state
        supportFragmentManager.addOnBackStackChangedListener {
            val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
            if (currentFragment != null) {
                updateUIState(currentFragment)
            }
        }

        observeUserProfile()
    }

    private fun observeUserProfile() {
        val viewModel: FruitViewModel by viewModels()
        viewModel.userProfile.observe(this) { profile ->
            profile?.avatarUrl?.let { url ->
                if (url.isNotBlank()) {
                    binding.btnTopProfile.load(url) {
                        placeholder(R.drawable.ic_user_placeholder)
                        error(R.drawable.ic_user_placeholder)
                        transformations(CircleCropTransformation())
                    }
                } else {
                    binding.btnTopProfile.setImageResource(R.drawable.ic_user_placeholder)
                }
            } ?: binding.btnTopProfile.setImageResource(R.drawable.ic_user_placeholder)
        }
    }

    fun loadFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
        
        // Navigation logic: Home, Loading and Onboarding are root screens
        if (fragment is HomeFragment || fragment is LoadingFragment || fragment is OnboardingFragment) {
            supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        } else {
            transaction.addToBackStack(null)
        }
        
        transaction.commit()
        updateUIState(fragment)
    }

    private fun updateUIState(fragment: Fragment) {
        val isHome = fragment is HomeFragment
        val isRoot = isHome || fragment is LoadingFragment || fragment is OnboardingFragment
        
        // 1. Back button: Visible everywhere except root screens
        binding.btnBack.visibility = if (isRoot) View.GONE else View.VISIBLE
        
        // 2. Profile button: ONLY visible on Home screen
        binding.profileHeaderContainer.visibility = if (isHome) View.VISIBLE else View.GONE
        
        // 3. Bottom Nav: Hidden on system screens (Loading/Onboarding) or full-screen camera
        val hideNav = fragment is LoadingFragment || fragment is OnboardingFragment || fragment is DetectionFragment
        binding.bottomNavigationContainer.visibility = if (hideNav) View.GONE else View.VISIBLE
    }

    fun setBottomNavVisible(isVisible: Boolean) {
        binding.bottomNavigationContainer.visibility = if (isVisible) View.VISIBLE else View.GONE
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (currentFragment != null) {
            updateUIState(currentFragment)
        }
    }
}
