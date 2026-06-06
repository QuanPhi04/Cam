package com.example.cam

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import coil.load
import coil.transform.CircleCropTransformation
import com.example.cam.databinding.FragmentProfileBinding
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FruitViewModel by activityViewModels()
    private lateinit var userRepository: UserRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        userRepository = UserRepository(requireContext())
        return binding.root
    }

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { uploadAvatar(it) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadProfile()

        binding.ivAvatar.setOnClickListener {
            pickImage.launch("image/*")
        }

        binding.etDob.setOnClickListener {
            showDatePicker()
        }

        binding.btnSave.setOnClickListener {
            saveProfile()
        }

        binding.btnLogout.setOnClickListener {
            logout()
        }
        
        observeProfile()
    }

    private fun observeProfile() {
        viewModel.userProfile.observe(viewLifecycleOwner) { profile ->
            profile?.avatarUrl?.let { url ->
                if (url.isNotBlank()) {
                    binding.ivAvatar.load(url) {
                        placeholder(R.drawable.ic_user_placeholder)
                        error(R.drawable.ic_user_placeholder)
                        transformations(CircleCropTransformation())
                    }
                }
            }
        }
    }

    private fun loadProfile() {
        val username = viewModel.username.value ?: return
        if (username == "Bạn") {
            binding.btnLogout.text = "Đăng nhập / Bắt đầu"
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val profile = userRepository.getProfile(username)
            profile?.let {
                // Only set text if the field is currently empty to avoid overwriting user input
                if (binding.etName.text.isNullOrBlank()) binding.etName.setText(it.name)
                if (binding.etAge.text.isNullOrBlank()) binding.etAge.setText(it.age?.toString() ?: "")
                if (binding.etWeight.text.isNullOrBlank()) binding.etWeight.setText(it.weight?.toString() ?: "")
                if (binding.etDob.text.isNullOrBlank()) binding.etDob.setText(it.dob)
                if (binding.etHobbies.text.isNullOrBlank()) binding.etHobbies.setText(it.hobbies)
                if (binding.etDiet.text.isNullOrBlank()) binding.etDiet.setText(it.diet)
                
                if (!it.avatarUrl.isNullOrBlank()) {
                    binding.ivAvatar.load(it.avatarUrl) {
                        placeholder(R.drawable.ic_user_placeholder)
                        error(R.drawable.ic_user_placeholder)
                        transformations(CircleCropTransformation())
                    }
                }
            }
        }
    }

    private fun uploadAvatar(uri: android.net.Uri) {
        val username = viewModel.username.value ?: return
        if (username == "Bạn") return

        val storage = com.google.firebase.storage.FirebaseStorage.getInstance()
        val storageRef = storage.reference
            .child("avatars/${UserRepository.Companion.run { username.toUserKey() }}.jpg")

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                Toast.makeText(context, "Đang tải ảnh lên...", Toast.LENGTH_SHORT).show()
                
                // Upload task
                val uploadTask = storageRef.putFile(uri)
                uploadTask.await() // Requires kotlinx-coroutines-play-services
                
                // Get download URL
                val downloadUrl = storageRef.downloadUrl.await().toString()
                
                val currentProfile = userRepository.getProfile(username) ?: UserProfile()
                val updatedProfile = currentProfile.copy(avatarUrl = downloadUrl)
                
                viewModel.updateProfile(updatedProfile)
                Toast.makeText(context, "Đã cập nhật ảnh đại diện", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Lỗi khi tải ảnh: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
            val dob = "$selectedDay/${selectedMonth + 1}/$selectedYear"
            binding.etDob.setText(dob)
        }, year, month, day).show()
    }

    private fun saveProfile() {
        val username = viewModel.username.value ?: return
        if (username == "Bạn") {
            Toast.makeText(context, "Vui lòng đăng nhập để lưu thông tin", Toast.LENGTH_SHORT).show()
            return
        }

        val profile = UserProfile(
            name = binding.etName.text.toString(),
            age = binding.etAge.text.toString().toIntOrNull(),
            weight = binding.etWeight.text.toString().toFloatOrNull(),
            dob = binding.etDob.text.toString(),
            hobbies = binding.etHobbies.text.toString(),
            diet = binding.etDiet.text.toString(),
            avatarUrl = viewModel.userProfile.value?.avatarUrl
        )

        viewModel.updateProfile(profile)
        // Cập nhật lại tên người dùng chính nếu họ đổi tên ở đây để Header thay đổi theo
        if (!profile.name.isNullOrBlank()) {
            viewModel.setUsername(profile.name)
        }
        
        Toast.makeText(context, "Đã lưu thông tin cá nhân", Toast.LENGTH_SHORT).show()
    }

    private fun logout() {
        viewModel.logout()
        (activity as? MainActivity)?.loadFragment(OnboardingFragment())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
