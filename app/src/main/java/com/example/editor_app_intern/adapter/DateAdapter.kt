package com.example.editor_app_intern.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.editor_app_intern.constant.Constants.PATH_IMAGE_FROM_ALBUM
import com.example.editor_app_intern.databinding.DateParentItemLayoutBinding
import com.example.editor_app_intern.model.Date
import com.example.editor_app_intern.ui.album.AlbumActivity
import com.example.editor_app_intern.ui.imagedetail.ImageDetailActivity


class DateAdapter(
    private var dates: List<Date>
) : RecyclerView.Adapter<DateAdapter.DateViewHolder>() {

    inner class DateViewHolder(val binding: DateParentItemLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("ResourceType")
        fun bind(item: Date) {
            binding.apply {
                tvDateTime.text = item.day

                val sortedImages = item.imageModel.sortedByDescending { it.timestamp }
                rcvListImageChild.layoutManager = GridLayoutManager(itemView.context,2)
                rcvListImageChild.adapter = ImageAdapter(
                    sortedImages,
                    onImageClick = { image ->
                        val context = itemView.context as? Activity
                        val intent = Intent(context, ImageDetailActivity::class.java)
                        intent.putExtra(PATH_IMAGE_FROM_ALBUM, image.uri)
                        if (context != null) {
                            context.startActivityForResult(intent, AlbumActivity.REQUEST_CODE)
                        }
                    }
                )
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
}