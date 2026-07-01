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

package io.drishti.formula

/**
 * Bridge to the mXparser math evaluation engine.
 *
 * Uses expect/actual to bridge between common Kotlin code and the
 * Java-based mXparser library.
 */
public expect object MxparserBridge {
    /**
     * Evaluate a math expression string with optional variable bindings.
     *
     * @param expression mXparser-compatible expression (e.g. "(2)+(3)")
     * @param variables Variable name → value bindings
     * @return Numeric result, or NaN if evaluation fails
     */
    public fun evaluate(expression: String, variables: Map<String, Double>): Double
}
