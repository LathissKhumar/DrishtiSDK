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

class GeometryTest {

    @Test
    fun pointCreation() {
        val point = Point(3.0f, 4.0f)
        assertEquals(3.0f, point.x)
        assertEquals(4.0f, point.y)
    }

    @Test
    fun pointEquality() {
        val p1 = Point(1.0f, 2.0f)
        val p2 = Point(1.0f, 2.0f)
        assertEquals(p1, p2)
    }

    @Test
    fun boundingBoxCreation() {
        val box = BoundingBox(10f, 20f, 100f, 200f)
        assertEquals(10f, box.x)
        assertEquals(20f, box.y)
        assertEquals(100f, box.width)
        assertEquals(200f, box.height)
    }

    @Test
    fun axesDefaults() {
        val axes = Axes()
        assertEquals("", axes.x.label)
        assertEquals("", axes.y.label)
    }

    @Test
    fun axesCreation() {
        val axes = Axes(
            x = Axis(label = "Time", range = 0f..24f),
            y = Axis(label = "Temperature", range = -10f..40f)
        )
        assertEquals("Time", axes.x.label)
        assertEquals("Temperature", axes.y.label)
    }

    @Test
    fun dataPointCreation() {
        val point = DataPoint(5.0f, 10.0f, "Point A")
        assertEquals(5.0f, point.x)
        assertEquals(10.0f, point.y)
        assertEquals("Point A", point.label)
    }

    @Test
    fun dataPointDefaults() {
        val point = DataPoint(1.0f, 2.0f)
        assertNull(point.label)
    }

    @Test
    fun trendLineCreation() {
        val start = Point(0f, 0f)
        val end = Point(10f, 10f)
        val line = TrendLine(start, end, "y = x")
        assertEquals(start, line.start)
        assertEquals(end, line.end)
        assertEquals("y = x", line.equation)
    }

    @Test
    fun trendLineDefaults() {
        val line = TrendLine(Point(0f, 0f), Point(1f, 1f))
        assertNull(line.equation)
    }

    @Test
    fun geometryCreation() {
        val points = listOf(Point(0f, 0f), Point(1f, 1f))
        val box = BoundingBox(0f, 0f, 1f, 1f)
        val geo = Geometry(points = points, boundingBox = box)
        assertEquals(2, geo.points.size)
        assertNotNull(geo.boundingBox)
    }

    @Test
    fun geometryDefaults() {
        val geo = Geometry()
        assertTrue(geo.points.isEmpty())
        assertNull(geo.boundingBox)
    }
}
