package LasTrack.duxtami.app.ui.places

import LasTrack.duxtami.app.core.model.PlaceCategory
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.EvStation
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.Hiking
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalMall
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.LocalPizza
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Museum
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Theaters
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector

data class PlaceIconItem(val key: String, val label: String, val icon: ImageVector)

/**
 * Modern Material 3 Expressive icon set for saved places and locations.
 */
object PlaceIcons {
    val allIcons = listOf(
        PlaceIconItem("pin", "Pin", Icons.Filled.Place),
        PlaceIconItem("star", "Favorite", Icons.Filled.Star),
        PlaceIconItem("heart", "Love", Icons.Filled.Favorite),
        PlaceIconItem("bookmark", "Saved", Icons.Filled.Bookmark),
        PlaceIconItem("flag", "Flag", Icons.Filled.Flag),
        PlaceIconItem("explore", "Explore", Icons.Filled.Explore),
        PlaceIconItem("near_me", "Compass", Icons.Filled.NearMe),

        // Transit & Vehicles
        PlaceIconItem("bike", "Bicycle", Icons.Filled.DirectionsBike),
        PlaceIconItem("motorcycle", "Motorcycle", Icons.Filled.TwoWheeler),
        PlaceIconItem("car", "Car", Icons.Filled.DirectionsCar),
        PlaceIconItem("parking", "Parking", Icons.Filled.LocalParking),
        PlaceIconItem("gas", "Gas Station", Icons.Filled.LocalGasStation),
        PlaceIconItem("ev", "EV Station", Icons.Filled.EvStation),
        PlaceIconItem("bus", "Bus", Icons.Filled.DirectionsBus),
        PlaceIconItem("train", "Train", Icons.Filled.Train),
        PlaceIconItem("boat", "Boat", Icons.Filled.DirectionsBoat),
        PlaceIconItem("walk", "Walk", Icons.Filled.DirectionsWalk),
        PlaceIconItem("hiking", "Hiking", Icons.Filled.Hiking),

        // Places & Living
        PlaceIconItem("home", "Home", Icons.Filled.Home),
        PlaceIconItem("work", "Work", Icons.Filled.Work),
        PlaceIconItem("apartment", "Apartment", Icons.Filled.Apartment),
        PlaceIconItem("hotel", "Hotel", Icons.Filled.Hotel),
        PlaceIconItem("school", "School", Icons.Filled.School),
        PlaceIconItem("bank", "Bank", Icons.Filled.AccountBalance),
        PlaceIconItem("atm", "ATM", Icons.Filled.LocalAtm),

        // Food & Drink
        PlaceIconItem("restaurant", "Restaurant", Icons.Filled.Restaurant),
        PlaceIconItem("cafe", "Cafe", Icons.Filled.LocalCafe),
        PlaceIconItem("fastfood", "Fast Food", Icons.Filled.Fastfood),
        PlaceIconItem("pizza", "Pizza", Icons.Filled.LocalPizza),
        PlaceIconItem("bar", "Bar", Icons.Filled.LocalBar),
        PlaceIconItem("bakery", "Bakery", Icons.Filled.BakeryDining),

        // Nature & Recreation
        PlaceIconItem("park", "Park", Icons.Filled.Park),
        PlaceIconItem("forest", "Forest", Icons.Filled.Forest),
        PlaceIconItem("mountain", "Mountain", Icons.Filled.Terrain),
        PlaceIconItem("beach", "Beach", Icons.Filled.BeachAccess),
        PlaceIconItem("camera", "Scenic Photo", Icons.Filled.PhotoCamera),
        PlaceIconItem("gym", "Gym & Fitness", Icons.Filled.FitnessCenter),
        PlaceIconItem("sports", "Sports", Icons.Filled.SportsSoccer),
        PlaceIconItem("cinema", "Cinema", Icons.Filled.Theaters),
        PlaceIconItem("museum", "Museum", Icons.Filled.Museum),

        // Commerce
        PlaceIconItem("shop", "Shopping", Icons.Filled.ShoppingBag),
        PlaceIconItem("store", "Store", Icons.Filled.Storefront),
        PlaceIconItem("mall", "Mall", Icons.Filled.LocalMall),

        // Health & Emergency
        PlaceIconItem("hospital", "Hospital", Icons.Filled.LocalHospital),
        PlaceIconItem("pharmacy", "Pharmacy", Icons.Filled.LocalPharmacy),
        PlaceIconItem("medical", "Medical", Icons.Filled.MedicalServices),
    )

    private val byKey = allIcons.associateBy { it.key }

    fun resolve(iconKey: String?, category: PlaceCategory): ImageVector {
        if (!iconKey.isNullOrBlank()) {
            byKey[iconKey]?.let { return it.icon }
        }
        return when (category) {
            PlaceCategory.PARKING -> Icons.Filled.DirectionsBike
            PlaceCategory.FAVORITE -> Icons.Filled.Star
            PlaceCategory.WORK -> Icons.Filled.Work
            PlaceCategory.HOME -> Icons.Filled.Home
            PlaceCategory.SCENIC -> Icons.Filled.Park
            PlaceCategory.FOOD -> Icons.Filled.Restaurant
            PlaceCategory.FUEL -> Icons.Filled.LocalGasStation
            PlaceCategory.CAMPING -> Icons.Filled.Forest
            PlaceCategory.CUSTOM -> Icons.Filled.Place
        }
    }
}
