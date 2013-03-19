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

package boofcv.alg.feature.detect.interest;

import boofcv.abst.feature.detect.interest.InterestPointScaleSpacePyramid;
import boofcv.abst.filter.ImageFunctionSparse;
import boofcv.abst.filter.derivative.AnyImageDerivative;
import boofcv.struct.QueueCorner;
import boofcv.struct.feature.ScalePoint;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.pyramid.PyramidFloat;
import georegression.struct.point.Point2D_I16;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * A Pyramidal implementation of {@link FeatureLaplaceScaleSpace}
 * </p>
 * <p/>
 * <p>
 * COMMENT ON SCALEPOWER: To normalize feature intensity across scales each feature intensity is multiplied by the scale to the power of 'scalePower'.
 * See [1,2] for how to compute 'scalePower'.  Inside of the image pyramid sub-sampling of the image causes the image
 * gradient to be a factor of 'scale' larger than it would be without sub-sampling.  In some situations this can negate
 * the need to adjust feature intensity further.
 * </p>
 * <p/>
 * <p>
 * [1] Krystian Mikolajczyk and Cordelia Schmid, "Indexing based on scale invariant interest points"  ICCV 2001. Proceedings.<br>
 * [2] Lindeberg, T., "Feature detection with automatic scale selection." IJCV 30(2) (1998) 79 – 116
 * </p>
 *
 * @author Peter Abeles
 * @see boofcv.factory.feature.detect.interest.FactoryInterestPoint
 */
