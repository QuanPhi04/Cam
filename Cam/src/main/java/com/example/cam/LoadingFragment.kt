package com.example.cam

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.cam.databinding.FragmentLoadingBinding

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LoadingFragment : Fragment() {
    private var _binding: FragmentLoadingBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FruitViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoadingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (arguments?.getBoolean(ARG_AUTH_LOADING) == true) return
        
        // Giả lập loading 2 giây rồi chuyển sang Onboarding
        viewLifecycleOwner.lifecycleScope.launch {
            delay(2000)
            if (isAdded) {
                val nextFragment = if (viewModel.username.value == "Bạn") {
                    OnboardingFragment()
                } else {
                    HomeFragment()
                }
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, nextFragment)
                    .commit()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_AUTH_LOADING = "auth_loading"
    }
}
