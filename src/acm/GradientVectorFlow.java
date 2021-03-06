/****************************************************************************
Copyright (c) 2013, Colorado School of Mines and others. All rights reserved.
This program and accompanying materials are made available under the terms of
the Common Public License - v1.0, which accompanies this distribution, and is 
available at http://www.eclipse.org/legal/cpl-v10.html
****************************************************************************/
package acm;

import edu.mines.jtk.dsp.*;
import edu.mines.jtk.util.*;
import static edu.mines.jtk.util.ArrayMath.*;

import ipfx.*;

/**
 * Compute gradient vector flow from a given image.
 * <p>
 * Reference: Snakes, Shapes, and Gradient Vector Flow by Chenyang Xu, 1998
 * The gradient vector flow in this class is efficiently solved using a CG method.
 * @author Xinming Wu, Colorado School of Mines
 * @version 2015.06.22
 */

public class GradientVectorFlow {

   /**
   * Sets half-widths for smoothings in all dimensions.
   * These smoothings serve as a preconditioner; they accelerate convergence
   * of the iterative solver used to compute mappings.
   * @param sigma half-width for smoothing in all dimensions, in samples.
   */
  public void setSmoothing(double sigma) {
    _sigma = (float)sigma;
  }

  /**
   * Sets parameters that control the number of solver iterations.
   * @param small stop iterations when error norm is reduced by this fraction.
   * @param niter maximum number of solver iterations.
   */
  public void setIterations(double small, int niter) {
    _small = (float)small;
    _niter = niter;
  }

  public void setScale(double scale) {
    _scale = (float)scale;
  }

  public float[][][] applyForGradient(float sigma, float[][] fx) {
    int n2 = fx.length;
    int n1 = fx[0].length;
    float[][] g1 = new float[n2][n1];
    float[][] g2 = new float[n2][n1];
    float[][] gs = new float[n2][n1];
    RecursiveGaussianFilter rgf = new RecursiveGaussianFilter(sigma);
    rgf.apply10(fx,g1);
    rgf.apply01(fx,g2);
    for (int i2=0; i2<n2; ++i2) {
    for (int i1=0; i1<n1; ++i1) {
      float g1i = g1[i2][i1];
      float g2i = g2[i2][i1];
      gs[i2][i1] = g1i*g1i+g2i*g2i;
    }}
    return new float[][][]{g1,g2,gs};
  }

  public float[][][][] applyForGradient(float sigma, float[][][] fx) {
    int n3 = fx.length;
    int n2 = fx[0].length;
    int n1 = fx[0][0].length;
    float[][][] g1 = new float[n3][n2][n1];
    float[][][] g2 = new float[n3][n2][n1];
    float[][][] g3 = new float[n3][n2][n1];
    float[][][] gs = new float[n3][n2][n1];
    RecursiveGaussianFilter rgf = new RecursiveGaussianFilter(sigma);
    rgf.apply100(fx,g1);
    rgf.apply010(fx,g2);
    rgf.apply001(fx,g3);
    for (int i3=0; i3<n3; ++i3) {
    for (int i2=0; i2<n2; ++i2) {
    for (int i1=0; i1<n1; ++i1) {
      float g1i = g1[i3][i2][i1];
      float g2i = g2[i3][i2][i1];
      float g3i = g3[i3][i2][i1];
      gs[i3][i2][i1] = g1i*g1i+g2i*g2i+g3i*g3i;
    }}}
    return new float[][][][]{g1,g2,g3,gs};
  }


