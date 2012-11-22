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

package boofcv.core.image;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.struct.image.ImageInteger;
import boofcv.struct.image.ImageSingleBand;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestConvertImage {

	Random rand = new Random(34);
	int imgWidth = 10;
	int imgHeight = 20;

	@Test
	public void checkAllConvert() {
		int count = 0;
		Method methods[] = ConvertImage.class.getMethods();

		for (Method m : methods) {
			if( !m.getName().contains("convert"))
				continue;

			Class<?> inputType = m.getParameterTypes()[0];
			Class<?> outputType = m.getParameterTypes()[1];

//			System.out.println(m.getName()+" "+inputType.getSimpleName()+" "+outputType.getSimpleName()+" "+m.getReturnType());
			
			// make sure the return type equals the output type
			assertTrue( outputType == m.getReturnType() );

			checkConvert(m,inputType,outputType);
			count++;
		}

		assertEquals(42,count);
	}

	private void checkConvert( Method m , Class inputType , Class outputType ) {
		ImageSingleBand input = GeneralizedImageOps.createSingleBand(inputType, imgWidth, imgHeight);
		ImageSingleBand output = GeneralizedImageOps.createSingleBand(outputType, imgWidth, imgHeight);

		boolean inputSigned = true;
		boolean outputSigned = true;

		if( ImageInteger.class.isAssignableFrom(inputType) )
			inputSigned = ((ImageInteger)input).getTypeInfo().isSigned();
		if( ImageInteger.class.isAssignableFrom(outputType) )
			outputSigned = ((ImageInteger)output).getTypeInfo().isSigned();

	   // only provide signed numbers of both data types can handle them
		if( inputSigned && outputSigned ) {
			GImageMiscOps.fillUniform(input, rand, -10, 10);
		} else {
			GImageMiscOps.fillUniform(input, rand, 0, 20);
		}

		BoofTesting.checkSubImage(this,"checkConvert",true,m,input,output);
	}

	public void checkConvert( Method m , ImageSingleBand<?> input , ImageSingleBand<?> output ) {
		try {
			// check it with a non-null output
			ImageSingleBand<?> ret = (ImageSingleBand<?>)m.invoke(null,input,output);
			BoofTesting.assertEqualsGeneric(input,ret,0,1e-4);

			// check it with a null output
			ret = (ImageSingleBand<?>)m.invoke(null,input,null);
			BoofTesting.assertEqualsGeneric(input,ret,0,1e-4);

		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}
}
