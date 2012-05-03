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

package boofcv.alg.feature.disparity.impl;

import boofcv.alg.feature.disparity.DisparitySparseScoreSadRect;
import boofcv.struct.image.ImageUInt8;

import java.util.Arrays;

/**
 * <p>
 * Implementation of {@link DisparitySparseScoreSadRect} that processes images of type {@link ImageUInt8}.
 * </p>
 *
 * <p>
 * DO NOT MODIFY. Generated by {@link GenerateDisparitySparseScoreSadRect}.
 * </p>
 *
 * @author Peter Abeles
 */
public class ImplDisparitySparseScoreSadRect_U8 extends DisparitySparseScoreSadRect<int[],ImageUInt8> {

	// scores up to the maximum baseline
	int scores[];

	public ImplDisparitySparseScoreSadRect_U8(int maxDisparity, int radiusX, int radiusY) {
		super(maxDisparity,radiusX, radiusY);

		scores = new int[ maxDisparity ];
	}

	public void process( int x , int y ) {
		if( x < radiusX || x >= left.width-radiusX || x < radiusY || x >= right.width-radiusY )
			throw new IllegalArgumentException("Too close to image border");

		// adjust disparity for image border
		localMaxDisparity = Math.min(maxDisparity,x-radiusX+1);

		Arrays.fill(scores,0);

		// sum up horizontal errors in the region
		for( int row = 0; row < regionHeight; row++ ) {
			// pixel indexes
			int startLeft = left.startIndex + left.stride*(y-radiusY+row) + x-radiusX;
			int startRight = right.startIndex + right.stride*(y-radiusY+row) + x-radiusX;

			for( int i = 0; i < localMaxDisparity; i++ ) {
				int indexLeft = startLeft;
				int indexRight = startRight-i;

				int score = 0;
				for( int j = 0; j < regionWidth; j++ ) {
					int diff = (left.data[ indexLeft++ ]& 0xFF) - (right.data[ indexRight++ ]& 0xFF);

					score += Math.abs(diff);
				}
				scores[i] += score;
			}
		}
	}

	@Override
	public int[] getScore() {
		return scores;
	}
	@Override
	public Class<ImageUInt8> getImageType() {
		return ImageUInt8.class;
	}

}
