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
import java.awt.event.WindowAdapter;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import javax.media.opengl.GL2;
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
 *
 * @author Zach Glueckert <zach.glueckert@missiondrivenresearch.com>
 */
public class GLSimpleStuff implements GLEventListener {
    
    public static final int WIDTH = 800;
    public static final int HEIGHT = 600;
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

        jframe.getContentPane().add( this.canvas, BorderLayout.CENTER );
        jframe.setSize( WIDTH, HEIGHT );
        jframe.setVisible( true );
        jframe.setLocationRelativeTo(null);
        jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.animator.start();
        
    }

    //private int vao;
    private int vboBox;
    private int vboFloor;
    private int ebo;
    private int program;
    private int stipScaleLocation;
    private int textureUniformLocation;
    private int mvpLocation;
    private Texture textureImage;
    
    private final float[] box = {
        -10f, -10f, 0f, 1f, 0f, 0f, 1f, 0f,
        10f, -10f, 0f, 0f, 1f, 0f, 1f, 0f,
        10f,  10f,  0f, 0f, 1f, 0f, 0f, 0f,
        -10f,  10f,  0f, 0.1f, 0.2f, 0.8f, 0f, 0f
    };
    
    private final float[] floor = {
        -10.5f, -10.5f, 0f, 1f, 1f, 1f, 0f, 0f,
        10.5f, -10.5f, 0f, 1f, 1f, 1f, 0f, 0f,
        10.5f, 10.5f, 0f, 1f, 1f, 1f, 0f, 0f,
        -10.5f, 10.5f, 0f, 1f, 1f, 1f, 0f, 0f,
    };
    
    private final int[] idx = {
        3,0,2,1
    };

    private final String vertShaderSource = 
        "#version 150 core\n" +
        "in vec3 position;\n" +
        "in vec3 color;\n" +
        "in vec2 texcoord;\n" +
        "uniform mat4 mvp;\n" +
        "out vec3 Color;\n" +
        "out vec2 Texcoord;\n" +
        "void main()\n" +
        "{\n" +
        "    Color = color;\n" +
        "    Texcoord = texcoord;\n" +
        "    gl_Position = mvp * vec4(position, 1.0);\n" +
        "}";
    
    private final String fragShaderSource = 
        "#version 150 core\n" +
        "in vec3 Color;\n" +
        "in vec2 Texcoord;\n" +
        "out vec4 outColor;\n" +
        "uniform float stipScale;\n" +
        "uniform sampler2D tex;\n" +
        "void main()\n" +
        "{\n" +
        //"    vec4 alpha = texture(tex, TexCoord).aaaa;\n" +
        //"    outColor = alpha*vec4(Color, 1.0);\n" +
        "    outColor = vec4(Color, 1.0) * texture(tex, Texcoord * stipScale);\n" +
        //"    outColor = texture(tex, Texcoord);\n" +
        "}";
    
    @Override
    public void init(GLAutoDrawable glad) {
        
        GL3 gl = glad.getGL().getGL3();
        
        int[] a = new int[3];
        
        gl.glGenBuffers(3, a, 0);
        this.vboFloor = a[0];
        gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, this.vboFloor);
        FloatBuffer stippleBuffer = Buffers.newDirectFloatBuffer(floor);
        stippleBuffer.flip();
        gl.glBufferData(GL3.GL_ARRAY_BUFFER, floor.length * Buffers.SIZEOF_FLOAT, stippleBuffer, GL3.GL_STATIC_DRAW);
        
        this.vboBox = a[1];
        gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, this.vboBox);
        FloatBuffer boxBuffer = Buffers.newDirectFloatBuffer(box);
        boxBuffer.flip();
        gl.glBufferData(GL3.GL_ARRAY_BUFFER, box.length * Buffers.SIZEOF_FLOAT, boxBuffer, GL3.GL_STATIC_DRAW);
        
        this.ebo = a[2];
        gl.glBindBuffer(GL3.GL_ELEMENT_ARRAY_BUFFER, this.ebo);
        IntBuffer idxBuffer = Buffers.newDirectIntBuffer(idx);
        idxBuffer.flip();
        gl.glBufferData(GL3.GL_ELEMENT_ARRAY_BUFFER, idx.length * Buffers.SIZEOF_INT, idxBuffer, GL3.GL_STATIC_DRAW);
        
        int v = gl.glCreateShader(GL3.GL_VERTEX_SHADER);
        gl.glShaderSource(v, 1, getVertShader(), null, 0);
        gl.glCompileShader(v);
        this.checkCompileShaderStatus(gl, v);
        
        int f = gl.glCreateShader(GL3.GL_FRAGMENT_SHADER);
        gl.glShaderSource(f, 1, getFragShader(), null, 0);
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
        
        this.stipScaleLocation = gl.glGetUniformLocation(this.program, "stipScale");
        this.mvpLocation = gl.glGetUniformLocation(this.program, "mvp");
        
