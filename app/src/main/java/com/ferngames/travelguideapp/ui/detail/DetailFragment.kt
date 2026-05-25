package com.ferngames.travelguideapp.ui.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.ferngames.travelguideapp.R
import com.ferngames.travelguideapp.data.local.TravelDatabase
import com.ferngames.travelguideapp.data.model.Destination
import com.ferngames.travelguideapp.data.model.TripPlan
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DetailFragment : Fragment() {

    private val args: DetailFragmentArgs by navArgs()
    private lateinit var database: TravelDatabase
    private var currentDestination: Destination? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        database = TravelDatabase.getDatabase(requireContext())

        // Load destination
        viewLifecycleOwner.lifecycleScope.launch {
            val destination = database.destinationDao()
                .getDestinationById(args.destinationId)
            destination?.let {
                currentDestination = it
                displayDestination(view, it)
            }
        }

        // Back button
        view.findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun displayDestination(view: View, destination: Destination) {
        // Image
        Glide.with(this)
            .load(destination.imageUrl)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .into(view.findViewById<ImageView>(R.id.ivDestinationImage))

        // Text fields
        view.findViewById<TextView>(R.id.tvDestinationName).text = destination.name
        view.findViewById<TextView>(R.id.tvCountry).text = "📍 ${destination.country}"
        view.findViewById<TextView>(R.id.tvRating).text = "⭐ ${destination.rating}"
        view.findViewById<TextView>(R.id.tvCategory).text = destination.category
        view.findViewById<TextView>(R.id.tvBestTime).text = destination.bestTimeToVisit
        view.findViewById<TextView>(R.id.tvCurrency).text = destination.currency
        view.findViewById<TextView>(R.id.tvLanguage).text = destination.language
        view.findViewById<TextView>(R.id.tvDescription).text = destination.description

        // Wishlist button
        val btnWishlist = view.findViewById<ImageButton>(R.id.btnWishlist)
        btnWishlist.setImageResource(
            if (destination.isWishlisted) android.R.drawable.btn_star_big_on
            else android.R.drawable.btn_star_big_off
        )
        btnWishlist.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                val newStatus = !destination.isWishlisted
                database.destinationDao()
                    .updateWishlistStatus(destination.id, newStatus)
                btnWishlist.setImageResource(
                    if (newStatus) android.R.drawable.btn_star_big_on
                    else android.R.drawable.btn_star_big_off
                )
                val msg = if (newStatus) "Added to wishlist! ⭐"
                else "Removed from wishlist"
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            }
        }

        // Add to Planner button
        view.findViewById<Button>(R.id.btnAddToPlanner).setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val today = sdf.format(Date())
                val tripPlan = TripPlan(
                    destinationName = destination.name,
                    startDate = today,
                    endDate = today,
                    notes = "Trip to ${destination.name}, ${destination.country}",
                    budget = 0.0
                )
                database.tripPlanDao().insertTripPlan(tripPlan)
                Toast.makeText(
                    requireContext(),
                    "Added to Trip Planner! 📅",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // Add Journal button
        view.findViewById<Button>(R.id.btnAddJournal).setOnClickListener {
            Toast.makeText(
                requireContext(),
                "Go to Journal tab to write your entry! 📔",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}