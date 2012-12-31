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

package boofcv.factory.transform.gss;

import boofcv.alg.transform.gss.NoCacheScaleSpace;
import boofcv.core.image.ImageGenerator;
import boofcv.core.image.inst.SingleBandGenerator;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSInt16;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageUInt8;

/**
 * Factory which removes some of the drudgery from creating {@link boofcv.struct.gss.GaussianScaleSpace}
 *
 * @author Peter Abeles
 */
public class FactoryGaussianScaleSpace {

	public static <T extends ImageSingleBand, D extends ImageSingleBand>
	NoCacheScaleSpace<T,D> nocache( Class<T> imageType  ) {
		if( imageType == ImageFloat32.class ) {
			return (NoCacheScaleSpace<T,D>)nocache_F32();
		} else if( imageType == ImageUInt8.class ) {
			return (NoCacheScaleSpace<T,D>)nocache_U8();
		} else {
			throw new IllegalArgumentException("Doesn't handle "+imageType.getSimpleName()+" yet.");
		}
	}

	public static NoCacheScaleSpace<ImageFloat32,ImageFloat32> nocache_F32() {
		ImageGenerator<ImageFloat32> imageGen = new SingleBandGenerator<ImageFloat32>(ImageFloat32.class);
		return new NoCacheScaleSpace<ImageFloat32,ImageFloat32>(imageGen,imageGen);
	}

	public static NoCacheScaleSpace<ImageUInt8, ImageSInt16> nocache_U8() {
		ImageGenerator<ImageUInt8> imageGen = new SingleBandGenerator<ImageUInt8>(ImageUInt8.class);
		ImageGenerator<ImageSInt16> derivGen = new SingleBandGenerator<ImageSInt16>(ImageSInt16.class);
		return new NoCacheScaleSpace<ImageUInt8,ImageSInt16>(imageGen,derivGen);
	}
}
