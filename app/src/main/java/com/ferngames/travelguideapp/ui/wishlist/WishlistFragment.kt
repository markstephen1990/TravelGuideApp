package com.ferngames.travelguideapp.ui.wishlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ferngames.travelguideapp.DestinationAdapter
import com.ferngames.travelguideapp.R
import com.ferngames.travelguideapp.data.local.TravelDatabase
import com.ferngames.travelguideapp.data.model.Destination
import kotlinx.coroutines.launch

class WishlistFragment : Fragment() {

    private lateinit var adapter: DestinationAdapter
    private lateinit var database: TravelDatabase

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_wishlist, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        database = TravelDatabase.getDatabase(requireContext())

        val rvWishlist = view.findViewById<RecyclerView>(R.id.rvWishlist)
        val emptyState = view.findViewById<LinearLayout>(R.id.emptyState)

        adapter = DestinationAdapter(
            emptyList(),
            onItemClick = { destination ->
                val action = WishlistFragmentDirections
                    .actionWishlistToDetail(destination.id)
                findNavController().navigate(action)
            },
            onWishlistClick = { destination ->
                toggleWishlist(destination)
            }
        )
        rvWishlist.layoutManager = LinearLayoutManager(requireContext())
        rvWishlist.adapter = adapter

        // Observe wishlisted destinations
        database.destinationDao().getWishlistedDestinations()
            .observe(viewLifecycleOwner) { destinations ->
                adapter.updateList(destinations)
                if (destinations.isEmpty()) {
                    emptyState.visibility = View.VISIBLE
                    rvWishlist.visibility = View.GONE
                } else {
                    emptyState.visibility = View.GONE
                    rvWishlist.visibility = View.VISIBLE
                }
            }
    }

    private fun toggleWishlist(destination: Destination) {
        val newStatus = !destination.isWishlisted
        viewLifecycleOwner.lifecycleScope.launch {
            database.destinationDao()
                .updateWishlistStatus(destination.id, newStatus)
        }
    }
}