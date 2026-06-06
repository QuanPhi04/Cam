package com.example.cam

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.cam.databinding.FragmentOnboardingBinding
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch

class OnboardingFragment : Fragment() {
    private var _binding: FragmentOnboardingBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FruitViewModel by activityViewModels()

    private var username: String = ""
    private var password: String = ""
    private var confirmPassword: String = ""
    private var authMode: OnboardingAuthMode = OnboardingAuthMode.USERNAME
    private var authMessage: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = OnboardingAdapter(
            onStartClick = {
                binding.viewPager.currentItem = 1
            },
            onLoginClick = {
                resetAuthState()
                binding.viewPager.currentItem = 4
            },
            onInputChanged = { name, pass, confirm ->
                username = name
                password = pass
                confirmPassword = confirm
                updateNextButton()
            },
            getAuthMode = { authMode },
            getAuthMessage = { authMessage },
            getUsername = { username },
            getPassword = { password },
            getConfirmPassword = { confirmPassword }
        )
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { _, _ -> }.attach()

        binding.viewPager.registerOnPageChangeCallback(object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateNextButton()
            }
        })

        binding.btnNext.setOnClickListener {
            val current = binding.viewPager.currentItem
            if (current < 4) {
                binding.viewPager.currentItem = current + 1
            } else {
                handleAuthStep(adapter)
            }
        }
    }

    private fun handleAuthStep(adapter: OnboardingAdapter) {
        when (authMode) {
            OnboardingAuthMode.USERNAME -> checkUsername(adapter)
            OnboardingAuthMode.LOGIN_PASSWORD -> submitLogin()
            OnboardingAuthMode.REGISTER_PASSWORD -> submitRegister()
        }
    }

    private fun checkUsername(adapter: OnboardingAdapter) {
        val trimmedName = username.trim()
        if (trimmedName.isBlank()) return

        setLoading(true)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val exists = viewModel.userExists(trimmedName)
                authMode = if (exists) {
                    authMessage = "Cam nhớ bạn rồi, nhập mật khẩu để đăng nhập nhé"
                    OnboardingAuthMode.LOGIN_PASSWORD
                } else {
                    authMessage = "Đây là lần đầu Cam gặp bạn, hãy đặt mật khẩu nhé"
                    OnboardingAuthMode.REGISTER_PASSWORD
                }
                adapter.notifyItemChanged(4)
            } catch (error: Exception) {
                Toast.makeText(requireContext(), "Không kiểm tra được tài khoản", Toast.LENGTH_SHORT).show()
            } finally {
                setLoading(false)
                updateNextButton()
            }
        }
    }

    private fun submitLogin() {
        val trimmedName = username.trim()
        if (password.isBlank()) {
            Toast.makeText(requireContext(), "Bạn nhập mật khẩu nhé", Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true)
        viewLifecycleOwner.lifecycleScope.launch {
            val success = try {
                viewModel.login(trimmedName, password)
            } catch (error: Exception) {
                false
            }

            setLoading(false)
            if (success) {
                (activity as? MainActivity)?.loadFragment(HomeFragment())
            } else {
                Toast.makeText(requireContext(), "Tên hoặc mật khẩu chưa đúng", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun submitRegister() {
        val trimmedName = username.trim()
        if (password.isBlank()) {
            Toast.makeText(requireContext(), "Bạn nhập mật khẩu nhé", Toast.LENGTH_SHORT).show()
            return
        }
        if (password != confirmPassword) {
            Toast.makeText(requireContext(), "Mật khẩu nhập lại chưa khớp", Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true)
        viewLifecycleOwner.lifecycleScope.launch {
            val success = try {
                viewModel.register(trimmedName, password)
                true
            } catch (error: Exception) {
                false
            }

            setLoading(false)
            if (success) {
                (activity as? MainActivity)?.loadFragment(HomeFragment())
            } else {
                Toast.makeText(requireContext(), "Chưa tạo được tài khoản", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.loadingOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnNext.isEnabled = !isLoading
        binding.viewPager.isUserInputEnabled = !isLoading
    }

    private fun resetAuthState() {
        username = ""
        password = ""
        confirmPassword = ""
        authMode = OnboardingAuthMode.USERNAME
        authMessage = ""
        binding.btnNext.text = "Tiếp tục"
        setLoading(false)
        updateNextButton()
    }

    private fun updateNextButton() {
        val position = binding.viewPager.currentItem
        if (position == 0) {
            binding.btnNext.visibility = View.GONE
            return
        }

        binding.btnNext.visibility = View.VISIBLE
        if (position < 4) {
            binding.btnNext.text = "Tiếp tục"
            binding.btnNext.isEnabled = true
            return
        }

        binding.btnNext.text = when (authMode) {
            OnboardingAuthMode.USERNAME -> "Tiếp tục"
            OnboardingAuthMode.LOGIN_PASSWORD -> "Đăng nhập"
            OnboardingAuthMode.REGISTER_PASSWORD -> "Tạo tài khoản"
        }
        binding.btnNext.isEnabled = when (authMode) {
            OnboardingAuthMode.USERNAME -> username.isNotBlank()
            OnboardingAuthMode.LOGIN_PASSWORD -> username.isNotBlank() && password.isNotBlank()
            OnboardingAuthMode.REGISTER_PASSWORD -> username.isNotBlank() &&
                password.isNotBlank() &&
                confirmPassword.isNotBlank()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
