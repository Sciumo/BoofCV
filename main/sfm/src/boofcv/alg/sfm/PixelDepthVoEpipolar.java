package boofcv.alg.sfm;

import boofcv.abst.feature.tracker.KeyFramePointTracker;
import boofcv.abst.geo.TriangulateTwoViewsCalibrated;
import boofcv.factory.geo.FactoryTriangulate;
import boofcv.numerics.fitting.modelset.ModelMatcher;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.image.ImageBase;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Visual odometry where a ranging device is assumed for pixels in the primary view.  Typical
 * inputs would include a stereo or depth camera.
 *
 * @author Peter Abeles
 */

// TODO Use the mode instead of median to resolve scale ambiguity

public class PixelDepthVoEpipolar<T extends ImageBase> {
	// TODO Make relative to the last update or remove?
	double MIN_PIXEL_CHANGE = 100;

	double TOL_TRIANGULATE = 20*Math.PI/180.0;

	int MIN_TRACKS = 100;

	// tracks features in the image
	private KeyFramePointTracker<T,PointPoseTrack> tracker;
	// used to estimate a feature's 3D position from image range data
	private ImagePixelTo3D pixelTo3D;

	// triangulate feature's 3D location
	private TriangulateTwoViewsCalibrated triangulate =
			FactoryTriangulate.twoGeometric();

	// estimate the camera motion up to a scale factor from two sets of point correspondences
	private ModelMatcher<Se3_F64, AssociatedPair> motionEstimator;

	SelectTriangulationPoints selectScalePoints = new SelectTriangulationPoints();

	Se3_F64 keyToWorld = new Se3_F64();
	Se3_F64 currToKey = new Se3_F64();
	Se3_F64 currToWorld = new Se3_F64();

	int numTracksUsed;

	boolean hasSignificantChange;

	int motionFailed;

	public PixelDepthVoEpipolar(int MIN_TRACKS, ModelMatcher<Se3_F64, AssociatedPair> motionEstimator,
								ImagePixelTo3D pixelTo3D,
								KeyFramePointTracker<T, PointPoseTrack> tracker,
								TriangulateTwoViewsCalibrated triangulate)
	{
		this.MIN_TRACKS = MIN_TRACKS;
		this.motionEstimator = motionEstimator;
		this.pixelTo3D = pixelTo3D;
		this.tracker = tracker;
		this.triangulate = triangulate;
	}

	public void reset() {
		tracker.reset();
		keyToWorld.reset();
		currToKey.reset();
		motionFailed = 0;
	}

	public boolean process( T leftImage , T rightImage ) {
		tracker.process(leftImage);

		checkForReallyCloseTrack();

		boolean foundMotion = estimateMotion();

		if( !foundMotion ) {
			System.out.println("MOTION FAILED!");
			motionFailed++;
		}

		if( numTracksUsed < MIN_TRACKS || !foundMotion ) {
			pixelTo3D.initialize();

			System.out.println("----------- CHANGE KEY FRAME ---------------");
			concatMotion();

			tracker.setKeyFrame();
			tracker.spawnTracks();

			List<PointPoseTrack> tracks = tracker.getPairs();
			List<PointPoseTrack> drop = new ArrayList<PointPoseTrack>();

			// estimate 3D coordinate using stereo vision
			for( PointPoseTrack p : tracks ) {
				Point2D_F64 pixel = p.getPixel().keyLoc;
				// discard point if it can't triangulate
				if( !pixelTo3D.process(pixel.x,pixel.y) || pixelTo3D.getW() == 0 ) {
					drop.add(p);
				} else {
					double w = pixelTo3D.getW();
					p.getLocation().set( pixelTo3D.getX()/w , pixelTo3D.getY()/w, pixelTo3D.getZ()/w);

					System.out.println("Stereo z = "+p.getLocation().getZ());
					if( p.getLocation().z < 100 )
						System.out.println("   * ");
				}
			}

			// drop tracks which couldn't be triangulated
			for( PointPoseTrack p : drop ) {
				tracker.dropTrack(p);
			}

			hasSignificantChange = false;

			checkForReallyCloseTrack();

			return foundMotion;
		} else {

			checkForReallyCloseTrack();
			return true;
		}


	}

