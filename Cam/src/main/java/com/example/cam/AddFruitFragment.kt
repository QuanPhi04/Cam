package com.example.cam

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.cam.databinding.FragmentAddFruitBinding

import coil.load

class AddFruitFragment : Fragment() {
    private var _binding: FragmentAddFruitBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FruitViewModel by activityViewModels()
    private var currentImageUri: String? = null

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            currentImageUri = it.toString()
            binding.imgPlaceholder.load(it)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddFruitBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fruitId = arguments?.getString("fruit_id")
        val imageUriFromCamera = arguments?.getString("image_uri")
        val detectedName = arguments?.getString("detected_name")
        val detectedConfidence = arguments?.takeIf { it.containsKey("confidence") }?.getFloat("confidence")
        val isEditing = fruitId != null
        var existingScanDate = ""
        var confidenceToSave = detectedConfidence

        // Nếu có ảnh từ Camera/Gallery truyền sang
        if (imageUriFromCamera != null) {
            currentImageUri = imageUriFromCamera
            binding.imgPlaceholder.load(imageUriFromCamera)
        }

        if (isEditing) {
            binding.tvTitle.text = "Edit Fruit"
            val existingFruit = viewModel.fruits.value?.find { it.id == fruitId }
            existingFruit?.let { fruit ->
                binding.etFruitName.setText(fruit.name)
                currentImageUri = fruit.imageUri?.toString() ?: fruit.imageUrl
                binding.imgPlaceholder.load(currentImageUri)
                existingScanDate = fruit.scanDate
                confidenceToSave = fruit.confidence

                if (fruit.isAiDetected && fruit.confidence != null) {
                    binding.tvConfidenceDetail.visibility = View.VISIBLE
                    binding.tvConfidenceDetail.text = "Độ chính xác: ${(fruit.confidence * 100).toInt()}%"
                } else {
                    binding.tvConfidenceDetail.visibility = View.GONE
                }
                
                binding.tagsContainer.removeAllViews()
                fruit.tags.forEach { (name, content) ->
                    val tagBinding = com.example.cam.databinding.ItemTagEditBinding.inflate(layoutInflater, binding.tagsContainer, true)
                    tagBinding.etTagName.setText(name)
                    tagBinding.etTagContent.setText(content)
                }
            }
        } else {
            binding.tvTitle.text = "Add Fruit"
            detectedName?.let { binding.etFruitName.setText(it) }
            if (detectedConfidence != null) {
                binding.tvConfidenceDetail.visibility = View.VISIBLE
                binding.tvConfidenceDetail.text = "Độ chính xác: ${(detectedConfidence * 100).toInt()}%"
            } else {
                binding.tvConfidenceDetail.visibility = View.GONE
            }
        }

        binding.rootLayout.setOnClickListener {
            if (!parentFragmentManager.popBackStackImmediate()) {
                (activity as? MainActivity)?.loadFragment(HomeFragment())
            }
        }
        binding.contentLayout.setOnClickListener { }
        binding.imgPlaceholder.setOnClickListener { pickImageLauncher.launch("image/*") }

        binding.btnAddTag.setOnClickListener {
            com.example.cam.databinding.ItemTagEditBinding.inflate(layoutInflater, binding.tagsContainer, true)
        }

        binding.btnSave.setOnClickListener {
            val fruitName = binding.etFruitName.text.toString()
            val tags = mutableMapOf<String, String>()
            for (i in 0 until binding.tagsContainer.childCount) {
                val child = binding.tagsContainer.getChildAt(i)
                val tagName = child.findViewById<android.widget.EditText>(R.id.et_tag_name)?.text.toString()
                val tagContent = child.findViewById<android.widget.EditText>(R.id.et_tag_content)?.text.toString()
                if (tagName.isNotEmpty()) tags[tagName] = tagContent
            }

            val fruitToSave = Fruit(
                id = fruitId ?: java.util.UUID.randomUUID().toString(),
                name = if (fruitName.isEmpty()) "Unnamed Fruit" else fruitName,
                imageUri = currentImageUri?.let { android.net.Uri.parse(it) },
                tags = tags,
                isAiDetected = confidenceToSave != null,
                confidence = confidenceToSave,
                scanDate = existingScanDate
            )

            if (isEditing) viewModel.updateFruit(fruitToSave) else viewModel.addFruit(fruitToSave)

            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, SuccessFragment())
                .commit()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
