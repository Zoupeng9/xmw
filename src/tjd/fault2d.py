"""
Demonstrate 2d fault processing
Author: Xinming Wu, University of Texas at Austin
Version: 2017.05.05
"""
import sys

from java.awt import *
from java.io import *
from java.nio import *
from java.lang import *
from javax.swing import *

from edu.mines.jtk.awt import *
from edu.mines.jtk.dsp import *
from edu.mines.jtk.io import *
from edu.mines.jtk.mosaic import *
from edu.mines.jtk.util import *
from edu.mines.jtk.util.ArrayMath import *

from hv import *
from mef import *
from util import *

n1,n2= 800,5181
s1,s2=Sampling(n1),Sampling(n2)
seismicDir = "../../../data/seis/tj/yk/"
# Names and descriptions of image files used below.
fxfile  = "cosl-3d-iline2011-tmig-wxm-test.pc" # migrated image 
gxfile  = "seisSub" # migrated image 
gffile  = "gf"
smfile  = "sm" # migrated image 
pkfile  = "pk" # picked velocity 
epfile  = "ep" # eigenvalue-derived planarity
p2file  = "p2" # inline slopes
flfile  = "fl" # fault likelihood
ftfile  = "ft" # fault dip (theta)
fltfile = "flt" # fault likelihood thinned
fttfile = "ftt" # fault dip thinned


# These parameters control the scan over fault strikes and dips.
# See the class FaultScanner for more information.
minTheta,maxTheta = 70,85
sigmaTheta = 60

# These parameters control the construction of fault skins.
# See the class FaultSkinner for more information.
lowerLikelihood = 0.2
upperLikelihood = 0.6
minSize = 80

minThrow = 0.0
maxThrow = 30.0

# Directory for saved png images. If None, png images will not be saved;
# otherwise, must create the specified directory before running this script.
plotOnly = False
pngDir = "../../../png/tj/yk/"
pngDir = None

# Processing begins here. When experimenting with one part of this demo, we
# can comment out earlier parts that have already written results to files.
def main(args):
  goDataCut()
  goScan()
  goFaultCurve()
  goFaultThrow()
def goDataCut():
  n1,n2= 2000,5181
  fx = readImage(n1,n2,fxfile)
  gx = copy(800,n2,0,0,fx)
  writeImage(gxfile,gx)
  plot2(s1,s2,gx)

def goScan():
  print "goScan ..."
  gx = readImage(n1,n2,gxfile)
  lof = LocalOrientFilterP(4,2)
  ets = lof.applyForTensors(gx)
  lsf = LocalSmoothingFilter()
  ets.setEigenvalues(0.01,1.0)
  lsf.apply(ets,8,gx,gx)
  if not plotOnly:
    gx = FaultScanner2.taper(10,0,gx)
    fs = FaultScanner2(sigmaTheta)
    sig1,sig2,smooth=16.0,2.0,4.0
    fl,ft = fs.scan(minTheta,maxTheta,sig1,sig2,smooth,gx)
    print "fl min =",min(fl)," max =",max(fl)
    print "ft min =",min(ft)," max =",max(ft)
    writeImage(flfile,fl)
    writeImage(ftfile,ft)
  else:
    fl = readImage(n1,n2,flfile)
    ft = readImage(n1,n2,ftfile)
  plot2(s1,s2,gx,g=fl,cmin=0.20,cmax=1,cmap=jetRamp(1.0),
      label="Fault likelihood",png="fl")
  '''
  plot2(s1,s2,gx,g=abs(ft),cmin=minTheta,cmax=maxTheta,cmap=jetFill(1.0),
      label="Fault dip (degrees)",png="ft")
  '''

def goThin():
  print "goThin ..."
  gx = readImage(n1,n2,gxfile)
  if not plotOnly:
    fl = readImage(n1,n2,flfile)
    ft = readImage(n1,n2,ftfile)
    fs = FaultScanner2(sigmaTheta)
    flt,ftt = fs.thin([fl,ft])
    writeImage(fltfile,flt)
    writeImage(fttfile,ftt)
  else:
    flt = readImage(n1,n2,fltfile)
    ftt = readImage(n1,n2,fttfile)
  plot2(s1,s2,gx)
  plot2(s1,s2,gx,g=flt,cmin=0.2,cmax=1,cmap=jetFillExceptMin(1.0))
  plot2(s1,s2,gx,g=abs(ftt),cmin=minTheta,cmax=maxTheta,cmap=jetFill(1.0),
      label="Fault dip (degrees)",png="ft")

