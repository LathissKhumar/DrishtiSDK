package io.drishti.vision

import io.drishti.core.Frame
import io.drishti.core.Point
import io.drishti.core.BoundingBox
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Extract features from frames for content detection.
 *
 * Provides basic image-processing feature extraction in pure Kotlin
 * (commonMain). For RGB frames the extractor computes a grayscale
 * intensity map and detects edges via horizontal/vertical gradient
 * analysis. Contours, lines, and text regions are derived from the
 * detected edge points.
 *
 * Concrete Android implementations can replace this with OpenCV-backed
 * processing for higher accuracy and lower latency.
 */
class FeatureExtractor {

    // --- Edge-detection thresholds ---

    /** Minimum gradient magnitude to count as an edge pixel. */
    private val edgeThreshold: Int = 30

    /** Minimum number of collinear edge points to form a line. */
    private val minLinePoints: Int = 15

    /** Pixel gap to bridge when grouping edge points into contours. */
    private val contourGap: Int = 8

    // --- Public API ---

    /**
     * Extract contours from a frame.
     *
     * For RGB frames the method:
     * 1. Converts to grayscale using standard luminance weights.
     * 2. Runs a Sobel-like 3×3 gradient to find edge pixels.
     * 3. Groups connected edge pixels into contour polygons.
     *
     * @return Detected [Contour] objects, each defined by a polygon and area.
     *         Returns an empty list when [Frame.data] is null or too small.
     */
    fun extractContours(frame: Frame): List<Contour> {
        val data = frame.data ?: return emptyList()
        if (data.size < frame.width * frame.height * 3) return emptyList()

        val gray = toGrayscale(data, frame.width, frame.height)
        val edges = detectEdges(gray, frame.width, frame.height)
        return groupContours(edges, frame.width, frame.height)
    }

    /**
     * Extract lines from a frame.
     *
     * Uses a simplified Hough-like approach: edge pixels are binned by
     * angle and accumulated. Peaks above [minLinePoints] are reported
     * as [Line] segments.
     *
     * @return Detected [Line] objects using gradient-based line detection.
     */
    fun extractLines(frame: Frame): List<Line> {
        val data = frame.data ?: return emptyList()
        if (data.size < frame.width * frame.height * 3) return emptyList()

        val gray = toGrayscale(data, frame.width, frame.height)
        val edges = detectEdges(gray, frame.width, frame.height)
        return findLines(edges, frame.width, frame.height)
    }

    /**
     * Extract text regions from a frame.
     *
     * Text regions are identified as dense horizontal clusters of edge
     * points. The method scans rows for high edge density and groups
     * adjacent rows into rectangular [TextRegion] candidates.
     *
     * @return Detected [TextRegion] objects with bounding boxes and
     *         placeholder OCR text (actual OCR requires platform-specific
     *         ML Kit or Tesseract integration).
     */
    fun extractTextRegions(frame: Frame): List<TextRegion> {
        val data = frame.data ?: return emptyList()
        if (data.size < frame.width * frame.height * 3) return emptyList()

        val gray = toGrayscale(data, frame.width, frame.height)
        val edges = detectEdges(gray, frame.width, frame.height)
        return findTextRegions(edges, frame.width, frame.height)
    }

    /**
     * Extract regions of interest.
     *
     * ROIs are high-contrast rectangular regions identified by sliding
     * window analysis of the grayscale image. A region is flagged when
     * its local standard deviation exceeds a threshold relative to the
     * global mean.
     *
     * @return Detected [RegionOfInterest] objects with bounding boxes
     *         and contrast-based confidence scores.
     */
    fun extractROIs(frame: Frame): List<RegionOfInterest> {
        val data = frame.data ?: return emptyList()
        if (data.size < frame.width * frame.height * 3) return emptyList()

        val gray = toGrayscale(data, frame.width, frame.height)
        return findROIs(gray, frame.width, frame.height)
    }

    // --- Internal: grayscale conversion ---

    /**
     * Convert RGB byte data to a grayscale intensity array.
     *
     * Uses standard luminance weights: 0.299R + 0.587G + 0.114B.
     */
    private fun toGrayscale(data: ByteArray, width: Int, height: Int): IntArray {
        val gray = IntArray(width * height)
        val pixelCount = width * height
        for (i in 0 until pixelCount) {
            val offset = i * 3
            if (offset + 2 >= data.size) break
            val r = data[offset].toInt() and 0xFF
            val g = data[offset + 1].toInt() and 0xFF
            val b = data[offset + 2].toInt() and 0xFF
            gray[i] = (0.299f * r + 0.587f * g + 0.114f * b).toInt()
        }
        return gray
    }

