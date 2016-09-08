// Declare package.
package com.wizzer.mle.title.modeltest;

// Import standard Java classes.
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;

// Import Android classes.
import android.app.Activity;
import android.app.ActivityManager;
import android.content.res.Resources;
import android.content.pm.ConfigurationInfo;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

// Import Magic Lantern Runtime Engine classes.
import com.wizzer.mle.math.MlMath;
import com.wizzer.mle.math.MlRotation;
import com.wizzer.mle.math.MlTransform;
import com.wizzer.mle.math.MlVector3;
import com.wizzer.mle.math.MlVector4;

import com.wizzer.mle.runtime.MleTitle;
import com.wizzer.mle.runtime.ResourceManager;
import com.wizzer.mle.runtime.core.MleRuntimeException;
import com.wizzer.mle.runtime.core.MleSet;
import com.wizzer.mle.runtime.core.MleProp;
import com.wizzer.mle.runtime.core.MleStage;
import com.wizzer.mle.runtime.event.MleEventDispatcher;
import com.wizzer.mle.runtime.event.MleEventManager;
import com.wizzer.mle.runtime.scheduler.MlePhase;
import com.wizzer.mle.runtime.scheduler.MleScheduler;

// Import Magic Lantern J3D Parts classes;
import com.wizzer.mle.parts.MleShutdownCallback;
import com.wizzer.mle.parts.j3d.MleJ3dPlatformData;
import com.wizzer.mle.parts.j3d.props.I3dNodeTypeProperty;
import com.wizzer.mle.parts.actors.MleModelActor;
import com.wizzer.mle.parts.roles.Mle3dRole;
import com.wizzer.mle.parts.stages.Mle3dStage;
import com.wizzer.mle.parts.sets.Mle3dSet;

public class ModelTest extends Activity
{
   // The number of phases in the scheduler.
    private static int NUM_PHASES = 6;
    
    // The width of the Image to display.
    private static int m_width = 320;
    // The height of the Image to display.
    private static int m_height = 480;
    
    // Container for title specific data.
    private MleTitle m_title = null;
    // The name of the model file to display.
    private String m_model = null;
    // The name of the texture map file to use.
    private String m_texture = null;
    // The position of the model
    private MlTransform m_position;
    // The orientation of the model.
    private MlRotation m_orientation;
    // The scale of the model;
    private MlVector3 m_scale;
    // Output tag for logging status messages.
    private static String DEBUG_TAG = "Magic Lantern Model Test";
    
    /**
     * The main loop of execution.
     */
    protected class Mainloop extends Thread
    {
    	/**
    	 * Dispatch Magic Lantern events and process scheduled phases.
    	 * This will continue indefinitely until the application indicates
    	 * that it is Ok to exit via the <code>MleEventManager</code>.
    	 */
    	public void run()
    	{
	        while (! MleEventManager.okToExit())
	        {
	            // Process delayed events.
	            m_title.m_theDispatcher.dispatchEvents();
	        
	            // Run the scheduled phases.
	            m_title.m_theScheduler.run();
	            
                // Attempt to garbage collect.
	            System.gc();
	        }
    	}
    }
    
    // Parse the title resources.
    private boolean parseResources(Resources resources)
    {
        boolean retValue = false;
        
        if (resources != null)
        {
            m_model = resources.getString(R.string.model_box); // ToDo: Set this to a OBJ file.
            retValue = true;
        }
        
        return retValue;
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Check if the system supports OpenGL ES 2.0.
        final ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        final ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();
        final boolean supportsEs2 = configurationInfo.reqGlEsVersion >= 0x20000;

        if (! supportsEs2)
        {
            Log.e(com.wizzer.mle.title.modeltest.ModelTest.DEBUG_TAG, "OpenGL ES 2.0 required to run this title.");
            System.exit(-1);
        }
        
        // Parse the application resources.
        if (! parseResources(getResources()))
        {
        	Log.e(com.wizzer.mle.title.modeltest.ModelTest.DEBUG_TAG, "Unable to parse title resources.");
            System.exit(-1);
        }
        
        // Get a reference to the global title container.
        m_title  = MleTitle.getInstance();
        
        // Initialize the platform specific data.
        MleJ3dPlatformData platformData = new MleJ3dPlatformData();
        platformData.m_width = m_width;
        platformData.m_height = m_height;
        platformData.m_context = this;
        platformData.m_R = com.wizzer.mle.title.modeltest.R.class;
        m_title.m_platformData = platformData;
        
        // Create the event dispatcher.
        MleEventDispatcher manager = new MleEventDispatcher();
        m_title.m_theDispatcher = manager;
        
        //  Create the scheduler.
        MleScheduler scheduler = new MleScheduler(NUM_PHASES);
        MleTitle.g_theActorPhase = new MlePhase("Actor Phase");
        scheduler.addPhase(MleTitle.g_theActorPhase);
        MleTitle.g_thePostActorPhase = new MlePhase("Post Actor Phase");
        scheduler.addPhase(MleTitle.g_thePostActorPhase);
        MleTitle.g_thePreRolePhase = new MlePhase("Pre Role Phase");
        scheduler.addPhase(MleTitle.g_thePreRolePhase);
        MleTitle.g_theRolePhase = new MlePhase("Role Phase");
        scheduler.addPhase(MleTitle.g_theRolePhase);
        MleTitle.g_theSetPhase = new MlePhase("Set Phase");
        scheduler.addPhase(MleTitle.g_theSetPhase);
        MleTitle.g_theStagePhase = new MlePhase("Stage Phase");
        scheduler.addPhase(MleTitle.g_theStagePhase);
        m_title.m_theScheduler = scheduler;
        
        MleEventManager.setExitStatus(false);
     
        // Create a Stage.
        try
        {
        	Mle3dStage theStage = new Mle3dStage();
	        theStage.init();
	        
	        // Set the Activity's View.
	        setContentView(theStage.m_windowView);
	        
        } catch (MleRuntimeException ex)
        {
        	Log.e(com.wizzer.mle.title.modeltest.ModelTest.DEBUG_TAG, "Unable to create and initialize the Stage.");
            System.exit(-1);
        }
    }