def goFaultCurve():
  gx = readImage(n1,n2,gxfile)
  fl = readImage(n1,n2,flfile)
  ft = readImage(n1,n2,ftfile)
  rgf1 = RecursiveGaussianFilter(1)
  rgf2 = RecursiveGaussianFilter(1)
  rgf1.apply0X(fl,fl)
  rgf2.applyX0(fl,fl)
  fc = FaultCurver()
  fc.setMinCurveSize(minSize)
  fc.setGrowLikelihoods(lowerLikelihood,upperLikelihood)
  ps = fc.findPoints([fl,ft])
  cs = fc.findCurves(ps)
  ftt = zerofloat(n1,n2)
  flt = fillfloat(-1000,n1,n2)
  FaultCurve.getFtImage(cs,ftt)
  FaultCurve.getFlImage(cs,flt)
  writeImage(fltfile,flt)
  plot2(s1,s2,gx,g=flt,cmin=0.01,cmax=1,cmap=jetFillExceptMin(1.0))

def goFaultThrow():
  gx = readImage(n1,n2,gxfile)
  fl = readImage(n1,n2,flfile)
  ft = readImage(n1,n2,ftfile)
  fc = FaultCurver()
  fc.setMinCurveSize(minSize)
  fc.setGrowLikelihoods(lowerLikelihood,upperLikelihood)
  rgf1 = RecursiveGaussianFilter(1)
  rgf2 = RecursiveGaussianFilter(2)
  rgf1.apply0X(fl,fl)
  rgf2.applyX0(fl,fl)
  ps = fc.findPoints([fl,ft])
  cc = fc.findCurves(ps)
  ftt = zerofloat(n1,n2)
  flt = zerofloat(n1,n2)
  FaultCurve.getFlsImage(cc,flt)
  lof = LocalOrientFilterP(4,2)
  ets = lof.applyForTensors(gx)
  ets.setEigenvalues(0.01,1.0)
  lsf = LocalSmoothingFilter()
  wp = sub(1,flt);
  wp = pow(wp,10)
  gs = zerofloat(n1,n2)
  lsf.apply(ets,20,wp,gx,gs)
  writeImage("gs",gs)
  lsp = LocalSlopeFinder(8,2,5)
  el = zerofloat(n1,n2)
  p2 = zerofloat(n1,n2)
  lsp.findSlopes(gx,p2,el)
  fcr = FaultCorrelater(gs,p2)
  fcr.setOffset(2)
  fcr.setZeroSlope(False) # True only if we want to show the error
  fcr.computeThrow(cc,minThrow,maxThrow)
  fst = zerofloat(n1,n2)
  FaultCurve.getFsImage(cc,fst)
  fs1 = fillfloat(-1000,n1,n2)
  fs2 = fillfloat(-1000,n1,n2)
  FaultCurve.getFsImage(cc,fs1,fs2)
  writeImage("seis",gx)
  writeImage("slip1",fs1)
  writeImage("slip2",fs2)
  print min(fst)
  print max(fst)
  plot2(s1,s2,gx)
  plot2(s1,s2,gs)
  smark = -999.999
  p1,p2 = fcr.getDipSlips(n1,n2,cc,smark)
  p1,p2 = fcr.interpolateDipSlips([p1,p2],smark)
  gw = fcr.unfault([p1,p2],gx)
  plot2(s1,s2,gw,label="Amplitude",png="gw")
  return cc

def like(x):
  n2 = len(x)
  n1 = len(x[0])
  return zerofloat(n1,n2)

def gain(x):
  g = mul(x,x) 
  ref = RecursiveExponentialFilter(80.0)
  ref.apply1(g,g)
  y = zerofloat(n1,n2)
  div(x,sqrt(g),y)
  return y

#############################################################################
# graphics

gray = ColorMap.GRAY
jet = ColorMap.JET
backgroundColor = Color(0xfd,0xfe,0xff) # easy to make transparent
def jetFill(alpha):
  return ColorMap.setAlpha(ColorMap.JET,alpha)

def jetFillExceptMin(alpha):
  a = fillfloat(alpha,256)
  a[0] = 0.0
  return ColorMap.setAlpha(ColorMap.JET,a)
def bwrNotch(alpha):
  a = zerofloat(256)
  for i in range(len(a)):
    if i<128:
      a[i] = alpha*(128.0-i)/128.0
    else:
      a[i] = alpha*(i-127.0)/128.0
  return ColorMap.setAlpha(ColorMap.BLUE_WHITE_RED,a)


def bwrFillExceptMin(alpha):
  a = fillfloat(alpha,256)
  a[0] = 0.0
  return ColorMap.setAlpha(ColorMap.BLUE_WHITE_RED,a)

def jetRamp(alpha):
  return ColorMap.setAlpha(ColorMap.JET,rampfloat(0.0,alpha/256,256))

