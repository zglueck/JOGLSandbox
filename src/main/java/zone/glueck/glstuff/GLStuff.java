/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package zone.glueck.glstuff;

import com.jogamp.newt.event.WindowEvent;
import com.jogamp.opengl.util.FPSAnimator;
import gov.nasa.worldwind.geom.Matrix;
import gov.nasa.worldwind.geom.Vec4;
import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import javax.media.opengl.GL3;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import static zone.glueck.glstuff.GLManager.RESTART_INDEX;

/**
 *
 * @author Zach Glueckert
 */
public class GLStuff implements GLEventListener {
    
    public static final int WIDTH = 800;
    public static final int HEIGHT = 600;
    public static final int FPS = 60;

    private final GLCanvas canvas;
    private final FPSAnimator animator;
    private final DiagnosticSceneController sceneController;
    
    private GLStuff() {
        
        GLProfile glprofile = GLProfile.getMaxProgrammableCore(true);
        System.out.println(glprofile.getName() + " " + glprofile.getGLImplBaseClassName() + " " + glprofile.getImplName());
        GLCapabilities glcapabilities = new GLCapabilities( glprofile );
        this.canvas = new GLCanvas(glcapabilities);
        this.canvas.addGLEventListener(this);
        this.animator = new FPSAnimator(this.canvas, FPS);
        
        final JFrame jframe = new JFrame( "One Triangle Swing GLCanvas" ); 
        jframe.addWindowListener( new WindowAdapter() {
            public void windowClosing( WindowEvent windowevent ) {
                animator.stop();
                jframe.dispose();
            }
        });

        jframe.getContentPane().add( this.canvas, BorderLayout.CENTER );
        jframe.setSize( WIDTH, HEIGHT );
        jframe.setVisible( true );
        jframe.setLocationRelativeTo(null);
        jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.animator.start();
        
        /*
        Setup the Scene Controller
        */
        this.sceneController = new DiagnosticSceneController();
        this.sceneController.viewMatrix = Matrix.fromViewLookAt(new Vec4(-10.0,5.0,10.0), Vec4.ZERO, Vec4.UNIT_Y);
        
        /*
        Setup the physical positions of all of the objects.
        */
        
        // Table
        float[] vertexData = {
            -0.5f,  -0.5f,  0.5f,   0f, 1f, 0f, 0f, 0f,
            0f,     0.5f,   0.5f,   1f, 0f, 0f, 0f, 1f,
            0.5f,   -0.5f,  0.5f,   0f, 1f, 0f, 1f, 1f,
            -0.5f,  -0.5f,  -0.5f,  0f, 0f, 1f, 1f, 0f,
            0.5f,   -0.5f,  -0.5f,  0f, 0f, 1f, 0f, 0f,
            -0.5f,  0.5f,   -0.5f,  1f, 0f, 0f, 0f, 1f,
            0.5f,   0.5f,   -0.5f,  1f, 0f, 0f, 1f, 1f
        };
        
//        vertexData = new float[]{
//            -0.5f,  -0.5f,  0.5f,   0f, 0f, 0f, 0f, 0f,
//            0f,     0.5f,   0.5f,   0f, 0f, 0f, 0f, 1f,
//            0.5f,   -0.5f,  0.5f,   0f, 0f, 0f, 1f, 1f,
//            -0.5f,  -0.5f,  -0.5f,  0f, 0f, 0f, 1f, 0f,
//            0.5f,   -0.5f,  -0.5f,  0f, 0f, 0f, 0f, 0f,
//            -0.5f,  0.5f,   -0.5f,  0f, 0f, 0f, 0f, 1f,
//            0.5f,   0.5f,   -0.5f,  0f, 0f, 0f, 1f, 1f
//        };
        
        int[] indexes = {0,2,1,RESTART_INDEX,1,2,6,RESTART_INDEX,2,4,6,RESTART_INDEX,
            1,5,0,RESTART_INDEX,3,0,5,RESTART_INDEX,1,6,5,RESTART_INDEX,4,6,5,RESTART_INDEX,
            5,3,4,RESTART_INDEX,3,0,2,RESTART_INDEX,4,3,2
        };
//        WorldObject table = new WorldObjectTexture("table",vertexData,indexes);
//        table.glManager.setVertexCount(indexes.length);
//        this.sceneController.objects.add(table);
        WorldObject table = new WorldObject("table",vertexData,indexes);
        table.glManager.setVertexCount(indexes.length);
        this.sceneController.objects.add(table);

//        final float[] d = {
//            -1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f,
//            0f, 1f, 0f, 0f, 0f, 0f, 0f, 1f,
//            1f, 0f, 0f, 0f, 0f, 0f, 1f, 1f
//        };
//        final int[] i = {0,1,2};
//        WorldObject picture = new WorldObjectTexture("picture", d, i);
//        picture.glManager.setVertexCount(i.length);
//        this.sceneController.objects.add(picture);
        
        // Floor
        vertexData = new float[]{
            -1000f, 0f, -1000f, 0.2f, 0.2f, 0.2f,
            1000f, 0f, -1000f, 0.2f, 0.2f, 0.2f,
            0f, 0f, 1000f, 0.2f, 0.2f, 0.2f
        };
        indexes = new int[]{
            0,1,2
        };
        WorldObject floor = new WorldObject("floor",vertexData,indexes);
        floor.glManager.setVertexCount(indexes.length);
        this.sceneController.objects.add(floor);
        
    }

    @Override
    public void init(GLAutoDrawable glad) {
        
        GL3 gl = glad.getGL().getGL3();
        
        this.sceneController.setup(gl);
        
    }

    @Override
    public void dispose(GLAutoDrawable glad) {
        
    }

    @Override
    public void display(GLAutoDrawable glad) {
        
        GL3 gl = glad.getGL().getGL3();
        
        gl.glEnable(GL3.GL_DEPTH_TEST);
        gl.glEnable(GL3.GL_PRIMITIVE_RESTART);
        gl.glPrimitiveRestartIndex(GLManager.RESTART_INDEX);
        gl.glClear(GL3.GL_COLOR_BUFFER_BIT | GL3.GL_DEPTH_BUFFER_BIT);
        
        this.sceneController.render(gl);
        
    }

    @Override
    public void reshape(GLAutoDrawable glad, int i, int i1, int i2, int i3) {
        
    }
    
    public static void main(String[] args) {
        
        SwingUtilities.invokeLater(() -> {
            new GLStuff();
        });
        
    }
    
}
