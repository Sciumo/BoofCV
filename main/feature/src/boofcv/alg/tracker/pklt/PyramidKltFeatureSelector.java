/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://boofcv.org).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package boofcv.alg.tracker.pklt;

import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.pyramid.ImagePyramid;

import java.util.List;

/**
 * Interface for automatically selects {@link PyramidKltFeature} from an image.
 *
 * @author Peter Abeles
 */
public interface PyramidKltFeatureSelector
		<InputImage extends ImageSingleBand, DerivativeImage extends ImageSingleBand>
{
	/**
	 * Given the image and its gradient, select features for the tracker to track.
	 *
	 * @param image  Original image.
	 * @param derivX Image derivative along the x-axis.
	 * @param derivY Image derivative along the y-axis.
	 * @return List of KLT features.
	 */
	public void setInputs(ImagePyramid<InputImage> image,
						  ImagePyramid<DerivativeImage> derivX, ImagePyramid<DerivativeImage> derivY);

	/**
	 * Selects new features using provided data structures.
	 *
	 * @param active List of currently active features which should not be returned again.
	 * @param availableData List of feature data that can be used to create a new features.
	 */
	public void compute( List<PyramidKltFeature> active , List<PyramidKltFeature> availableData );
}
