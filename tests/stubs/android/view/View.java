package android.view;
import android.content.Context; import android.graphics.Canvas;
public class View { final Context c; int w,h; public View(Context c){this.c=c;}
 public android.content.res.Resources getResources(){return c.getResources();} public Context getContext(){return c;}
 public int getWidth(){return w;} public int getHeight(){return h;}
 public void layout(int w,int h){int ow=this.w,oh=this.h;this.w=w;this.h=h;onSizeChanged(w,h,ow,oh);}
 protected void onSizeChanged(int w,int h,int ow,int oh){} protected void onDraw(Canvas c){}
 public void postInvalidateOnAnimation(){} public void invalidate(){} public void postInvalidateDelayed(long ms){} public boolean onTouchEvent(MotionEvent e){return false;} public boolean performHapticFeedback(int c){return true;}
 public boolean onKeyDown(int k, KeyEvent e){return false;} public boolean onKeyUp(int k, KeyEvent e){return false;} public boolean onGenericMotionEvent(MotionEvent e){return false;}
 public void setFocusable(boolean f){} public void setFocusableInTouchMode(boolean f){} public boolean requestFocus(){return true;} }
