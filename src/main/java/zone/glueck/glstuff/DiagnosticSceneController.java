/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package zone.glueck.glstuff;

import gov.nasa.worldwind.View;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Matrix;
import gov.nasa.worldwind.geom.Vec4;
import java.util.ArrayList;
import java.util.List;
import javax.media.opengl.GL3;

/**
 *
 * @author Zach Glueckert <zach.glueckert@missiondrivenresearch.com>
 */
public class DiagnosticSceneController {
    
    protected final List<WorldObject> objects = new ArrayList<>();
    
    protected Matrix viewMatrix = Matrix.IDENTITY; //Matrix.fromModelLookAt(Vec4.UNIT_NEGATIVE_Z.multiply3(15.0), Vec4.ZERO, Vec4.UNIT_Y);
    
    protected Matrix projectionMatrix = Matrix.fromPerspective(Angle.fromDegrees(45.0), (double) GLStuff.WIDTH, (double) GLStuff.HEIGHT, 1.0, 100.0);

    protected Matrix matrix = Matrix.IDENTITY;
    
    double[] dMatrix = new double[16];
    float[] fMatrix = new float[16];
    
    private static final double DELTA = Angle.fromDegrees(360.0/1800.0).getRadians();
    private int cnt = 0;
    private Vec4 camera = new Vec4(0.0,10.0,-20.0);
    
    public void render(GL3 gl){
        
//        Vec4 cam = camera.transformBy3(Matrix.fromRotationY(Angle.fromRadians((DELTA*0.5) * cnt)));
//        this.viewMatrix = Matrix.fromModelLookAt(cam, Vec4.ZERO, Vec4.UNIT_Y);
        
        for(WorldObject object : objects){
            
            if(object.name.equals("table")){
                object.modelMatrix = Matrix.fromRotationX(Angle.fromRadians(DELTA * cnt));
            }
            //matrix = object.modelMatrix.multiply(viewMatrix).multiply(projectionMatrix);
            matrix = projectionMatrix.multiply(viewMatrix).multiply(object.modelMatrix);
            matrix.toArray(dMatrix, 0, false);
            castArray();
            //displayTransformedVertices(object);
            
            object.glManager.useObject(gl);
            object.glManager.setTransformMatrix(gl, fMatrix);
            gl.glDrawElements(GL3.GL_TRIANGLES, object.glManager.getVertexCount(), GL3.GL_UNSIGNED_INT, 0L);
            object.glManager.endObject(gl);
            
        }
        cnt++;
        
    }
    
    public void setup(GL3 gl){
        
        for(WorldObject object : objects){
            object.setup(gl);
        }
        
    }
    
    private void castArray(){
        for(int i = 0; i<dMatrix.length; i++){
            fMatrix[i] = (float) dMatrix[i];
        }
    }
    
    private void displayTransformedVertices(WorldObject object){
        System.out.println(object.name);
        int stride = 6;
        int idx;
        Vec4 vertex;
        for(int i = 0; i<object.indexes.length; i++){

            if(object.indexes[i] == GLManager.RESTART_INDEX){
                continue;
            } else {
                idx = object.indexes[i];
            }
            
            vertex = new Vec4(object.vertexData[idx*stride], object.vertexData[idx*stride+1], object.vertexData[idx*stride+2], 1);
            vertex = vertex.transformBy4(matrix);
            System.out.println(vertex);
        }
    }
    
}