@SuppressWarnings({"unchecked"})
public class FeatureLaplacePyramid<T extends ImageSingleBand, D extends ImageSingleBand>
		implements InterestPointScaleSpacePyramid<T> {

	// used to compute feature intensity across scale space
	private ImageFunctionSparse<T> sparseLaplace;

	// generalized feature detector.  Used to find candidate features in each scale's image
	private GeneralFeatureDetector<T, D> detector;
	private float baseThreshold;
	// location of recently computed features in layers
	protected int spaceIndex = 0;
	protected List<Point2D_I16> maximums[];

	// List of found feature points
	protected List<ScalePoint> foundPoints = new ArrayList<ScalePoint>();

	protected AnyImageDerivative<T, D> computeDerivative;

	// how much the feature intensity is scaled in each level
	// varies depending on feature type, used to adjust detection threshold
	protected double scalePower;

	/**
	 * Create a feature detector.
	 *
	 * @param detector          Point feature detector which is used to find candidates in each scale level
	 * @param sparseLaplace     Used to compute the Laplacian at each candidates
	 * @param computeDerivative Used to compute image derivatives
	 * @param scalePower        Used to normalize features intensity across scale space.  For many features this value should be one.
	 */
	public FeatureLaplacePyramid(GeneralFeatureDetector<T, D> detector,
								 ImageFunctionSparse<T> sparseLaplace,
								 AnyImageDerivative<T, D> computeDerivative,
								 double scalePower) {
		this.detector = detector;
		this.baseThreshold = detector.getThreshold();
		this.computeDerivative = computeDerivative;
		this.sparseLaplace = sparseLaplace;
		this.scalePower = scalePower;
	}

	/**
	 * Searches for features inside the provided scale space
	 *
	 * @param ss Scale space of an image
	 */
	@Override
	public void detect(PyramidFloat<T> ss) {
		spaceIndex = 0;
		if (maximums == null) {
			maximums = new List[3];
			maximums[0] = new ArrayList<Point2D_I16>();
			maximums[1] = new ArrayList<Point2D_I16>();
			maximums[2] = new ArrayList<Point2D_I16>();
		}
		foundPoints.clear();

		// compute feature intensity in each level
		for (int i = 0; i < ss.getNumLayers(); i++) {
			// detect features in 2D space.  Don't need to compute features at the tail ends of scale-space
			if (i > 0 && i < ss.getNumLayers() - 1)
				detectCandidateFeatures(ss.getLayer(i), ss.scale[i]);

			spaceIndex++;
			if (spaceIndex >= 3)
				spaceIndex = 0;

			// find maximum in NxNx3 (local image and scale space) region
			if (i >= 2) {
				findLocalScaleSpaceMax(ss, i - 1);
			}
		}
	}


	/**
	 * Use the feature detector to find candidate features in each level.  Only compute the needed image derivatives.
	 */
	private void detectCandidateFeatures(T image, double scale) {
		// adjust corner intensity threshold based upon the current scale factor
		float scaleThreshold = (float) (baseThreshold / Math.pow(scale, scalePower));
		detector.setThreshold(scaleThreshold);
		computeDerivative.setInput(image);

		D derivX = null, derivY = null;
		D derivXX = null, derivYY = null, derivXY = null;

		if (detector.getRequiresGradient()) {
			derivX = computeDerivative.getDerivative(true);
			derivY = computeDerivative.getDerivative(false);
		}
		if (detector.getRequiresHessian()) {
			derivXX = computeDerivative.getDerivative(true, true);
			derivYY = computeDerivative.getDerivative(false, false);
			derivXY = computeDerivative.getDerivative(true, false);
		}

		detector.process(image, derivX, derivY, derivXX, derivYY, derivXY);

		List<Point2D_I16> m = maximums[spaceIndex];
		m.clear();
		if( detector.isDetectMaximums() ) {
			QueueCorner q = detector.getMaximums();
			for (int i = 0; i < q.size; i++) {
				m.add(q.get(i).copy());
			}
		}
		if( detector.isDetectMinimums() ) {
			QueueCorner q = detector.getMinimums();
			for (int i = 0; i < q.size; i++) {
				m.add(q.get(i).copy());
			}
		}
	}

	/**
	 * See if each feature is a maximum in its local scale-space.
	 */
	protected void findLocalScaleSpaceMax(PyramidFloat<T> ss, int layerID) {
		int index1 = (spaceIndex + 1) % 3;

		List<Point2D_I16> candidates = maximums[index1];

		float scale0 = (float) ss.scale[layerID - 1];
		float scale1 = (float) ss.scale[layerID];
		float scale2 = (float) ss.scale[layerID + 1];

		// Because the image is blurred and scaled these are all one, I think.
		float ss0 = 1;//(float)Math.pow(scale0,0);
		float ss1 = 1;//(float)Math.pow(scale1,0);
		float ss2 = 1;//(float)Math.pow(scale2,0);

		for (Point2D_I16 c : candidates) {
			sparseLaplace.setImage(ss.getLayer(layerID));
			float val = ss1 * (float) Math.abs(sparseLaplace.compute(c.x, c.y));

			// find pixel location in each image's local coordinate
			int x0 = (int) (c.x * scale1 / scale0);
			int y0 = (int) (c.y * scale1 / scale0);

			int x2 = (int) (c.x * scale1 / scale2);
			int y2 = (int) (c.y * scale1 / scale2);

			if (checkMax(ss.getLayer(layerID - 1), val / ss0, x0, y0) && checkMax(ss.getLayer(layerID + 1), val / ss2, x2, y2)) {
				// put features into the scale of the upper image
				foundPoints.add(new ScalePoint(c.x * scale1, c.y * scale1, scale1));
			}
		}
	}

	/**
	 * See if the best score is better than the local adjusted scores at this scale
	 */
	private boolean checkMax(T image, float bestScore, int c_x, int c_y) {
		sparseLaplace.setImage(image);
		boolean isMax = true;
		beginLoop:
		for (int i = c_y - 1; i <= c_y + 1; i++) {
			for (int j = c_x - 1; j <= c_x + 1; j++) {

				if (Math.abs(sparseLaplace.compute(j, i)) >= bestScore) {
					isMax = false;
					break beginLoop;
				}
			}
		}
		return isMax;
	}

	@Override
	public List<ScalePoint> getInterestPoints() {
		return foundPoints;
	}
}
