/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package zone.glueck.glstuff;

import gov.nasa.worldwind.geom.Matrix;
import gov.nasa.worldwind.geom.Vec4;

/**
 *
 * @author Zach Glueckert
 */
public class PerspectiveMathCheck {
    
    public static class AMatrix {
        
        public static Matrix perspective(double viewportWidth,
                double viewportHeight, double fovyDegrees, double nearDistance,
                double farDistance) {

            double aspect = viewportWidth / viewportHeight;
            double tanfovy_2 = Math.tan(Math.toRadians(fovyDegrees * 0.5));
            double nearHeight = 2 * nearDistance * tanfovy_2;
            double nearWidth = nearHeight * aspect;
            double near = nearDistance;
            double far = farDistance;
            
            Matrix m = new Matrix(
                    (2 * near)/nearWidth,       0.0,                0.0,                    0.0,
                    0.0,                    (2 * near)/nearHeight,  0.0,                    0.0,
                    0.0,                        0.0,                -(far+near)/(far-near), -2 * (near*far)/(far-near),
                    0.0,                        0.0,                -1.0,                   0.0
            );
          
            return m;
        }

    }
    
    public static class SBMatrix {
        
        public static Matrix perspective(double fovy, double aspect, double n, double f) {
            double q = 1.0f / Math.tan(Math.toRadians(0.5f * fovy));
            double A = q / aspect;
            double B = (n + f) / (n - f);
            double C = (2.0f * n * f) / (n - f);

            Matrix m = new Matrix(A, 0.0f, 0.0f, 0.0f,
                                  0.0f, q, 0.0f, 0.0f,
                                  0.0f, 0.0f, B, C,
                                  0.0f, 0.0f, -1.0, 0.0f);

            return m;
        }
        
    }
    
    public static void main(String[] args) {
        
        double fovyDegrees = 45.0;
        double viewportHeightDelta = 50.0;
        for (int i = 0; i<((int) 600.0/viewportHeightDelta - 1); i++) {
            Vec4[] test = runTest(fovyDegrees, 600.0 - (i * viewportHeightDelta));
            StringBuilder sb = new StringBuilder();
            for (Vec4 t : test) {
                sb.append(t.toString()).append(" | ");
            }
            System.out.println(sb.toString());
        }
        
    }
    
    public static Vec4[] runTest(double fovyDegrees, double viewportHeight) {
        
        // Simple Object
        Vec4 topLeft = new Vec4(-1.0, 1.0, 1.0);
        Vec4 bottomLeft = new Vec4(-1.0, 0.0, 1.0);
        Vec4 bottomRight = new Vec4(1.0, 0.0, 1.0);
        Vec4 topRight = new Vec4(1.0, 1.0, 1.0);
        
        Matrix view = Matrix.fromViewLookAt(Vec4.UNIT_Z.multiply3(10.0), Vec4.ZERO, Vec4.UNIT_Y);
        
        Vec4 topLeftV = topLeft.transformBy4(view);
        Vec4 bottomLeftV = bottomLeft.transformBy4(view);
        Vec4 bottomRightV = bottomRight.transformBy4(view);
        Vec4 topRightV = topRight.transformBy4(view);
        
//        System.out.println(topLeftV);
//        System.out.println(bottomLeftV);
//        System.out.println(bottomRightV);
//        System.out.println(topRightV);
        
        double viewportWidth = 800;
        double nearDistance = 2.0;
        double farDistance = 20.0;
        Matrix aProj = PerspectiveMathCheck.AMatrix.perspective(viewportWidth, 
                viewportHeight, fovyDegrees, nearDistance, farDistance);
        Matrix sProj = PerspectiveMathCheck.SBMatrix.perspective(fovyDegrees, 
                viewportWidth/viewportHeight, nearDistance, farDistance);
        
//        System.out.println("\n\n" + aProj);
//        System.out.println("\n" + sProj);
        
        Matrix aTrans = aProj.multiply(view);
        Matrix sTrans = sProj.multiply(view);
        
        Vec4 topLeftFA = topLeft.transformBy4(aTrans);
        Vec4 topLeftFS = topLeft.transformBy4(sTrans);
        Vec4 bottomLeftFA = bottomLeft.transformBy4(aTrans);
        Vec4 bottomLeftFS = bottomLeft.transformBy4(sTrans);
        
//        System.out.println("\n\n" + topLeftFA);
//        System.out.println(topLeftFS);
//        
//        System.out.println("\n\n" + bottomLeftFA);
//        System.out.println(bottomLeftFS);
        
        return new Vec4[]{topLeftFA, topLeftFS, bottomLeftFA, bottomLeftFS};
    }
    
}
