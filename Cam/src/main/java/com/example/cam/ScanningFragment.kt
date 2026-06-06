package com.example.cam

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.cam.databinding.FragmentScanningBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

class ScanningFragment : Fragment() {
    private var _binding: FragmentScanningBinding? = null
    private val binding get() = _binding!!
    private var scanningAnimator: ObjectAnimator? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScanningBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val imageUri = arguments?.getString("image_uri")
        imageUri?.let {
            binding.imgPreview.load(it)
        }

        startScanningAnimation()

        // Giả lập thời gian quét AI (khoảng 3 giây)
        viewLifecycleOwner.lifecycleScope.launch {
            delay(3000)
            if (isAdded) {
                // Tỉ lệ thành công 80% để test cả 2 màn hình
                if (Random.nextFloat() < 0.8f) {
                    navigateToResult(imageUri, detectFruitName(), Random.nextFloat() * 0.16f + 0.82f)
                } else {
                    navigateToError()
                }
            }
        }
    }

    private fun startScanningAnimation() {
        binding.root.post {
            val height = binding.root.height.toFloat()
            scanningAnimator = ObjectAnimator.ofFloat(
                binding.scanningLine,
                "translationY",
                0f,
                height
            ).apply {
                duration = 2000
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
                interpolator = LinearInterpolator()
                start()
            }
        }
    }

    private fun navigateToResult(imageUri: String?, detectedName: String, confidence: Float) {
        val fragment = AddFruitFragment().apply {
            arguments = Bundle().apply {
                putString("image_uri", imageUri)
                putString("detected_name", detectedName)
                putFloat("confidence", confidence)
            }
        }
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun navigateToError() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, ErrorFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun detectFruitName(): String {
        val fruits = listOf("Táo", "Cam", "Chuối", "Xoài", "Ổi", "Dưa hấu", "Nho")
        return fruits.random()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        scanningAnimator?.cancel()
        _binding = null
    }
}
