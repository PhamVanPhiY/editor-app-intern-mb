package com.example.editor_app_intern.adapter

import android.annotation.SuppressLint
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.editor_app_intern.R
import com.example.editor_app_intern.databinding.StickerItemLayoutBinding
import com.example.editor_app_intern.model.Sticker
import com.google.firebase.storage.FirebaseStorage
import java.io.File


class StickerAdapter(
    var stickerList: List<Sticker>, private val onStickerClick: (Sticker) -> Unit
) : RecyclerView.Adapter<StickerAdapter.StickerViewHolder>() {
    private var stickerCount = 0

    inner class StickerViewHolder(val binding: StickerItemLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {


        @SuppressLint("ResourceType")
        fun bind(item: Sticker) {
            binding.apply {
                Glide.with(binding.root.context).load(item.url).placeholder(R.drawable.icon_sticker)
                    .into(imageSticker)

                if (isStickerDownloaded(item.name)) {
                    btnDownloadSticker.visibility = ViewGroup.GONE
                    cwItem.setOnClickListener {
                        onStickerClick(item)
                    }
                } else {
                    btnDownloadSticker.setOnClickListener {
                        downloadSticker(item.url, item.name)
                    }
                    cwItem.setOnClickListener {
                        downloadSticker(item.url, item.name)
                    }
                    btnDownloadSticker.visibility = ViewGroup.VISIBLE
                }
            }
        }
    }

    private fun isStickerDownloaded(name: String): Boolean {
        val localFile = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "$name.png"
        )
        return localFile.exists()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun downloadSticker(url: String, name: String) {
        val storage = FirebaseStorage.getInstance()
        val storageRef = storage.getReferenceFromUrl(url)

        val picturesDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val localFile = File(picturesDir, "$name.png")

        if (localFile.exists()) {
            Log.d("StickerAdapter", "Sticker already exists: ${localFile.absolutePath}")
            return
        }

        storageRef.getFile(localFile).addOnSuccessListener {
            Log.d("StickerAdapter", "Sticker downloaded successfully to: ${localFile.absolutePath}")
            notifyDataSetChanged()
        }.addOnFailureListener {
            Log.e("StickerAdapter", "Failed to download sticker: ${it.message}")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StickerViewHolder {
        val view =
            StickerItemLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StickerViewHolder(view)
    }

    override fun onBindViewHolder(holder: StickerViewHolder, position: Int) {
        holder.bind(stickerList[position])

    }

    override fun getItemCount(): Int = stickerList.size
}

