package com.example.cam

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cam.databinding.FragmentFruitListBinding

class FruitListFragment : Fragment() {
    private var _binding: FragmentFruitListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FruitViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFruitListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, HomeFragment())
                .commit()
        }

        binding.btnClearHistory.setOnClickListener {
            viewModel.clearAllFruits()
        }

        val adapter = FruitAdapter(
            fruits = emptyList(),
            onDeleteClick = { fruit ->
                viewModel.removeFruit(fruit.id)
            },
            onItemClick = { fruit ->
                // Mở màn hình chỉnh sửa (dùng chung AddFruitFragment nhưng truyền data)
                val fragment = AddFruitFragment().apply {
                    arguments = Bundle().apply {
                        putString("fruit_id", fruit.id)
                    }
                }
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit()
            }
        )
        binding.recyclerViewFruits.layoutManager = LinearLayoutManager(context)
        binding.recyclerViewFruits.adapter = adapter

        viewModel.fruits.observe(viewLifecycleOwner) { fruits ->
            adapter.updateData(fruits)
            binding.tvEmptyHistory.visibility = if (fruits.isEmpty()) View.VISIBLE else View.GONE
            binding.recyclerViewFruits.visibility = if (fruits.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
