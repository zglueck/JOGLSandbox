/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package zone.glueck.glstuff;

import com.jogamp.common.nio.Buffers;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.media.opengl.GL3;

/**
 *
 * @author Zach Glueckert <zach.glueckert@missiondrivenresearch.com>
 */
public class GLManager {
    
    public static final int RESTART_INDEX = Short.MAX_VALUE - 1;
    
    public static final String TRANSFORM_MATRIX_UNIFORM = "transform";
    
    public static final String POSITION_ATTRIBUTE_NAME = "position";
    
    public static final String COLOR_ATTRIBUTE_NAME = "color";
    
    public static final String IN_TEXTURE_COORDINATE_NAME = "texcoord";
    
    public static final String OUT_TEXTURE_COORDINATE_NAME = "Texcoord";
    
    public static final String TEXTURE_SAMPLE_ATTRIBUTE = "tex";
    
    public static final String VERTEX_SHADER = 
            "#version 150 core\n"
            + "\n"
            + "uniform mat4 " + TRANSFORM_MATRIX_UNIFORM + ";\n"
            + "\n"
            + "in vec3 " + POSITION_ATTRIBUTE_NAME + ";\n"
            + "in vec3 " + COLOR_ATTRIBUTE_NAME + ";\n"
            + "\n"
            + "out vec3 Color;\n"
            + "\n"
            + "void main() {\n"
            + " gl_Position = " + TRANSFORM_MATRIX_UNIFORM + " * vec4(" + POSITION_ATTRIBUTE_NAME + ", 1.0);\n"
            + " Color = " + COLOR_ATTRIBUTE_NAME + ";\n"
            + "}";
    
    public static final String VERTEX_SHADER_TEXTURE = 
            "#version 150 core\n"
            + "\n"
            + "uniform mat4 " + TRANSFORM_MATRIX_UNIFORM + ";\n"
            + "\n"
            + "in vec2 " + IN_TEXTURE_COORDINATE_NAME + ";\n"
            + "in vec3 " + POSITION_ATTRIBUTE_NAME + ";\n"
            + "in vec3 " + COLOR_ATTRIBUTE_NAME + ";\n"
            + "\n"
            + "out vec3 Color;\n"
            + "out vec2 " + OUT_TEXTURE_COORDINATE_NAME + ";\n"
            + "\n"
            + "void main() {\n"
            + " " + OUT_TEXTURE_COORDINATE_NAME + " = " + IN_TEXTURE_COORDINATE_NAME + ";\n"
            + " gl_Position = " + TRANSFORM_MATRIX_UNIFORM + " * vec4(" + POSITION_ATTRIBUTE_NAME + ", 1.0);\n"
            + " Color = " + COLOR_ATTRIBUTE_NAME + ";\n"
            + "}";
    
    public static final String FRAGMENT_SHADER = 
            "#version 150 core\n"
            + "\n"
            + "in vec3 Color;\n"
            + "out vec4 outColor;\n"
            + "\n"
            + "void main() {\n"
            + " outColor = vec4(Color, 1.0);\n"
            + "}";
    
    public static final String FRAGMENT_SHADER_TEXTURE = 
            "#version 150 core\n"
            + "\n"
            + "in vec3 Color;\n"
            + "in vec2 " + OUT_TEXTURE_COORDINATE_NAME + ";\n"
            + "out vec4 outColor;\n"
            + "\n"
            + "uniform sampler2D " + TEXTURE_SAMPLE_ATTRIBUTE + ";\n"
            + "\n"
            + "void main() {\n"
            + " outColor = texture(tex, Texcoord);\n"
            + "}";
    
    private static final int RESET = -1;
    private static final int[] VAO_ARRAY = new int[1];
    private static final int[] VBO_ARRAY = new int[1];
    
    protected String name;
    
    protected int vao;
    
    protected int program;
    
    protected int vertexCount;
    
    protected int textureName;
    
    protected int uniformLocation = RESET;
    
    protected boolean textures = false;
    
    protected final List<Integer> shaderObjects = new ArrayList<>();
    
    public GLManager(String name) {
        this.name = name;
    }

    public int getVertexCount() {
        return vertexCount;
    }

    public void setVertexCount(int vertexCount) {
        this.vertexCount = vertexCount;
    }
    
    public void bufferData(GL3 gl, float[] vertexData){
        
        generateVertexAttributeObject(gl);
        
        generateBufferObject(gl, GL3.GL_ARRAY_BUFFER);

        FloatBuffer vertexBuffer = Buffers.newDirectFloatBuffer(vertexData);
        vertexBuffer.flip();
        gl.glBufferData(GL3.GL_ARRAY_BUFFER, 
                Buffers.SIZEOF_FLOAT * vertexData.length, 
                vertexBuffer, 
                GL3.GL_STATIC_DRAW);
        
    }
    
    public void bufferElements(GL3 gl, int[] indexes){
        
        generateBufferObject(gl, GL3.GL_ELEMENT_ARRAY_BUFFER);
        
        IntBuffer indexBuffer = Buffers.newDirectIntBuffer(indexes);
        indexBuffer.flip();
        gl.glBufferData(GL3.GL_ELEMENT_ARRAY_BUFFER,
                Buffers.SIZEOF_INT * indexes.length,
                indexBuffer, 
                GL3.GL_STATIC_DRAW);
        
    }
    
