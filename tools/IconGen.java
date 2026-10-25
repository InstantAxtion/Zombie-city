import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Draws the launcher icon at every density. (11.5) The same badge as the menu's logo: a night skyline inside a ring
 * broken where something got through, a biohazard trefoil over the towers, all in the game's toxic green on near-black.
 */
public class IconGen {
    public static void main(String[] args) throws Exception {
        String res = args[0];
        String[] dirs = {"mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi"};
        int[] sizes = {48, 72, 96, 144, 192};
        for (int i = 0; i < dirs.length; i++) {
            File dir = new File(res, "mipmap-" + dirs[i]);
            dir.mkdirs();
            ImageIO.write(draw(sizes[i]), "png", new File(dir, "ic_launcher.png"));
        }
        if (args.length > 1) ImageIO.write(draw(512), "png", new File(args[1]));
    }

    static final Color ACCENT = new Color(0x7CE04A), DARK = new Color(0x0C0F13), TOWER = new Color(0x26331F);

    static BufferedImage draw(int s) {
        BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.scale(s / 96.0, s / 96.0);
        // The tile: near-black, a faint green glow rising from the bottom.
        RoundRectangle2D tile = new RoundRectangle2D.Double(2, 2, 92, 92, 26, 26);
        g.setClip(tile);
        g.setPaint(new GradientPaint(0, 0, new Color(0x161B20), 0, 96, DARK));
        g.fillRect(0, 0, 96, 96);
        g.setPaint(new RadialGradientPaint(new Point2D.Double(48, 110), 70, new float[]{0, 1},
                new Color[]{new Color(0x447CE04A, true), new Color(0x007CE04A, true)}));
        g.fillRect(0, 0, 96, 96);
        double cx = 48, cy = 48, r = 34;
        // The skyline inside the ring.
        Shape inner = new Ellipse2D.Double(cx - r + 6, cy - r + 6, (r - 6) * 2, (r - 6) * 2);
        g.setClip(inner);
        g.setColor(TOWER);
        double[][] towers = {{-0.72, 0.16, 0.2}, {-0.5, -0.22, 0.18}, {-0.3, 0.02, 0.14}, {-0.15, -0.5, 0.2},
                {0.06, -0.12, 0.16}, {0.22, -0.36, 0.18}, {0.4, 0.0, 0.14}, {0.54, -0.18, 0.2}};
        for (double[] t : towers) g.fill(new Rectangle2D.Double(cx + t[0] * r, cy + t[1] * r, t[2] * r, r * 0.52 - t[1] * r));
        g.setColor(new Color(0xB8E07A));
        for (int k = 0; k < 9; k++) {
            double wx = cx + (-0.62 + k * 0.14) * r, wy = cy + (0.26 - (k * 37 % 5) * 0.12) * r;
            if (k % 3 == 1) continue;
            g.fill(new Rectangle2D.Double(wx, wy, r * 0.05, r * 0.06));
        }
        g.setClip(tile);
        // The street line under it.
        g.setColor(ACCENT);
        g.fill(new Rectangle2D.Double(cx - r * 0.78, cy + r * 0.52, r * 1.56, r * 0.07));
        // The trefoil: three blades round a hub, a dark gap between hub and blades.
        double tx = cx, ty = cy - r * 0.3, tr = r * 0.4;
        Area tref = new Area();
        for (int k = 0; k < 3; k++)
            tref.add(new Area(new Arc2D.Double(tx - tr, ty - tr, tr * 2, tr * 2, 60 + k * 120, 60, Arc2D.PIE)));
        tref.subtract(new Area(new Ellipse2D.Double(tx - tr * 0.36, ty - tr * 0.36, tr * 0.72, tr * 0.72)));
        g.setColor(DARK);
        g.setStroke(new BasicStroke((float) (r * 0.08), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(tref);
        g.setColor(ACCENT);
        g.fill(tref);
        g.fill(new Ellipse2D.Double(tx - tr * 0.2, ty - tr * 0.2, tr * 0.4, tr * 0.4));
        // The ring, broken at the bottom right; a scrap of it flung off.
        g.setStroke(new BasicStroke((float) (r * 0.13), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Rectangle2D ring = new Rectangle2D.Double(cx - r, cy - r, r * 2, r * 2);
        g.draw(new Arc2D.Double(ring, -20, 300, Arc2D.OPEN));
        g.setColor(new Color(0x997CE04A, true));
        g.setStroke(new BasicStroke((float) (r * 0.06), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Arc2D.Double(new Rectangle2D.Double(cx - r * 1.08, cy - r * 1.08, r * 2.16, r * 2.16), -58, 24, Arc2D.OPEN));
        g.dispose();
        return img;
    }
}
