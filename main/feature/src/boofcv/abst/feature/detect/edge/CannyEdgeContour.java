/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.feature.detect.edge;

import boofcv.abst.filter.blur.BlurFilter;
import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.feature.detect.edge.EdgeContour;
import boofcv.alg.feature.detect.edge.GGradientToEdgeFeatures;
import boofcv.alg.feature.detect.edge.GradientToEdgeFeatures;
import boofcv.alg.feature.detect.edge.HysteresisEdgeTrace4;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.FastQueue;
import boofcv.struct.image.*;
import georegression.struct.point.Point2D_I32;

import java.util.ArrayList;
import java.util.List;


/**
 * Implementation of canny edge detector.  The canny edge detector detects the edges of objects
 * using a hysteresis threshold.  When scanning the image pixels with edge intensities below
 * the high threshold are ignored.  After a pixel is found that exceeds the high threshold any
 * pixel that is connect to it directly or indirectly just needs to exceed the low threshold.
 *
 * @author Peter Abeles
 */
public class CannyEdgeContour<T extends ImageSingleBand, D extends ImageSingleBand> implements DetectEdgeContour<T> {

	// blurs the input image
	BlurFilter<T> blur;

	// computes the image gradient
	private ImageGradient<T,D> gradient;
	// thresholds for hysteresis thresholding
	protected float threshLow;
	protected float threshHigh;

	// blurred input image
	private T blurred;

	// image gradient
	private D derivX;
	private D derivY;

	// edge intensity
	protected ImageFloat32 intensity = new ImageFloat32(1,1);
	protected ImageFloat32 suppressed = new ImageFloat32(1,1);
	// edge direction in radians
	protected ImageFloat32 angle = new ImageFloat32(1,1);
	// quantized direction
	protected ImageSInt8 direction = new ImageSInt8(1,1);
	// work space
	protected ImageSInt32 label = new ImageSInt32(1,1);
	protected ImageUInt8 work = new ImageUInt8(1,1);

	HysteresisEdgeTrace4 hysteresis = new HysteresisEdgeTrace4();

	List<List<Point2D_I32>> contours = new ArrayList<List<Point2D_I32>>();

	public CannyEdgeContour(BlurFilter<T> blur, ImageGradient<T, D> gradient, float threshLow, float threshHigh) {
		this.blur = blur;
		this.gradient = gradient;
		this.threshLow = threshLow;
		this.threshHigh = threshHigh;

		Class<T> imageType = blur.getInputType();
		Class<D> derivType = gradient.getDerivType();

		blurred = GeneralizedImageOps.createSingleBand(imageType, 1, 1);
		derivX = GeneralizedImageOps.createSingleBand(derivType, 1, 1);
		derivY = GeneralizedImageOps.createSingleBand(derivType, 1, 1);
	}

	@Override
	public void process(T input) {

		// setup internal data structures
		blurred.reshape(input.width,input.height);
		derivX.reshape(input.width,input.height);
		derivY.reshape(input.width,input.height);
		intensity.reshape(input.width,input.height);
		suppressed.reshape(input.width,input.height);
		angle.reshape(input.width,input.height);
		direction.reshape(input.width,input.height);
		label.reshape(input.width,input.height);
		work.reshape(input.width,input.height);

		// run canny edge detector
		blur.process(input,blurred);
		gradient.process(blurred,derivX,derivY);
		GGradientToEdgeFeatures.intensityAbs(derivX,derivY,intensity);
		GGradientToEdgeFeatures.direction(derivX,derivY,angle);
		GradientToEdgeFeatures.discretizeDirection4(angle,direction);
		GradientToEdgeFeatures.nonMaxSuppression4(intensity,direction, suppressed);

		updateThresholds();

		hysteresis.setImages(suppressed,direction);
		hysteresis.process(threshLow,threshHigh);
		FastQueue<EdgeContour> found = hysteresis.getQueueContour();

		contours.clear();
		for( EdgeContour e : found.toList() ) {
			contours.addAll( e.edges );
		}
	}


	/**
	 * This function exists so that the wrapper can be overridden to add an adaptive threshold
	 */
	protected void updateThresholds() {

	}

	@Override
	public List<List<Point2D_I32>> getContours() {
		return contours;
	}
}