  public float[][][][] getGradient(int n1, int n2, int n3, FaultCell[] fcs) {
    float[][][] g1 = new float[n3][n2][n1];
    float[][][] g2 = new float[n3][n2][n1];
    float[][][] g3 = new float[n3][n2][n1];
    float[][][] gs = fillfloat(0.1f,n1,n2,n3);
    for (FaultCell fci:fcs) {
      int[] im = fci.getIm();
      int[] ip = fci.getIp();
      float[] us = fci.getW();
      int i1m = im[0]; 
      int i2m = im[1];
      int i3m = im[2];
      int i1p = ip[0];
      int i2p = ip[1];
      int i3p = ip[2];
      i2m = max(i2m,0);
      i3m = max(i3m,0);
      i2p = max(i2p,0);
      i3p = max(i3p,0);
      i2m = min(i2m,n2-1);
      i3m = min(i3m,n3-1);
      i2p = min(i2p,n2-1);
      i3p = min(i3p,n3-1);
      g1[i3m][i2m][i1m] = us[0];
      g2[i3m][i2m][i1m] = us[1];
      g3[i3m][i2m][i1m] = us[2];
      g1[i3p][i2p][i1p] = us[0];
      g2[i3p][i2p][i1p] = us[1];
      g3[i3p][i2p][i1p] = us[2];
      gs[i3m][i2m][i1m] = fci.getFl();
      gs[i3p][i2p][i1p] = fci.getFl();
    }
    return new float[][][][]{g1,g2,g3,gs};
  }



  public float[][][] applyForGVF(
    float[][] u1, float[][] u2, float[][] wp) 
  {
    int n2 = u1.length;
    int n1 = u1[0].length;
    float[][][] v = new float[2][n2][n1];
    v[0] = linearSolver(u1,wp);
    v[1] = linearSolver(u2,wp);
    return v;
  }

  public float[][][][] applyForGVF(
    float[][][] g1, float[][][] g2, float[][][] g3, float[][][] wp) 
  {
    int n3 = g1.length;
    int n2 = g1[0].length;
    int n1 = g1[0][0].length;
    float[][][][] v = new float[3][n3][n2][n1];
    v[0] = linearSolver(g1,wp);
    v[1] = linearSolver(g2,wp);
    v[2] = linearSolver(g3,wp);
    return v;
  }

  public float[][] linearSolver(float[][] u, float[][] wp) {
    int n2 = u.length;
    int n1 = u[0].length;
    float[][] r = new float[n2][n1];
    float[][] b = new float[n2][n1];
    Smoother smoother = new Smoother(_sigma);
    VecArrayFloat2 vb = new VecArrayFloat2(b);
    VecArrayFloat2 vr = new VecArrayFloat2(r);
    CgSolver cs = new CgSolver(_small,_niter);
    A2 a2 = new A2(_scale,smoother,wp);
    makeRhs(wp,u,b);
    smoother.applyTranspose(b);
    cs.solve(a2,vb,vr);
    smoother.apply(r);
    return r;
  }

  public float[][][] linearSolver(float[][][] u, float[][][] wp) {
    int n3 = u.length;
    int n2 = u[0].length;
    int n1 = u[0][0].length;
    float[][][] r = new float[n3][n2][n1];
    float[][][] b = new float[n3][n2][n1];
    Smoother smoother = new Smoother(_sigma);
    VecArrayFloat3 vb = new VecArrayFloat3(b);
    VecArrayFloat3 vr = new VecArrayFloat3(r);
    CgSolver cs = new CgSolver(_small,_niter);
    A3 a3 = new A3(_scale,smoother,wp);
    makeRhs(wp,u,b);
    smoother.applyTranspose(b);
    cs.solve(a3,vb,vr);
    smoother.apply(r);
    return r;
  }


  ////////////////////////////////////////////////////////////////
  //private

  private float _scale = 1.0f;
  private float _sigma = 8.0f;
  private float _small = 0.01f;
  private int _niter = 200;

  private static class A2 implements CgSolver.A {
    A2(float scale, Smoother smoother, float[][] wp) {
      _wp = wp;
      _scale = scale;
      _smoother = smoother;
    }
    public void apply(Vec vx, Vec vy) {
      VecArrayFloat2 v2x = (VecArrayFloat2)vx;
      VecArrayFloat2 v2y = (VecArrayFloat2)vy;
      float[][] x = v2x.getArray();
      float[][] y = v2y.getArray();
      float[][] z = copy(x);
      v2y.zero();
      applyLhs(_scale,_wp,z,y);
      _smoother.applyTranspose(y);
    }

