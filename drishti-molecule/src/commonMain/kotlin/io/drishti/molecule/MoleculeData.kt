/*
 * Copyright 2026 DrishtiSTEM
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.drishti.molecule

import io.drishti.core.*

/**
 * Rich molecule data from PubChem API, extending beyond [MoleculeContent].
 *
 * Contains full chemical information including molecular properties,
 * IUPAC naming, SMILES representation, and 3D atomic coordinates.
 *
 * @property cid PubChem Compound ID
 * @property molecularFormula Chemical formula (e.g., "C2H6O")
 * @property molecularWeight Molar mass in g/mol
 * @property iupacName Systematic IUPAC name
 * @property canonicalSmiles Canonical SMILES string
 * @property inchiKey InChI key identifier
 * @property atoms List of atoms with 3D coordinates from conformer data
 * @property bonds List of bonds between atoms
 * @property moleculeType Classification of the molecule
 * @property name Common or IUPAC name for display
 */
public data class MoleculeData(
    val cid: Int = 0,
    val molecularFormula: String = "",
    val molecularWeight: Double = 0.0,
    val iupacName: String = "",
    val canonicalSmiles: String = "",
    val inchiKey: String = "",
    val atoms: List<Atom> = emptyList(),
    val bonds: List<Bond> = emptyList(),
    val moleculeType: MoleculeType = MoleculeType.SIMPLE,
    val name: String = ""
) {
    /**
     * Convert to [MoleculeContent] for compatibility with the core API.
     */
    public fun toMoleculeContent(): MoleculeContent = MoleculeContent(
        moleculeType = moleculeType,
        atoms = atoms,
        bonds = bonds,
        name = name.ifEmpty { iupacName },
        geometry = if (atoms.isNotEmpty()) {
            Geometry(
                points = atoms.map { it.position },
                boundingBox = computeBoundingBox()
            )
        } else null,
        cid = cid,
        molecularFormula = molecularFormula,
        molecularWeight = molecularWeight,
        iupacName = iupacName,
        canonicalSmiles = canonicalSmiles,
        inchiKey = inchiKey,
        confidence = 1.0f
    )

    private fun computeBoundingBox(): BoundingBox? {
        if (atoms.isEmpty()) return null
        val minX = atoms.minOf { it.position.x }
        val minY = atoms.minOf { it.position.y }
        val maxX = atoms.maxOf { it.position.x }
        val maxY = atoms.maxOf { it.position.y }
        return BoundingBox(
            x = minX,
            y = minY,
            width = maxX - minX,
            height = maxY - minY
        )
    }
}
