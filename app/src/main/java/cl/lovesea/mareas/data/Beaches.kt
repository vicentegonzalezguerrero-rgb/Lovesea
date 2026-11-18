package cl.lovesea.mareas.data

/**
 * Playas de la V región, de norte a sur.
 *
 * [seaBearing] es hacia dónde mira la playa (grados, 0 = norte, 270 = oeste).
 * Sirve para saber si el viento empuja mar adentro (viento terral).
 * Coordenadas aproximadas, suficientes para la grilla de los modelos (~5 km).
 * Todas usan Valparaíso como puerto de referencia de la marea; [offsetMinutes]
 * permite corregir la hora para puertos secundarios.
 */
data class Beach(
    val id: String,
    val name: String,
    val comuna: String,
    val lat: Double,
    val lon: Double,
    val seaBearing: Int = 270,
    val offsetMinutes: Int = 0,
)

object Beaches {
    val all: List<Beach> = listOf(
        Beach("los_molles", "Los Molles", "La Ligua", -32.240, -71.515, 270),
        Beach("pichicuy", "Pichicuy", "La Ligua", -32.340, -71.455, 300),
        Beach("papudo", "Playa Grande de Papudo", "Papudo", -32.507, -71.447, 300),
        Beach("zapallar", "Zapallar", "Zapallar", -32.553, -71.462, 300),
        Beach("cachagua", "Cachagua", "Zapallar", -32.583, -71.452, 270),
        Beach("la_laguna", "La Laguna", "Zapallar", -32.605, -71.445, 270),
        Beach("maitencillo", "Maitencillo", "Puchuncaví", -32.650, -71.440, 270),
        Beach("horcon", "Horcón", "Puchuncaví", -32.710, -71.490, 315),
        Beach("ventanas", "Ventanas", "Puchuncaví", -32.742, -71.488, 300),
        Beach("loncura", "Loncura", "Quintero", -32.770, -71.505, 315),
        Beach("el_durazno", "El Durazno", "Quintero", -32.783, -71.530, 0),
        Beach("ritoque", "Ritoque", "Quintero", -32.840, -71.525, 270),
        Beach("las_conchitas", "Las Conchitas", "Concón", -32.927, -71.536, 300),
        Beach("la_boca", "La Boca", "Concón", -32.917, -71.512, 315),
        Beach("playa_amarilla", "Playa Amarilla", "Concón", -32.925, -71.530, 315),
        Beach("playa_negra", "Playa Negra", "Concón", -32.931, -71.540, 300),
        Beach("higuerillas", "Higuerillas", "Concón", -32.936, -71.546, 270),
        Beach("cochoa", "Cochoa", "Viña del Mar", -32.955, -71.550, 270),
        Beach("renaca", "Reñaca", "Viña del Mar", -32.970, -71.545, 270),
        Beach("las_salinas", "Las Salinas", "Viña del Mar", -32.990, -71.551, 290),
        Beach("los_marineros", "Los Marineros", "Viña del Mar", -33.003, -71.552, 300),
        Beach("acapulco", "Acapulco", "Viña del Mar", -33.008, -71.553, 315),
        Beach("el_sol", "El Sol", "Viña del Mar", -33.012, -71.557, 315),
        Beach("caleta_abarca", "Caleta Abarca", "Viña del Mar", -33.018, -71.567, 0),
        Beach("las_torpederas", "Las Torpederas", "Valparaíso", -33.024, -71.636, 0),
        Beach("san_mateo", "San Mateo", "Valparaíso", -33.030, -71.644, 315),
        Beach("laguna_verde", "Laguna Verde", "Valparaíso", -33.105, -71.670, 270),
        Beach("quintay", "Quintay", "Casablanca", -33.192, -71.698, 315),
        Beach("tunquen", "Tunquén", "Casablanca", -33.270, -71.660, 270),
        Beach("algarrobo_grande", "Playa Grande de Algarrobo", "Algarrobo", -33.360, -71.663, 0),
        Beach("las_cadenas", "Las Cadenas", "Algarrobo", -33.365, -71.668, 0),
        Beach("san_pedro", "San Pedro", "Algarrobo", -33.370, -71.660, 315),
        Beach("el_canelo", "El Canelo", "Algarrobo", -33.378, -71.682, 300),
        Beach("mirasol", "Mirasol", "Algarrobo", -33.385, -71.650, 270),
        Beach("el_quisco", "El Quisco", "El Quisco", -33.400, -71.697, 315),
        Beach("punta_de_tralca", "Punta de Tralca", "El Quisco", -33.428, -71.705, 270),
        Beach("isla_negra", "Isla Negra", "El Quisco", -33.442, -71.688, 270),
        Beach("el_tabo", "El Tabo", "El Tabo", -33.455, -71.672, 270),
        Beach("las_cruces", "Las Cruces", "El Tabo", -33.502, -71.630, 270),
        Beach("costa_azul", "Costa Azul", "Cartagena", -33.528, -71.618, 270),
        Beach("cartagena_chica", "Playa Chica de Cartagena", "Cartagena", -33.545, -71.610, 300),
        Beach("cartagena_grande", "Playa Grande de Cartagena", "Cartagena", -33.552, -71.607, 270),
        Beach("llolleo", "Llolleo", "San Antonio", -33.620, -71.625, 270),
        Beach("santo_domingo", "Santo Domingo", "Santo Domingo", -33.640, -71.650, 270),
    )

    fun byId(id: String): Beach? = all.firstOrNull { it.id == id }
}
