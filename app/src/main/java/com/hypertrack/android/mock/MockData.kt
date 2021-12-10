package com.hypertrack.android.mock

import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.api.*
import com.hypertrack.android.interactors.PhotoForUpload
import com.hypertrack.android.interactors.PhotoUploadingState
import com.hypertrack.android.models.*
import com.hypertrack.android.models.local.LocalOrder
import com.hypertrack.android.models.local.LocalTrip
import com.hypertrack.android.models.local.OrderStatus
import com.hypertrack.android.models.local.TripStatus
import com.hypertrack.android.repository.TripsRepositoryImpl
import com.hypertrack.android.ui.common.util.toMap
import com.hypertrack.android.utils.Constants
import com.hypertrack.android.utils.Injector
import com.hypertrack.android.utils.MyApplication
import com.hypertrack.android.utils.formatters.toIso
import com.hypertrack.logistics.android.github.BuildConfig
import com.hypertrack.logistics.android.github.R
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

object MockData {

    private val paloAltoLatLng = LatLng(37.381451, -122.063616)
    val PALO_ALTO_LAT_LNG = paloAltoLatLng

    private val addr = listOf(
        "2875 El Camino Real",
        "567 Melville Ave",
        "1295 Middlefield Rd",
        "630 Seale Ave",
        "1310 Bryant St",
        "475 Homer Ave",
        "1102 Ramona St",
        "117 University Ave",
        "130 Lytton Ave",
    )

    private val MOCK_HISTORY_JSON: String by lazy {
        MyApplication.context.resources.openRawResource(R.raw.mock_history).bufferedReader()
            .use { it.readText() }
    }

    private val MOCK_SUMMARY = Summary(
        totalDistance = 12 * 857,
        totalDuration = 3 * 57 * 58,
        totalDriveDistance = 4 * 997,
        totalDriveDuration = 1 * 59 * 57,
        stepsCount = 3794,
        totalWalkDuration = 2 * 57 * 59,
        totalStopDuration = 20 * 57,
    )

    val MOCK_HISTORY_RESPONSE by lazy {
        Injector.getMoshi().adapter(HistoryResponse::class.java)
            .fromJson(MOCK_HISTORY_JSON)
    }

    val EMPTY_HISTORY = History(
        MOCK_SUMMARY,
        listOf(),
        listOf()
    )

    val MOCK_HISTORY: HistoryResult by lazy {
        Injector.getMoshi().adapter(HistoryResponse::class.java)
            .fromJson(MOCK_HISTORY_JSON)!!.let { h: HistoryResponse ->
                History(
                    MOCK_SUMMARY,
                    h.locations.coordinates.map {
                        Location(
                            latitude = it.latitude,
                            longitude = it.longitude
                        ) to it.timestamp
                    },
                    mutableListOf<Marker>().apply {
                        var ts = 0
//                addAll(listOf())
                        add(createStatusMarker(Status.DRIVE, ++ts * 10))
                        add(createStatusMarker(Status.WALK, ++ts * 10))
                        add(createGeofenceMarker(++ts * 10))
                        add(createStatusMarker(Status.DRIVE, ++ts * 10))
                        add(createStatusMarker(Status.STOP, ++ts * 10))
//                add(createStatusMarker(Status.INACTIVE))
                        add(createGeotagMarker(++ts * 10))
                        add(createStatusMarker(Status.DRIVE, ++ts * 10))
                        add(createStatusMarker(Status.OUTAGE, ++ts * 10))
                    }
                )
            }
    }

    private fun createStatusMarker(status: Status, plus: Int = 0): StatusMarker {
        var ts = plus
        return StatusMarker(
            MarkerType.STATUS,
            createTimestamp(++ts),
            createLocation(),
            createLocation(),
            createLocation(),
            createTimestamp(++ts),
            createTimestamp(++ts),
            createTimestamp(++ts),
            createTimestamp(++ts),
            status,
            (5 * 60 + Math.random() * 2000).toInt(),
            (1000 + Math.random() * 5000).toInt(),
            514,
            addr[plus / 10],
            "location_services_disabled"
        )
    }

    private fun createGeofenceMarker(plus: Int = 0): GeofenceMarker {
        var ts = plus
        return GeofenceMarker(
            MarkerType.GEOFENCE_ENTRY,
            createTimestamp(++ts),
            createLocation(),
            mapOf("name" to "Home"),
            createLocation(),
            createLocation(),
            createTimestamp(++ts),
            createTimestamp(++ts),
        )
    }

