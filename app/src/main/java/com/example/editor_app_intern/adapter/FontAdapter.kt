package com.example.editor_app_intern.adapter

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.editor_app_intern.R
import com.example.editor_app_intern.databinding.FontItemLayoutBinding
import com.example.editor_app_intern.model.FontItem


class FontAdapter(
    var fontList: List<FontItem>, private val onFontClick: (FontItem) -> Unit
) : RecyclerView.Adapter<FontAdapter.FontViewHolder>() {

    private var selectedFontIndex: Int = -1

    inner class FontViewHolder(val binding: FontItemLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("ResourceType", "NotifyDataSetChanged")
        fun bind(item: FontItem) {
            binding.apply {
                val typeface = Typeface.createFromAsset(itemView.context.assets, item.fontPath)
                btnFont.typeface = typeface

                if (adapterPosition == selectedFontIndex) {
                    btnFont.setTextColor(Color.BLACK)
                    btnFont.background = ContextCompat.getDrawable(itemView.context, R.drawable.custom_button_font_active)
                } else {
                    btnFont.setTextColor(Color.WHITE)
                    btnFont.background = ContextCompat.getDrawable(itemView.context, R.drawable.custom_button_font)
                }

                cwItem.setOnClickListener {
                    selectedFontIndex = adapterPosition
                    notifyDataSetChanged()
                    onFontClick(item)

                }
            }


        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FontViewHolder {
        val view =
            FontItemLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FontViewHolder(view)
    }

    override fun onBindViewHolder(holder: FontViewHolder, position: Int) {
        holder.bind(fontList[position])

    }

    override fun getItemCount(): Int = fontList.size
}
