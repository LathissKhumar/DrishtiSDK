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

import kotlin.test.*

class DrishtiTest {

    @Test
    fun builderWithNoPluginsThrows() {
        assertFailsWith<IllegalArgumentException> {
            Drishti.Builder().build()
        }
    }

    @Test
    fun builderWithDetectorAndRenderer() {
        val drishti = Drishti.Builder()
            .addDetector(StubDetector(ContentType.GRAPH))
            .addRenderer(StubHapticsRenderer())
            .build()
        assertNotNull(drishti)
    }

    @Test
    fun readAsyncReturnsDiagram() = kotlinx.coroutines.test.runTest {
        val drishti = Drishti.Builder()
            .addDetector(StubDetector(ContentType.GRAPH, TestFixtures.graphContent()))
            .addRenderer(StubHapticsRenderer())
            .build()
        val frame = TestFixtures.frame()
        val diagram = drishti.readAsync(frame)
        assertNotNull(diagram)
        assertEquals(1, diagram.contentItems.size)
    }

    @Test
    fun readAsyncWithNoMatchingDetectors() = kotlinx.coroutines.test.runTest {
        val drishti = Drishti.Builder()
            .addDetector(StubDetector(ContentType.FORMULA))
            .build()
        val frame = TestFixtures.frame()
        val diagram = drishti.readAsync(frame)
        assertNotNull(diagram)
        assertTrue(diagram.contentItems.isEmpty())
    }
}
