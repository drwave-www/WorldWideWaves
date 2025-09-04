package com.worldwidewaves.shared.utils

/*
 * Copyright 2025 DrWave
 *
 * WorldWideWaves is an ephemeral mobile app designed to orchestrate human waves through cities and
 * countries, culminating in a global wave. The project aims to transcend physical and cultural
 * boundaries, fostering unity, community, and shared human experience by leveraging real-time
 * coordination and location-based services.
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

import io.github.aakira.napier.Napier
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.UIKit.UIGraphicsBeginImageContext
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage

@OptIn(ExperimentalForeignApi::class)
class IOSImageResolver : ImageResolver<UIImage> {
    
    /**
     * Resolves a resource path to a UIImage.
     * 
     * @param path The resource path to resolve.
     * @return The UIImage for the resource, or null if not found.
     */
    override fun resolve(path: String): UIImage? {
        try {
            // Try to load image from main bundle
            return UIImage.imageNamed(path)
                ?: run {
                    Napier.e("Failed to load image: $path")
                    null
                }
        } catch (e: Exception) {
            Napier.e("Error loading image $path: ${e.message}")
            return null
        }
    }
    
    /**
     * Extracts a specific frame from a sprite sheet.
     * 
     * @param path The resource path of the sprite sheet.
     * @param frameIndex The index of the frame to extract (0-based).
     * @param frameWidth The width of a single frame in pixels.
     * @param frameHeight The height of a single frame in pixels.
     * @param frameCount The total number of frames in the sprite sheet.
     * @return The UIImage for the specific frame, or null if extraction fails.
     */
    @OptIn(ExperimentalForeignApi::class)
    fun resolveFrame(
        path: String,
        frameIndex: Int,
        frameWidth: Int,
        frameHeight: Int,
        frameCount: Int
    ): UIImage? {
        if (frameIndex < 0 || frameIndex >= frameCount) {
            Napier.e("Invalid frame index: $frameIndex (must be between 0 and ${frameCount - 1})")
            return null
        }
        
        try {
            // Load the full sprite sheet image
            val fullImage = resolve(path) ?: return null
            
            // Calculate the frame rectangle
            val x = frameIndex * frameWidth.toDouble()
            val y = 0.0
            val width = frameWidth.toDouble()
            val height = frameHeight.toDouble()
            
            // Begin a new image context with the size of our desired frame
            UIGraphicsBeginImageContext(CGSizeMake(width, height))
            
            // Extract the CValue<CGSize> into concrete width / height values
            val (imgWidth, imgHeight) = fullImage.size.useContents { width to height }
            
            // Draw the full image at a negative offset so only the desired portion appears in the context
            fullImage.drawInRect(CGRectMake(-x, -y, imgWidth, imgHeight))
            
            // Get the cropped image from the context
            val croppedImage = UIGraphicsGetImageFromCurrentImageContext()
            
            // End the image context
            UIGraphicsEndImageContext()
            
            return croppedImage ?: run {
                Napier.e("Failed to create cropped image for $path at index $frameIndex")
                null
            }
        } catch (e: Exception) {
            Napier.e("Error extracting frame from $path at index $frameIndex: ${e.message}")
            return null
        }
    }
}
