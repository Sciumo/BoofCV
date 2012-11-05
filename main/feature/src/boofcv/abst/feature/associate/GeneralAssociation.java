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

package boofcv.abst.feature.associate;

import boofcv.struct.FastQueue;
import boofcv.struct.feature.AssociatedIndex;


/**
 * <p>
 * Generalized interface for associating features.
 * </p>
 *
 * <p>
 * DESIGN NOTE: {@link FastQueue} is used instead of {@link java.util.List} because in the association
 * micro benchmark it produced results that were about 20% faster consistently.  Which is surprising since
 * one would think the comparisons would dominate.
 * </p>
 *
 * @author Peter Abeles
 */
public interface GeneralAssociation<T> {

	/**
	 * Finds the best match for each item in the src list with an item in the 'dst' list.
	 *
	 * @param listSrc Source list that is being matched to dst list.
	 * @param listDst Destination list of items that are matched to source.
	 */
	public void associate( FastQueue<T> listSrc , FastQueue<T> listDst );

	/**
	 * List of associated features.  Indexes refer to the index inside the input lists.
	 *
	 * @return List of associated features.
	 */
	public FastQueue<AssociatedIndex> getMatches();
}