//        gl.glGenVertexArrays(1, a, 0);
//        this.vao = a[0];
//        gl.glBindVertexArray(this.vao);
        
        
        
        try {
            this.textureImage = TextureIO.newTexture(this.getClass().getResourceAsStream("/4x4_White_Checkerboard.png"), false, ".png");
            if (this.textureImage == null) {
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
        
        this.textureImage.setTexParameteri(gl, GL3.GL_TEXTURE_WRAP_S, GL3.GL_REPEAT);
        this.textureImage.setTexParameteri(gl, GL3.GL_TEXTURE_WRAP_T, GL3.GL_REPEAT);
        this.textureImage.setTexParameteri(gl, GL3.GL_TEXTURE_MIN_FILTER, GL3.GL_NEAREST);
        this.textureImage.setTexParameteri(gl, GL3.GL_TEXTURE_MAG_FILTER, GL3.GL_NEAREST);
//        gl.glGenTextures(1, a, 0);
//        this.texture = a[0];
//        gl.glActiveTexture(GL3.GL_TEXTURE0);
//        gl.glBindTexture(GL3.GL_TEXTURE_2D, this.texture);
//        FloatBuffer textureBuffer = Buffers.newDirectFloatBuffer(this.textureData);
//        textureBuffer.rewind();
//        gl.glTexImage2D(GL3.GL_TEXTURE_2D, 0, GL3.GL_RGBA32F, 2, 2, 0, GL3.GL_RGBA, GL3.GL_FLOAT, textureBuffer);
//        this.textureUniformLocation = gl.glGetUniformLocation(this.program, "tex");
//        gl.glUniform1i(this.textureUniformLocation, 0);
//        gl.glTexParameteri(GL3.GL_TEXTURE_2D, GL3.GL_TEXTURE_WRAP_S, GL3.GL_REPEAT);
//        gl.glTexParameteri(GL3.GL_TEXTURE_2D, GL3.GL_TEXTURE_WRAP_T, GL3.GL_REPEAT);
        
    }

    @Override
    public void dispose(GLAutoDrawable glad) {
        
    }
    double cameraRotationRadians = 0.0;
    double modelRotationRadians = 0.0;
    @Override
    public void display(GLAutoDrawable glad) {
        
        // Matrix Setup
        Vec4 eyePoint = new Vec4(Math.sin(cameraRotationRadians)*(60.0), 5.0, Math.cos(cameraRotationRadians)*(60.0));
        Matrix modelBox = Matrix.fromRotationX(Angle.fromRadians(this.modelRotationRadians));
        Matrix modelStipple = Matrix.IDENTITY;
        //Matrix model = Matrix.IDENTITY;
        Matrix modelView = Matrix.fromViewLookAt(eyePoint, Vec4.ZERO, Vec4.UNIT_Y);
        Matrix projection = Matrix.fromPerspective(Angle.fromDegrees(45.0), WIDTH, HEIGHT, 1.0, 100.0);
        Matrix mvpBox = projection.multiply(modelView.multiply(modelBox));
        Matrix mvpStipple = projection.multiply(modelView.multiply(modelStipple));
        float[] mvpFloat = new float[16];
        double[] mvpDouble = new double[16];
        mvpBox.toArray(mvpDouble, 0, true);
        for(int i = 0; i<16; i++){
            mvpFloat[i] = (float) mvpDouble[i];
        }
        
        // Standard GL Setup
        GL3 gl = glad.getGL().getGL3();
        gl.glClear(GL3.GL_COLOR_BUFFER_BIT | GL3.GL_DEPTH_BUFFER_BIT);
        gl.glEnable(GL3.GL_DEPTH_TEST);
        gl.glEnable(GL3.GL_PRIMITIVE_RESTART);
//        gl.glEnable(GL3.GL_BLEND);
//        gl.glBlendEquation(GL3.GL_FUNC_ADD);
//        gl.glBlendFunc(GL3.GL_SRC_ALPHA, GL3.GL_ONE_MINUS_SRC_ALPHA);
        gl.glPrimitiveRestartIndex(GLManager.RESTART_INDEX);
        
        // First Draw
        //gl.glBindVertexArray(this.vao);
        gl.glUseProgram(this.program);
        gl.glBindBuffer(GL3.GL_ELEMENT_ARRAY_BUFFER, this.ebo);
        gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, this.vboBox);
        
        int posAttrib = gl.glGetAttribLocation(this.program, "position");
        gl.glEnableVertexAttribArray(posAttrib);
        gl.glVertexAttribPointer(posAttrib, 3, GL3.GL_FLOAT, false, 8 * Buffers.SIZEOF_FLOAT, 0);
        
        int texAttrib = gl.glGetAttribLocation(this.program, "texcoord");
        gl.glEnableVertexAttribArray(texAttrib);
        gl.glVertexAttribPointer(texAttrib, 2, GL3.GL_FLOAT, false, 8 * Buffers.SIZEOF_FLOAT, 6 * Buffers.SIZEOF_FLOAT);
        
        int colAttrib = gl.glGetAttribLocation(this.program, "color");
        gl.glEnableVertexAttribArray(colAttrib);
        gl.glVertexAttribPointer(colAttrib, 3, GL3.GL_FLOAT, false, 8 * Buffers.SIZEOF_FLOAT, 3 * Buffers.SIZEOF_FLOAT);
        
        
        gl.glActiveTexture(GL3.GL_TEXTURE0);
        this.textureImage.enable(gl);
        this.textureImage.bind(gl);
        gl.glUniform1i(this.textureUniformLocation, 0);
        gl.glUniform1f(this.stipScaleLocation, 10f);
        gl.glUniformMatrix4fv(this.mvpLocation, 1, true, mvpFloat, 0);
        
        gl.glDrawElements(GL3.GL_LINE_STRIP, 4, GL3.GL_UNSIGNED_INT, 0);
        
        this.textureImage.disable(gl);
        
        // Second Draw
//        //gl.glBindVertexArray(this.vao);
//        gl.glUseProgram(this.program);
//        gl.glBindBuffer(GL3.GL_ELEMENT_ARRAY_BUFFER, this.ebo);
//        gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, this.vboFloor);
//        
//        gl.glEnableVertexAttribArray(posAttrib);
//        gl.glVertexAttribPointer(posAttrib, 3, GL3.GL_FLOAT, false, 8 * Buffers.SIZEOF_FLOAT, 0);
//        
//        gl.glEnableVertexAttribArray(texAttrib);
//        gl.glVertexAttribPointer(texAttrib, 2, GL3.GL_FLOAT, false, 8 * Buffers.SIZEOF_FLOAT, 6 * Buffers.SIZEOF_FLOAT);
//        
//        gl.glEnableVertexAttribArray(colAttrib);
//        gl.glVertexAttribPointer(colAttrib, 3, GL3.GL_FLOAT, false, 8 * Buffers.SIZEOF_FLOAT, 3 * Buffers.SIZEOF_FLOAT);
//        
//        gl.glActiveTexture(GL3.GL_TEXTURE0);
//        this.textureImage.enable(gl);
//        this.textureImage.bind(gl);
//        gl.glUniform1i(this.textureUniformLocation, 0);
//        gl.glUniform1f(this.stipScaleLocation, 1f);
//        mvpStipple.toArray(mvpDouble, 0, true);
//        for(int i = 0; i<16; i++){
//            mvpFloat[i] = (float) mvpDouble[i];
////        }
//        gl.glUniformMatrix4fv(this.mvpLocation, 1, true, mvpFloat, 0);
//        
//        gl.glDrawElements(GL3.GL_LINE_STRIP, 4, GL3.GL_UNSIGNED_INT, 0);
//        
//        this.textureImage.disable(gl);

        // Modify world
        this.cameraRotationRadians += 0.01;
        this.modelRotationRadians +=0.003;
    }

    @Override
    public void reshape(GLAutoDrawable glad, int i, int i1, int i2, int i3) {
        
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
    
    private String[] getVertShader(){
        return loadShader(true);
    }
    
    private String[] getFragShader(){
        return loadShader(false);
    }
    
    private String[] loadShader(boolean vert){
        if(vert)
            return new String[]{this.vertShaderSource};
        else
            return new String[]{this.fragShaderSource};
    }

    
    public static void main(String[] args) {
        
        SwingUtilities.invokeLater(() -> {
            new GLSimpleStuff();
        });
        
    }
    
}