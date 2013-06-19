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

package boofcv.alg.feature.describe;

import boofcv.factory.filter.kernel.FactoryKernelGaussian;
import boofcv.struct.convolve.Kernel2D_F64;
import boofcv.struct.feature.SurfFeature;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.sparse.GradientValue;
import boofcv.struct.sparse.SparseImageGradient;

/**
 * <p>
 * Modified SURF descriptor which attempts to smooth out edge conditions.  Based upon MU-SURF described in
 * [1] it computes features in over lapping sub-regions and has a separate set of weights for the large grid and
 * for sub-regions.  Due to these improvements it will in general produce better results than {@link DescribePointSurf}
 * at the cost of additional computations.
 * </p>

 * <p>
 * [1] M. Agrawal, K. Konolige, and M. Blas, "CenSurE: Center Surround Extremas for Realtime Feature Detection and
 * Matching,"  Computer Vision – ECCV 2008
 * </p>
 *
 * @author Peter Abeles
 */
public class DescribePointSurfMod<II extends ImageSingleBand> extends DescribePointSurf<II> {

	// how many sample points sub-regions overlap.
	private int overLap;

	// used to weigh feature computation
	private Kernel2D_F64 weightGrid;
	private Kernel2D_F64 weightSub;

	private double samplesX[];
	private double samplesY[];

	// how wide the grid is that's being sampled in units of samples
	int sampleWidth;
	
	/**
	 * Creates a SURF descriptor of arbitrary dimension by changing how the local region is sampled.
	 *
	 * @param widthLargeGrid Number of sub-regions wide the large grid is.  Typically 4.
	 * @param widthSubRegion Number of sample points wide a sub-region is.  Typically 5.
	 * @param widthSample The size of a sample point. Typically 3.
	 * @param overLap Number of sample points sub-regions overlap, Typically 2.
	 * @param sigmaLargeGrid Sigma used to weight points in the large grid. Typically 2.5
	 * @param sigmaSubRegion Sigma used to weight points in the sub-region grid. Typically 2.5
	 * @param useHaar If true the Haar wavelet will be used (what was used in [1]), false means an image gradient
	 * approximation will be used.  True is recommended.
	 */
	public DescribePointSurfMod(int widthLargeGrid, int widthSubRegion,
								int widthSample, int overLap ,
								double sigmaLargeGrid , double sigmaSubRegion ,
								boolean useHaar, Class<II> imageType ) {
		super(widthLargeGrid, widthSubRegion, widthSample, 1, useHaar,imageType);

		this.overLap = overLap;

		weightGrid = FactoryKernelGaussian.gaussianWidth(sigmaLargeGrid, widthLargeGrid);
		weightSub = FactoryKernelGaussian.gaussianWidth(sigmaSubRegion, widthSubRegion + 2 * overLap);

		double div = weightGrid.get(weightGrid.getRadius(),weightGrid.getRadius());
		for( int i = 0; i < weightGrid.data.length; i++ )
			weightGrid.data[i] /= div;

		div = weightSub.get(weightSub.getRadius(),weightSub.getRadius());
		for( int i = 0; i < weightSub.data.length; i++ )
			weightSub.data[i] /= div;

		sampleWidth = widthLargeGrid*widthSubRegion+overLap*2;
		samplesX = new double[sampleWidth*sampleWidth];
		samplesY = new double[sampleWidth*sampleWidth];
	}

	/**
	 * Create a SURF-64 descriptor.  See [1] for details.
	 */
	public DescribePointSurfMod(Class<II> imageType) {
		this(4,5,3,2, 2.5 , 2.5 , false ,imageType);
	}

