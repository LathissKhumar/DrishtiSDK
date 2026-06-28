package io.drishti.molecule

import io.drishti.core.*
import kotlinx.serialization.json.*

/**
 * PubChem PUG REST API response models and parsers.
 *
 * Uses kotlinx.serialization.json for robust JSON parsing that handles
 * escaped strings, nested structures, and scientific notation correctly.
 */

/**
 * Parsed PubChem compound data used internally by the client.
 */
internal data class PubChemCompoundData(
    val cid: Int = 0,
    val molecularFormula: String = "",
    val molecularWeight: Double = 0.0,
    val iupacName: String = "",
    val canonicalSmiles: String = "",
    val inchiKey: String = "",
    val atoms: List<PubChemAtomData> = emptyList(),
    val bonds: List<PubChemBondData> = emptyList()
)

internal data class PubChemAtomData(
    val id: Int,
    val element: Int,
    val x: Double,
    val y: Double,
    val z: Double
)

internal data class PubChemBondData(
    val from: Int,
    val to: Int,
    val order: Int
)

/**
 * Parse PubChem JSON responses using kotlinx.serialization.json primitives.
 */
internal object PubChemResponseParser {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Parse CID list from compound lookup response.
     *
     * Expected format: `{"IdentifierList":{"CID":[1983]}}`
     */
    fun parseCidResponse(jsonString: String): Int? {
        return try {
            val root = json.parseToJsonElement(jsonString).jsonObject
            root["IdentifierList"]
                ?.jsonObject?.get("CID")
                ?.jsonArray?.firstOrNull()?.jsonPrimitive?.intOrNull
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Parse property table from compound properties response.
     *
     * Expected format: `{"PropertyTable":{"Properties":[{...}]}}`
     */
    fun parsePropertiesResponse(jsonString: String): PubChemCompoundData? {
        return try {
            val root = json.parseToJsonElement(jsonString).jsonObject
            val props = root["PropertyTable"]?.jsonObject?.get("Properties")?.jsonArray
            if (props.isNullOrEmpty()) return null
            val first = props[0].jsonObject
            PubChemCompoundData(
                cid = first["CID"]?.jsonPrimitive?.intOrNull ?: 0,
                molecularFormula = first["MolecularFormula"]?.jsonPrimitive?.content ?: "",
                molecularWeight = first["MolecularWeight"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                iupacName = first["IUPACName"]?.jsonPrimitive?.content ?: "",
                canonicalSmiles = first["CanonicalSMILES"]?.jsonPrimitive?.content ?: "",
                inchiKey = first["InChIKey"]?.jsonPrimitive?.content ?: ""
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Parse 3D conformer response.
     *
     * Expected format: `{"PC_Compounds":[{"atoms":{...},"bonds":{...}}]}`
     */
    fun parseConformerResponse(jsonString: String): Pair<List<PubChemAtomData>, List<PubChemBondData>> {
        return try {
            val root = json.parseToJsonElement(jsonString).jsonObject
            val compound = root["PC_Compounds"]?.jsonArray?.firstOrNull()?.jsonObject
            if (compound == null) return emptyList<PubChemAtomData>() to emptyList()

            val atomsJson = compound["atoms"]?.jsonObject
            val bondsJson = compound["bonds"]?.jsonObject

            val atoms = parseAtoms(atomsJson)
            val bonds = parseBonds(bondsJson)

            atoms to bonds
        } catch (e: Exception) {
            emptyList<PubChemAtomData>() to emptyList()
        }
    }

    private fun parseAtoms(atoms: JsonObject?): List<PubChemAtomData> {
        if (atoms == null) return emptyList()
        return try {
            val aid = atoms["aid"]?.jsonArray?.map { it.jsonPrimitive.int } ?: return emptyList()
            val element = atoms["element"]?.jsonArray?.map { it.jsonPrimitive.int } ?: emptyList()
            val coordsArray = atoms["coords"]?.jsonArray?.firstOrNull()?.jsonObject
            val xCoords = coordsArray?.get("x")?.jsonArray?.map { it.jsonPrimitive.double } ?: emptyList()
            val yCoords = coordsArray?.get("y")?.jsonArray?.map { it.jsonPrimitive.double } ?: emptyList()
            val zCoords = coordsArray?.get("z")?.jsonArray?.map { it.jsonPrimitive.double } ?: emptyList()

            aid.mapIndexed { i, id ->
                PubChemAtomData(
                    id = id,
                    element = element.getOrElse(i) { 0 },
                    x = xCoords.getOrElse(i) { 0.0 },
                    y = yCoords.getOrElse(i) { 0.0 },
                    z = zCoords.getOrElse(i) { 0.0 }
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseBonds(bonds: JsonObject?): List<PubChemBondData> {
        if (bonds == null) return emptyList()
        return try {
            val aid1 = bonds["aid1"]?.jsonArray?.map { it.jsonPrimitive.int } ?: return emptyList()
            val aid2 = bonds["aid2"]?.jsonArray?.map { it.jsonPrimitive.int } ?: emptyList()
            val order = bonds["order"]?.jsonArray?.map { it.jsonPrimitive.int } ?: emptyList()

            aid1.indices.map { i ->
                PubChemBondData(
                    from = aid1[i],
                    to = aid2.getOrElse(i) { 0 },
                    order = order.getOrElse(i) { 1 }
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

/**
 * Maps PubChem element atomic numbers to element symbols.
 */
internal object ElementMapper {
    private val numberToSymbol = mapOf(
        1 to "H", 2 to "He", 3 to "Li", 4 to "Be", 5 to "B", 6 to "C",
        7 to "N", 8 to "O", 9 to "F", 10 to "Ne", 11 to "Na", 12 to "Mg",
        13 to "Al", 14 to "Si", 15 to "P", 16 to "S", 17 to "Cl", 18 to "Ar",
        19 to "K", 20 to "Ca", 21 to "Sc", 22 to "Ti", 23 to "V", 24 to "Cr",
        25 to "Mn", 26 to "Fe", 27 to "Co", 28 to "Ni", 29 to "Cu", 30 to "Zn",
        31 to "Ga", 32 to "Ge", 33 to "As", 34 to "Se", 35 to "Br", 36 to "Kr",
        37 to "Rb", 38 to "Sr", 39 to "Y", 40 to "Zr", 41 to "Nb", 42 to "Mo",
        43 to "Tc", 44 to "Ru", 45 to "Rh", 46 to "Pd", 47 to "Ag", 48 to "Cd",
        49 to "In", 50 to "Sn", 51 to "Sb", 52 to "Te", 53 to "I", 54 to "Xe",
        55 to "Cs", 56 to "Ba", 57 to "La", 58 to "Ce", 59 to "Pr", 60 to "Nd",
        61 to "Pm", 62 to "Sm", 63 to "Eu", 64 to "Gd", 65 to "Tb", 66 to "Dy", 67 to "Ho",
        68 to "Er", 69 to "Tm", 70 to "Yb", 71 to "Lu", 72 to "Hf", 73 to "Ta",
        74 to "W", 75 to "Re", 76 to "Os", 77 to "Ir", 78 to "Pt", 79 to "Au",
        80 to "Hg", 81 to "Tl", 82 to "Pb", 83 to "Bi", 84 to "Po", 85 to "At",
        86 to "Rn", 87 to "Fr", 88 to "Ra", 89 to "Ac", 90 to "Th", 91 to "Pa",
        92 to "U", 93 to "Np", 94 to "Pu", 95 to "Am", 96 to "Cm", 97 to "Bk",
        98 to "Cf", 99 to "Es", 100 to "Fm", 101 to "Md", 102 to "No", 103 to "Lr",
        104 to "Rf", 105 to "Db", 106 to "Sg", 107 to "Bh", 108 to "Hs", 109 to "Mt",
        110 to "Ds", 111 to "Rg", 112 to "Cn", 113 to "Nh", 114 to "Fl", 115 to "Mc",
        116 to "Lv", 117 to "Ts", 118 to "Og"
    )

    fun symbolFor(atomicNumber: Int): String = numberToSymbol[atomicNumber] ?: "X"

    fun bondTypeFor(order: Int): BondType = when (order) {
        1 -> BondType.SINGLE
        2 -> BondType.DOUBLE
        3 -> BondType.TRIPLE
        5 -> BondType.AROMATIC
        else -> BondType.SINGLE
    }
}
