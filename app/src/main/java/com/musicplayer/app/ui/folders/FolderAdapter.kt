package com.musicplayer.app.ui.folders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.musicplayer.app.databinding.ItemFolderBinding
import com.musicplayer.app.model.Folder

class FolderAdapter(
    private val onClick: (Folder) -> Unit
) : ListAdapter<Folder, FolderAdapter.FolderViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
        val b = ItemFolderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FolderViewHolder(b)
    }

    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FolderViewHolder(private val b: ItemFolderBinding) :
        RecyclerView.ViewHolder(b.root) {
        fun bind(folder: Folder) {
            b.folderName.text = folder.name
            b.folderSongCount.text = "${folder.songCount} songs"
            b.root.setOnClickListener { onClick(folder) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Folder>() {
            override fun areItemsTheSame(a: Folder, b: Folder) = a.path == b.path
            override fun areContentsTheSame(a: Folder, b: Folder) = a == b
        }
    }
}
