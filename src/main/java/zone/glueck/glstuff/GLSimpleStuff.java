/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package zone.glueck.glstuff;

import com.jogamp.common.nio.Buffers;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureIO;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Matrix;
import gov.nasa.worldwind.geom.Vec4;
import java.awt.BorderLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import javax.media.opengl.GL3;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 */
public class GLSimpleStuff implements GLEventListener, KeyListener {
    
    public static final int WIDTH = 1000;
    public static final int HEIGHT = 700;
    public static final int FPS = 60;

    private final GLCanvas canvas;
    private final FPSAnimator animator;
    
    private GLSimpleStuff() {
        
        GLProfile glprofile = GLProfile.getMaxProgrammableCore(true);
        System.out.println(glprofile.getName() + " " + glprofile.getGLImplBaseClassName() + " " + glprofile.getImplName());
        GLCapabilities glcapabilities = new GLCapabilities( glprofile );
        this.canvas = new GLCanvas(glcapabilities);
        this.canvas.addGLEventListener(this);
        this.animator = new FPSAnimator(this.canvas, FPS);
        
        final JFrame jframe = new JFrame( "Simple GL Proving Grounds" );
        jframe.addWindowListener( new WindowAdapter() {
            public void windowClosing( WindowEvent windowevent ) {
                animator.stop();
                jframe.dispose();
            }
        });
        canvas.addKeyListener(this);
        jframe.getContentPane().add( this.canvas, BorderLayout.CENTER );
        jframe.setSize( WIDTH, HEIGHT );
        jframe.setVisible( true );
        jframe.setLocationRelativeTo(null);
        jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.animator.start();
        
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // nothing
    }

    @Override
    public void keyPressed(KeyEvent e) {

        switch(e.getKeyCode()){
            case KeyEvent.VK_UP:
                this.pitched = this.pitched - this.step;
                break;
            case KeyEvent.VK_DOWN:
                this.pitched = this.pitched + this.step;
                break;
            case KeyEvent.VK_LEFT:
                this.yaw = this.yaw - this.step;
                break;
            case KeyEvent.VK_RIGHT:
                this.yaw = this.yaw + this.step;
                break;
            case KeyEvent.VK_A:
                if (this.zoom>=10.0) {
                    this.zoom = this.zoom - this.step;
                }
                break;
            case KeyEvent.VK_Z:
                this.zoom = this.zoom + this.step;
                break;
            case KeyEvent.VK_Q:
                this.fillEnabled = !this.fillEnabled;
                break;
        }

    }
    
    // World Movement
    protected double step = 5.0;
    protected double pitched = 0.0;
    protected double yaw = 0.0;
    protected double zoom = 60.0;
    protected boolean fillEnabled = true;
    
    // Single Shader Program
    private int program;
    private int posAttrLoc;
    private int colAttrLoc;
    private int texAttrLoc;
    private int mvpUniLoc;
    private int sampUniLoc;
    private int tmatUniLoc;
    
    private static final String VERT = 
        "#version 150 core\n" +
        "in vec3 position;\n" +
        "in vec3 color;\n" +
        "in vec2 texcoord;\n" +
        "uniform mat4 mvp;\n" +
        "uniform mat4 texMat;\n" +
        "out vec3 Color;\n" +
        "out vec2 Texcoord;\n" +
        "void main()\n" +
        "{\n" +
        "    Color = color;\n" +
        "    vec4 texTransformed = texMat * vec4(texcoord.x, texcoord.y, 0.0, 0.0);\n" +
        "    Texcoord = vec2(texTransformed.x, texTransformed.y);\n" +
        "    gl_Position = mvp * vec4(position, 1.0);\n" +
        "}";
    
    private static final String FRAG = 
        "#version 150 core\n" +
        "in vec3 Color;\n" +
        "in vec2 Texcoord;\n" +
        "out vec4 outColor;\n" +
        "uniform sampler2D tex;\n" +
        "void main()\n" +
        "{\n" +
        "    outColor = vec4(Color, 1.0) * texture(tex, Texcoord);\n" +
        "}";
    
