package com.musicplayer.app.ui.playlists

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.musicplayer.app.databinding.ItemPlaylistBinding
import com.musicplayer.app.db.PlaylistEntity

class AddToPlaylistAdapter(
    private val onClick: (PlaylistEntity) -> Unit
) : ListAdapter<PlaylistEntity, AddToPlaylistAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemPlaylistBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VH(private val b: ItemPlaylistBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(playlist: PlaylistEntity) {
            b.tvPlaylistName.text = playlist.name
            b.tvSongCount.text = ""
            b.root.setOnClickListener { onClick(playlist) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<PlaylistEntity>() {
            override fun areItemsTheSame(a: PlaylistEntity, b: PlaylistEntity) = a.id == b.id
            override fun areContentsTheSame(a: PlaylistEntity, b: PlaylistEntity) = a == b
        }
    }
}
