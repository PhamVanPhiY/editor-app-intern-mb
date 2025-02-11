package com.example.editor_app_intern.adapter

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.editor_app_intern.R
import com.example.editor_app_intern.databinding.ImageItemLayoutBinding
import com.example.editor_app_intern.model.Image


class ImageAdapter(
    var images: List<Image>, private val onImageClick: (Image) -> Unit
) : RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(val binding: ImageItemLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("ResourceType")
        fun bind(item: Image, onImageClick: (Image) -> Unit) {
            binding.apply {
                Glide.with(binding.root.context).load(item.uri).placeholder(R.drawable.album_home).into(ivPhotoInAlbum)
                Log.d("ImageAdapter", "Image URI adapter: ${item.uri}")
                cwItem.setOnClickListener {
                    onImageClick(item)
                }
            }

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view =
            ImageItemLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(images[position], onImageClick)

    }

    override fun getItemCount(): Int = images.size
}