    // Background Starscape
    private int vboStarscape;
    private Texture textureStarscape;
    
    // Simple Icon
    private int vboSimpleIcon;
    private Texture textureSimpleIcon;
    
    // Simple Icon Border
    private int vboSimpleIconBorder;
    
    @Override
    public void keyReleased(KeyEvent e) {
        // nothing
    }
    
//    private static final String gpuVertShaderSource = 
//        "#version 150 core\n" +
//        "in vec3 position;\n" +
//        "in vec3 color;\n" +
//        "in vec3 diffVector;\n" +
//        "uniform mat4 mvp;\n" +
//        "uniform mat4 mv;\n" +
//        "out vec3 Color;\n" +
//        "out vec2 Texcoord;\n" +
//        "float calcTexCoord(vec3 source, mat4 mv) {\n" +
//        "   vec4 vector = mv * vec4(source, 1.0);\n" +
//        "   return sqrt(vector.x * vector.x + vector.y * vector.y)/2.0;\n" +
//        "}\n" +
//        "void main()\n" +
//        "{\n" +
//        "    Color = color;\n" +
//        "    Texcoord = vec2(calcTexCoord(diffVector, mv), 0.0);\n" +
//        "    gl_Position = mvp * vec4(position, 1.0);\n" +
//        "}";
//
//    private static final String gpuFragShaderSource = 
//        "#version 150 core\n" +
//        "in vec3 Color;\n" +
//        "in vec2 Texcoord;\n" +
//        "out vec4 outColor;\n" +
//        "uniform sampler2D tex;\n" +
//        "void main()\n" +
//        "{\n" +
//        "    outColor = vec4(Color, 1.0) * texture(tex, Texcoord);\n" +
//        "}";
//    
//    private static final String cpuVertShaderSource = 
//        "#version 150 core\n" +
//        "in vec3 position;\n" +
//        "in vec3 color;\n" +
//        "in float lintextcoord;\n" +
//        "uniform mat4 mvp;\n" +
//        "out vec3 Color;\n" +
//        "out vec2 Texcoord;\n" +
//        "void main()\n" +
//        "{\n" +
//        "    Color = color;\n" +
//        "    Texcoord = vec2(lintextcoord, 0.0);\n" +
//        "    gl_Position = mvp * vec4(position, 1.0);\n" +
//        "}";
//
//    private static final String cpuFragShaderSource = 
//        "#version 150 core\n" +
//        "in vec3 Color;\n" +
//        "in vec2 Texcoord;\n" +
//        "out vec4 outColor;\n" +
//        "uniform sampler2D tex;\n" +
//        "void main()\n" +
//        "{\n" +
//        "    outColor = vec4(Color, 1.0) * texture(tex, Texcoord);\n" +
//        "}";
//    
//    private static final String vertShaderSource = 
//        "#version 150 core\n" +
//        "in vec3 position;\n" +
//        "in vec3 color;\n" +
//        "in vec2 texcoord;\n" +
//        "uniform mat4 mvp;\n" +
//        "uniform float eyeScalingFactor;\n" +
//        "out vec3 Color;\n" +
//        "out vec2 Texcoord;\n" +
//        "void main()\n" +
//        "{\n" +
//        "    Color = color;\n" +
//        "    Texcoord = vec2(texcoord.x * eyeScalingFactor, 0.0);\n" +
//        "    gl_Position = mvp * vec4(position, 1.0);\n" +
//        "}";
//    
//    private static final String fragShaderSource = 
//        "#version 150 core\n" +
//        "in vec3 Color;\n" +
//        "in vec2 Texcoord;\n" +
//        "out vec4 outColor;\n" +
//        "uniform sampler2D tex;\n" +
//        "void main()\n" +
//        "{\n" +
//        "    outColor = vec4(Color, 1.0) * texture(tex, Texcoord);\n" +
//        "}";
    
