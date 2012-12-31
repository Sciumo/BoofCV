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

package boofcv.alg.filter.blur;

import boofcv.alg.InputSanityCheck;
import boofcv.alg.filter.blur.impl.ImplMedianHistogramInner;
import boofcv.alg.filter.blur.impl.ImplMedianSortEdgeNaive;
import boofcv.alg.filter.blur.impl.ImplMedianSortNaive;
import boofcv.alg.filter.convolve.ConvolveImageMean;
import boofcv.alg.filter.convolve.ConvolveNormalized;
import boofcv.factory.filter.kernel.FactoryKernelGaussian;
import boofcv.struct.convolve.Kernel1D_F32;
import boofcv.struct.convolve.Kernel1D_I32;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageUInt8;

/**
 * Catch all class for function which "blur" an image, typically used to "reduce" the amount
 * of noise in the image.
 *
 * @author Peter Abeles
 */
public class BlurImageOps {


	public static ImageUInt8 mean(ImageUInt8 input, ImageUInt8 output, int radius, ImageUInt8 storage) {

		output = InputSanityCheck.checkDeclare(input,output);
		storage = InputSanityCheck.checkDeclare(input,storage);

		ConvolveImageMean.horizontal(input, storage, radius);
		ConvolveImageMean.vertical(storage, output, radius);

		return output;
	}

	public static ImageUInt8 median(ImageUInt8 input, ImageUInt8 output, int radius) {
		output = InputSanityCheck.checkDeclare(input,output);

		int w = radius*2+1;
		int offset[] = new int[ w*w ];
		int histogram[] = new int[ 256 ];

		ImplMedianHistogramInner.process(input, output, radius, offset, histogram);
		ImplMedianSortEdgeNaive.process(input, output, radius, offset);

		return output;
	}

	public static ImageUInt8 gaussian(ImageUInt8 input, ImageUInt8 output, double sigma , int radius,
									  ImageUInt8 storage ) {
		output = InputSanityCheck.checkDeclare(input,output);
		storage = InputSanityCheck.checkDeclare(input,storage);

		Kernel1D_I32 kernel = FactoryKernelGaussian.gaussian(Kernel1D_I32.class,sigma,radius);

		ConvolveNormalized.horizontal(kernel,input,storage);
		ConvolveNormalized.vertical(kernel,storage,output);

		return output;
	}

	public static ImageFloat32 mean(ImageFloat32 input, ImageFloat32 output, int radius, ImageFloat32 storage) {

		output = InputSanityCheck.checkDeclare(input,output);
		storage = InputSanityCheck.checkDeclare(input,storage);

		ConvolveImageMean.horizontal(input,storage,radius);
		ConvolveImageMean.vertical(storage,output,radius);

		return output;
	}

	public static ImageFloat32 median(ImageFloat32 input, ImageFloat32 output, int radius) {
		output = InputSanityCheck.checkDeclare(input,output);

		ImplMedianSortNaive.process(input,output,radius,null);

		return output;
	}

	public static ImageFloat32 gaussian(ImageFloat32 input, ImageFloat32 output,
										double sigma , int radius,
										ImageFloat32 storage ) {
		output = InputSanityCheck.checkDeclare(input,output);
		storage = InputSanityCheck.checkDeclare(input,storage);

		Kernel1D_F32 kernel = FactoryKernelGaussian.gaussian(Kernel1D_F32.class,sigma, radius);

		ConvolveNormalized.horizontal(kernel,input,storage);
		ConvolveNormalized.vertical(kernel,storage,output);

		return output;
	}
}
