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

/**
 * Interface for content detection plugins.
 *
 * Implement this to add support for new content types.
 */
public interface DetectorPlugin {
    /**
     * The type of content this plugin detects.
     */
    public val contentType: ContentType

    /**
     * Confidence threshold for this detector.
     */
    public val confidence: Float

    /**
     * Detect content in the given frame.
     *
     * @param frame The input image frame
     * @return Detected content item, or null if nothing detected
     */
    public suspend fun detect(frame: Frame): ContentItem?
}
