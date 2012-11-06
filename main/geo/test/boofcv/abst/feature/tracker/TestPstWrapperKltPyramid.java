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

package boofcv.abst.feature.tracker;

import boofcv.alg.tracker.pklt.PkltManager;
import boofcv.alg.tracker.pklt.PkltManagerConfig;
import boofcv.struct.image.ImageFloat32;


/**
 * @author Peter Abeles
 */
public class TestPstWrapperKltPyramid extends StandardImagePointTracker<ImageFloat32> {

	PkltManagerConfig<ImageFloat32,ImageFloat32> config;
	PkltManager<ImageFloat32,ImageFloat32> manager;
	PstWrapperKltPyramid<ImageFloat32,ImageFloat32> pointTracker;

	@Override
	public ImagePointTracker<ImageFloat32> createTracker() {
		config = PkltManagerConfig.createDefault(ImageFloat32.class,ImageFloat32.class);
		manager = new PkltManager<ImageFloat32,ImageFloat32>(config);
		pointTracker = new PstWrapperKltPyramid<ImageFloat32,ImageFloat32>(manager);
		return pointTracker;
	}
}
