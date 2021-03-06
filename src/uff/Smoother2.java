package uff;

import edu.mines.jtk.util.Parallel;
import edu.mines.jtk.dsp.LocalSmoothingFilter;
import edu.mines.jtk.dsp.RecursiveExponentialFilter;
import static edu.mines.jtk.util.ArrayMath.*;

public class Smoother2 {

  public Smoother2(float sigma1, float sigma2, float[][] wp) {
    _wp = copy(wp);
    _sigma1 = sigma1;
    _sigma2 = sigma2;
  }

  public void apply(float[][][] x) {
    int n3 = x.length;
    for (int i3=0; i3<n3; ++i3) {
      smooth1(_sigma1,_wp,x[i3]);
      smooth2(_sigma2,_wp,x[i3]);
      smooth2(_sigma2,_wp,x[i3]);
      smooth1(_sigma1,_wp,x[i3]);
    }
  }

  public void applyOriginal(float[][][] x) {
    int n3 = x.length;
    for (int i3=0; i3<n3; ++i3)
      applyOriginal(x[i3]);
  }

  public void applyOriginal(float[][] x) {
    smooth1(_sigma1,x);
    smooth2(_sigma2,_wp,x); 
  }


  public void applyTranspose(float[][][] x) {
    int n3 = x.length;
    for (int i3=0; i3<n3; ++i3) {
      smooth2(_sigma2,_wp,x[i3]); 
      smooth1(_sigma1,x[i3]);
    }
  }

  ///////////////////////////////////////////////////////////////////////////
  // private
  private float[][] _wp;

  private float _sigma1,_sigma2;

  // Smoothing for dimension 1.
  private static void smooth1(float sigma, float[][] s, float[][] x) {
    if (sigma<1.0f)
      return;
    int n2 = x.length;
    int n1 = x[0].length;
    float c = 0.5f*sigma*sigma;
    float[] st = fillfloat(1.0f,n1);
    float[] xt = zerofloat(n1);
    float[] yt = zerofloat(n1);
    LocalSmoothingFilter lsf = new LocalSmoothingFilter();
    for (int i2=0; i2<n2; ++i2) {
      if (s!=null) {
        for (int i1=0; i1<n1; ++i1)
          st[i1] = s[i2][i1];
      }
      for (int i1=0; i1<n1; ++i1)
        xt[i1] = x[i2][i1];
      lsf.apply(c,st,xt,yt);
      for (int i1=0; i1<n1; ++i1)
        x[i2][i1] = yt[i1];
    }
  }

  // Smoothing for dimension 2.
  private static void smooth2(float sigma, float[][] s, float[][] x) {
    if (sigma<1.0f)
      return;
    float c = 0.5f*sigma*sigma;
    int n1 = x[0].length;
    int n2 = x.length;
    float[] st = fillfloat(1.0f,n2);
    float[] xt = zerofloat(n2);
    float[] yt = zerofloat(n2);
    LocalSmoothingFilter lsf = new LocalSmoothingFilter();
    for (int i1=0; i1<n1; ++i1) {
      if (s!=null) {
        for (int i2=0; i2<n2; ++i2)
          st[i2] = s[i2][i1];
      }
      for (int i2=0; i2<n2; ++i2)
        xt[i2] = x[i2][i1];
      lsf.apply(c,st,xt,yt);
      for (int i2=0; i2<n2; ++i2)
        x[i2][i1] = yt[i2];
    }
  }
  private static void smooth2(
    final float sigma, final float[][][] s, final float[][][] x) 
  {
    final int n3 = x.length;
    Parallel.loop(n3,new Parallel.LoopInt() {
    public void compute(int i3) {
      float[][] s3 = (s!=null)?s[i3]:null;
      float[][] x3 = x[i3];
      smooth2(sigma,s3,x3);
    }});
  }

  // Smoothing for dimension 1.
  private void smooth1(float sigma, float[][] x) {
    new RecursiveExponentialFilter(sigma).apply1(x,x);
  }

}
