package com.example.editor_app_intern.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.editor_app_intern.databinding.FilterItemLayoutBinding
import com.example.editor_app_intern.model.FilterCamera


class FilterCameraAdapter(
    var filterCameraList: List<FilterCamera>, private val onFilterClick: (FilterCamera) -> Unit
) : RecyclerView.Adapter<FilterCameraAdapter.FilterCameraViewHolder>() {

    inner class FilterCameraViewHolder(val binding: FilterItemLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("ResourceType")
        fun bind(item: FilterCamera) {
            binding.apply {
                Glide.with(binding.root.context).load(item.imageFilter).transition(
                    DrawableTransitionOptions.withCrossFade()
                ).into(imageFilter)
                nameFilter.text = item.nameFilter
                cwItem.setOnClickListener {
                    onFilterClick(item)
                }
            }


        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterCameraViewHolder {
        val view =
            FilterItemLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FilterCameraViewHolder(view)
    }

    override fun onBindViewHolder(holder: FilterCameraViewHolder, position: Int) {
        holder.bind(filterCameraList[position])

    }

    override fun getItemCount(): Int = filterCameraList.size
}
