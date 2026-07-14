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

package io.drishti.core

import kotlinx.serialization.Serializable

/**
 * Types of content that can be detected.
 *
 * Extensible sealed class: built-in types cover the standard STEM
 * content categories, while [Custom] lets plugins define their own.
 */
@Serializable
public sealed class ContentType {

    @Serializable public data object Graph : ContentType()
    @Serializable public data object Formula : ContentType()
    @Serializable public data object Molecule : ContentType()
    @Serializable public data object Shape : ContentType()
    @Serializable public data object Table : ContentType()
    @Serializable public data object Text : ContentType()
    @Serializable public data class Custom(val typeName: String) : ContentType()

    /** Human-readable name for this content type. */
    public val name: String get() = when (this) {
        is Graph -> "GRAPH"
        is Formula -> "FORMULA"
        is Molecule -> "MOLECULE"
        is Shape -> "SHAPE"
        is Table -> "TABLE"
        is Text -> "TEXT"
        is Custom -> typeName
    }
}