    /**
     * Called after onCreate(Bundle) or onStop() when the current activity is now being displayed to the user.
     * It will be followed by onResume(). 
     */
    @Override
    public void onStart()
    {
    	super.onStart();
        
        // Create a Set. The model specified by the Actor will be
        // rendered onto this Set via the Role.
        try
        {
	        Mle3dSet modelSet = new Mle3dSet();
	        modelSet.init();
	        MleSet.setCurrentSet(modelSet);
        } catch (MleRuntimeException ex)
        {
        	Log.e(com.wizzer.mle.title.modeltest.ModelTest.DEBUG_TAG, "Unable to create and initialize the Set.");
            System.exit(-1);
        }

        // Create a model Actor.
        MleModelActor modelActor = new MleModelActor();
        
        // Initialize the Actor's properties. Note that this will usually be done
        // by loading a Group from a Digital Workprint (Rehearsal Player) or the
        // Digital Playprint (Target Player).

        //
        String resourceName = m_model.substring(0, m_model.lastIndexOf('.'));
        try
        {
        	// Validate that the resource, indicated by m_filename, exists.
        	// The id for the resource is extracted from the Android R.raw class using reflection
        	// The name of the id MUST match the value extracted from m_filename (e.g. if the expected
        	// id is R.raw.wwlogo, then the value of m_filename should be wwlogo or wwlogo.gif).
        	int id = ResourceManager.getResourceId(R.raw.class, resourceName);
        	
	        Resources resources = getResources();
	        InputStream in = resources.openRawResource(id);
	        in.close();
        } catch (Resources.NotFoundException ex)
        {
        	Log.e(com.wizzer.mle.title.modeltest.ModelTest.DEBUG_TAG, "Resource, " + resourceName + ", does not exist.");
            System.exit(-1);
        } catch (IOException ex)
        {
        	Log.e(com.wizzer.mle.title.modeltest.ModelTest.DEBUG_TAG, "Unable to access resource, " + resourceName + ", due to IO error.");
            System.exit(-1);        	
        } catch (MleRuntimeException ex)
        {
        	Log.e(com.wizzer.mle.title.modeltest.ModelTest.DEBUG_TAG, ex.getMessage());
        	System.exit(-1);   
        }
        
        try
        {
        	// Use the resource name for the property since we will be pulling the model data
        	// out of the Resource cache.
            int length = resourceName.length();
            byte[] buffer = resourceName.getBytes();
            ByteArrayInputStream input = new ByteArrayInputStream(buffer);
            MleProp modelProp = new MleProp(length, input);
            modelActor.setProperty("model", modelProp);

            // ToDo: Set the 'texture' property on the actor. This demonstration should
            // not require a texture to test Magic Lantern 3D capabilities.

            // Set the 'position' property on the actor.
            byte[] position = createPositionProperty(0.0F, 0.0F, 0.0F);
            MleProp positionProp = new MleProp(position.length, new ByteArrayInputStream(position));
            modelActor.setProperty("position", positionProp);

            // Set the 'orientation' property on the actor.
            byte[] orientation = createOrientationProperty(0.0F, 0.0F, 0.0F, 1.0F);
            MleProp orientationProp = new MleProp(orientation.length, new ByteArrayInputStream(orientation));
            modelActor.setProperty("orientation", orientationProp);

            // Set the 'scale' property on the actor.
            byte[] scale = createScaleProperty(1.0F, 1.0F, 1.0F);
            MleProp scaleProp = new MleProp(scale.length, new ByteArrayInputStream(scale));
            modelActor.setProperty("scale", scaleProp);
        } catch (MleRuntimeException ex)
        {
        	Log.e(com.wizzer.mle.title.modeltest.ModelTest.DEBUG_TAG, "Unable to set property.");
            System.exit(-1);
        }
       
        // Create a 3D Role. This constructor will attach the Role to
        // the specified Actor. It will also be associated with the current Set.
        Mle3dRole modelRole = new Mle3dRole(modelActor, I3dNodeTypeProperty.NodeType.GEOMETRY);
        modelRole.init();

        // Attach the Role to the Set.
        try
        {
            ((Mle3dSet)MleSet.getCurrentSet()).attachRoles(null,modelRole);
        } catch (MleRuntimeException ex)
        {
        	Log.e(com.wizzer.mle.title.modeltest.ModelTest.DEBUG_TAG, "Unable to bind Role to Set.");
            System.exit(-1);            
        }
        
        // Initialize the Actor after it has been properly bound to the Role.
        try
        {
            modelActor.init();
        } catch (MleRuntimeException ex)
        {
        	Log.e(com.wizzer.mle.title.modeltest.ModelTest.DEBUG_TAG, "Unable to initialize Actor.");
            System.exit(-1);            
        }
        
        // Install a callback for exiting the title cleanly.
        try
        {
            MleTitle.getInstance().m_theDispatcher.installEventCB(
                    MleEventManager.MLE_QUIT,new MleShutdownCallback(),null);
        } catch (MleRuntimeException ex)
        {
        	Log.e(com.wizzer.mle.title.modeltest.ModelTest.DEBUG_TAG, "Unable to install shutdown callback.");
            System.exit(-1);            
        }
        
        // Return to Android application life cycle.
    }
    