    private fun createGeotagMarker(plus: Int = 0): GeoTagMarker {
        return GeoTagMarker(
            MarkerType.GEOTAG,
            createTimestamp(plus),
            Location(0.0, 0.0),
            mapOf("type" to Constants.PICK_UP),
        )
    }

    private fun createTimestamp(plus: Int = 0) =
        ZonedDateTime.now().plusMinutes(plus.toLong() * 27).format(
            DateTimeFormatter.ISO_INSTANT
        )

    private fun createLocation() = Location(
        37.4750,
        -122.1709
    )

    fun createGeofence(
        page: Int? = 0,
        lat: Double = 37.794763,
        lon: Double = 122.395223,
        polygon: Boolean = false,
        metadata: Map<String, Any> = mapOf("name" to page.toString())
    ): Geofence {
        val geometry = if (!polygon) {
            """
                {
                "type":"Point",
                "coordinates": [${lon ?: 37.794763}, ${lat ?: 122.395223}]
            }
            """.let {
                Injector.getMoshi().adapter(Geometry::class.java).fromJson(it)!!
            }
        } else {
            val coords: MutableList<List<Double>> = mutableListOf()
            coords.add(listOf(lon, lat))
            coords.add(listOf(lon + 0.001, lat))
            coords.add(listOf(lon + 0.001, lat + 0.001))
            coords.add(listOf(lon, lat + 0.001))
            Polygon(listOf(coords))
        }

        return Geofence(
            UUID.randomUUID().hashCode().toString(),
            "",
            ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT),
            metadata,
            geometry,
            null,
            (1000 * Math.random()).toInt(),
            "",
            false,
        )
    }

    fun createGeofenceVisit(
        _date: LocalDate? = null,
        deviceId: String = "device_id",
        arrival: ZonedDateTime? = null
    ): GeofenceVisit {
        val mockExitDt = arrival ?: _date?.let {
            ZonedDateTime.now().withMonth(it.monthValue).withDayOfMonth(it.dayOfMonth)
                .minusHours(1)
        } ?: ZonedDateTime.now()

        val mockArrivalDt = arrival ?: mockExitDt.minusHours(1)

        val date = _date ?: LocalDate.now()
        return GeofenceVisit(
            "1",
            "1",
            deviceId,
            Arrival(mockArrivalDt.toIso()),
            Exit(mockExitDt.toIso()),
            Point(1.0, 1.0),
            RouteTo(
                100,
                100,
                100,
            ),
            100,
            null,
            null,
        )
    }

    fun createGraphQlGeofenceVisit(arrival: ZonedDateTime): GraphQlGeofenceVisit {
        return GraphQlGeofenceVisit(
            "1",
            Arrival(arrival.toIso()),
            Exit(arrival.plusHours(1).toIso()),
            RouteTo(
                100,
                100,
                100,
            ),
            GraphQlGeofence(
                "1", "1", "{}", GraphQlPointGeometry(
                    center = listOf(
                        PALO_ALTO_LAT_LNG.longitude, PALO_ALTO_LAT_LNG.latitude
                    ), null
                ), 100
            ),
        )
    }

    fun createTrip(
        id: String? = null,
        orders: List<Order> = listOf(
            createOrder(status = OrderStatus.ONGOING),/*.copy(estimate=null)*/
            createOrder(status = OrderStatus.ONGOING),
            createOrder(status = OrderStatus.SNOOZED),
            createOrder(status = OrderStatus.CANCELED),
            createOrder(status = OrderStatus.COMPLETED),
        )
    ): Trip {
        return Trip(
            id ?: "tripId " + Math.random(),
            null,
            TripStatus.ACTIVE.value,
            ZonedDateTime.now().toIso(),
            mapOf(),
            null,
            Estimate(
                ZonedDateTime.now().toIso(),
                null
            ),
            orders,
        )
    }

    fun createOrder(
        id: String? = null,
        status: OrderStatus = OrderStatus.ONGOING
    ): Order {
        return Order(
            id ?: ("order " + Math.random()),
            TripDestination(paloAltoLatLng, "Test Address"),
            status.value,
            ZonedDateTime.now().toIso(),
            ZonedDateTime.now().toIso(),
            Estimate(ZonedDateTime.now().plusMinutes(29).toIso(), null),
            mapOf(),
        )
    }

    val MOCK_GEOFENCES_JSON by lazy {
        MyApplication.context.resources.openRawResource(R.raw.mock_geofences).bufferedReader()
            .use { it.readText() }
    }

    const val MOCK_INTEGRATIONS_RESPONSE =
        "{\"data\":[{\"name\":null,\"id\":\"4441136372\"},{\"name\":\"100X.VC\",\"id\":\"5231315202\"},{\"name\":\"1Password\",\"id\":\"4950449208\"},{\"name\":\"4MATIV\",\"id\":\"4441162290\"},{\"name\":\"A&B Courier Service Limited\",\"id\":\"4441148258\"},{\"name\":\"A/B Smartly\",\"id\":\"5860436823\"},{\"name\":\"Aaj Tak\",\"id\":\"5225918494\"},{\"name\":\"abatox.hu\",\"id\":\"4441135165\"},{\"name\":\"Able\",\"id\":\"4441135151\"},{\"name\":\"Accenture Limited\",\"id\":\"4441136368\"},{\"name\":\"Acuity\",\"id\":\"4547060422\"},{\"name\":\"ADESA\",\"id\":\"6094023069\"},{\"name\":\"Advantary\",\"id\":\"4647658256\"},{\"name\":\"advengersmedia.com\",\"id\":\"4441123975\"},{\"name\":\"Advent Trinity\",\"id\":\"5224222119\"},{\"name\":\"adyog\",\"id\":\"4441135170\"},{\"name\":\"Afn\",\"id\":\"5894607104\"},{\"name\":\"AfricaSokoni\",\"id\":\"4441136383\"},{\"name\":\"ahuja.email\",\"id\":\"5776265606\"},{\"name\":\"AI Software Company\",\"id\":\"4912961151\"},{\"name\":\"Airbase\",\"id\":\"4493511265\"},{\"name\":\"Airtel\",\"id\":\"5626580327\"},{\"name\":\"Aita\",\"id\":\"4804557609\"},{\"name\":\"ALANTRA\",\"id\":\"5094378652\"},{\"name\":\"Albatrot\",\"id\":\"4874606289\"},{\"name\":\"Alberto Gal\\u00e1n\",\"id\":\"4441135150\"},{\"name\":\"Albertsons\",\"id\":\"5102828712\"},{\"name\":\"Alfred Colombia\",\"id\":\"5008107555\"},{\"name\":\"Algo Scientific\",\"id\":\"4441123973\"},{\"name\":\"Alliance of Chief Executives\",\"id\":\"4620135151\"},{\"name\":\"Allsopp & Allsopp\",\"id\":\"4441162280\"},{\"name\":\"Altus Global Trade Solutions\",\"id\":\"5028460957\"},{\"name\":\"Amazon\",\"id\":\"4568501748\"},{\"name\":\"Amazon\",\"id\":\"4876163965\"},{\"name\":\"Amazon\",\"id\":\"5577668465\"},{\"name\":\"Amazon.com\",\"id\":\"5810269778\"},{\"name\":\"American Society of Furniture Designers\",\"id\":\"6077764411\"},{\"name\":\"Americana Group\",\"id\":\"4535979648\"},{\"name\":\"amplifiercorporation.com\",\"id\":\"5413739190\"},{\"name\":\"AngelList\",\"id\":\"4777997295\"},{\"name\":\"AngelList\",\"id\":\"5713976065\"},{\"name\":\"ANI Technologies\",\"id\":\"5134132501\"},{\"name\":\"Anirit Urban Agrofoods\",\"id\":\"5349470225\"},{\"name\":\"annumtechnologies.co\",\"id\":\"5912370092\"},{\"name\":\"Apax Partners\",\"id\":\"5602819213\"},{\"name\":\"API2Cart\",\"id\":\"5603200832\"},{\"name\":\"API3\",\"id\":\"5200513113\"},{\"name\":\"App Innovation Technologies\",\"id\":\"4441136375\"},{\"name\":\"Appify\",\"id\":\"4676237558\"},{\"name\":\"Apple Inc.\",\"id\":\"4871616142\"},{\"name\":\"Apple Inc.\",\"id\":\"4925000375\"},{\"name\":\"Arch Grants\",\"id\":\"5767142815\"},{\"name\":\"archipel.tech\",\"id\":\"4452595627\"},{\"name\":\"Arrowroot Capital\",\"id\":\"5394439929\"},{\"name\":\"Article Writing\",\"id\":\"5183590425\"},{\"name\":\"Arun Gas\",\"id\":\"5338990157\"},{\"name\":\"asdf\",\"id\":\"6077872762\"},{\"name\":\"Ashika Stock Broking Limited\",\"id\":\"4556268517\"},{\"name\":\"ASSOCHAM\",\"id\":\"4791497603\"},{\"name\":\"Atlantic Cargo Pvt. Ltd.\",\"id\":\"6017237026\"},{\"name\":\"automonkey\",\"id\":\"5209410081\"},{\"name\":\"Autonomic\",\"id\":\"5923524676\"},{\"name\":\"Autosys Industrial Solutions Private Limited\",\"id\":\"5838194705\"},{\"name\":\"Avataar Venture Partners\",\"id\":\"5378716080\"},{\"name\":\"Avendus\",\"id\":\"4640555526\"},{\"name\":\"Axis Bank\",\"id\":\"5310293148\"},{\"name\":\"BajuGali\",\"id\":\"4751763111\"},{\"name\":\"Balabing\",\"id\":\"4441136382\"},{\"name\":\"Bank of Montreal\",\"id\":\"5669629354\"},{\"name\":\"BASIC\",\"id\":\"5458196529\"},{\"name\":\"basig.me\",\"id\":\"4441123976\"},{\"name\":\"bcg\",\"id\":\"4525992500\"},{\"name\":\"Beacons Point, Inc.\",\"id\":\"4683046751\"},{\"name\":\"becfertilizers.in\",\"id\":\"4715736362\"},{\"name\":\"bectron.in\",\"id\":\"5834053024\"},{\"name\":\"Beintoo\",\"id\":\"6092456759\"},{\"name\":\"belolabs.com\",\"id\":\"5757784964\"},{\"name\":\"berkeley college\",\"id\":\"4646156932\"},{\"name\":\"Berkeley Unified School District\",\"id\":\"4527698510\"},{\"name\":\"Berkshire Hathaway GUARD Insurance Companies\",\"id\":\"5269323666\"},{\"name\":\"better.network\",\"id\":\"4441135171\"},{\"name\":\"Beyond Type 1\",\"id\":\"4515823168\"},{\"name\":\"Bhilai Engineering Corporation Limited\",\"id\":\"4715761730\"},{\"name\":\"Bhilai Engineering Corporation Limited\",\"id\":\"4719101830\"},{\"name\":\"bi2ai\",\"id\":\"4886971594\"},{\"name\":\"bigdatamartech\",\"id\":\"4505441949\"},{\"name\":\"Bilanz Capital Advisors\",\"id\":\"5117707488\"},{\"name\":\"billiondevices.com\",\"id\":\"6023782063\"},{\"name\":\"BitCot\",\"id\":\"5338990182\"},{\"name\":\"Blackvt\",\"id\":\"5025996337\"},{\"name\":\"Block Gemini\",\"id\":\"4441138712\"},{\"name\":\"Blowhorn\",\"id\":\"5395978031\"},{\"name\":\"BMSCE\",\"id\":\"4441136371\"},{\"name\":\"Bond\",\"id\":\"4515681734\"},{\"name\":\"Box\",\"id\":\"5838013270\"},{\"name\":\"Brado\",\"id\":\"5150108550\"},{\"name\":\"brego.ai\",\"id\":\"5310279314\"},{\"name\":\"Brex\",\"id\":\"4488376430\"},{\"name\":\"Bridge\",\"id\":\"5388281110\"},{\"name\":\"Brightgrove\",\"id\":\"4640375932\"}],\"after\":\"100\"}"

    val MOCK_TRIPS_JSON: String by lazy {
        MyApplication.context.resources.openRawResource(R.raw.mock_trips).bufferedReader()
            .use { it.readText() }
    }

    val mockTrip: LocalTrip by lazy {
        Injector.getMoshi().adapter(TripResponse::class.java)
            .fromJson(MOCK_TRIPS_JSON)!!.trips.first().let { remoteTrip ->
                LocalTrip(
                    remoteTrip.id!!,
                    TripStatus.fromString(remoteTrip.status),
                    mapOf(),
                    remoteTrip.orders!!.map { order ->
                        LocalOrder(
                            order,
                            isPickedUp = true,
                            note = null,
                            photos = mutableSetOf(),
                            metadata = null
                        )
                    }.toMutableList(),
                    remoteTrip.views
                )
            }
    }


    //todo remove
    val MOCK_TRIP_V1_JSON: String by lazy {
        MyApplication.context.resources.openRawResource(R.raw.mock_trip_v1).bufferedReader()
            .use { it.readText() }
    }

}
