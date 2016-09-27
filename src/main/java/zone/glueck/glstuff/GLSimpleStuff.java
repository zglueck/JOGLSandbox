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
                this.zoom = this.zoom - this.step;
                break;
            case KeyEvent.VK_Z:
                this.zoom = this.zoom + this.step;
                break;
        }

    }
    protected double step = 5.0;
    protected double pitched = 0.0;
    protected double yaw = 0.0;
    protected double zoom = 60.0;

    @Override
    public void keyReleased(KeyEvent e) {
        // nothing
    }

    //private int vao;
    private int vboBox;
    private int vboFloor;
    private int vboCpu;
    private int ebo;
    private int program;
    private int cpuProgram;
    private int eyeScalingFactorLocation;
    private int textureUniformLocation;
    private int mvpLocation;
    private int cpuMvpLocation;
    private int cpuLinTextCoordLocation;
    private Texture textureImage;
    private double eyeDistance = 60.0;
    
    private final float[] box = {
        -10f,  10f,  0f, 1f, 1f, 1f, 0f, 0f,
        -10f, -10f, 0f, 1f, 1f, 1f, 20f, 0f,
        10f,  10f,  0f, 1f, 1f, 1f,(float) (Math.sqrt(2)*20.0+20.0), 0f,
        10f, -10f, 0f, 1f, 1f, 1f, (float) (Math.sqrt(2)*20.0+40.0), 0f
    };
    
    private final float[] floor = {
        -10.5f,  10f,  -1f, 1f, 0f, 0f, 0f, 0f,
        -10.5f, -10f, -1f, 1f, 0f, 0f, 20f, 0f,
        9.5f,  10f,  -1f, 1f, 0f, 0f, (float) (Math.sqrt(2)*20.0+20.0), 0f,
        9.5f, -10f, -1f, 1f, 0f, 0f, (float) (Math.sqrt(2)*20.0+40.0), 0f
    };
    
    private final Vec4[] cpuPos = new Vec4[]{
        new Vec4(-9.5, 10.0, 1.0),
        new Vec4(-9.5, -10.0, 1.0),
        new Vec4(10.5, 10.0, 1.0),
        new Vec4(10.5, -10.0, 1.0)
    };
    