    @Override
    public void onRestart()
    {
    	super.onRestart();
    	Log.i(MleTitle.DEBUG_TAG, "Received onRestart().");
    }
    
    @Override
    public void onResume()
    {
    	super.onResume();
    	Log.i(MleTitle.DEBUG_TAG, "Received onResume().");

        // The activity must call the GL surface view's onResume() on activity onResume().
        // This is handled indirectly by the 3D Stage because it owns the GLViewSurface.
        MleStage theStage = Mle3dStage.getInstance();
        ((Mle3dStage) theStage).resume();

        // Begin main loop execution.
        Mainloop mainloop = new Mainloop();
        mainloop.start();
    }
      
    @Override
    public void onPause()
    {
    	super.onPause();
    	Log.i(MleTitle.DEBUG_TAG, "Received onPause().");

        // The activity must call the GL surface view's onPause() on activity onPause().
        // This is handled indirectly by the 3D Stage because it owns the GLViewSurface.
        MleStage theStage = Mle3dStage.getInstance();
        ((Mle3dStage) theStage).pause();

    	// Stop the scheduler and event manager.
    	MleEventManager.setExitStatus(true);
    }
    
    @Override
    public void onStop()
    {
    	super.onStop();
    	Log.i(MleTitle.DEBUG_TAG, "Received onStop().");

        // Todo: stop the stage
    }
    
    @Override
    public void onDestroy()
    {
        super.onDestroy();
    	Log.i(MleTitle.DEBUG_TAG, "Received onDestroy().");
    }

    // Convenience utility for packing position parameters into a byte array.
    private byte[] createPositionProperty(float x, float y, float z)
        throws MleRuntimeException
    {
        byte[] property = new byte[12];
        MlVector3 value = new MlVector3(x, y, z);

        try {
            MlMath.convertVector3ToByteArray(0, property, value);
        } catch (IOException ex) {
            throw new MleRuntimeException(ex.getMessage());
        }

        return property;
    }

    // Convenience utility for packing orientation parameters into a byte array.
    private byte[] createOrientationProperty(float x, float y, float z, float w)
        throws MleRuntimeException
    {
        byte[] property = new byte[16];
        MlVector4 value = new MlVector4(x, y, z, w);

        try {
            MlMath.convertVector4ToByteArray(0, property, value);
        } catch (IOException ex) {
            throw new MleRuntimeException(ex.getMessage());
        }

        return property;
    }

    // Convenience utility for packing scale parameters into a byte array.
    private byte[] createScaleProperty(float x, float y, float z)
       throws MleRuntimeException
    {
        byte[] property = new byte[12];
        MlVector3 value = new MlVector3(x, y, z);

        try {
            MlMath.convertVector3ToByteArray(0, property, value);
        } catch (IOException ex) {
            throw new MleRuntimeException(ex.getMessage());
        }

        return property;
    }
}