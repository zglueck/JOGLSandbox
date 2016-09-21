/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package zone.glueck.glstuff;

import com.jogamp.common.nio.Buffers;
import gov.nasa.worldwind.geom.Matrix;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.media.opengl.GL3;
import static zone.glueck.glstuff.GLManager.COLOR_ATTRIBUTE_NAME;
import static zone.glueck.glstuff.GLManager.POSITION_ATTRIBUTE_NAME;

/**
 *
 * @author Zach Glueckert <zach.glueckert@missiondrivenresearch.com>
 */
public class WorldObject {
    
    protected final String name;
    
    protected final GLManager glManager;
    
    protected final float[] vertexData;
    
    protected final int[] indexes;
    
    protected Matrix modelMatrix = Matrix.IDENTITY;

    public WorldObject(String name, float[] vertexData, int[] indexes) {
        this.name = name;
        this.glManager = new GLManager(name);
        this.vertexData = Arrays.copyOf(vertexData, vertexData.length);
        this.indexes = Arrays.copyOf(indexes, indexes.length);
    }
    
    public void setup(GL3 gl){
        
        this.glManager.bufferData(gl, vertexData);
        this.glManager.bufferElements(gl, indexes);
        this.glManager.addVertexShader(gl, new String[]{GLManager.VERTEX_SHADER});
        this.glManager.addFragmentShader(gl, new String[]{GLManager.FRAGMENT_SHADER});
        this.glManager.createProgram(gl);
        GLManager.ShaderAttribute attr = new GLManager.ShaderAttribute(POSITION_ATTRIBUTE_NAME, 3, GL3.GL_FLOAT);
        attr.setOffset(0);
        attr.setStride(Buffers.SIZEOF_FLOAT * 8);
        List<GLManager.ShaderAttribute> attrs = new ArrayList<>();
        attrs.add(attr);
        attr = new GLManager.ShaderAttribute(COLOR_ATTRIBUTE_NAME, 3, GL3.GL_FLOAT);
        attr.setOffset(Buffers.SIZEOF_FLOAT * 3);
        attr.setStride(Buffers.SIZEOF_FLOAT * 8);
        attrs.add(attr);
        this.glManager.initializeAndRelease(gl, attrs);
        
    }
    
}
