package cl.lovesea.mareas.data

/**
 * Playas de la V región, de norte a sur.
 * Todas usan Valparaíso como puerto de referencia. [offsetMinutes] permite
 * corregir la hora para puertos secundarios cuando tengamos las diferencias del SHOA.
 */
data class Beach(
    val id: String,
    val name: String,
    val comuna: String,
    val offsetMinutes: Int = 0,
)

object Beaches {
    val all: List<Beach> = listOf(
        Beach("los_molles", "Los Molles", "La Ligua"),
        Beach("pichicuy", "Pichicuy", "La Ligua"),
        Beach("papudo", "Playa Grande de Papudo", "Papudo"),
        Beach("zapallar", "Zapallar", "Zapallar"),
        Beach("cachagua", "Cachagua", "Zapallar"),
        Beach("la_laguna", "La Laguna", "Zapallar"),
        Beach("maitencillo", "Maitencillo", "Puchuncaví"),
        Beach("horcon", "Horcón", "Puchuncaví"),
        Beach("ventanas", "Ventanas", "Puchuncaví"),
        Beach("loncura", "Loncura", "Quintero"),
        Beach("el_durazno", "El Durazno", "Quintero"),
        Beach("ritoque", "Ritoque", "Quintero"),
        Beach("las_conchitas", "Las Conchitas", "Concón"),
        Beach("la_boca", "La Boca", "Concón"),
        Beach("playa_amarilla", "Playa Amarilla", "Concón"),
        Beach("playa_negra", "Playa Negra", "Concón"),
        Beach("higuerillas", "Higuerillas", "Concón"),
        Beach("cochoa", "Cochoa", "Viña del Mar"),
        Beach("renaca", "Reñaca", "Viña del Mar"),
        Beach("las_salinas", "Las Salinas", "Viña del Mar"),
        Beach("acapulco", "Acapulco", "Viña del Mar"),
        Beach("el_sol", "El Sol", "Viña del Mar"),
        Beach("caleta_abarca", "Caleta Abarca", "Viña del Mar"),
        Beach("los_marineros", "Los Marineros", "Viña del Mar"),
        Beach("las_torpederas", "Las Torpederas", "Valparaíso"),
        Beach("san_mateo", "San Mateo", "Valparaíso"),
        Beach("laguna_verde", "Laguna Verde", "Valparaíso"),
        Beach("quintay", "Quintay", "Casablanca"),
        Beach("tunquen", "Tunquén", "Casablanca"),
        Beach("algarrobo_grande", "Playa Grande de Algarrobo", "Algarrobo"),
        Beach("las_cadenas", "Las Cadenas", "Algarrobo"),
        Beach("san_pedro", "San Pedro", "Algarrobo"),
        Beach("el_canelo", "El Canelo", "Algarrobo"),
        Beach("mirasol", "Mirasol", "Algarrobo"),
        Beach("el_quisco", "El Quisco", "El Quisco"),
        Beach("punta_de_tralca", "Punta de Tralca", "El Quisco"),
        Beach("isla_negra", "Isla Negra", "El Quisco"),
        Beach("el_tabo", "El Tabo", "El Tabo"),
        Beach("las_cruces", "Las Cruces", "El Tabo"),
        Beach("cartagena_grande", "Playa Grande de Cartagena", "Cartagena"),
        Beach("cartagena_chica", "Playa Chica de Cartagena", "Cartagena"),
        Beach("costa_azul", "Costa Azul", "Cartagena"),
        Beach("llolleo", "Llolleo", "San Antonio"),
        Beach("santo_domingo", "Santo Domingo", "Santo Domingo"),
    )

    fun byId(id: String): Beach? = all.firstOrNull { it.id == id }
}
