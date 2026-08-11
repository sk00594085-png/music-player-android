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

/**
 * Adapter for song lists.
 * onSongClick      — called when a row is tapped (play the song)
 * onFavouriteClick — called when the heart icon is tapped (toggle favourite); pass null to hide it
 * onSongLongClick  — optional long-press callback
 */
class SongAdapter(
    private val onSongClick: (Song) -> Unit,
    private val onFavouriteClick: ((Song) -> Unit)? = null,
    private val onSongLongClick: ((Song) -> Unit)? = null
) : ListAdapter<Song, SongAdapter.SongViewHolder>(DIFF) {

    private var activeSongId: Long = -1L
    private var favouriteIds: Set<Long> = emptySet()

    fun setActiveSong(id: Long) {
        val old = currentList.indexOfFirst { it.id == activeSongId }
        val new = currentList.indexOfFirst { it.id == id }
        activeSongId = id
        if (old >= 0) notifyItemChanged(old)
        if (new >= 0) notifyItemChanged(new)
    }

    fun setFavouriteIds(ids: Set<Long>) {
        val changed = ids != favouriteIds
        favouriteIds = ids
        if (changed) notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val binding = ItemSongBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SongViewHolder(binding, parent.context)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        holder.bind(getItem(position), activeSongId, favouriteIds)
    }

    inner class SongViewHolder(
        private val binding: ItemSongBinding,
        private val ctx: Context
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(song: Song, activeId: Long, favIds: Set<Long>) {
            binding.songTitle.text = song.displayTitle
            binding.songArtist.text = song.displayArtist
            binding.songDuration.text = song.durationFormatted

            Glide.with(ctx)
                .load(song.albumArtUri)
                .placeholder(R.drawable.ic_album_art_placeholder)
                .error(R.drawable.ic_album_art_placeholder)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .centerCrop()
                .into(binding.songArt)

            binding.root.isActivated = (song.id == activeId)
            binding.nowPlayingIndicator.visibility =
                if (song.id == activeId) android.view.View.VISIBLE else android.view.View.GONE

            // Favourite heart icon
            if (onFavouriteClick != null) {
                binding.btnFavourite.visibility = android.view.View.VISIBLE
                val isFav = song.id in favIds
                binding.btnFavourite.setImageResource(
                    if (isFav) R.drawable.ic_favourite_filled else R.drawable.ic_favourite
                )
                binding.btnFavourite.setOnClickListener { onFavouriteClick.invoke(song) }
            } else {
                binding.btnFavourite.visibility = android.view.View.GONE
            }

            binding.root.setOnClickListener { onSongClick(song) }
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
