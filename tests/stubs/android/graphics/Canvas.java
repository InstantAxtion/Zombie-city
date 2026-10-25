package android.graphics;
import java.awt.*; import java.awt.geom.*; import java.awt.image.BufferedImage; import java.util.ArrayDeque;
public class Canvas {
 public static long CALLS, BG; public static final java.util.HashMap<String,Integer> COUNTS=new java.util.HashMap<String,Integer>(); public static boolean TRACE; public static final java.util.HashMap<String,Integer> SITES=new java.util.HashMap<String,Integer>();
 static void site(){ if(!TRACE) return; StackTraceElement[] st=new Throwable().getStackTrace(); for(int i=2;i<st.length;i++){ String c=st[i].getClassName(); if(c.contains("zombiesandbox")){ SITES.merge(c.substring(c.lastIndexOf('.')+1)+"."+st[i].getMethodName(),1,Integer::sum); return;} } }
 final Graphics2D g; final ArrayDeque<AffineTransform> stack=new ArrayDeque<>(); final ArrayDeque<Shape> clips=new ArrayDeque<>(); final ArrayDeque<Matrix> pstack=new ArrayDeque<>(); Matrix P;
 static final Graphics2D MG=new BufferedImage(1,1,BufferedImage.TYPE_INT_ARGB).createGraphics();
 static FontMetrics metrics(float ts){return MG.getFontMetrics(new Font("SansSerif",Font.BOLD,Math.round(ts)));}
 public Canvas(Bitmap b){this(b.img.createGraphics());}
 public Canvas(Graphics2D g){this.g=g; g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);}
 void ap(Paint p){ g.setColor(new Color(p.color,true)); g.setStroke(new BasicStroke(p.sw,p.cap==Paint.Cap.ROUND?BasicStroke.CAP_ROUND:p.cap==Paint.Cap.SQUARE?BasicStroke.CAP_SQUARE:BasicStroke.CAP_BUTT,BasicStroke.JOIN_ROUND)); }
 void sh(Shape s,Paint p){ap(p); if(p.style==Paint.Style.FILL) g.fill(s); else g.draw(s);}
 public int getSaveCount(){return stack.size()+1;}
 public void restoreToCount(int n){while(stack.size()+1>n) restore();}
 public int save(){stack.push(g.getTransform()); clips.push(g.getClip()==null?new Rectangle(0,0,100000,100000):g.getClip());pstack.push(P==null?new Matrix():P);return stack.size();}
 public void concat(Matrix mm){P=mm; Matrix c=new Matrix(); c.m=mm.m.clone(); P=c;}
 public void drawPath(Path p,Paint pa){if(Thread.currentThread().getName().startsWith("map")){BG++;} else {CALLS++; site(); COUNTS.merge("drawPath",1,Integer::sum);}sh(p.p,pa);}
 public void drawBitmap(Bitmap bm,Rect s,RectF d,Paint p){if(Thread.currentThread().getName().startsWith("map")){BG++;} else {CALLS++; site(); COUNTS.merge("drawBitmap",1,Integer::sum);}java.awt.Composite oc=g.getComposite(); if(p!=null&&p.alpha<255) g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,p.alpha/255f)); Object oi=g.getRenderingHint(RenderingHints.KEY_INTERPOLATION); if(p!=null&&p.filter) g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR); g.drawImage(bm.img,Math.round(d.left),Math.round(d.top),Math.round(d.right),Math.round(d.bottom),s.left,s.top,s.right,s.bottom,null); if(oi!=null) g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,oi); else g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR); g.setComposite(oc);}
 public void restore(){g.setTransform(stack.pop()); AffineTransform t=g.getTransform(); g.setTransform(new AffineTransform()); Shape cl=clips.pop(); g.setClip(null); g.setTransform(t); Matrix q=pstack.pop();P=q;}
 public void translate(float x,float y){g.translate(x,y);} public void scale(float x,float y){g.scale(x,y);}
 public void clipRect(float l, float t, float r, float b){g.clip(new Rectangle2D.Float(l,t,r-l,b-t));}
 public void clipRect(RectF r){g.clip(new Rectangle2D.Float(r.left,r.top,r.width(),r.height()));}
 public void drawRect(RectF r,Paint p){if(Thread.currentThread().getName().startsWith("map")){BG++;} else {CALLS++; site(); COUNTS.merge("drawRect",1,Integer::sum);}drawRect(r.left,r.top,r.right,r.bottom,p);}
 public void rotate(float d){g.rotate(Math.toRadians(d));}
 public void drawColor(int c){if(Thread.currentThread().getName().startsWith("map")){BG++;} else {CALLS++; site(); COUNTS.merge("drawColor",1,Integer::sum);}AffineTransform t=g.getTransform(); g.setTransform(new AffineTransform()); g.setColor(new Color(c,true)); g.fillRect(0,0,10000,10000); g.setTransform(t);}
 public void drawRect(float l,float t,float r,float b,Paint p){if(Thread.currentThread().getName().startsWith("map")){BG++;} else {CALLS++; site(); COUNTS.merge("drawRect",1,Integer::sum);} if(P!=null){Path2D.Double q=new Path2D.Double(); double[] a=P.map(l,t);q.moveTo(a[0],a[1]); a=P.map(r,t);q.lineTo(a[0],a[1]); a=P.map(r,b);q.lineTo(a[0],a[1]); a=P.map(l,b);q.lineTo(a[0],a[1]); q.closePath(); sh(q,p);} else sh(new Rectangle2D.Float(l,t,r-l,b-t),p);}
 public void drawPoints(float[] pts,int offset,int count,Paint p){if(Thread.currentThread().getName().startsWith("map")){BG++;} else {CALLS++; site(); COUNTS.merge("drawPoints",1,Integer::sum);} ap(p); for(int k=offset;k+1<offset+count;k+=2) g.draw(new java.awt.geom.Line2D.Float(pts[k],pts[k+1],pts[k],pts[k+1]));}
 public void drawCircle(float x,float y,float r,Paint p){if(Thread.currentThread().getName().startsWith("map")){BG++;} else {CALLS++; site(); COUNTS.merge("drawCircle",1,Integer::sum);}sh(new Ellipse2D.Float(x-r,y-r,2*r,2*r),p);}
 public void drawOval(RectF o,Paint p){if(Thread.currentThread().getName().startsWith("map")){BG++;} else {CALLS++; site(); COUNTS.merge("drawOval",1,Integer::sum);}sh(new Ellipse2D.Float(o.left,o.top,o.width(),o.height()),p);}
 public void drawArc(RectF o,float start,float sweep,boolean center,Paint p){if(Thread.currentThread().getName().startsWith("map")){BG++;} else {CALLS++; site(); COUNTS.merge("drawArc",1,Integer::sum);}ap(p); g.draw(new Arc2D.Float(o.left,o.top,o.width(),o.height(),-start,-sweep,center?Arc2D.PIE:Arc2D.OPEN));}
 public void drawRoundRect(RectF o,float rx,float ry,Paint p){if(Thread.currentThread().getName().startsWith("map")){BG++;} else {CALLS++; site(); COUNTS.merge("drawRoundRect",1,Integer::sum);}sh(new RoundRectangle2D.Float(o.left,o.top,o.width(),o.height(),rx*2,ry*2),p);}
 public void drawLine(float a,float b,float c,float d,Paint p){if(Thread.currentThread().getName().startsWith("map")){BG++;} else {CALLS++; site(); COUNTS.merge("drawLine",1,Integer::sum);}ap(p); g.draw(new Line2D.Float(a,b,c,d));}
 public void drawBitmap(Bitmap bm,float x,float y,Paint p){if(Thread.currentThread().getName().startsWith("map")){BG++;} else {CALLS++; site(); COUNTS.merge("drawBitmap",1,Integer::sum);}g.drawImage(bm.img,(int)x,(int)y,null);}
 public void drawText(String s,float x,float y,Paint p){if(Thread.currentThread().getName().startsWith("map")){BG++;} else {CALLS++; site(); COUNTS.merge("drawText",1,Integer::sum);} g.setColor(new Color(p.color,true)); Font f=new Font("SansSerif",Font.BOLD,Math.round(p.ts)); g.setFont(f);
   float w=g.getFontMetrics().stringWidth(s); if(p.align==Paint.Align.CENTER)x-=w/2; else if(p.align==Paint.Align.RIGHT)x-=w; g.drawString(s,x,y);}
}
