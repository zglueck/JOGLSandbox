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
import java.util.ArrayList;
import java.util.List;
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
    private final JFrame jframe;
    
    private GLSimpleStuff() {
        
        GLProfile glprofile = GLProfile.getGL2GL3();
        System.out.println(glprofile.getName() + " " + glprofile.getGLImplBaseClassName() + " " + glprofile.getImplName());
        GLCapabilities glcapabilities = new GLCapabilities( glprofile );
        this.canvas = new GLCanvas(glcapabilities);
        this.canvas.addGLEventListener(this);
        this.animator = new FPSAnimator(this.canvas, FPS);
        
        jframe = new JFrame( "Simple GL Proving Grounds" );
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
    private int mvUniLoc;
    
    private static final String VERT = 
        "#version 150 core\n" +
        "in vec3 position;\n" +
        "in vec3 color;\n" +
        //"in vec2 texcoord;\n" +
        "uniform mat4 mvp;\n" +
        "uniform mat4 mv;\n" +
        "uniform mat4 texMat;\n" +
        "out vec3 Color;\n" +
        "out vec2 Texcoord;\n" +
        "void main()\n" +
        "{\n" +
        "    Color = color;\n" +
        //"    vec4 texTransformed = texMat * vec4(texcoord.x, texcoord.y, 0.0, 0.0);\n" +
        //"    Texcoord = vec2(texTransformed.x, texTransformed.y);\n" +
        //"    vec4 eyePosition = mv * vec4(position, 1.0);\n" +
        "    vec4 scaledEyePosition = texMat * vec4(position.xy, 0.0, 1.0);\n" +
        "    gl_Position = mvp * vec4(position, 1.0);\n" +
        "    Texcoord = scaledEyePosition.xy;\n" +
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
    
    private static final String TEX_VERT = 
        "#version 150 core\n" +
        "in vec3 position;\n" +
        "in vec2 texcoord;\n" +
        "uniform mat4 mvp;\n" +
        "out vec2 Texcoord;\n" +
        "void main()\n" +
        "{\n" +
        "    gl_Position = mvp * vec4(position, 1.0);\n" +
        "    Texcoord = texcoord;\n" +
        "}";
    
    private static final String TEX_FRAG = 
        "#version 150 core\n" +
        "in vec2 Texcoord;\n" +
        "out vec4 outColor;\n" +
        "uniform sampler2D tex;\n" +
        "void main()\n" +
        "{\n" +
        "    outColor = texture(tex, Texcoord);\n" +
        "}";
    
    private static final String SS_VERT = 
            "#version 150 core\n" +
            "in vec2 bPosition;\n" +
            "out vec3 bColor;\n" +
            "void main() {\n" +
            "   gl_Position = vec4(bPosition, 0.0, 1.0);\n" +
            "   bColor = vec3(0.0, 0.0, 1.0);\n" +
            "}\n";
    
    private static final String SS_FRAG =
            "#version 150 core\n" +
            "in vec3 bColor;\n" +
            "out vec3 outColor;\n" +
            "void main() {\n" +
            "   outColor = bColor;\n" +
            "}\n";
    
    // Background Starscape
    private int vboStarscape;
    private Texture textureStarscape;
    
    // Simple Icon
    private int vboSimpleIcon;
    private Texture textureSimpleIcon;
    
    // Simple Jet Tile
    private int vboJet;
    private Texture textureJet;
    private int programJet;
    private int posJetAttrLoc;
    private int texJetAttrLoc;
    private int mvpJetUniLoc;
    private int framebufferJet;
    
    // Screen Space Crosshairs or Border
    private int vboScreenSpace;
    private int ssProgram;
    private int ssAttribLocation;
    
    @Override
    public void keyReleased(KeyEvent e) {
        // nothing yet
    }
    
    @Override
    public void init(GLAutoDrawable glad) {
        
        GL3 gl = glad.getGL().getGL3();

        // Buffer Setup
        int[] a = new int[4];
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
        List<float[]> verts = new ArrayList<>();
        verts.add(new float[]{0f, 0f, 10f, 1f, 1f, 1f});
        for (int i = 0; i<16; i++){
            float theta = (float) Math.toRadians((360/15) * i);
            float[] vert = new float[6];
            vert[0] = (float) (Math.cos(theta) * 10.0);
            vert[1] = (float) (Math.sin(theta) * 10.0);
            //vert[2] = (float) (Math.cos(Math.sin(i) + Math.cos(i)) * 5f);
            vert[3] = 1f;
            vert[4] = 1f;
            vert[5] = 1f;
            verts.add(vert);
        }
        FloatBuffer bufferSimpleIcon = Buffers.newDirectFloatBuffer(6 * verts.size());
        for(float[] vert : verts){
            bufferSimpleIcon.put(vert);
        }
        bufferSimpleIcon.flip();
        gl.glBufferData(GL3.GL_ARRAY_BUFFER, 6 * verts.size() * Buffers.SIZEOF_FLOAT, bufferSimpleIcon, GL3.GL_STATIC_DRAW);
        
        this.vboJet = a[2];
        gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, this.vboJet);
        float[] jet = {
            //position x3 and texture coordinates x3
            -15f, -15f, -3f, 0f, 0f,
             15f, -15f, -3f, 1f, 0f,
            -15f,  15f, -3f, 0f, 1f,
             15f,  15f, -3f, 1f, 1f
        };
        FloatBuffer jetBuffer = Buffers.newDirectFloatBuffer(jet);
        jetBuffer.flip();
        gl.glBufferData(GL3.GL_ARRAY_BUFFER, jet.length * Buffers.SIZEOF_FLOAT, jetBuffer, GL3.GL_STATIC_DRAW);
        
        this.vboScreenSpace = a[3];
        gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, this.vboScreenSpace);
        float[] ssElement = {
            -0.95f, -0.95f,
            0.95f,  -0.95f,
            0.95f,  0.95f,
            -0.95f, 0.95f
        };
        FloatBuffer bufferScreenSpace = Buffers.newDirectFloatBuffer(ssElement);
        bufferScreenSpace.flip();
        gl.glBufferData(GL3.GL_ARRAY_BUFFER, ssElement.length * Buffers.SIZEOF_FLOAT, bufferScreenSpace, GL3.GL_STATIC_DRAW);
        
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
        this.checkLinkStatus(this.program, gl);
        gl.glUseProgram(this.program);
        gl.glValidateProgram(this.program);
        this.checkValidation(this.program, gl);
        
        this.posAttrLoc = gl.glGetAttribLocation(this.program, "position");
        this.colAttrLoc = gl.glGetAttribLocation(this.program, "color");
        //this.texAttrLoc = gl.glGetAttribLocation(this.program, "texcoord");
        this.mvpUniLoc = gl.glGetUniformLocation(this.program, "mvp");
        this.sampUniLoc = gl.glGetUniformLocation(this.program, "tex");
        this.tmatUniLoc = gl.glGetUniformLocation(this.program, "texMat");
        this.mvUniLoc = gl.glGetUniformLocation(this.program, "mv");
        
        gl.glUseProgram(0);
        v = gl.glCreateShader(GL3.GL_VERTEX_SHADER);
        gl.glShaderSource(v, 1, new String[]{SS_VERT}, null, 0);
        gl.glCompileShader(v);
        this.checkCompileShaderStatus(gl, v);
        
        f = gl.glCreateShader(GL3.GL_FRAGMENT_SHADER);
        gl.glShaderSource(f, 1, new String[]{SS_FRAG}, null, 0);
        gl.glCompileShader(f);
        this.checkCompileShaderStatus(gl, f);
        
        this.ssProgram = gl.glCreateProgram();
        gl.glAttachShader(this.ssProgram, v);
        gl.glAttachShader(this.ssProgram, f);
        gl.glBindFragDataLocation(this.ssProgram, 0, "outColor");
        gl.glLinkProgram(this.ssProgram);
        this.checkLinkStatus(this.ssProgram, gl);
        gl.glUseProgram(this.ssProgram);
        gl.glValidateProgram(this.ssProgram);
        this.checkValidation(this.ssProgram, gl);
        
        this.ssAttribLocation = gl.glGetAttribLocation(this.ssProgram, "bPosition");
        
        gl.glUseProgram(0);
        v = gl.glCreateShader(GL3.GL_VERTEX_SHADER);
        gl.glShaderSource(v, 1, new String[]{TEX_VERT}, null, 0);
        gl.glCompileShader(v);
        this.checkCompileShaderStatus(gl, v);
        
        f = gl.glCreateShader(GL3.GL_FRAGMENT_SHADER);
        gl.glShaderSource(f, 1, new String[]{TEX_FRAG}, null, 0);
        gl.glCompileShader(f);
        this.checkCompileShaderStatus(gl, f);
        
        this.programJet = gl.glCreateProgram();
        gl.glAttachShader(this.programJet, v);
        gl.glAttachShader(this.programJet, f);
        gl.glBindFragDataLocation(this.programJet, 0, "outColor");
        gl.glLinkProgram(this.programJet);
        this.checkLinkStatus(this.programJet, gl);
        gl.glUseProgram(this.programJet);
        gl.glValidateProgram(this.programJet);
        this.checkValidation(this.programJet, gl);
        
        this.posJetAttrLoc = gl.glGetAttribLocation(this.programJet, "position");
        this.texJetAttrLoc = gl.glGetAttribLocation(this.programJet, "texcoord");
        this.mvpJetUniLoc = gl.glGetUniformLocation(this.programJet, "mvp");
        
        gl.glUseProgram(0);
        
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
        
        try {
            this.textureJet = TextureIO.newTexture(this.getClass().getResourceAsStream("/fa-18.png"), false, ".png");
            if (this.textureJet == null) {
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
        this.textureJet.setTexParameteri(gl, GL3.GL_TEXTURE_WRAP_S, GL3.GL_CLAMP_TO_EDGE);
        this.textureJet.setTexParameteri(gl, GL3.GL_TEXTURE_WRAP_T, GL3.GL_CLAMP_TO_EDGE);
        this.textureJet.setTexParameteri(gl, GL3.GL_TEXTURE_MIN_FILTER, GL3.GL_LINEAR);
        this.textureJet.setTexParameteri(gl, GL3.GL_TEXTURE_MAG_FILTER, GL3.GL_LINEAR);
        
        // Framebuffer Setup
        a = new int[1];
        gl.glGenFramebuffers(a.length, a, 0);
        this.framebufferJet = a[0];
        gl.glBindFramebuffer(GL3.GL_FRAMEBUFFER, this.framebufferJet);
        gl.glViewport(0, 0, 2048, 2048);
        gl.glFramebufferTexture2D(GL3.GL_FRAMEBUFFER, GL3.GL_COLOR_ATTACHMENT0, GL3.GL_TEXTURE_2D, this.textureJet.getTextureObject(), 0);
        gl.glBindFramebuffer(GL3.GL_FRAMEBUFFER, 0);
        
        // Get line sizes
        float[] b = new float[2];
        gl.glGetFloatv(GL3.GL_LINE_WIDTH, b, 0);
        this.lineSize = b[0];
        gl.glGetFloatv(GL3.GL_LINE_WIDTH_RANGE, b, 0);
        this.maxLineSize = Math.max(b[0], b[1]);
        
    }

    private float lineSize;
    private float maxLineSize;
    
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
        double height = (this.jframe.getHeight() < 1.0) ? HEIGHT : this.jframe.getHeight();
        double width = (this.jframe.getWidth() < 1.0) ? WIDTH: this.jframe.getWidth();
        //Matrix projection = Matrix.fromPerspective(Angle.fromDegrees(45.0), width, height, 1.0, 1000.0);
        Matrix projection = PerspectiveMathCheck.AMatrix.perspective(width, height, 45.0, 1.0, 1000.0);
        Matrix mvpStarscape = projection.multiply(modelView.multiply(modelStarscape));
        Matrix mvpSimpleIcon = projection.multiply(modelView.multiply(modelSimpleIcon));
        float[] mvpFStarscape = mTof(mvpStarscape);
        float[] mvpFSimpleIcon = mTof(mvpSimpleIcon);
        
        // Standard GL Setup
        GL3 gl = glad.getGL().getGL3();
        gl.glClear(GL3.GL_COLOR_BUFFER_BIT | GL3.GL_DEPTH_BUFFER_BIT);
        gl.glEnable(GL3.GL_DEPTH_TEST);
        gl.glEnable(GL3.GL_BLEND);
        gl.glBlendEquation(GL3.GL_FUNC_ADD);
        gl.glBlendFunc(GL3.GL_SRC_ALPHA, GL3.GL_ONE_MINUS_SRC_ALPHA);
        gl.glLineWidth(this.lineSize);
        
        // First Draw
        gl.glUseProgram(this.program);
        gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, this.vboStarscape);
        
        gl.glEnableVertexAttribArray(this.posAttrLoc);
        gl.glVertexAttribPointer(this.posAttrLoc, 3, GL3.GL_FLOAT, false, 8 * Buffers.SIZEOF_FLOAT, 0);
        
        gl.glEnableVertexAttribArray(this.colAttrLoc);
        gl.glVertexAttribPointer(this.colAttrLoc, 3, GL3.GL_FLOAT, false, 8 * Buffers.SIZEOF_FLOAT, 3 * Buffers.SIZEOF_FLOAT);
        
        gl.glActiveTexture(GL3.GL_TEXTURE0);
        this.textureStarscape.enable(gl);
        this.textureStarscape.bind(gl);

        gl.glUniform1i(this.sampUniLoc, 0);
        gl.glUniformMatrix4fv(this.mvpUniLoc, 1, true, mvpFStarscape, 0);
        gl.glUniformMatrix4fv(this.tmatUniLoc, 1, true, mTof(Matrix.fromScale(0.03)), 0);
        
        gl.glDrawArrays(GL3.GL_TRIANGLE_STRIP, 0, 4);
        
        this.textureStarscape.disable(gl);
        
        // Texture Matrix Modifications
        double depthValue = 60.0/eyePoint.getLength3();
        Matrix textureScale = Matrix.fromScale(depthValue);
        
        if (this.fillEnabled) {
            // Second Draw - SimpleIcon
            gl.glUseProgram(this.program);
            gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, this.vboSimpleIcon);

            gl.glEnableVertexAttribArray(this.posAttrLoc);
            gl.glVertexAttribPointer(this.posAttrLoc, 3, GL3.GL_FLOAT, false, 6 * Buffers.SIZEOF_FLOAT, 0);

            gl.glEnableVertexAttribArray(this.colAttrLoc);
            gl.glVertexAttribPointer(this.colAttrLoc, 3, GL3.GL_FLOAT, false, 6 * Buffers.SIZEOF_FLOAT, 3 * Buffers.SIZEOF_FLOAT);
            
            gl.glActiveTexture(GL3.GL_TEXTURE0);
            this.textureSimpleIcon.enable(gl);
            this.textureSimpleIcon.bind(gl);
            
            gl.glUniform1i(this.sampUniLoc, 0);
            gl.glUniformMatrix4fv(this.mvpUniLoc, 1, true, mvpFSimpleIcon, 0);
            gl.glUniformMatrix4fv(this.tmatUniLoc, 1, true, mTof(Matrix.IDENTITY), 0);
            gl.glUniformMatrix4fv(this.mvUniLoc, 1, true, mTof(modelView.multiply(modelSimpleIcon)), 0);

            gl.glDrawArrays(GL3.GL_TRIANGLE_FAN, 0, 361);

            this.textureSimpleIcon.disable(gl);
        }
        
        // Third Draw - Border on the framebuffer object
        gl.glBindFramebuffer(GL3.GL_FRAMEBUFFER, this.framebufferJet);
        
        gl.glUseProgram(this.ssProgram);
        gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, this.vboScreenSpace);
        gl.glViewport(0, 0, 2048, 2048);
        
        gl.glEnableVertexAttribArray(this.ssAttribLocation);
        gl.glVertexAttribPointer(this.ssAttribLocation, 2, GL3.GL_FLOAT, false, 2 * Buffers.SIZEOF_FLOAT, 0);
        gl.glLineWidth(this.maxLineSize);
        gl.glDrawArrays(GL3.GL_LINE_LOOP, 0, 4);
        gl.glViewport(0, 0, WIDTH, HEIGHT);
        gl.glBindFramebuffer(GL3.GL_FRAMEBUFFER, 0);
        
        // Fourth Draw - Simple Jet Texture
        gl.glUseProgram(this.programJet);
        gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, this.vboJet);
        
        gl.glEnableVertexAttribArray(this.posJetAttrLoc);
        gl.glVertexAttribPointer(this.posJetAttrLoc, 3, GL3.GL_FLOAT, false, 5 * Buffers.SIZEOF_FLOAT, 0);
        
        gl.glEnableVertexAttribArray(this.texJetAttrLoc);
        gl.glVertexAttribPointer(this.texJetAttrLoc, 2, GL3.GL_FLOAT, false, 5 * Buffers.SIZEOF_FLOAT, 3 * Buffers.SIZEOF_FLOAT);
        
        gl.glActiveTexture(GL3.GL_TEXTURE0);
        this.textureJet.enable(gl);
        this.textureJet.bind(gl);
        
        // Texture Matrix Modifications
        // double depthValue = 60.0/eyePoint.getLength3();
        // Matrix textureScale = modelView.multiply(modelSimpleIcon.multiply(Matrix.fromScale(depthValue)));

        gl.glUniform1i(this.sampUniLoc, 0);
        gl.glUniformMatrix4fv(this.mvpJetUniLoc, 1, true, mvpFSimpleIcon, 0);
        
        gl.glDrawArrays(GL3.GL_TRIANGLE_STRIP, 0, 4);
        //gl.glDrawElements(GL3.GL_LINE_STRIP, 4, GL3.GL_UNSIGNED_INT, 0);
        
        this.textureJet.disable(gl);
        
        gl.glUseProgram(this.ssProgram);
        gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, this.vboScreenSpace);
        
        gl.glEnableVertexAttribArray(this.ssAttribLocation);
        gl.glVertexAttribPointer(this.ssAttribLocation, 2, GL3.GL_FLOAT, false, 2 * Buffers.SIZEOF_FLOAT, 0);

        gl.glLineWidth(this.maxLineSize);
        gl.glDrawArrays(GL3.GL_LINE_LOOP, 0, 4);
        
//        GL2 gl2 = glad.getGL().getGL2();
//        
//        gl2.glBegin(GL2.GL_LINES);
//        Vec4 line = new Vec4(20.0, 20.0, 0.0);
//        Vec4 res = line.transformBy4(mvpSimpleIcon);
//        Vec4 cent = Vec4.ZERO.transformBy4(mvpSimpleIcon);
//        gl2.glColor3f(1f, 1f, 1f);
//        gl2.glVertex3f((float) cent.x, (float) cent.y, (float) cent.z);
//        gl2.glColor3f(1f, 1f, 1f);
//        gl2.glVertex3f((float) res.x, (float) res.y, (float) res.z);
//        gl2.glEnd();
    }

    @Override
    public void reshape(GLAutoDrawable glad, int i, int i1, int i2, int i3) {
        double sizeFactor = 2 * Math.tan(Math.toRadians(45.0  * 0.5)) / jframe.getHeight();
        System.out.println("pixelSizeAtDistance: " + (this.zoom * sizeFactor));
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
    
    private void checkLinkStatus(int program, GL3 gl){
        
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
    
    private void checkValidation(int program, GL3 gl){
        
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
        System.out.println(VERT);
        System.out.println("");
        System.out.println(FRAG);
    }
    
}