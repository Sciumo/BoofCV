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

package boofcv.abst.feature.associate;

import boofcv.alg.feature.associate.DescriptorDistance;
import boofcv.struct.feature.MatchScoreType;
import boofcv.struct.feature.TupleDesc_F32;


/**
 * Scores based on Euclidean distance squared
 *
 * @see {@link DescriptorDistance#euclideanSq(boofcv.struct.feature.TupleDesc_F32, boofcv.struct.feature.TupleDesc_F32)}
 *
 * @author Peter Abeles
 */
public class ScoreAssociateEuclideanSq_F32 implements ScoreAssociation<TupleDesc_F32> {
	@Override
	public double score(TupleDesc_F32 a, TupleDesc_F32 b) {
		return DescriptorDistance.euclideanSq(a, b);
	}

	@Override
	public MatchScoreType getScoreType() {
		return MatchScoreType.NORM_ERROR;
	}
}
