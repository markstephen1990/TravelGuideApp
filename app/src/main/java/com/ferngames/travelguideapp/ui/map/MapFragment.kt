package com.ferngames.travelguideapp.ui.map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.ferngames.travelguideapp.R
import com.ferngames.travelguideapp.data.local.TravelDatabase
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.launch

class MapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private lateinit var database: TravelDatabase

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        database = TravelDatabase.getDatabase(requireContext())

        // Initialize Google Map
        val mapFragment = childFragmentManager
            .findFragmentById(R.id.mapView) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // Style the map
        googleMap.uiSettings.isZoomControlsEnabled = true
        googleMap.uiSettings.isCompassEnabled = true

        // Load destinations and add markers
        database.destinationDao().getAllDestinations()
            .observe(viewLifecycleOwner) { destinations ->
                googleMap.clear()
                destinations.forEach { destination ->
                    val position = LatLng(destination.latitude, destination.longitude)
                    val markerColor = when (destination.category) {
                        "Beach" -> BitmapDescriptorFactory.HUE_AZURE
                        "City" -> BitmapDescriptorFactory.HUE_VIOLET
                        "Adventure" -> BitmapDescriptorFactory.HUE_GREEN
                        else -> BitmapDescriptorFactory.HUE_RED
                    }
                    googleMap.addMarker(
                        MarkerOptions()
                            .position(position)
                            .title(destination.name)
                            .snippet("${destination.country} ⭐ ${destination.rating}")
                            .icon(BitmapDescriptorFactory.defaultMarker(markerColor))
                    )
                }

                // Move camera to world view
                googleMap.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(LatLng(20.0, 0.0), 2f)
                )
            }

        // Marker click listener
        googleMap.setOnMarkerClickListener { marker ->
            marker.showInfoWindow()
            true
        }
    }
}