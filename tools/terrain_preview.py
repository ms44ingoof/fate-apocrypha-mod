import numpy as np, math, sys
from PIL import Image, ImageDraw

class JRandom:
    def __init__(s, seed): s.seed=(seed ^ 0x5DEECE66D) & ((1<<48)-1)
    def nxt(s,bits):
        s.seed=(s.seed*0x5DEECE66D+0xB)&((1<<48)-1)
        v=s.seed>>(48-bits)
        if v>=(1<<(bits-1)) and bits==32: v-=1<<32
        return v
    def nextInt(s,bound):
        r=s.nxt(31); m=bound-1
        if bound&m==0: return (bound*r)>>31
        u=r
        while True:
            r=u%bound
            if u-r+m < (1<<31): return r
            u=s.nxt(31)

class Noise:
    def __init__(s,seed):
        p=list(range(256)); r=JRandom(seed)
        for i in range(255,0,-1):
            j=r.nextInt(i+1); p[i],p[j]=p[j],p[i]
        s.perm=np.array(p+p,dtype=np.int64)
    @staticmethod
    def fade(t): return t*t*t*(t*(t*6-15)+10)
    @staticmethod
    def grad(h,x,y):
        h=h&7
        out=np.where(h==0,x+y,0.0)
        out=np.where(h==1,x-y,out); out=np.where(h==2,-x+y,out); out=np.where(h==3,-x-y,out)
        out=np.where(h==4,x,out); out=np.where(h==5,-x,out); out=np.where(h==6,y,out); out=np.where(h==7,-y,out)
        return out
    def noise(s,x,y):
        xf0=np.floor(x); yf0=np.floor(y); xf=x-xf0; yf=y-yf0
        xi=(xf0.astype(np.int64))&255; yi=(yf0.astype(np.int64))&255
        u=s.fade(xf); v=s.fade(yf); P=s.perm
        aa=P[P[xi]+yi]; ab=P[P[xi]+yi+1]; ba=P[P[xi+1]+yi]; bb=P[P[xi+1]+yi+1]
        x1=s.grad(aa,xf,yf)*(1-u)+s.grad(ba,xf-1,yf)*u
        x2=s.grad(ab,xf,yf-1)*(1-u)+s.grad(bb,xf-1,yf-1)*u
        return (x1*(1-v)+x2*v)*1.1
    def fbm(s,x,y,o):
        sm=0; amp=1; norm=0; f=1
        for _ in range(o):
            sm=sm+s.noise(x*f,y*f)*amp; norm+=amp; amp*=0.5; f*=2
        return np.clip(sm/norm,-1,1)

def smooth(a,b,t):
    x=np.clip((t-a)/(b-a),0,1); return x*x*(3-2*x)
def terrace(h,step):
    f=h/step; fl=np.floor(f); fr=f-fl; return (fl+smooth(0.72,1.0,fr))*step

Y0=72; SEA=63
N={k:Noise(s) for k,s in dict(H=1101,WARP=1102,RIDGE=1103,MASK=1104,MESA=1105,RIVER=1106,COAST=1107,ISLE=1108,TEMP=1109,HUM=1110,DETAIL=1111).items()}
PADS=[(100,100,190,130),(100,298,130,306),(100,398,208,416),(500,350,659,509),(784,784,816,816),(790,890,810,920),(780,980,820,1020),
 (1180,786,1220,814),(1175,870,1225,920),(1180,986,1220,1014),(1172,1072,1228,1128),(1165,1170,1217,1220),
 (1570,1070,1630,1130),(1966,1066,2034,1134),(1966,1266,2034,1334),(2366,1066,2434,1134)]
MARG=16.0; BLEND=46.0
def padDist(x,z):
    best=np.full(x.shape,1e9)
    for (x1,z1,x2,z2) in PADS:
        dx=np.maximum(np.maximum(x1-x,0),x-x2); dz=np.maximum(np.maximum(z1-z,0),z-z2)
        best=np.minimum(best,np.sqrt(dx*dx+dz*dz))
    return best