    // --- Internal: edge detection ---

    /**
     * Detect edge pixels using a 3×3 Sobel-like gradient.
     *
     * Returns a list of (x, y) coordinates of edge pixels.
     */
    private fun detectEdges(gray: IntArray, width: Int, height: Int): List<Pair<Int, Int>> {
        val edges = mutableListOf<Pair<Int, Int>>()

        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                // Horizontal gradient (Sobel X)
                val gx = -gray[(y - 1) * width + (x - 1)] +
                    gray[(y - 1) * width + (x + 1)] +
                    -2 * gray[y * width + (x - 1)] +
                    2 * gray[y * width + (x + 1)] +
                    -gray[(y + 1) * width + (x - 1)] +
                    gray[(y + 1) * width + (x + 1)]

                // Vertical gradient (Sobel Y)
                val gy = -gray[(y - 1) * width + (x - 1)] +
                    -2 * gray[(y - 1) * width + x] +
                    -gray[(y - 1) * width + (x + 1)] +
                    gray[(y + 1) * width + (x - 1)] +
                    2 * gray[(y + 1) * width + x] +
                    gray[(y + 1) * width + (x + 1)]

                val magnitude = abs(gx) + abs(gy)
                if (magnitude >= edgeThreshold) {
                    edges.add(x to y)
                }
            }
        }
        return edges
    }

    // --- Internal: contour grouping ---

    /**
     * Group edge pixels into contour polygons using flood-fill-like
     * neighbor connectivity within [contourGap] pixels.
     */
    private fun groupContours(
        edges: List<Pair<Int, Int>>,
        width: Int,
        height: Int
    ): List<Contour> {
        if (edges.isEmpty()) return emptyList()

        val visited = BooleanArray(edges.size)
        val contours = mutableListOf<Contour>()

        for (i in edges.indices) {
            if (visited[i]) continue
            val region = mutableListOf<Point>()
            val stack = mutableListOf(i)

            while (stack.isNotEmpty()) {
                val idx = stack.removeAt(stack.lastIndex)
                if (visited[idx]) continue
                visited[idx] = true
                val (ex, ey) = edges[idx]
                region.add(Point(ex.toFloat(), ey.toFloat()))

                // Check neighbors within contourGap
                for (j in edges.indices) {
                    if (visited[j]) continue
                    val (nx, ny) = edges[j]
                    if (abs(nx - ex) <= contourGap && abs(ny - ey) <= contourGap) {
                        stack.add(j)
                    }
                }
            }

            if (region.size >= 3) {
                val area = computePolygonArea(region)
                contours.add(Contour(points = region, area = area))
            }
        }
        return contours
    }

    /**
     * Compute the signed area of a polygon using the shoelace formula.
     */
    private fun computePolygonArea(points: List<Point>): Float {
        var area = 0f
        val n = points.size
        for (i in 0 until n) {
            val j = (i + 1) % n
            area += points[i].x * points[j].y
            area -= points[j].x * points[i].y
        }
        return abs(area) / 2f
    }

    // --- Internal: line detection ---

    /**
     * Find line segments from edge points using angle-binning.
     *
     * Edge points are grouped by their gradient direction into 36 bins
     * (10° each). Bins with enough collinear points produce [Line]
     * segments from the bounding points of the cluster.
     */
    private fun findLines(
        edges: List<Pair<Int, Int>>,
        width: Int,
        height: Int
    ): List<Line> {
        if (edges.size < minLinePoints) return emptyList()

        // Compute gradient directions at each edge point
        val gray = IntArray(width * height) // already available if needed; recompute minimal
        // For simplicity, use position-based angle estimation for edge clusters
        val binCount = 36
        val bins = Array(binCount) { mutableListOf<Pair<Int, Int>>() }

        // Estimate direction by comparing to neighboring edges
        for (i in edges.indices) {
            val (x, y) = edges[i]
            val angle = estimateLocalAngle(edges, i)
            val bin = ((angle + 180f) / 10f).toInt().coerceIn(0, binCount - 1)
            bins[bin].add(x to y)
        }

        val lines = mutableListOf<Line>()
        for (bin in bins) {
            if (bin.size < minLinePoints) continue

            // Find the two most distant points in this bin (line endpoints)
            var maxDist = 0f
            var start = bin[0]
            var end = bin.last()
            for (p in bin.indices) {
                for (q in p + 1 until bin.size) {
                    val dx = bin[p].first - bin[q].first
                    val dy = bin[p].second - bin[q].second
                    val d = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                    if (d > maxDist) {
                        maxDist = d
                        start = bin[p]
                        end = bin[q]
                    }
                }
            }

            val sx = start.first.toFloat()
            val sy = start.second.toFloat()
            val ex = end.first.toFloat()
            val ey = end.second.toFloat()
            val angle = kotlin.math.atan2((ey - sy).toDouble(), (ex - sx).toDouble()).toFloat() *
                180f / kotlin.math.PI.toFloat()
            lines.add(
                Line(
                    start = Point(sx, sy),
                    end = Point(ex, ey),
                    angle = angle
                )
            )
        }
        return lines
    }

    /**
     * Estimate the local gradient direction at an edge point by averaging
     * the direction to its nearest neighbors.
     */
    private fun estimateLocalAngle(edges: List<Pair<Int, Int>>, index: Int): Float {
        val (cx, cy) = edges[index]
        var sumAngle = 0f
        var count = 0
        for (j in edges.indices) {
            if (j == index) continue
            val (nx, ny) = edges[j]
            val dx = nx - cx
            val dy = ny - cy
            val dist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
            if (dist <= contourGap.toFloat() && dist > 0f) {
                sumAngle += kotlin.math.atan2(dy.toDouble(), dx.toDouble()).toFloat() * 180f / kotlin.math.PI.toFloat()
                count++
            }
        }
        return if (count > 0) sumAngle / count else 0f
    }

    // --- Internal: text region detection ---

    /**
     * Find text regions by detecting horizontal strips with high edge density.
     *
     * Text typically produces dense horizontal edge patterns. The algorithm:
     * 1. Computes a row-wise edge density histogram.
     * 2. Finds rows where density exceeds a threshold.
     * 3. Merges adjacent high-density rows into text-line bounding boxes.
     */
    private fun findTextRegions(
        edges: List<Pair<Int, Int>>,
        width: Int,
        height: Int
    ): List<TextRegion> {
        if (edges.isEmpty()) return emptyList()

        // Row-wise edge density
        val rowDensity = IntArray(height)
        for ((_, y) in edges) {
            if (y in 0 until height) rowDensity[y]++
        }

        // Find rows above density threshold
        val avgDensity = rowDensity.average().toFloat()
        val threshold = maxOf(avgDensity * 2f, 3f)
        val textRows = (0 until height).filter { rowDensity[it] >= threshold }

        if (textRows.isEmpty()) return emptyList()

        // Merge consecutive text rows into regions
        val regions = mutableListOf<TextRegion>()
        var regionStart = textRows[0]
        var regionEnd = textRows[0]

        for (i in 1 until textRows.size) {
            if (textRows[i] - textRows[i - 1] <= 3) {
                regionEnd = textRows[i]
            } else {
                regions.add(buildTextRegion(edges, regionStart, regionEnd, width))
                regionStart = textRows[i]
                regionEnd = textRows[i]
            }
        }
        regions.add(buildTextRegion(edges, regionStart, regionEnd, width))

        return regions
    }

    /**
     * Build a [TextRegion] from a horizontal band of edge points.
     */
    private fun buildTextRegion(
        edges: List<Pair<Int, Int>>,
        startRow: Int,
        endRow: Int,
        @Suppress("UNUSED_PARAMETER") width: Int
    ): TextRegion {
        val bandPoints = edges.filter { it.second in startRow..endRow }
        val minX = bandPoints.minOfOrNull { it.first } ?: 0
        val maxX = bandPoints.maxOfOrNull { it.first } ?: 0
        val padding = 4

        return TextRegion(
            boundingBox = BoundingBox(
                x = (minX - padding).coerceAtLeast(0).toFloat(),
                y = (startRow - padding).coerceAtLeast(0).toFloat(),
                width = (maxX - minX + 2 * padding).toFloat(),
                height = (endRow - startRow + 2 * padding).toFloat()
            ),
            text = "" // OCR requires platform-specific ML integration
        )
    }

    // --- Internal: ROI detection ---

    /**
     * Find regions of interest using a sliding-window contrast analysis.
     *
     * A fixed-size window scans the image. Windows where the local
     * standard deviation exceeds the global mean standard deviation
     * are flagged as ROIs with a confidence proportional to contrast.
     */
    private fun findROIs(gray: IntArray, width: Int, height: Int): List<RegionOfInterest> {
        if (width < windowSize || height < windowSize) return emptyList()

        // Global mean and stddev
        val globalMean = gray.average().toFloat()
        val globalVar = gray.map { (it - globalMean) * (it - globalMean) }.average().toFloat()
        val globalStd = sqrt(globalVar.toDouble()).toFloat()
        if (globalStd < 1f) return emptyList()

        val rois = mutableListOf<RegionOfInterest>()
        val step = windowSize / 2

        var y = 0
        while (y + windowSize <= height) {
            var x = 0
            while (x + windowSize <= width) {
                val localMean = computeWindowMean(gray, x, y, width, windowSize)
                val localVar = computeWindowVar(gray, x, y, width, windowSize, localMean)
                val localStd = sqrt(localVar.toDouble()).toFloat()

                val contrast = localStd / globalStd
                if (contrast > roiContrastThreshold) {
                    val confidence = (contrast / (roiContrastThreshold * 3f)).coerceIn(0f, 1f)
                    rois.add(
                        RegionOfInterest(
                            boundingBox = BoundingBox(
                                x = x.toFloat(),
                                y = y.toFloat(),
                                width = windowSize.toFloat(),
                                height = windowSize.toFloat()
                            ),
                            confidence = confidence
                        )
                    )
                }
                x += step
            }
            y += step
        }

        return mergeOverlappingROIs(rois)
    }

    private fun computeWindowMean(gray: IntArray, startX: Int, startY: Int, imgWidth: Int, size: Int): Float {
        var sum = 0L
        for (dy in 0 until size) {
            for (dx in 0 until size) {
                sum += gray[(startY + dy) * imgWidth + (startX + dx)]
            }
        }
        return sum.toFloat() / (size * size)
    }

    private fun computeWindowVar(gray: IntArray, startX: Int, startY: Int, imgWidth: Int, size: Int, mean: Float): Float {
        var sum = 0.0
        for (dy in 0 until size) {
            for (dx in 0 until size) {
                val v = gray[(startY + dy) * imgWidth + (startX + dx)] - mean
                sum += v * v
            }
        }
        return (sum / (size * size)).toFloat()
    }

    /**
     * Merge ROIs that overlap significantly, keeping the highest confidence.
     */
    private fun mergeOverlappingROIs(rois: List<RegionOfInterest>): List<RegionOfInterest> {
        if (rois.isEmpty()) return emptyList()
        val sorted = rois.sortedByDescending { it.confidence }
        val merged = mutableListOf<RegionOfInterest>()

        for (roi in sorted) {
            val overlaps = merged.any { existing ->
                computeOverlapArea(existing.boundingBox, roi.boundingBox) > 0f
            }
            if (!overlaps) merged.add(roi)
        }
        return merged
    }

    private fun computeOverlapArea(a: BoundingBox, b: BoundingBox): Float {
        val overlapX = maxOf(0f, minOf(a.x + a.width, b.x + b.width) - maxOf(a.x, b.x))
        val overlapY = maxOf(0f, minOf(a.y + a.height, b.y + b.height) - maxOf(a.y, b.y))
        return overlapX * overlapY
    }

    fun extractAll(frame: Frame): VisionFeatures {
        val data = frame.data ?: return VisionFeatures()
        if (data.size < frame.width * frame.height * 3) return VisionFeatures()

        val contours = extractContours(frame)
        val lines = extractLines(frame)
        val textRegions = extractTextRegions(frame)
        val rois = extractROIs(frame)
        val shapes = contours.mapNotNull { classifyShape(it) }

        return VisionFeatures(
            contours = contours,
            lines = lines,
            textRegions = textRegions,
            regionsOfInterest = rois,
            shapes = shapes
        )
    }

    fun extractShapes(frame: Frame): List<DetectedShape> {
        val contours = extractContours(frame)
        return contours.mapNotNull { classifyShape(it) }
    }

    private fun classifyShape(contour: Contour): DetectedShape? {
        val rawPoints = contour.points
        if (rawPoints.size < 3) return null

        val hull = convexHull(rawPoints)
        val perimeter = computePerimeter(hull)
        if (perimeter < 1f) return null

        val simplified = simplifyContour(hull, epsilon = 0.04f * perimeter)
        val vertexCount = simplified.size

        val shapeType = when {
            vertexCount == 3 -> ShapeKind.TRIANGLE
            vertexCount == 4 -> {
                if (isSquare(simplified)) ShapeKind.SQUARE else ShapeKind.RECTANGLE
            }
            vertexCount == 5 -> ShapeKind.PENTAGON
            vertexCount == 6 -> ShapeKind.HEXAGON
            vertexCount > 6 -> {
                val circularity = computeCircularity(contour.area, perimeter)
                if (circularity > 0.7f) ShapeKind.CIRCLE else ShapeKind.POLYGON
            }
            else -> ShapeKind.UNKNOWN
        }

        return DetectedShape(
            type = shapeType,
            vertices = simplified,
            boundingBox = computeBoundingBox(rawPoints),
            area = contour.area,
            perimeter = perimeter,
            confidence = computeShapeConfidence(contour, shapeType)
        )
    }

    /**
     * Ramer-Douglas-Peucker polygon simplification for closed contours.
     */
    private fun simplifyContour(points: List<Point>, epsilon: Float): List<Point> {
        if (points.size <= 3) return points

        val closedPoints = points + points.first()
        val simplified = rdpSimplify(closedPoints, epsilon)

        return if (simplified.size > 1 && simplified.last() == simplified.first()) {
            simplified.dropLast(1)
        } else {
            simplified
        }
    }

    private fun rdpSimplify(points: List<Point>, epsilon: Float): List<Point> {
        if (points.size <= 2) return points

        val first = points.first()
        val last = points.last()
        var maxDist = 0f
        var maxIndex = 0

        for (i in 1 until points.size - 1) {
            val dist = perpendicularDistance(points[i], first, last)
            if (dist > maxDist) {
                maxDist = dist
                maxIndex = i
            }
        }

        return if (maxDist > epsilon) {
            val left = rdpSimplify(points.subList(0, maxIndex + 1), epsilon)
            val right = rdpSimplify(points.subList(maxIndex, points.size), epsilon)
            left.dropLast(1) + right
        } else {
            listOf(first, last)
        }
    }

    private fun perpendicularDistance(point: Point, lineStart: Point, lineEnd: Point): Float {
        val dx = lineEnd.x - lineStart.x
        val dy = lineEnd.y - lineStart.y
        val lengthSquared = dx * dx + dy * dy
        if (lengthSquared == 0f) {
            val ex = point.x - lineStart.x
            val ey = point.y - lineStart.y
            return sqrt(ex * ex + ey * ey)
        }
        val t = ((point.x - lineStart.x) * dx + (point.y - lineStart.y) * dy) / lengthSquared
        val clampedT = t.coerceIn(0f, 1f)
        val projX = lineStart.x + clampedT * dx
        val projY = lineStart.y + clampedT * dy
        val ex = point.x - projX
        val ey = point.y - projY
        return sqrt(ex * ex + ey * ey)
    }

    private fun sortByAngleFromCentroid(points: List<Point>): List<Point> {
        val cx = points.map { it.x }.average().toFloat()
        val cy = points.map { it.y }.average().toFloat()
        return points.sortedBy { kotlin.math.atan2((it.y - cy).toDouble(), (it.x - cx).toDouble()) }
    }

    private fun convexHull(points: List<Point>): List<Point> {
        if (points.size <= 3) return points

        val sorted = points.sortedWith(compareBy<Point> { it.x }.thenBy { it.y })
        val hull = mutableListOf<Point>()

        for (p in sorted) {
            while (hull.size >= 2) {
                val a = hull[hull.size - 2]
                val b = hull[hull.size - 1]
                val cross = (b.x - a.x) * (p.y - a.y) - (b.y - a.y) * (p.x - a.x)
                if (cross <= 0f) hull.removeAt(hull.lastIndex)
                else break
            }
            hull.add(p)
        }

        val lowerSize = hull.size
        for (i in sorted.lastIndex downTo 0) {
            val p = sorted[i]
            while (hull.size > lowerSize) {
                val a = hull[hull.size - 2]
                val b = hull[hull.size - 1]
                val cross = (b.x - a.x) * (p.y - a.y) - (b.y - a.y) * (p.x - a.x)
                if (cross <= 0f) hull.removeAt(hull.lastIndex)
                else break
            }
            hull.add(p)
        }

        hull.removeAt(hull.lastIndex)
        return hull
    }

    private fun computePerimeter(points: List<Point>): Float {
        if (points.size < 2) return 0f
        var perimeter = 0f
        for (i in points.indices) {
            val j = (i + 1) % points.size
            val dx = points[j].x - points[i].x
            val dy = points[j].y - points[i].y
            perimeter += sqrt(dx * dx + dy * dy)
        }
        return perimeter
    }

    private fun computeBoundingBox(points: List<Point>): BoundingBox {
        if (points.isEmpty()) return BoundingBox(0f, 0f, 0f, 0f)
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        for (p in points) {
            if (p.x < minX) minX = p.x
            if (p.y < minY) minY = p.y
            if (p.x > maxX) maxX = p.x
            if (p.y > maxY) maxY = p.y
        }
        return BoundingBox(
            x = minX,
            y = minY,
            width = maxX - minX,
            height = maxY - minY
        )
    }

    /**
     * Compute circularity: 4pi x area / perimeter^2.
     *
     * A perfect circle has circularity approximately 1.0.
     * Regular polygons have circularity < 1.0.
     */
    private fun computeCircularity(area: Float, perimeter: Float): Float {
        if (perimeter == 0f) return 0f
        return (4f * kotlin.math.PI.toFloat() * area) / (perimeter * perimeter)
    }

    private fun isSquare(vertices: List<Point>): Boolean {
        if (vertices.size != 4) return false
        val sides = (0 until 4).map { i ->
            val j = (i + 1) % 4
            val dx = vertices[j].x - vertices[i].x
            val dy = vertices[j].y - vertices[i].y
            sqrt(dx * dx + dy * dy)
        }
        val avgSide = sides.average().toFloat()
        if (avgSide < 1f) return false
        return sides.all { kotlin.math.abs(it - avgSide) / avgSide < 0.15f }
    }

    private fun computeShapeConfidence(contour: Contour, shapeType: ShapeKind): Float {
        val baseConfidence = when (shapeType) {
            ShapeKind.TRIANGLE -> 0.85f
            ShapeKind.RECTANGLE -> 0.90f
            ShapeKind.SQUARE -> 0.92f
            ShapeKind.PENTAGON -> 0.80f
            ShapeKind.HEXAGON -> 0.80f
            ShapeKind.CIRCLE -> 0.88f
            ShapeKind.POLYGON -> 0.75f
            ShapeKind.ELLIPSE -> 0.78f
            ShapeKind.LINE_SEGMENT -> 0.70f
            ShapeKind.UNKNOWN -> 0.50f
        }
        val areaBoost = if (contour.area > 1000f) 0.05f else 0f
        return (baseConfidence + areaBoost).coerceIn(0f, 1f)
    }

    companion object {
        /** Sliding window size for ROI detection (pixels). */
        private const val windowSize = 32

        /** Minimum contrast ratio to flag a region as ROI. */
        private const val roiContrastThreshold = 1.5f
    }
}

/**
 * A closed polygon contour detected in a frame.
 *
 * @param points Ordered vertices of the contour polygon.
 * @param area Enclosed area in square pixels.
 */
data class Contour(val points: List<Point>, val area: Float)

/**
 * A straight line segment detected in a frame.
 *
 * @param start Start point of the line.
 * @param end End point of the line.
 * @param angle Angle of the line in degrees.
 */
data class Line(val start: Point, val end: Point, val angle: Float)

/**
 * A rectangular region containing detected text.
 *
 * @param boundingBox Bounding rectangle of the text region.
 * @param text Recognized text content (empty until OCR is integrated).
 */
data class TextRegion(val boundingBox: BoundingBox, val text: String)

/**
 * A rectangular region of interest detected in a frame.
 *
 * @param boundingBox Bounding rectangle of the region.
 * @param confidence Detection confidence score between 0.0 and 1.0.
 */
data class RegionOfInterest(val boundingBox: BoundingBox, val confidence: Float)