    private float _scale;
    private Smoother _smoother;
    private float[][] _wp=null;
  }

  private static class A3 implements CgSolver.A {
    A3(float scale, Smoother smoother, float[][][] wp) {
      _wp = wp;
      _scale = scale;
      _smoother = smoother;
    }
    public void apply(Vec vx, Vec vy) {
      VecArrayFloat3 v3x = (VecArrayFloat3)vx;
      VecArrayFloat3 v3y = (VecArrayFloat3)vy;
      float[][][] x = v3x.getArray();
      float[][][] y = v3y.getArray();
      float[][][] z = copy(x);
      v3y.zero();
      applyLhs(_scale,_wp,z,y);
      _smoother.applyTranspose(y);
    }

    private float _scale;
    private Smoother _smoother;
    private float[][][] _wp=null;
  }


  // Smoother used as a preconditioner. 
  private static class Smoother {
    public Smoother(float sigma) {
      _sigma = sigma;
    }

    public void apply(float[][] x) {
      smooth(_sigma,x);
    }

    public void apply(float[][][] x) {
      smooth(_sigma,x);
    }

    public void applyTranspose(float[][] x) {
      smooth(_sigma,x);
    }

    public void applyTranspose(float[][][] x) {
      smooth(_sigma,x);
    }
    private float _sigma;
  }

  private static void smooth(float sigma, float[][] x) {
    if (sigma<=0.0f)
      return;
    RecursiveExponentialFilter.Edges edges =
      RecursiveExponentialFilter.Edges.OUTPUT_ZERO_SLOPE;
    RecursiveExponentialFilter ref = new RecursiveExponentialFilter(sigma);
    ref.setEdges(edges);
    ref.apply(x,x);
  }

  private static void smooth(float sigma, float[][][] x) {
    if (sigma<=0.0f)
      return;
    RecursiveExponentialFilter.Edges edges =
      RecursiveExponentialFilter.Edges.OUTPUT_ZERO_SLOPE;
    RecursiveExponentialFilter ref = new RecursiveExponentialFilter(sigma);
    ref.setEdges(edges);
    ref.apply(x,x);
  }


  private static void applyLaplace(
    final float[][] x, final float[][] y) {
    zero(y);
    int n2 = x.length;
    int n1 = x[0].length;
    for (int i2=1; i2<n2; ++i2) {
      for (int i1=1; i1<n1; ++i1) {
        float xa = 0.0f;
        float xb = 0.0f;
        xa += x[i2  ][i1  ];
        xb -= x[i2  ][i1-1];
        xb += x[i2-1][i1  ];
        xa -= x[i2-1][i1-1];
        float x1 = xa+xb;
        float x2 = xa-xb;
        x1 *= 0.25f;
        x2 *= 0.25f;
        float ya = x1+x2;
        float yb = x1-x2;
        y[i2  ][i1  ] += ya;
        y[i2  ][i1-1] -= yb;
        y[i2-1][i1  ] += yb;
        y[i2-1][i1-1] -= ya;
      }
    }

  }
  private static void applyLhs(final float scale,
    final float[][] wp, final float[][] x, final float[][] y)
  {
    zero(y);
    int n2 = x.length;
    int n1 = x[0].length;
    float[][] t = new float[n2][n1];
    applyLaplace(x,t);
    applyLaplace(t,y);
    for (int i2=0; i2<n2; ++i2) {
    for (int i1=0; i1<n1; ++i1) {
      float wpi = (wp!=null)?wp[i2][i1]:1.0f;
      float wps = wpi*wpi;
      y[i2][i1] = scale*y[i2][i1]+wps*x[i2][i1];
    }}
  }

