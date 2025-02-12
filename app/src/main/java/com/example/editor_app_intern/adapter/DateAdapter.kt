package com.example.editor_app_intern.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.editor_app_intern.databinding.DateParentItemLayoutBinding
import com.example.editor_app_intern.extension.GridSpacingItemDecoration
import com.example.editor_app_intern.interfaces.OnImageClickListener
import com.example.editor_app_intern.model.Date


class DateAdapter(
    private var dates: List<Date>,
    private val imageClickListener: OnImageClickListener
) : RecyclerView.Adapter<DateAdapter.DateViewHolder>() {

    inner class DateViewHolder(val binding: DateParentItemLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("ResourceType")
        fun bind(item: Date) {
            binding.apply {
                tvDateTime.text = item.day
                rcvListImageChild.layoutManager = GridLayoutManager(itemView.context, 2)
                rcvListImageChild.adapter = ImageAdapter(
                    item.imageModel,
                    onImageClick = { image ->
                        imageClickListener.onImageClick(image.uri)
                    }
                )
                rcvListImageChild.addItemDecoration(GridSpacingItemDecoration(2, 16))
            }

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DateViewHolder {
        val view =
            DateParentItemLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DateViewHolder(view)
    }

    override fun onBindViewHolder(holder: DateViewHolder, position: Int) {
        holder.bind(dates[position])

    }

    override fun getItemCount(): Int = dates.size

    fun updateDates(newDates: List<Date>) {
        dates = newDates
        notifyDataSetChanged()
    }
}