//    private final int[] idx = {
//        0, 1, 2, 3
//    };
    
    private static final String cpuVertShaderSource = 
        "#version 150 core\n" +
        "in vec3 position;\n" +
        "in vec3 color;\n" +
        "in float lintextcoord;\n" +
        "uniform mat4 mvp;\n" +
        "out vec3 Color;\n" +
        "out vec2 Texcoord;\n" +
        "void main()\n" +
        "{\n" +
        "    Color = color;\n" +
        "    Texcoord = vec2(lintextcoord, 0.0);\n" +
        "    gl_Position = mvp * vec4(position, 1.0);\n" +
        "}";

    private static final String cpuFragShaderSource = 
        "#version 150 core\n" +
        "in vec3 Color;\n" +
        "in vec2 Texcoord;\n" +
        "out vec4 outColor;\n" +
        "uniform sampler2D tex;\n" +
        "void main()\n" +
        "{\n" +
        "    outColor = vec4(Color, 1.0) * texture(tex, Texcoord);\n" +
        "}";
    
    private static final String vertShaderSource = 
        "#version 150 core\n" +
        "in vec3 position;\n" +
        "in vec3 color;\n" +
        "in vec2 texcoord;\n" +
        "uniform mat4 mvp;\n" +
        "uniform float eyeScalingFactor;\n" +
        "out vec3 Color;\n" +
        "out vec2 Texcoord;\n" +
        "void main()\n" +
        "{\n" +
        "    Color = color;\n" +
        "    Texcoord = vec2(texcoord.x/eyeScalingFactor, 0.0);\n" +
        "    gl_Position = mvp * vec4(position, 1.0);\n" +
        "}";
    
    private static final String fragShaderSource = 
        "#version 150 core\n" +
        "in vec3 Color;\n" +
        "in vec2 Texcoord;\n" +
        "out vec4 outColor;\n" +
        "uniform sampler2D tex;\n" +
        "void main()\n" +
        "{\n" +
        "    outColor = vec4(Color, 1.0) * texture(tex, Texcoord);\n" +
        "}";
    
    @Override
    public void init(GLAutoDrawable glad) {
        
        GL3 gl = glad.getGL().getGL3();
        
        int[] a = new int[3];
        
        gl.glGenBuffers(a.length, a, 0);
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
        
//        this.ebo = a[2];
//        gl.glBindBuffer(GL3.GL_ELEMENT_ARRAY_BUFFER, this.ebo);
//        IntBuffer idxBuffer = Buffers.newDirectIntBuffer(idx);
//        idxBuffer.flip();
//        gl.glBufferData(GL3.GL_ELEMENT_ARRAY_BUFFER, idx.length * Buffers.SIZEOF_INT, idxBuffer, GL3.GL_STATIC_DRAW);
        
        this.vboCpu = a[2];
        gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, this.vboCpu);
        gl.glBufferData(GL3.GL_ARRAY_BUFFER, 28 * Buffers.SIZEOF_FLOAT, null, GL3.GL_STREAM_DRAW);
        
        int v = gl.glCreateShader(GL3.GL_VERTEX_SHADER);
        gl.glShaderSource(v, 1, Shader.VERT.getShader(), null, 0);
        gl.glCompileShader(v);
        this.checkCompileShaderStatus(gl, v);
        
        int f = gl.glCreateShader(GL3.GL_FRAGMENT_SHADER);
        gl.glShaderSource(f, 1, Shader.FRAG.getShader(), null, 0);
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
        
        this.eyeScalingFactorLocation = gl.glGetUniformLocation(this.program, "eyeScalingFactor");
        this.mvpLocation = gl.glGetUniformLocation(this.program, "mvp");
        
        v = gl.glCreateShader(GL3.GL_VERTEX_SHADER);
        gl.glShaderSource(v, 1, Shader.C_VERT.getShader(), null, 0);
        gl.glCompileShader(v);
        this.checkCompileShaderStatus(gl, v);
        
        f = gl.glCreateShader(GL3.GL_FRAGMENT_SHADER);
        gl.glShaderSource(f, 1, Shader.C_FRAG.getShader(), null, 0);
        gl.glCompileShader(f);
        this.checkCompileShaderStatus(gl, f);
        
        this.cpuProgram = gl.glCreateProgram();
        gl.glAttachShader(this.cpuProgram, v);
        gl.glAttachShader(this.cpuProgram, f);
        gl.glBindFragDataLocation(this.cpuProgram, 0, "outColor");
        gl.glLinkProgram(this.cpuProgram);
        this.checkLinkStatus(gl);
        gl.glUseProgram(this.cpuProgram);
        gl.glValidateProgram(this.cpuProgram);
        this.checkValidation(gl);
  
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
    double iniDist = -Double.MAX_VALUE;
    @Override
    public void display(GLAutoDrawable glad) {
        
        // Matrix Setup
        Vec4 eyePoint = new Vec4(0.0, 0.0, this.zoom);
        Matrix modelBox = Matrix.fromRotationXYZ(Angle.fromDegrees(this.pitched), Angle.fromDegrees(this.yaw), Angle.ZERO);
        // Matrix modelBox = Matrix.fromRotationX(Angle.fromRadians(this.modelRotationRadians));
        Matrix modelView = Matrix.fromViewLookAt(eyePoint, Vec4.ZERO, Vec4.UNIT_Y);
        Matrix projection = Matrix.fromPerspective(Angle.fromDegrees(45.0), WIDTH, HEIGHT, 1.0, 1000.0);
        Matrix mvpBox = projection.multiply(modelView.multiply(modelBox));
        Matrix mvpStipple = projection.multiply(modelView.multiply(modelBox));
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
        //gl.glBindBuffer(GL3.GL_ELEMENT_ARRAY_BUFFER, this.ebo);
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
        if (this.iniDist==-Double.MAX_VALUE){
            this.iniDist = eyePoint.getLength3()/60.0;
        }
        gl.glUniform1f(this.eyeScalingFactorLocation, (float) (eyePoint.getLength3()/60.0));
        gl.glUniformMatrix4fv(this.mvpLocation, 1, true, mvpFloat, 0);
        
        gl.glDrawArrays(GL3.GL_LINE_STRIP, 0, 4);
        //gl.glDrawElements(GL3.GL_LINE_STRIP, 4, GL3.GL_UNSIGNED_INT, 0);
        
        this.textureImage.disable(gl);
        
        // Second Draw
        //gl.glBindVertexArray(this.vao);
        gl.glUseProgram(this.program);
        //gl.glBindBuffer(GL3.GL_ELEMENT_ARRAY_BUFFER, this.ebo);
        gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, this.vboFloor);
        
        gl.glEnableVertexAttribArray(posAttrib);
        gl.glVertexAttribPointer(posAttrib, 3, GL3.GL_FLOAT, false, 8 * Buffers.SIZEOF_FLOAT, 0);
        
        gl.glEnableVertexAttribArray(texAttrib);
        gl.glVertexAttribPointer(texAttrib, 2, GL3.GL_FLOAT, false, 8 * Buffers.SIZEOF_FLOAT, 6 * Buffers.SIZEOF_FLOAT);
        
        gl.glEnableVertexAttribArray(colAttrib);
        gl.glVertexAttribPointer(colAttrib, 3, GL3.GL_FLOAT, false, 8 * Buffers.SIZEOF_FLOAT, 3 * Buffers.SIZEOF_FLOAT);
        
        gl.glActiveTexture(GL3.GL_TEXTURE0);
        this.textureImage.enable(gl);
        this.textureImage.bind(gl);
        gl.glUniform1i(this.textureUniformLocation, 0);
        gl.glUniform1f(this.eyeScalingFactorLocation, (float) this.iniDist);
        mvpStipple.toArray(mvpDouble, 0, true);
        for(int i = 0; i<16; i++){
            mvpFloat[i] = (float) mvpDouble[i];
        }
        gl.glUniformMatrix4fv(this.mvpLocation, 1, true, mvpFloat, 0);
        
        gl.glDrawArrays(GL3.GL_LINE_STRIP, 0, 4);
        // gl.glDrawElements(GL3.GL_LINE_STRIP, 4, GL3.GL_UNSIGNED_INT, 0);
        
        this.textureImage.disable(gl);
        
        // Third Draw
        //gl.glBindVertexArray(this.vao);
        gl.glUseProgram(this.cpuProgram);
        //gl.glBindBuffer(GL3.GL_ELEMENT_ARRAY_BUFFER, this.ebo);
        // rebuffer the data
        
        float[] data = new float[28];
        for (int i = 0; i<4; i++) {
            data[(i * 7)] = (float) this.cpuPos[i].x;
            data[(i * 7) + 1] = (float) this.cpuPos[i].y;
            data[(i * 7) + 2] = (float) this.cpuPos[i].z;
            data[(i * 7) + 3] = 0f;
            data[(i * 7) + 4] = 1f;
            data[(i * 7) + 5] = 0f;
            if (i == 0) {
                data[(i * 7) + 6] = 0f;    
            } else {
                Vec4 diffVector = this.cpuPos[i].subtract3(this.cpuPos[i - 1]);
                diffVector = diffVector.transformBy4(mvpBox);
                double scaleValue = Math.sqrt(diffVector.x * diffVector.x + diffVector.y * diffVector.y);
                scaleValue = scaleValue * (60.0 / eyePoint.getLength3());
                data[(i * 7) + 6] = (float) (scaleValue * 0.25 + data[((i - 1) * 7) + 6]);
            }
        }
        FloatBuffer cpuBuffer = Buffers.newDirectFloatBuffer(data);
        cpuBuffer.flip();
        gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, this.vboCpu);
        gl.glBufferData(GL3.GL_ARRAY_BUFFER, data.length * Buffers.SIZEOF_FLOAT, cpuBuffer, GL3.GL_STREAM_DRAW);
        
        posAttrib = gl.glGetAttribLocation(this.cpuProgram, "position");
        gl.glEnableVertexAttribArray(posAttrib);
        gl.glVertexAttribPointer(posAttrib, 3, GL3.GL_FLOAT, false, 7 * Buffers.SIZEOF_FLOAT, 0);
        
        colAttrib = gl.glGetAttribLocation(this.cpuProgram, "color");
        gl.glEnableVertexAttribArray(colAttrib);
        gl.glVertexAttribPointer(colAttrib, 3, GL3.GL_FLOAT, false, 7 * Buffers.SIZEOF_FLOAT, 3 * Buffers.SIZEOF_FLOAT);
        
        int linTextCoordLocation = gl.glGetAttribLocation(this.cpuProgram, "lintextcoord");
        gl.glEnableVertexAttribArray(linTextCoordLocation);
        gl.glVertexAttribPointer(linTextCoordLocation, 1, GL3.GL_FLOAT, false, 7 * Buffers.SIZEOF_FLOAT, 6 * Buffers.SIZEOF_FLOAT);
        
        gl.glActiveTexture(GL3.GL_TEXTURE0);
        this.textureImage.enable(gl);
        this.textureImage.bind(gl);
        gl.glUniform1i(this.textureUniformLocation, 0);
        //gl.glUniform1f(this.eyeScalingFactorLocation, (float) this.iniDist);
        gl.glUniformMatrix4fv(this.mvpLocation, 1, true, mvpFloat, 0);
        
        gl.glDrawArrays(GL3.GL_LINE_STRIP, 0, 4);
        // gl.glDrawElements(GL3.GL_LINE_STRIP, 4, GL3.GL_UNSIGNED_INT, 0);
        
        this.textureImage.disable(gl);
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



    enum Shader {
        VERT, FRAG, C_VERT, C_FRAG;
        
        public String[] getShader(){
            switch(this){
                case VERT:
                    return new String[]{vertShaderSource};
                case FRAG:
                    return new String[]{fragShaderSource};
                case C_VERT:
                    return new String[]{cpuVertShaderSource};
                case C_FRAG:
                    return new String[]{cpuFragShaderSource};
            }
            return null;
        }
    }
    
    public static void main(String[] args) {
        
        SwingUtilities.invokeLater(() -> {
            new GLSimpleStuff();
        });
        
    }
    
}