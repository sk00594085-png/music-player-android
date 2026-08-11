package com.musicplayer.app.ui.albums

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.musicplayer.app.R
import com.musicplayer.app.databinding.ItemAlbumBinding

class AlbumAdapter(
    private val onClick: (AlbumItem) -> Unit
) : ListAdapter<AlbumItem, AlbumAdapter.AlbumVH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumVH {
        val b = ItemAlbumBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AlbumVH(b)
    }

    override fun onBindViewHolder(holder: AlbumVH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AlbumVH(private val b: ItemAlbumBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: AlbumItem) {
            b.tvAlbumName.text = item.name
            b.tvAlbumArtist.text = item.artist
            b.tvSongCount.text = "${item.songCount} songs"
            Glide.with(b.root.context)
                .load(item.artUri)
                .placeholder(R.drawable.ic_album_art_placeholder)
                .error(R.drawable.ic_album_art_placeholder)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .centerCrop()
                .into(b.ivAlbumArt)
            b.root.setOnClickListener { onClick(item) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<AlbumItem>() {
            override fun areItemsTheSame(a: AlbumItem, b: AlbumItem) = a.name == b.name
            override fun areContentsTheSame(a: AlbumItem, b: AlbumItem) = a == b
        }
    }
}