def bwrRamp(alpha):
  return ColorMap.setAlpha(ColorMap.BLUE_WHITE_RED,rampfloat(0.0,alpha/256,256))

def grayRamp(alpha):
  return ColorMap.setAlpha(ColorMap.GRAY,rampfloat(0.0,alpha/256,256))

def plotTensors(g,s1,s2,d=None,dscale=1,ne=20,mk=None,cmin=0,cmax=0,png=None):
  sp = SimplePlot(SimplePlot.Origin.UPPER_LEFT)
  sp.setBackground(backgroundColor)
  sp.setHLabel("Lateral position (km)")
  sp.setVLabel("Time (s)")

  sp.setHInterval(1.0)
  sp.setVInterval(1.0)
  sp.setFontSize(24)
  #sp.setFontSizeForPrint(8,240)
  #sp.setFontSizeForSlide(1.0,0.9)
  sp.setSize(423,700)
  pv = sp.addPixels(s1,s2,g)
  pv.setColorModel(ColorMap.GRAY)
  pv.setInterpolation(PixelsView.Interpolation.LINEAR)
  if cmin<cmax:
    pv.setClips(cmin,cmax)
  else:
    pv.setPercentiles(1,99)
  if d:
    tv = TensorsView(s1,s2,d)
    tv.setOrientation(TensorsView.Orientation.X1DOWN_X2RIGHT)
    tv.setLineColor(Color.YELLOW)
    tv.setLineWidth(3)
    if(mk):
      tv.setEllipsesDisplayed(mk)
    else:
      tv.setEllipsesDisplayed(ne)
    tv.setScale(dscale)
    tile = sp.plotPanel.getTile(0,0)
    tile.addTiledView(tv)
  if pngDir and png:
    sp.paintToPng(360,3.3,pngDir+png+".png")
    #sp.paintToPng(720,3.3,pngDir+png+".png")

def plot2(s1,s2,f,g=None,cmin=None,cmax=None,cmap=None,label=None,png=None):
  n2 = len(f)
  n1 = len(f[0])
  f1,f2 = s1.getFirst(),s2.getFirst()
  d1,d2 = s1.getDelta(),s2.getDelta()
  panel = panel2Teapot()
  panel.setHInterval(200.0)
  panel.setVInterval(100.0)
  panel.setHLabel("Inline (sample)")
  panel.setVLabel("Time (sample)")
  #panel.setHInterval(100.0)
  #panel.setVInterval(100.0)
  #panel.setHLabel("Pixel")
  #panel.setVLabel("Pixel")
  if label:
    panel.addColorBar(label)
  else:
    panel.addColorBar()
  panel.setColorBarWidthMinimum(80)
  pv = panel.addPixels(s1,s2,f)
  pv.setInterpolation(PixelsView.Interpolation.LINEAR)
  pv.setColorModel(ColorMap.GRAY)
  pv.setClips(-2,2)
  if g:
    pv = panel.addPixels(s1,s2,g)
    pv.setInterpolation(PixelsView.Interpolation.NEAREST)
    pv.setColorModel(cmap)
    if label:
      panel.addColorBar(label)
    else:
      panel.addColorBar()
  if cmin and cmax:
    pv.setClips(cmin,cmax)
  frame2Teapot(panel,png)
def panel2Teapot():
  panel = PlotPanel(1,1,
    PlotPanel.Orientation.X1DOWN_X2RIGHT)#,PlotPanel.AxesPlacement.NONE)
  return panel
def frame2Teapot(panel,png=None):
  frame = PlotFrame(panel)
  frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
  #frame.setFontSizeForPrint(8,240)
  #frame.setSize(1240,774)
  #frame.setFontSizeForSlide(1.0,0.9)
  frame.setFontSize(12)
  frame.setSize(n2/2,n1)
  frame.setVisible(True)
  if png and pngDir:
    frame.paintToPng(400,3.2,pngDir+png+".png")
  return frame


def readImage(n1,n2,name):
  fileName = seismicDir+name+".dat"
  image = zerofloat(n1,n2)
  ais = ArrayInputStream(fileName,ByteOrder.LITTLE_ENDIAN)
  ais.readFloats(image)
  ais.close()
  return image

def writeImage(name,image):
  fileName = seismicDir+name+".dat"
  aos = ArrayOutputStream(fileName,ByteOrder.LITTLE_ENDIAN)
  aos.writeFloats(image)
  aos.close()
  return image

#############################################################################
# Run the function main on the Swing thread
import sys
class _RunMain(Runnable):
  def __init__(self,main):
    self.main = main
  def run(self):
    self.main(sys.argv)
def run(main):
  SwingUtilities.invokeLater(_RunMain(main)) 
run(main)