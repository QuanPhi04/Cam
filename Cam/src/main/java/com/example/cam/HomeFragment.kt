package com.example.cam

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.cam.databinding.FragmentHomeBinding

import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FruitViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Show bottom navigation when entering Home
        (activity as? MainActivity)?.setBottomNavVisible(true)

        val adapter = FruitAdapter(
            fruits = emptyList(),
            onDeleteClick = { fruit -> viewModel.removeFruit(fruit.id) },
            onItemClick = { fruit ->
                val fragment = AddFruitFragment().apply {
                    arguments = Bundle().apply { putString("fruit_id", fruit.id) }
                }
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit()
            }
        )

        binding.rvRecentFruits.layoutManager = LinearLayoutManager(context)
        binding.rvRecentFruits.adapter = adapter

        binding.btnScan.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, DetectionFragment())
                .addToBackStack(null)
                .commit()
        }

        viewModel.username.observe(viewLifecycleOwner) { name ->
            binding.tvUsername.text = name
        }

        viewModel.fruits.observe(viewLifecycleOwner) { fruits ->
            if (fruits.isEmpty()) {
                binding.tvEmptyMessage.visibility = View.VISIBLE
                binding.rvRecentFruits.visibility = View.GONE
            } else {
                binding.tvEmptyMessage.visibility = View.GONE
                binding.rvRecentFruits.visibility = View.VISIBLE
                // Hiển thị tối đa 3 quả gần nhất ở màn hình chính
                adapter.updateData(fruits.take(3))
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