    public void addVertexShader(GL3 gl, String[] source){
        
        int vertexShaderName = gl.glCreateShader(GL3.GL_VERTEX_SHADER);
        gl.glShaderSource(vertexShaderName, 1, source, (int[]) null, 0);
        gl.glCompileShader(vertexShaderName);
        checkCompileShaderStatus(gl, vertexShaderName);
        this.shaderObjects.add(vertexShaderName);
        
    }
    
    public void addFragmentShader(GL3 gl, String[] source){
        
        int fragmentShaderName = gl.glCreateShader(GL3.GL_FRAGMENT_SHADER);
        gl.glShaderSource(fragmentShaderName, 1, source, (int[]) null, 0);
        gl.glCompileShader(fragmentShaderName);
        checkCompileShaderStatus(gl, fragmentShaderName);
        this.shaderObjects.add(fragmentShaderName);
        
    }
    
    public void addTexture(GL3 gl, float[] texture){
        
        this.textures = true;
        
        int[] temp = new int[1];
        
        gl.glGenTextures(temp.length, temp, 0);
        
        this.textureName = temp[0];
        
        gl.glActiveTexture(GL3.GL_TEXTURE0);
        gl.glBindTexture(GL3.GL_TEXTURE_2D, this.textureName);
        
        int widthHeight = (int) Math.sqrt(texture.length/3);
        
        FloatBuffer pixels = Buffers.newDirectFloatBuffer(texture);
        //pixels.flip();
        
        gl.glTexImage2D(GL3.GL_TEXTURE_2D, 0, GL3.GL_RGB, widthHeight, widthHeight, 0, GL3.GL_RGB, GL3.GL_FLOAT, pixels);
        
    }
    
    public void createProgram(GL3 gl){
        
        if(this.shaderObjects.size()<2){
            System.out.println("Not enough shaders defined, exiting...");
            System.exit(71);
        }
        
        this.program = gl.glCreateProgram();
        for(int shaderObject : this.shaderObjects){
            gl.glAttachShader(this.program, shaderObject);
        }
        gl.glLinkProgram(this.program);
        checkLinkStatus(gl);
        gl.glValidateProgram(this.program);
        checkValidation(gl);
        
        if(this.textures){
            gl.glUniform1i(gl.glGetUniformLocation(this.program, TEXTURE_SAMPLE_ATTRIBUTE), 0);
        }
        
    }
    
    public void initializeAndRelease(GL3 gl, List<GLManager.ShaderAttribute> attributes){
        
        gl.glUseProgram(this.program);

        if (attributes != null) {

            for (ShaderAttribute attr : attributes) {
                int attribPosition = gl.glGetAttribLocation(this.program, attr.name);
                gl.glVertexAttribPointer(attribPosition, attr.size, attr.type, false, attr.stride, attr.offset);
                gl.glEnableVertexAttribArray(attribPosition);
            }

        }
        
        gl.glBindVertexArray(0);
        gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, 0);
        gl.glUseProgram(0);
        
    }
    
    public void setTransformMatrix(GL3 gl, float[] transform){
        
        if(this.uniformLocation==RESET){
            int location = gl.glGetUniformLocation(this.program, TRANSFORM_MATRIX_UNIFORM);
            if(location<0){
                return;
            } else {
                this.uniformLocation = location;
            }
        }
        
        gl.glUniformMatrix4fv(this.uniformLocation, 1, false, transform, 0);
        
    }
    
    public void useObject(GL3 gl){
        gl.glBindVertexArray(this.vao);
        gl.glUseProgram(this.program);
    }
    
    public void endObject(GL3 gl){
        gl.glBindVertexArray(0);
        gl.glUseProgram(0);
    }
    
    public static class ShaderAttribute {
        
        private final String name;
        private final int size;
        private final int type;
        private int stride = 0;
        private int offset = 0;

        public ShaderAttribute(String name, int size, int type) {
            this.name = name;
            this.size = size;
            this.type = type;
        }

        public int getStride() {
            return stride;
        }

        public void setStride(int stride) {
            this.stride = stride;
        }

        public int getOffset() {
            return offset;
        }

        public void setOffset(int offset) {
            this.offset = offset;
        }
        
    }
    
    private void generateVertexAttributeObject(GL3 gl){
        
        gl.glGenVertexArrays(VAO_ARRAY.length, VAO_ARRAY, 0);
        this.vao = VAO_ARRAY[0];
        Arrays.fill(VAO_ARRAY, RESET);
        gl.glBindVertexArray(this.vao);
        
    }
    
    private void generateBufferObject(GL3 gl, int bufferType){
        
        int vbo = 0;
        
        gl.glGenBuffers(VBO_ARRAY.length, VBO_ARRAY, 0);
        vbo = VBO_ARRAY[0];
        Arrays.fill(VBO_ARRAY, RESET);
        gl.glBindBuffer(bufferType, vbo);
        
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
    
}
