package com.example.cam

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.cam.databinding.ItemFruitBinding

class FruitAdapter(
    private var fruits: List<Fruit>,
    private val onDeleteClick: (Fruit) -> Unit,
    private val onItemClick: (Fruit) -> Unit
) : RecyclerView.Adapter<FruitAdapter.FruitViewHolder>() {

    class FruitViewHolder(val binding: ItemFruitBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FruitViewHolder {
        val binding = ItemFruitBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FruitViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FruitViewHolder, position: Int) {
        val fruit = fruits[position]
        holder.binding.fruitName.text = fruit.name
        
        // Use local image URI if available, otherwise fallback to remote URL or placeholder
        val source = fruit.imageUri ?: fruit.imageUrl
        holder.binding.fruitImage.load(source) {
            crossfade(true)
            placeholder(R.drawable.ic_orange)
            error(R.drawable.ic_orange_broken)
        }

        if (fruit.isAiDetected) {
            holder.binding.tvDetected.visibility = android.view.View.VISIBLE
            fruit.confidence?.let {
                holder.binding.tvConfidence.visibility = android.view.View.VISIBLE
                holder.binding.tvConfidence.text = "Độ chính xác: ${(it * 100).toInt()}%"
            } ?: run {
                holder.binding.tvConfidence.visibility = android.view.View.GONE
            }
        } else {
            holder.binding.tvDetected.visibility = android.view.View.GONE
            holder.binding.tvConfidence.visibility = android.view.View.GONE
        }

        if (fruit.scanDate.isNotBlank()) {
            holder.binding.tvScanDate.visibility = android.view.View.VISIBLE
            holder.binding.tvScanDate.text = fruit.scanDate
        } else {
            holder.binding.tvScanDate.visibility = android.view.View.GONE
        }

        holder.binding.btnDelete.setOnClickListener {
            onDeleteClick(fruit)
        }

        holder.binding.root.setOnClickListener {
            onItemClick(fruit)
        }
    }

    override fun getItemCount(): Int = fruits.size

    fun updateData(newFruits: List<Fruit>) {
        this.fruits = newFruits
        notifyDataSetChanged()
    }
}