  private static void applyLhs(
    float scale, float[][][] wp, float[][][] x, float[][][] y) {
    int n3 = x.length;
    int n2 = x[0].length;
    int n1 = x[0][0].length;
    float[][][] t = new float[n3][n2][n1];
    applyLaplace(x,t);
    applyLaplace(t,y);
    for (int i3=0; i3<n3; ++i3) {
    for (int i2=0; i2<n2; ++i2) {
    for (int i1=0; i1<n1; ++i1) {
      float wpi = (wp!=null)?wp[i3][i2][i1]:1.0f;
      float wps = wpi*wpi;
      y[i3][i2][i1] = scale*y[i3][i2][i1]+wps*x[i3][i2][i1];
    }}}
  }

  private static void applyLaplace(
    final float[][][] x, final float[][][] y) {
    final int n3 = x.length;
    Parallel.loop(1,n3,2,new Parallel.LoopInt() { // i3 = 1, 3, 5, ...
    public void compute(int i3) {
      applyLhsSlice3(i3,x,y);
    }});
    Parallel.loop(2,n3,2,new Parallel.LoopInt() { // i3 = 2, 4, 6, ...
    public void compute(int i3) {
      applyLhsSlice3(i3,x,y);
    }});
  }

  private static void applyLhsSlice3(
    int i3, float[][][] x, float[][][] y) 
  {
    int n1 = x[0][0].length;
    int n2 = x[0].length;
    for (int i2=1; i2<n2; ++i2) {
      float[] x00 = x[i3  ][i2  ];
      float[] x01 = x[i3  ][i2-1];
      float[] x10 = x[i3-1][i2  ];
      float[] x11 = x[i3-1][i2-1];
      float[] y00 = y[i3  ][i2  ];
      float[] y01 = y[i3  ][i2-1];
      float[] y10 = y[i3-1][i2  ];
      float[] y11 = y[i3-1][i2-1];
      for (int i1=1,i1m=0; i1<n1; ++i1,++i1m) {
        float xa = 0.0f;
        float xb = 0.0f;
        float xc = 0.0f;
        float xd = 0.0f;
        xa += x00[i1 ];
        xd -= x00[i1m];
        xb += x01[i1 ];
        xc -= x01[i1m];
        xc += x10[i1 ];
        xb -= x10[i1m];
        xd += x11[i1 ];
        xa -= x11[i1m];
        float x1 = xa+xb+xc+xd;
        float x2 = xa-xb+xc-xd;
        float x3 = xa+xb-xc-xd;
        x1 *=0.0625f;
        x2 *=0.0625f;
        x3 *=0.0625f;
        float ya = x1+x2+x3;
        float yb = x1-x2+x3;
        float yc = x1+x2-x3;
        float yd = x1-x2-x3;
        y00[i1 ] += ya;
        y00[i1m] -= yd;
        y01[i1 ] += yb;
        y01[i1m] -= yc;
        y10[i1 ] += yc;
        y10[i1m] -= yb;
        y11[i1 ] += yd;
        y11[i1m] -= ya;
      }
    }
  }

  private static void makeRhs(float[][] wp, float[][] u, float[][] y) {
    int n2 = u.length;
    int n1 = u[0].length;
    for (int i2=0; i2<n2; ++i2) {
    for (int i1=0; i1<n1; ++i1) {
      float wpi = wp[i2][i1];
      float wps = wpi*wpi;
      y[i2][i1] = wps*u[i2][i1];
    }}
  }

  private static void makeRhs(float[][][] wp, float[][][] u, float[][][] y) {
    int n3 = u.length;
    int n2 = u[0].length;
    int n1 = u[0][0].length;
    for (int i3=0; i3<n3; ++i3) {
    for (int i2=0; i2<n2; ++i2) {
    for (int i1=0; i1<n1; ++i1) {
      float wpi = wp[i3][i2][i1];
      float wps = wpi*wpi;
      y[i3][i2][i1] = wps*u[i3][i2][i1];
    }}}
  }


}