	/**
	 * <p>
	 * Computes the SURF descriptor for the specified interest point.  If the feature
	 * goes outside of the image border (including convolution kernels) then null is returned.
	 * </p>
	 *
	 * @param x Location of interest point.
	 * @param y Location of interest point.
	 * @param angle The angle the feature is pointing at in radians.
	 * @param scale Scale of the interest point. Null is returned if the feature goes outside the image border.
	 * @param ret storage for the feature. Must have 64 values. If null a new feature will be declared internally.
	 */
	public void describe(double x, double y, double angle, double scale, SurfFeature ret)
	{
		double c = Math.cos(angle) , s= Math.sin(angle);
		// By assuming that the entire feature is inside the image faster algorithms can be used
		// the results are also of dubious value when interacting with the image border.
		boolean isInBounds =
				SurfDescribeOps.isInside(ii,x,y,(widthLargeGrid*widthSubRegion)/2+overLap,widthSample,scale,c,s);

		// declare the feature if needed
		if( ret == null )
			ret = new SurfFeature(featureDOF);
		else if( ret.value.length != featureDOF )
			throw new IllegalArgumentException("Provided feature must have "+featureDOF+" values");

		// extract descriptor
		gradient.setImage(ii);
		gradient.setScale(scale);

		// use a safe method if its along the image border
		SparseImageGradient gradient = isInBounds ? this.gradient : this.gradientSafe;

		// compute image features
		features(x, y, c, s, scale, gradient , ret.value);

		// normalize feature vector to have an Euclidean length of 1
		// adds light invariance
		SurfDescribeOps.normalizeFeatures(ret.value);

		// Laplacian's sign
		ret.laplacianPositive = computeLaplaceSign((int)Math.round(x),(int)Math.round(y), scale);
	}

	/**
	 * <p>
	 * An improved SURF descriptor as presented in CenSurE paper.   The sub-regions now overlap and more
	 * points are sampled in the sub-region to allow overlap.
	 * </p>
	 *
	 * @param c_x Center of the feature x-coordinate.
	 * @param c_y Center of the feature y-coordinate.
	 * @param c cosine of the orientation
	 * @param s sine of the orientation
	 * @param scale The scale of the wavelets.
	 * @param features Where the features are written to.  Must be 4*(widthLargeGrid*widthSubRegion)^2 large.
	 */
	public void features(double c_x, double c_y,
						 double c , double s,
						 double scale,  SparseImageGradient gradient , double[] features)
	{
		int regionSize = widthLargeGrid*widthSubRegion;

		int totalSampleWidth = widthSubRegion+overLap*2;

		int regionR = regionSize/2;
		int regionEnd = regionSize-regionR;

		int sampleGridWidth = regionSize+2*overLap;
		int regionIndex = 0;

		// when computing the pixel coordinates it is more precise to round to the nearest integer
		// since pixels are always positive round() is equivalent to adding 0.5 and then converting
		// to an int, which floors the variable.
		c_x += 0.5;
		c_y += 0.5;

		// first sample the whole grid at once to avoid sampling overlapping regions twice
		int index = 0;
		for( int rY = -regionR-overLap; rY < regionEnd+overLap; rY++) {
			double regionY = rY*scale;
			for( int rX = -regionR-overLap; rX < regionEnd+overLap; rX++,index++ ) {
				double regionX = rX*scale;

				// rotate the pixel along the feature's direction
				int pixelX = (int)(c_x + c*regionX - s*regionY);
				int pixelY = (int)(c_y + s*regionX + c*regionY);

				GradientValue g = gradient.compute(pixelX,pixelY);
				samplesX[index] = g.getX();
				samplesY[index] = g.getY();
			}
		}

		// compute descriptor using precomputed samples
		int indexGridWeight = 0;
		for( int rY = -regionR; rY < regionEnd; rY += widthSubRegion ) {
			for( int rX = -regionR; rX < regionEnd; rX += widthSubRegion ) {
				double sum_dx = 0, sum_dy=0, sum_adx=0, sum_ady=0;

				// compute and sum up the response  inside the sub-region
				for( int i = 0; i < totalSampleWidth; i++ ) {
					index = (rY+regionR+i)*sampleGridWidth + rX+regionR;
					for( int j = 0; j < totalSampleWidth; j++ , index++ ) {
						double w = weightSub.get(j,i);

						double dx = w*samplesX[index];
						double dy = w*samplesY[index];

						// align the gradient along image patch
						// note the transform is transposed
						double pdx =  c*dx + s*dy;
						double pdy = -s*dx + c*dy;

						sum_dx += pdx;
						sum_adx += Math.abs(pdx);
						sum_dy += pdy;
						sum_ady += Math.abs(pdy);
					}
				}

				double w = weightGrid.data[indexGridWeight++];
				features[regionIndex++] = w*sum_dx;
				features[regionIndex++] = w*sum_adx;
				features[regionIndex++] = w*sum_dy;
				features[regionIndex++] = w*sum_ady;
			}
		}
	}

	@Override
	public int getRadius() {
		return (widthLargeGrid*widthSubRegion+widthSample)/2 + overLap;
	}
}
