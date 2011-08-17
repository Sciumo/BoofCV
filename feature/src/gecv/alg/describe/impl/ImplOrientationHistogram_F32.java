/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.describe.impl;

import gecv.alg.InputSanityCheck;
import gecv.alg.describe.OrientationHistogram;
import gecv.struct.image.ImageFloat32;


/**
 * <p>
 * Implementation of {@link OrientationHistogram} for a specific image type.
 * </p>
 *
 * <p>
 * WARNING: Do not modify.  Automatically generated by {@link GenerateImplOrientationHistogram}.
 * </p>
 *
 * @author Peter Abeles
 */
public class ImplOrientationHistogram_F32 extends OrientationHistogram<ImageFloat32> {

	public ImplOrientationHistogram_F32(int numAngles) {
		super(numAngles);
	}

	@Override
	public void setImage(ImageFloat32 derivX, ImageFloat32 derivY) {
		InputSanityCheck.checkSameShape(derivX,derivY);

		this.derivX = derivX;
		this.derivY = derivY;
	}

	@Override
	protected void computeUnweightedScore() {
		// compute the score for each angle in the histogram
		for( int y = rect.y0; y < rect.y1; y++ ) {
			int indexX = derivX.startIndex + derivX.stride*y + rect.x0;
			int indexY = derivY.startIndex + derivY.stride*y + rect.x0;

			for( int x = rect.x0; x < rect.x1; x++ , indexX++ , indexY++ ) {
				float dx = derivX.data[indexX];
				float dy = derivY.data[indexY];

				double angle = Math.atan2(dy,dx);
				// compute which discretized angle it is
				int discreteAngle = (int)((angle + angleRound)/angleDiv) % numAngles;
				// sum up the "score" for this angle
				sumDerivX[discreteAngle] += dx;
				sumDerivY[discreteAngle] += dy;
			}
		}
	}

	@Override
	protected void computeWeightedScore( int c_x , int c_y ) {
		// compute the score for each angle in the histogram
		for( int y = rect.y0; y < rect.y1; y++ ) {
			int indexX = derivX.startIndex + derivX.stride*y + rect.x0;
			int indexY = derivY.startIndex + derivY.stride*y + rect.x0;
			int indexW = (y-c_y+radius)*weights.width + rect.x0-c_x+radius;

			for( int x = rect.x0; x < rect.x1; x++ , indexX++ , indexY++ , indexW++ ) {
				float w = weights.data[indexW];

				float dx = derivX.data[indexX];
				float dy = derivY.data[indexY];

				double angle = Math.atan2(dy,dx);
				// compute which discretized angle it is
				int discreteAngle = (int)((angle + angleRound)/angleDiv) % numAngles;
				// sum up the "score" for this angle
				sumDerivX[discreteAngle] += w*dx;
				sumDerivY[discreteAngle] += w*dy;
			}
		}
	}

	@Override
	public Class<ImageFloat32> getImageType() {
		return ImageFloat32.class;
	}
}
