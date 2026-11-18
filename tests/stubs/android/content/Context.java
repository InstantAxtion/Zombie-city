package android.content;
import java.util.HashMap;
public class Context { public static final int MODE_PRIVATE=0; public static final String CLIPBOARD_SERVICE="clipboard"; final ClipboardManager clipboard=new ClipboardManager(); public Object getSystemService(String n){return CLIPBOARD_SERVICE.equals(n)?clipboard:null;} final android.content.res.Resources r=new android.content.res.Resources();
 public android.content.res.Resources getResources(){return r;} public java.io.File getFilesDir(){ java.io.File d=new java.io.File("savetest"); d.mkdirs(); return d;}
 final HashMap<String,Integer> map=new HashMap<>(); final HashMap<String,String> strings=new HashMap<>();
 public SharedPreferences getSharedPreferences(String n,int m){ return new SharedPreferences(){
   public int getInt(String k,int d){Integer v=map.get(k);return v==null?d:v;}
   public String getString(String k,String d){String v=strings.get(k);return v==null?d:v;}
   public boolean contains(String k){return map.containsKey(k)||strings.containsKey(k);}
   public java.util.Map<String,?> getAll(){HashMap<String,Object> a=new HashMap<>(map);a.putAll(strings);return a;}
   public Editor edit(){ return new Editor(){ public Editor putInt(String k,int v){map.put(k,v);return this;} public Editor putString(String k,String v){strings.put(k,v);return this;} public Editor remove(String k){map.remove(k);strings.remove(k);return this;} public void apply(){} }; } }; } }
