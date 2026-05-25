package com.ferngames.travelguideapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ferngames.travelguideapp.data.model.Destination

class DestinationAdapter(
    private var destinations: List<Destination>,
    private val onItemClick: (Destination) -> Unit,
    private val onWishlistClick: (Destination) -> Unit
) : RecyclerView.Adapter<DestinationAdapter.DestinationViewHolder>() {

    inner class DestinationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivDestination: ImageView = itemView.findViewById(R.id.ivDestination)
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val tvCountry: TextView = itemView.findViewById(R.id.tvCountry)
        val tvRating: TextView = itemView.findViewById(R.id.tvRating)
        val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        val btnWishlist: ImageButton = itemView.findViewById(R.id.btnWishlist)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DestinationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_destination, parent, false)
        return DestinationViewHolder(view)
    }

    override fun onBindViewHolder(holder: DestinationViewHolder, position: Int) {
        val destination = destinations[position]

        holder.tvName.text = destination.name
        holder.tvCountry.text = "📍 ${destination.country}"
        holder.tvRating.text = "⭐ ${destination.rating}"
        holder.tvCategory.text = destination.category
        holder.tvDescription.text = destination.description

        // Load image
        Glide.with(holder.itemView.context)
            .load(destination.imageUrl)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .into(holder.ivDestination)

        // Wishlist icon
        holder.btnWishlist.setImageResource(
            if (destination.isWishlisted) android.R.drawable.btn_star_big_on
            else android.R.drawable.btn_star_big_off
        )

        // Click listeners
        holder.itemView.setOnClickListener { onItemClick(destination) }
        holder.btnWishlist.setOnClickListener { onWishlistClick(destination) }
    }

    override fun getItemCount() = destinations.size

    fun updateList(newList: List<Destination>) {
        destinations = newList
        notifyDataSetChanged()
    }
}