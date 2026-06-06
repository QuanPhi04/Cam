package com.example.cam

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.cam.databinding.ItemOnboardingFeatureBinding
import com.example.cam.databinding.ItemOnboardingInputBinding
import com.example.cam.databinding.ItemOnboardingWelcomeBinding

enum class OnboardingAuthMode {
    USERNAME,
    LOGIN_PASSWORD,
    REGISTER_PASSWORD
}

class OnboardingAdapter(
    private val onStartClick: () -> Unit,
    private val onLoginClick: () -> Unit,
    private val onInputChanged: (String, String, String) -> Unit,
    private val getAuthMode: () -> OnboardingAuthMode,
    private val getAuthMessage: () -> String,
    private val getUsername: () -> String,
    private val getPassword: () -> String,
    private val getConfirmPassword: () -> String
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_WELCOME = 0
        private const val VIEW_TYPE_FEATURE = 1
        private const val VIEW_TYPE_INPUT = 2
    }

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            0 -> VIEW_TYPE_WELCOME
            4 -> VIEW_TYPE_INPUT
            else -> VIEW_TYPE_FEATURE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_WELCOME -> WelcomeViewHolder(
                ItemOnboardingWelcomeBinding.inflate(inflater, parent, false),
                onStartClick,
                onLoginClick
            )
            VIEW_TYPE_INPUT -> InputViewHolder(
                ItemOnboardingInputBinding.inflate(inflater, parent, false),
                onInputChanged,
                getAuthMode,
                getAuthMessage,
                getUsername,
                getPassword,
                getConfirmPassword
            )
            else -> FeatureViewHolder(
                ItemOnboardingFeatureBinding.inflate(inflater, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is FeatureViewHolder -> holder.bind(position)
            is InputViewHolder -> holder.bind()
            is WelcomeViewHolder -> holder.bind()
        }
    }

    override fun getItemCount(): Int = 5

    class WelcomeViewHolder(
        private val binding: ItemOnboardingWelcomeBinding,
        private val onStartClick: () -> Unit,
        private val onLoginClick: () -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind() {
            binding.ivillustration.setImageResource(R.drawable.mascotp1)
            binding.btnGetStarted.setOnClickListener { onStartClick() }
            binding.btnLearnMore.setOnClickListener { onLoginClick() }
        }
    }

    class FeatureViewHolder(private val binding: ItemOnboardingFeatureBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(position: Int) {
            when (position) {
                1 -> {
                    binding.ivillustration.setImageResource(R.drawable.mascotp2)
                    binding.tvTitle.text = "Cam sẽ giúp bạn..."
                    binding.tvDesc.text = "Biết loại quả nào tốt cho chế độ ăn kiêng của bạn"
                }
                2 -> {
                    binding.ivillustration.setImageResource(R.drawable.mascotp3)
                    binding.tvTitle.text = "Cam sẽ giúp bạn..."
                    binding.tvDesc.text = "Nhận biết quả ăn được, không ăn được, bảo vệ những bạn bị dị ứng"
                }
                3 -> {
                    binding.ivillustration.setImageResource(R.drawable.mascotp4)
                    binding.tvTitle.text = "Cam sẽ giúp bạn..."
                    binding.tvDesc.text = "...Hoặc giúp các bạn hiểu thêm về thế giới của các loại quả chúng tớ."
                }
            }
        }
    }

    class InputViewHolder(
        private val binding: ItemOnboardingInputBinding,
        private val onInputChanged: (String, String, String) -> Unit,
        private val getAuthMode: () -> OnboardingAuthMode,
        private val getAuthMessage: () -> String,
        private val getUsername: () -> String,
        private val getPassword: () -> String,
        private val getConfirmPassword: () -> String
    ) : RecyclerView.ViewHolder(binding.root) {
        
        private var textWatcher: android.text.TextWatcher? = null

        fun bind() {
            val mode = getAuthMode()
            val message = getAuthMessage()
            val passwordVisibility = if (mode == OnboardingAuthMode.USERNAME) {
                android.view.View.GONE
            } else {
                android.view.View.VISIBLE
            }
            val confirmVisibility = if (mode == OnboardingAuthMode.REGISTER_PASSWORD) {
                android.view.View.VISIBLE
            } else {
                android.view.View.GONE
            }

            binding.tvAuthMessage.text = message
            binding.tvAuthMessage.visibility = if (message.isBlank()) android.view.View.GONE else android.view.View.VISIBLE
            binding.etPassword.visibility = passwordVisibility
            binding.passwordUnderline.visibility = passwordVisibility
            binding.etConfirmPassword.visibility = confirmVisibility
            binding.confirmPasswordUnderline.visibility = confirmVisibility

            // Remove old watcher if exists
            textWatcher?.let {
                binding.etUsername.removeTextChangedListener(it)
                binding.etPassword.removeTextChangedListener(it)
                binding.etConfirmPassword.removeTextChangedListener(it)
            }

            // Sync with current state before adding listener
            val u = getUsername()
            if (binding.etUsername.text.toString() != u) {
                binding.etUsername.setText(u)
            }
            val p = getPassword()
            if (binding.etPassword.text.toString() != p) {
                binding.etPassword.setText(p)
            }
            val cp = getConfirmPassword()
            if (binding.etConfirmPassword.text.toString() != cp) {
                binding.etConfirmPassword.setText(cp)
            }

            textWatcher = object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    onInputChanged(
                        binding.etUsername.text.toString(),
                        binding.etPassword.text.toString(),
                        binding.etConfirmPassword.text.toString()
                    )
                }
                override fun afterTextChanged(s: android.text.Editable?) {}
            }

            binding.etUsername.addTextChangedListener(textWatcher)
            binding.etPassword.addTextChangedListener(textWatcher)
            binding.etConfirmPassword.addTextChangedListener(textWatcher)
        }
    }
}
