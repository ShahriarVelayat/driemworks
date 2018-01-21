package com.driemworks.simplecv.graphics.rendering;

import android.opengl.GLSurfaceView;
import android.util.Log;

import com.driemworks.common.utils.TagUtils;
import com.driemworks.sensor.services.OrientationService;
import com.driemworks.sensor.utils.OrientationUtils;
import com.driemworks.common.enums.Resolution;
import com.driemworks.simplecv.utils.RenderUtils;
import com.threed.jpct.Camera;
import com.threed.jpct.FrameBuffer;
import com.threed.jpct.Light;
import com.threed.jpct.Object3D;
import com.threed.jpct.RGBColor;
import com.threed.jpct.SimpleVector;
import com.threed.jpct.World;
import com.threed.jpct.util.MemoryHelper;

import org.opencv.core.Rect;
import org.opencv.core.Scalar;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * @author Tony
 */
public class StaticCubeRenderer implements GLSurfaceView.Renderer {

    /**
     * The tag used for logging
     */
    private final String TAG = TagUtils.getTag(this);

    /**
     * The frame buffer
     */
    private FrameBuffer fb = null;

    /**
     * The world
     */
    private World world = null;

    /**
     * The light
     */
    private Light sun = null;

    /**
     * The camera
     */
    private Camera cam;

    /**
     * The previous rotation vector
     */
    private float[] previousRotationVector = new float[3];

    /**
     * The current rotation vector
     */
    private float[] currentRotationVector = new float[3];

    /**
     * The change in rotation
     */
    private float[] deltaRotation = new float[3];

    /** The x camera coordinate */
    private float x = 0;

    /**
     * The y camera coordinate
     */
    private float y = 0;

    /**
     * The z camera coordinate.
     */
    private float z = 100;

    /**
     * The width of the screen
     */
    private int width;

    /**
     * The height of the screen
     */
    private int height;

    private Object3D cube;

    private boolean isRotationEnabled;

    /**
     * The default constructor
     */
    public StaticCubeRenderer(Resolution resolution, boolean isRotationEnabled) {
        super();
        this.width = resolution.getWidth();
        this.height = resolution.getHeight();
        this.isRotationEnabled = isRotationEnabled;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onSurfaceChanged(GL10 g1, int w, int h) {
        Log.d(TAG, "surface changed");

        if (fb != null) {
            fb.dispose();
        }

        fb = new FrameBuffer(g1, w, h);
        initWorld();
    }

    /**
     * Initialize the world
     */
    private void initWorld() {
        world = new World();
        world.setAmbientLight(20, 30, 40);

        cube = RenderUtils.createCube(RGBColor.WHITE, 20, 0, 0, 300);
        cube.setVisibility(true);
        world.addObject(cube);

        // setup the camera
        cam = world.getCamera();
        cam.setPosition(0,0,0);
        cam.lookAt(new SimpleVector(0,0,300));

        sun = new Light(world);
        sun.setIntensity(255, 255, 255);
        SimpleVector sv = new SimpleVector();
        sv.set(cube.getTransformedCenter());
        sv.y = 0;
        sv.x = width / 2;
        sv.z += 300;
        sun.setPosition(sv);
        MemoryHelper.compact();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onSurfaceCreated(GL10 g1, EGLConfig config) {
        g1.glDisable(GL10.GL_DITHER);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDrawFrame(GL10 g1) {
        if (fb != null) {
            fb.clear();
            world.renderScene(fb);
            cam.setPosition(new SimpleVector(x, y, z));

            if (isRotationEnabled && currentRotationVector != null) {
                OrientationUtils.calcDeltaRotation(
                        1.0f, currentRotationVector, previousRotationVector, deltaRotation);
                updateRotation();
                OrientationUtils.copyVectors(currentRotationVector, previousRotationVector);
            } else {
                currentRotationVector = null;
            }

            Log.d(TAG, "camera direction: " + cam.getDirection());
            world.draw(fb);
            fb.display();
        }
    }

    /**
     * Update the camera rotation based on the calculated delta rotation vector
     */
    private void updateRotation() {
        cam.rotateCameraY(-deltaRotation[0]);
        cam.rotateCameraX(deltaRotation[1]);
        cam.rotateCameraZ(deltaRotation[2]);
    }

    /**
     * Set the camera coordinate
     * @param coord The float array to set as the current coordainte
     */
    public void setCameraCoordinate(float[] coord) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Getter for the x coordinate
     * @return x The x coordinate
     */
    public float getX() {
        return x;
    }

    /**
     * Setter for the x coordinate
     * @param x The x coordinate to set
     */
    public void setX(float x) {
        Log.d(TAG, "Setting x: " + x);
        this.x = x;
    }

    /**
     * Getter for the y coordinate
     * @return y The y coordinate
     */
    public float getY() {
        return y;
    }

    /**
     * Setter for the y coordinate
     * @param y The y coordinate to set
     */
    public void setY(float y) {
        Log.d(TAG, "Setting y: " + y);
        this.y = y;
    }

    /**
     * Getter for the z coordinate
     * @return z The z coordinate
     */
    public float getZ() {
        return z;
    }

    /**
     * Setter for the z coordinate
     * @param z The z coordinate to set
     */
    public void setZ(float z) {
        Log.d(TAG, "Setting z: " + z);
        this.z = z;
    }

    /**
     * Getter for the current rotation vector
     * @return currentRotationVector The current rotation vector
     */
    public float[] getCurrentRotationVector() {
        return currentRotationVector;
    }

    /**
     * Setter for the current rotation vector
     * @param currentRotationVector The current rotation vector
     */
    public void setCurrentRotationVector(float[] currentRotationVector) {
        this.currentRotationVector = currentRotationVector;
    }

    /**
     * Getter for the previous rotation vector
     * @return previousRotationVector The previous rotation vector
     */
    public float[] getPreviousRotationVector() {
        return previousRotationVector;
    }

    /**
     * Setter for the previous rotation vector
     * @param previousRotationVector The previous rotation vector
     */
    public void setPreviousRotationVector(float[] previousRotationVector) {
        this.previousRotationVector = previousRotationVector;
    }

    /**
     * Getter for the isRotationEnabled frame
     * @return {@link boolean}
     */
    public boolean isRotationEnabled() {
        return isRotationEnabled;
    }

    /**
     * Setter for the isRotationEnabled flag
     * @param rotationEnabled The isRotationEnabled flag to set
     */
    public void setRotationEnabled(boolean rotationEnabled) {
        isRotationEnabled = rotationEnabled;
    }
}