package com.junianto.facemaskdetection.ui.camera.recoginition

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.junianto.facemaskdetection.data.Recognition
import com.junianto.facemaskdetection.databinding.RecognitionItemBinding

class RecognitionAdapter (private val context: Context) : ListAdapter<Recognition, RecognitionViewHolder>(RecognitionDiffUtil()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecognitionViewHolder {
        val inflater = LayoutInflater.from(context)
        val binding = RecognitionItemBinding.inflate(inflater, parent, false)
        return RecognitionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecognitionViewHolder, position: Int) {
        holder.bindTo(getItem(position))
    }

    private class RecognitionDiffUtil : DiffUtil.ItemCallback<Recognition>() {
        override fun areItemsTheSame(oldItem: Recognition, newItem: Recognition): Boolean {
            return oldItem.label == newItem.label
        }

        override fun areContentsTheSame(oldItem: Recognition, newItem: Recognition): Boolean {
            return oldItem.confidence == newItem.confidence
        }
    }
}

class RecognitionViewHolder(private val binding: RecognitionItemBinding): RecyclerView.ViewHolder(binding.root) {

    fun bindTo(recognition: Recognition) {
        binding.recognitionItem = recognition
        binding.executePendingBindings()
    }
}
