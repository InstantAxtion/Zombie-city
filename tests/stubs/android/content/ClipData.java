package android.content;
public class ClipData { public CharSequence label, text; public static ClipData newPlainText(CharSequence l, CharSequence t){ClipData c=new ClipData();c.label=l;c.text=t;return c;} }
