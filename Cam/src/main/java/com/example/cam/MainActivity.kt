package com.example.cam

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
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

        binding.navHome.setOnClickListener {
            loadFragment(DetectionFragment())
        }

        binding.navAdd.setOnClickListener {
            loadFragment(AddFruitFragment())
        }

        binding.navHistory.setOnClickListener {
            loadFragment(FruitListFragment())
        }

        binding.profileHeaderContainer.setOnClickListener {
            loadFragment(ProfileFragment())
        }

        observeUserProfile()
    }

    private fun observeUserProfile() {
        val viewModel: FruitViewModel by viewModels()
        
        // Cập nhật tên hiển thị ở header
        viewModel.username.observe(this) { name ->
            binding.tvHeaderUsername.text = "Chào, $name"
        }

        // Cập nhật ảnh đại diện ở header
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
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    fun setBottomNavVisible(isVisible: Boolean) {
        binding.bottomNavigationContainer.visibility = if (isVisible) android.view.View.VISIBLE else android.view.View.GONE
        binding.profileHeaderContainer.visibility = if (isVisible) android.view.View.VISIBLE else android.view.View.GONE
    }
}
