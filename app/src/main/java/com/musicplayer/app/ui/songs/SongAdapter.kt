package com.musicplayer.app.ui.songs

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.musicplayer.app.R
import com.musicplayer.app.databinding.ItemSongBinding
import com.musicplayer.app.model.Song

class SongAdapter(
    private val onSongClick: (Int) -> Unit,
    private val onSongLongClick: ((Song) -> Unit)? = null
) : ListAdapter<Song, SongAdapter.SongViewHolder>(DIFF) {

    private var activeSongId: Long = -1L

    fun setActiveSong(id: Long) {
        val old = currentList.indexOfFirst { it.id == activeSongId }
        val new = currentList.indexOfFirst { it.id == id }
        activeSongId = id
        if (old >= 0) notifyItemChanged(old)
        if (new >= 0) notifyItemChanged(new)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val binding = ItemSongBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SongViewHolder(binding, parent.context)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        holder.bind(getItem(position), position, activeSongId)
    }

    inner class SongViewHolder(
        private val binding: ItemSongBinding,
        private val ctx: Context
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(song: Song, position: Int, activeId: Long) {
            binding.songTitle.text = song.displayTitle
            binding.songArtist.text = song.displayArtist
            binding.songDuration.text = song.durationFormatted

            // Album art
            Glide.with(ctx)
                .load(song.albumArtUri)
                .placeholder(R.drawable.ic_album_art_placeholder)
                .error(R.drawable.ic_album_art_placeholder)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .centerCrop()
                .into(binding.songArt)

            // Highlight active track
            binding.root.isActivated = (song.id == activeId)
            binding.nowPlayingIndicator.visibility =
                if (song.id == activeId) android.view.View.VISIBLE else android.view.View.GONE

            binding.root.setOnClickListener { onSongClick(position) }
            binding.root.setOnLongClickListener {
                onSongLongClick?.invoke(song)
                true
            }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Song>() {
            override fun areItemsTheSame(a: Song, b: Song) = a.id == b.id
            override fun areContentsTheSame(a: Song, b: Song) = a == b
        }
    }
}
