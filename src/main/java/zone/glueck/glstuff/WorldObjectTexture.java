/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package zone.glueck.glstuff;

import com.jogamp.common.nio.Buffers;
import java.util.ArrayList;
import java.util.List;
import javax.media.opengl.GL3;
import static zone.glueck.glstuff.GLManager.COLOR_ATTRIBUTE_NAME;
import static zone.glueck.glstuff.GLManager.POSITION_ATTRIBUTE_NAME;

/**
 *
 * @author Zach Glueckert <zach.glueckert@missiondrivenresearch.com>
 */
public class WorldObjectTexture extends WorldObject {
    
    public static final float[] SIMPLE_TEXTURE = {
        1f, 0f, 0f, 0f, 1f, 0f,
        0f, 0f, 1f, 1f, 1f, 1f
    };
    
    public WorldObjectTexture(String name, float[] vertexData, int[] indexes) {
        super(name, vertexData, indexes);
    }

    @Override
    public void setup(GL3 gl) {
        
        this.glManager.bufferData(gl, vertexData);
        this.glManager.bufferElements(gl, indexes);
        this.glManager.addTexture(gl, SIMPLE_TEXTURE);
        this.glManager.addVertexShader(gl, new String[]{GLManager.VERTEX_SHADER_TEXTURE});
        this.glManager.addFragmentShader(gl, new String[]{GLManager.FRAGMENT_SHADER_TEXTURE});
        this.glManager.createProgram(gl);
        
        int stride = 8 * Buffers.SIZEOF_FLOAT;
        
        GLManager.ShaderAttribute attr = new GLManager.ShaderAttribute(POSITION_ATTRIBUTE_NAME, 3, GL3.GL_FLOAT);
        attr.setOffset(0);
        attr.setStride(stride);
        List<GLManager.ShaderAttribute> attrs = new ArrayList<>();
        attrs.add(attr);
        attr = new GLManager.ShaderAttribute(COLOR_ATTRIBUTE_NAME, 3, GL3.GL_FLOAT);
        attr.setOffset(Buffers.SIZEOF_FLOAT * 3);
        attr.setStride(stride);
        attrs.add(attr);
        attr = new GLManager.ShaderAttribute(GLManager.IN_TEXTURE_COORDINATE_NAME, 2, GL3.GL_FLOAT);
        attr.setOffset(Buffers.SIZEOF_FLOAT * 6);
        attr.setStride(stride);
        attrs.add(attr);
        this.glManager.initializeAndRelease(gl, attrs);
        
    }
    
}