    @Override
    public void init(GLAutoDrawable glad) {
        
        GL3 gl = glad.getGL().getGL3();

        // Buffer Setup
        int[] a = new int[3];
        gl.glGenBuffers(a.length, a, 0);
        
        this.vboStarscape = a[0];
        gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, this.vboStarscape);
        float[] starScape = {
            -100f, 60f, -20f, 1f, 1f, 1f, 0f, 0f,
            -100f, -60f, -20f, 1f, 1f, 1f, 0f, 1f,
            100f, 60f, -20f, 1f, 1f, 1f, 1f, 0f,
            100f, -60f, -20f, 1f, 1f, 1f, 1f, 1f,
        };
        FloatBuffer bufferStarscape = Buffers.newDirectFloatBuffer(starScape);
        bufferStarscape.flip();
        gl.glBufferData(GL3.GL_ARRAY_BUFFER, starScape.length * Buffers.SIZEOF_FLOAT, bufferStarscape, GL3.GL_STATIC_DRAW);
        
        this.vboSimpleIcon = a[1];
        gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, this.vboSimpleIcon);
        float[] simpleIcon = {
            -10f, 10f, 0f, 1f, 1f, 1f, 0f, 0f,
            -10f, -10f, 0f, 1f, 1f, 1f, 0f, 10f,
            10f, 10f, 0f, 1f, 1f, 1f, 10f, 0f,
            10f, -10f, 0f, 1f, 1f, 1f, 10f, 10f
        };
        FloatBuffer bufferSimpleIcon = Buffers.newDirectFloatBuffer(simpleIcon);
        bufferSimpleIcon.flip();
        gl.glBufferData(GL3.GL_ARRAY_BUFFER, simpleIcon.length * Buffers.SIZEOF_FLOAT, bufferSimpleIcon, GL3.GL_STATIC_DRAW);
        
        this.vboSimpleIconBorder = a[2];
        gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, this.vboSimpleIconBorder);
        float[] simpleIconBorder = {
            simpleIcon[0], simpleIcon[1], simpleIcon[2], 1f, 0f, 0f, 0f, 0f,
            simpleIcon[8], simpleIcon[9], simpleIcon[10], 1f, 0f, 0f, 5f, 0f,
            simpleIcon[24], simpleIcon[25], simpleIcon[26], 1f, 0f, 0f, 0f, 0f,
            simpleIcon[16], simpleIcon[17], simpleIcon[18], 1f, 0f, 0f, 5f, 0f
        };
        FloatBuffer bufferSimpleIconBorder = Buffers.newDirectFloatBuffer(simpleIconBorder);
        bufferSimpleIconBorder.flip();
        gl.glBufferData(GL3.GL_ARRAY_BUFFER, simpleIconBorder.length * Buffers.SIZEOF_FLOAT, bufferSimpleIconBorder, GL3.GL_STATIC_DRAW);
        
        gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, 0);
        
        // Program Setup
        int v = gl.glCreateShader(GL3.GL_VERTEX_SHADER);
        gl.glShaderSource(v, 1, new String[]{VERT}, null, 0);
        gl.glCompileShader(v);
        this.checkCompileShaderStatus(gl, v);
        
        int f = gl.glCreateShader(GL3.GL_FRAGMENT_SHADER);
        gl.glShaderSource(f, 1, new String[]{FRAG}, null, 0);
        gl.glCompileShader(f);
        this.checkCompileShaderStatus(gl, f);
        
        this.program = gl.glCreateProgram();
        gl.glAttachShader(this.program, v);
        gl.glAttachShader(this.program, f);
        gl.glBindFragDataLocation(this.program, 0, "outColor");
        gl.glLinkProgram(this.program);
        this.checkLinkStatus(gl);
        gl.glUseProgram(this.program);
        gl.glValidateProgram(this.program);
        this.checkValidation(gl);
        
        this.posAttrLoc = gl.glGetAttribLocation(this.program, "position");
        this.colAttrLoc = gl.glGetAttribLocation(this.program, "color");
        this.texAttrLoc = gl.glGetAttribLocation(this.program, "texcoord");
        this.mvpUniLoc = gl.glGetUniformLocation(this.program, "mvp");
        this.sampUniLoc = gl.glGetUniformLocation(this.program, "tex");
        this.tmatUniLoc = gl.glGetUniformLocation(this.program, "texMat");
        
        // Texture Setup
        try {
            this.textureStarscape = TextureIO.newTexture(this.getClass().getResourceAsStream("/star_background.png"), false, ".png");
            if (this.textureStarscape == null) {
                System.err.println("null texture");
                System.exit(3);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(1);
        } catch (GLException ex) {
            ex.printStackTrace();
            System.exit(2);
        }
        this.textureStarscape.setTexParameteri(gl, GL3.GL_TEXTURE_WRAP_S, GL3.GL_MIRRORED_REPEAT);
        this.textureStarscape.setTexParameteri(gl, GL3.GL_TEXTURE_WRAP_T, GL3.GL_MIRRORED_REPEAT);
        this.textureStarscape.setTexParameteri(gl, GL3.GL_TEXTURE_MIN_FILTER, GL3.GL_LINEAR);
        this.textureStarscape.setTexParameteri(gl, GL3.GL_TEXTURE_MAG_FILTER, GL3.GL_LINEAR);
        
        try {
            this.textureSimpleIcon = TextureIO.newTexture(this.getClass().getResourceAsStream("/4x4_White_Checkerboard.png"), false, ".png");
            if (this.textureSimpleIcon == null) {
                System.err.println("null texture");
                System.exit(3);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(1);
        } catch (GLException ex) {
            ex.printStackTrace();
            System.exit(2);
        }
        this.textureSimpleIcon.setTexParameteri(gl, GL3.GL_TEXTURE_WRAP_S, GL3.GL_REPEAT);
        this.textureSimpleIcon.setTexParameteri(gl, GL3.GL_TEXTURE_WRAP_T, GL3.GL_REPEAT);
        this.textureSimpleIcon.setTexParameteri(gl, GL3.GL_TEXTURE_MIN_FILTER, GL3.GL_NEAREST);
        this.textureSimpleIcon.setTexParameteri(gl, GL3.GL_TEXTURE_MAG_FILTER, GL3.GL_NEAREST);
        
    }

    @Override
    public void dispose(GLAutoDrawable glad) {
        
    }
    double cameraRotationRadians = 0.0;
    double modelRotationRadians = 0.0;
    double iniDist = -Double.MAX_VALUE;
    @Override
    public void display(GLAutoDrawable glad) {
        
        // Matrix Setup
        Vec4 eyePoint = new Vec4(0.0, 0.0, this.zoom);
        Matrix modelStarscape = Matrix.IDENTITY;
        Matrix modelSimpleIcon = Matrix.fromRotationXYZ(Angle.fromDegrees(this.pitched), Angle.fromDegrees(this.yaw), Angle.ZERO);
        Matrix modelView = Matrix.fromViewLookAt(eyePoint, Vec4.ZERO, Vec4.UNIT_Y);
        Matrix projection = Matrix.fromPerspective(Angle.fromDegrees(45.0), WIDTH, HEIGHT, 1.0, 1000.0);
        Matrix mvpStarscape = projection.multiply(modelView.multiply(modelStarscape));
        Matrix mvpSimpleIcon = projection.multiply(modelView.multiply(modelSimpleIcon));
        float[] mvpFStarscape = mTof(mvpStarscape);
        float[] mvpFSimpleIcon = mTof(mvpSimpleIcon);
        
        // Standard GL Setup
        GL3 gl = glad.getGL().getGL3();
        gl.glClear(GL3.GL_COLOR_BUFFER_BIT | GL3.GL_DEPTH_BUFFER_BIT);
        gl.glEnable(GL3.GL_DEPTH_TEST);
//        gl.glEnable(GL3.GL_PRIMITIVE_RESTART);
        gl.glEnable(GL3.GL_BLEND);
        gl.glBlendEquation(GL3.GL_FUNC_ADD);
        gl.glBlendFunc(GL3.GL_SRC_ALPHA, GL3.GL_ONE_MINUS_SRC_ALPHA);
//        gl.glPrimitiveRestartIndex(GLManager.RESTART_INDEX);
        
        // First Draw
        //gl.glBindVertexArray(this.vao);
        gl.glUseProgram(this.program);
        gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, this.vboStarscape);
        
        gl.glEnableVertexAttribArray(this.posAttrLoc);
        gl.glVertexAttribPointer(this.posAttrLoc, 3, GL3.GL_FLOAT, false, 8 * Buffers.SIZEOF_FLOAT, 0);
        
        gl.glEnableVertexAttribArray(this.texAttrLoc);
        gl.glVertexAttribPointer(this.texAttrLoc, 2, GL3.GL_FLOAT, false, 8 * Buffers.SIZEOF_FLOAT, 6 * Buffers.SIZEOF_FLOAT);
        
        gl.glEnableVertexAttribArray(this.colAttrLoc);
        gl.glVertexAttribPointer(this.colAttrLoc, 3, GL3.GL_FLOAT, false, 8 * Buffers.SIZEOF_FLOAT, 3 * Buffers.SIZEOF_FLOAT);
        
        gl.glActiveTexture(GL3.GL_TEXTURE0);
        this.textureStarscape.enable(gl);
        this.textureStarscape.bind(gl);

        gl.glUniform1i(this.sampUniLoc, 0);
        gl.glUniformMatrix4fv(this.mvpUniLoc, 1, true, mvpFStarscape, 0);
        gl.glUniformMatrix4fv(this.tmatUniLoc, 1, true, mTof(Matrix.IDENTITY), 0);
        
        gl.glDrawArrays(GL3.GL_TRIANGLE_STRIP, 0, 4);
        //gl.glDrawElements(GL3.GL_LINE_STRIP, 4, GL3.GL_UNSIGNED_INT, 0);
        
        this.textureStarscape.disable(gl);
        
        // Texture Matrix Modifications
        double depthValue = 60.0/eyePoint.getLength3();
        Matrix textureScale = Matrix.fromScale(depthValue);
        
        if (this.fillEnabled) {
            // Second Draw - SimpleIcon
            gl.glUseProgram(this.program);
            gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, this.vboSimpleIcon);

            gl.glEnableVertexAttribArray(this.posAttrLoc);
            gl.glVertexAttribPointer(this.posAttrLoc, 3, GL3.GL_FLOAT, false, 8 * Buffers.SIZEOF_FLOAT, 0);

            gl.glEnableVertexAttribArray(this.texAttrLoc);
            gl.glVertexAttribPointer(this.texAttrLoc, 2, GL3.GL_FLOAT, false, 8 * Buffers.SIZEOF_FLOAT, 6 * Buffers.SIZEOF_FLOAT);

            gl.glEnableVertexAttribArray(this.colAttrLoc);
            gl.glVertexAttribPointer(this.colAttrLoc, 3, GL3.GL_FLOAT, false, 8 * Buffers.SIZEOF_FLOAT, 3 * Buffers.SIZEOF_FLOAT);

            gl.glActiveTexture(GL3.GL_TEXTURE0);
            this.textureSimpleIcon.enable(gl);
            this.textureSimpleIcon.bind(gl);

            gl.glUniform1i(this.sampUniLoc, 0);
            gl.glUniformMatrix4fv(this.mvpUniLoc, 1, true, mvpFSimpleIcon, 0);
            gl.glUniformMatrix4fv(this.tmatUniLoc, 1, true, mTof(textureScale), 0);

            gl.glDrawArrays(GL3.GL_TRIANGLE_STRIP, 0, 4);
            //gl.glDrawElements(GL3.GL_LINE_STRIP, 4, GL3.GL_UNSIGNED_INT, 0);

            this.textureSimpleIcon.disable(gl);
        }
        
        // Third Draw - SimpleIconBorder
        gl.glUseProgram(this.program);
        gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, this.vboSimpleIconBorder);
        
        gl.glEnableVertexAttribArray(this.posAttrLoc);
        gl.glVertexAttribPointer(this.posAttrLoc, 3, GL3.GL_FLOAT, false, 8 * Buffers.SIZEOF_FLOAT, 0);
        
        gl.glEnableVertexAttribArray(this.texAttrLoc);
        gl.glVertexAttribPointer(this.texAttrLoc, 2, GL3.GL_FLOAT, false, 8 * Buffers.SIZEOF_FLOAT, 6 * Buffers.SIZEOF_FLOAT);
        
        gl.glEnableVertexAttribArray(this.colAttrLoc);
        gl.glVertexAttribPointer(this.colAttrLoc, 3, GL3.GL_FLOAT, false, 8 * Buffers.SIZEOF_FLOAT, 3 * Buffers.SIZEOF_FLOAT);
        
        gl.glActiveTexture(GL3.GL_TEXTURE0);
        this.textureSimpleIcon.enable(gl);
        this.textureSimpleIcon.bind(gl);
        
        // Texture Matrix Modifications
        // double depthValue = 60.0/eyePoint.getLength3();
        // Matrix textureScale = modelView.multiply(modelSimpleIcon.multiply(Matrix.fromScale(depthValue)));

        gl.glUniform1i(this.sampUniLoc, 0);
        gl.glUniformMatrix4fv(this.mvpUniLoc, 1, true, mvpFSimpleIcon, 0);
        gl.glUniformMatrix4fv(this.tmatUniLoc, 1, true, mTof(textureScale), 0);
        
        gl.glDrawArrays(GL3.GL_LINE_LOOP, 0, 4);
        //gl.glDrawElements(GL3.GL_LINE_STRIP, 4, GL3.GL_UNSIGNED_INT, 0);
        
        this.textureSimpleIcon.disable(gl);
    }

    @Override
    public void reshape(GLAutoDrawable glad, int i, int i1, int i2, int i3) {
        
    }
    
    private float[] mTof(Matrix m) {
        double[] vals = new double[16];
        m.toArray(vals, 0, true);
        float[] fvals = new float[16];
        for (int i = 0; i<16; i++) {
            fvals[i] = (float) vals[i];
        }
        return fvals;
    }
        
    private void checkCompileShaderStatus(GL3 gl, int shaderObject){
        
        int[] iv = new int[1];
        byte[] bv = new byte[1024];
        
        gl.glGetShaderiv(shaderObject, GL3.GL_COMPILE_STATUS, iv, 0);
        
        if(iv[0]==GL3.GL_FALSE){
            System.out.println("shader " + shaderObject + " failed to compile");
            
            gl.glGetShaderInfoLog(shaderObject, bv.length, iv, 0, bv, 0);
            
            System.out.println("shader log report:\n" +
                    new String(bv, StandardCharsets.UTF_8));
            
            System.exit(shaderObject);
            
        }
        
        
    }
    
    private void checkLinkStatus(GL3 gl){
        
        int[] iv = new int[1];
        byte[] bv = new byte[1024];
        
        gl.glGetProgramiv(program, GL3.GL_LINK_STATUS, iv, 0);
        
        if(iv[0]==GL3.GL_FALSE){
            
            System.out.println("program " + program + " failed to link");
            
            gl.glGetProgramInfoLog(program, bv.length, iv, 0, bv, 0);
            
            System.out.println(
                    "link log report:\n" +
                    new String(bv, StandardCharsets.UTF_8));
            
        }
        
    }
    
    private void checkValidation(GL3 gl){
        
        int[] iv = new int[1];
        byte[] bv = new byte[1024];
        
        gl.glGetProgramiv(program, GL3.GL_VALIDATE_STATUS, iv, 0);
        
        if(iv[0]==GL3.GL_FALSE){
            
            System.out.println("program " + program + " failed to validate");
            
            gl.glGetProgramInfoLog(program, bv.length, iv, 0, bv, 0);
            
            System.out.println(
                    "validate log report:\n" +
                    new String(bv, StandardCharsets.UTF_8));
            
        }
        
    }

    public static void main(String[] args) {
        
        SwingUtilities.invokeLater(() -> {
            new GLSimpleStuff();
        });
        
    }
    
}