	private void checkForReallyCloseTrack() {
		List<PointPoseTrack> tracks = tracker.getPairs();
		for( PointPoseTrack p : tracks ) {
			if( p.getLocation().z < 100 )
				System.out.println("Oh Crap");

		}
	}

	private boolean estimateMotion() {

		if( tracker.getPairs().size() <= 0 )
			return false;

		// only estimate camera motion if there is a chance of the solution not being noise
		if( !hasSignificantChange  ) {
			if( checkSignificantMotion() )
				hasSignificantChange = true;
			else {
				numTracksUsed = tracker.getPairs().size();
				return true;
			}
		}

		// estimate the motion up to a scale factor in translation
		if( !motionEstimator.process( (List)tracker.getPairs()) )
			return false;

		// TODO add non-linear refinement

		Se3_F64 candidateCurrToKey = new Se3_F64();
		motionEstimator.getModel().invert(candidateCurrToKey);

		// estimate the scale factor using previously triangulated point locations
		int N = numTracksUsed = motionEstimator.getMatchSet().size();

		selectScalePoints.setFromAtoB(candidateCurrToKey);

		List<PointPoseTrack> good = new ArrayList<PointPoseTrack>();
		for( int i = 0; i < N; i++ ) {
			PointPoseTrack t = tracker.getPairs().get( motionEstimator.getInputIndex(i) );

			if( selectScalePoints.computeAcuteAngle(t.currLoc,t.keyLoc) >= TOL_TRIANGULATE ) {
				good.add(t);
			}
		}

		if( good.size() < 5 ) {
			// only use the rotation estimate
			currToKey.getR().set(candidateCurrToKey.getR());
			return true;
		}

		// update the translation and rotation
		double ratio[] = new double[good.size()];

		for( int i = 0; i < good.size(); i++ ) {
			PointPoseTrack t = good.get(i);

			Point3D_F64 P = t.getLocation();
			double origZ = P.z;

			triangulate.triangulate(t.currLoc,t.keyLoc,candidateCurrToKey,P);
			ratio[i] = origZ/P.z;

			System.out.println("  orig z = "+origZ);
		}

		Arrays.sort(ratio);

		double scale = ratio[ ratio.length/2 ];

		// correct the scale factors
		currToKey.set(candidateCurrToKey);
		GeometryMath_F64.scale(currToKey.getT(),scale);

		for( PointPoseTrack t : good ) {
			double before = t.getLocation().z;

			GeometryMath_F64.scale(t.getLocation(),scale);

			System.out.printf("Triangulate before = %.5f after z = %.5f\n",before,t.getLocation().getZ());
		}

		System.out.println("-----------------------");

		return true;
	}

	private boolean checkSignificantMotion() {
		List<PointPoseTrack> tracks = tracker.getPairs();

		int numOver = 0;

		for( int i = 0; i < tracks.size(); i++ ) {
			AssociatedPair p = tracks.get(i).getPixel();

			if( p.keyLoc.distance2(p.currLoc) > MIN_PIXEL_CHANGE )
				numOver++;
		}
		return numOver >= tracks.size()/2;
	}

	private void concatMotion() {
		Se3_F64 temp = new Se3_F64();
		currToKey.concat(keyToWorld,temp);
		keyToWorld.set(temp);
		currToKey.reset();
	}

	public Se3_F64 getCurrToWorld() {
		currToKey.concat(keyToWorld,currToWorld);
		return currToWorld;
	}

	public KeyFramePointTracker<T, PointPoseTrack> getTracker() {
		return tracker;
	}

	public ModelMatcher<Se3_F64, AssociatedPair> getMotionEstimator() {
		return motionEstimator;
	}
}
