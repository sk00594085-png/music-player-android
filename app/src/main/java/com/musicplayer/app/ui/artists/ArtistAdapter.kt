package com.musicplayer.app.ui.artists

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.musicplayer.app.databinding.ItemArtistBinding

class ArtistAdapter(
    private val onClick: (ArtistItem) -> Unit
) : ListAdapter<ArtistItem, ArtistAdapter.ArtistVH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtistVH {
        val b = ItemArtistBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ArtistVH(b)
    }

    override fun onBindViewHolder(holder: ArtistVH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ArtistVH(private val b: ItemArtistBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: ArtistItem) {
            b.tvArtistName.text = item.name
            b.tvSongCount.text = "${item.songCount} songs"
            b.root.setOnClickListener { onClick(item) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<ArtistItem>() {
            override fun areItemsTheSame(a: ArtistItem, b: ArtistItem) = a.name == b.name
            override fun areContentsTheSame(a: ArtistItem, b: ArtistItem) = a == b
        }
    }
}