def padWeight(x,z): return 1-smooth(MARG,MARG+BLEND,padDist(x,z))
def landMask(x,z):
    nx=(x-1250)/2100; nz=(z-700)/1400; d=np.sqrt(nx*nx+nz*nz)
    wob=N['COAST'].fbm(x*0.0016,z*0.0016,3)*0.22+N['COAST'].noise(x*0.012+5,z*0.012)*0.03
    return 1-smooth(0.80+wob,1.02+wob,d)
def riverWeight(x,z):
    rv=np.abs(N['RIVER'].fbm(x*0.0016+300,z*0.0016-120,3))
    return (1-smooth(0.006,0.030,rv))*smooth(0.2,0.6,landMask(x,z))
def mesaWeight(x,z):
    pw=1-smooth(30,210,padDist(x,z))
    return smooth(0.56,0.74,N['MESA'].fbm(x*0.0016-77,z*0.0016+31,2)*0.5+0.5)*(1-pw)
def natural(x,z):
    pw=1-smooth(30,210,padDist(x,z)); land=landMask(x,z)
    h=77-5*pw+N['H'].fbm(x*0.0042,z*0.0042,4)*(12-6*pw)
    wx=x+70*N['WARP'].noise(x*0.002,z*0.002); wz=z+70*N['WARP'].noise(x*0.002+40,z*0.002-17)
    rn=1-np.abs(N['RIDGE'].fbm(wx*0.0034,wz*0.0034,4)); rn=np.power(np.maximum(rn,0),2.7)
    mm=smooth(0.40,0.68,N['MASK'].fbm(x*0.0011+13.7,z*0.0011-9.1,3)*0.5+0.5)
    h=h+rn*175*mm*(1-0.9*pw)+N['DETAIL'].fbm(x*0.03,z*0.03,2)*2.5
    tm=mesaWeight(x,z); h=h*(1-tm)+terrace(h,13.0)*tm
    rw=riverWeight(x,z); h=h*(1-rw)+(SEA-5)*rw
    sea=36+N['H'].fbm(x*0.006,z*0.006,2)*8
    return sea+(h-sea)*smooth(0.02,0.9,land)
def height(x,z):
    w=padWeight(x,z); h=natural(x,z)*(1-w)+Y0*w
    return np.round(np.clip(h,4,300))

if __name__=='__main__':
    step=4; X0,X1,Z0,Z1=-900,2700,-500,1600
    xs=np.arange(X0,X1,step); zs=np.arange(Z0,Z1,step)
    X,Z=np.meshgrid(xs,zs)
    H=height(X.astype(float),Z.astype(float))
    print('height range',H.min(),H.max(),'| land share',float((H>SEA).mean()))
    # раскраска
    img=np.zeros(H.shape+(3,),dtype=np.float64)
    sea=H<=SEA
    depth=np.clip((SEA-H)/40,0,1)
    img[sea]=np.stack([30+30*(1-depth[sea]),80+60*(1-depth[sea]),150+70*(1-depth[sea])],-1)
    land=~sea
    hh=H[land]
    col=np.zeros((hh.size,3))
    t=np.clip((hh-SEA)/190,0,1)
    # пляж, трава, скала, снег
    beach=hh<=SEA+3
    col[:]=np.stack([60+60*t,130-30*t,60+20*t],-1)
    rock=hh>=125; col[rock]=np.stack([120+0*hh[rock],115+0*hh[rock],110+0*hh[rock]],-1)
    snow=hh>=165; col[snow]=[245,245,250]
    col[beach]=[225,210,150]
    img[land]=col
    # рельефное затенение
    gz,gx=np.gradient(H)
    shade=np.clip(1+(-gx*0.6-gz*0.4)*0.08,0.55,1.35)
    img=np.clip(img*shade[...,None],0,255).astype(np.uint8)
    im=Image.fromarray(img)
    d=ImageDraw.Draw(im)
    for (x1,z1,x2,z2) in PADS:
        px1=(x1-X0)/step; pz1=(z1-Z0)/step; px2=(x2-X0)/step; pz2=(z2-Z0)/step
        d.rectangle([px1-3,pz1-3,px2+3,pz2+3],outline=(255,40,40),width=2)
    im.save('map.png')
    print(im.size)
