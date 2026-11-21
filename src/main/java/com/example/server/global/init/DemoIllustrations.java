package com.example.server.global.init;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Random;
import javax.imageio.ImageIO;

/**
 * Swagger 데모 데이터용 루틴 일러스트를 코드로 그린다. (외부 이미지 · 저작권 문제 없음)
 * 텍스트 없이 도형만 사용해 컨테이너에 한글 폰트가 없어도 동일하게 렌더링된다.
 */
final class DemoIllustrations {

  enum Theme { SUNRISE, WATER, STRETCH, NOTEBOOK, RUNNING, BOOK, NIGHT, COFFEE }

  private static final int SIZE = 512;

  private DemoIllustrations() {
  }

  static byte[] png(Theme theme) {
    BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = image.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    switch (theme) {
      case SUNRISE -> sunrise(g);
      case WATER -> water(g);
      case STRETCH -> stretch(g);
      case NOTEBOOK -> notebook(g);
      case RUNNING -> running(g);
      case BOOK -> book(g);
      case NIGHT -> night(g);
      case COFFEE -> coffee(g);
    }
    g.dispose();
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      ImageIO.write(image, "png", out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static void background(Graphics2D g, int top, int bottom) {
    g.setPaint(new GradientPaint(0, 0, new Color(top), 0, SIZE, new Color(bottom)));
    g.fillRect(0, 0, SIZE, SIZE);
  }

  private static void sunrise(Graphics2D g) {
    background(g, 0xFDE68A, 0xFB923C);
    g.setColor(new Color(255, 255, 255, 70));
    for (int r = 300; r >= 180; r -= 60) {
      g.fill(new Ellipse2D.Double(256 - r / 2.0, 330 - r / 2.0, r, r));
    }
    g.setColor(new Color(0xFFF7ED));
    g.fill(new Ellipse2D.Double(176, 250, 160, 160));
    g.setColor(new Color(0xEA580C));
    g.fill(hill(0, 360, 300, 300, 512));
    g.setColor(new Color(0xC2410C));
    g.fill(hill(180, 390, 520, 340, 512));
  }

  private static Path2D hill(double x1, double y1, double x2, double y2, double bottom) {
    Path2D p = new Path2D.Double();
    p.moveTo(x1 - 200, bottom);
    p.quadTo(x1, y1 - 80, (x1 + x2) / 2, y1);
    p.quadTo(x2, y2 + 60, x2 + 200, bottom);
    p.closePath();
    return p;
  }

  private static void water(Graphics2D g) {
    background(g, 0xE0F2FE, 0x7DD3FC);
    RoundRectangle2D glass = new RoundRectangle2D.Double(176, 110, 160, 290, 28, 28);
    g.setColor(new Color(255, 255, 255, 140));
    g.fill(glass);
    g.setClip(glass);
    g.setPaint(new GradientPaint(0, 220, new Color(0x38BDF8), 0, 400, new Color(0x0284C7)));
    g.fillRect(176, 220, 160, 200);
    g.setColor(new Color(255, 255, 255, 160));
    Random random = new Random(7);
    for (int i = 0; i < 9; i++) {
      int d = 8 + random.nextInt(14);
      g.fill(new Ellipse2D.Double(195 + random.nextInt(110), 240 + random.nextInt(140), d, d));
    }
    g.setClip(null);
    g.setColor(new Color(0x0369A1));
    g.setStroke(new BasicStroke(6));
    g.draw(glass);
  }

  private static void stretch(Graphics2D g) {
    background(g, 0xDCFCE7, 0x86EFAC);
    g.setColor(new Color(0x166534));
    g.fill(new RoundRectangle2D.Double(96, 380, 320, 34, 17, 17));
    double[][] leaves = {{256, 360, 0}, {256, 300, -35}, {256, 300, 35}, {256, 240, -25}, {256, 240, 25}};
    g.setStroke(new BasicStroke(8, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    g.setColor(new Color(0x15803D));
    g.drawLine(256, 380, 256, 170);
    for (double[] leaf : leaves) {
      Graphics2D lg = (Graphics2D) g.create();
      lg.rotate(Math.toRadians(leaf[2]), leaf[0], leaf[1]);
      lg.setColor(new Color(0x22C55E));
      lg.fill(new Ellipse2D.Double(leaf[0] - (leaf[2] == 0 ? 0 : (leaf[2] < 0 ? 120 : 0)), leaf[1] - 32, 120, 64));
      lg.dispose();
    }
    g.setColor(new Color(0x4ADE80));
    g.fill(new Ellipse2D.Double(216, 130, 80, 80));
  }

  private static void notebook(Graphics2D g) {
    background(g, 0xFEF3C7, 0xFDE68A);
    g.setColor(new Color(0, 0, 0, 30));
    g.fill(new RoundRectangle2D.Double(128, 100, 270, 330, 24, 24));
    g.setColor(Color.WHITE);
    g.fill(new RoundRectangle2D.Double(118, 88, 270, 330, 24, 24));
    g.setStroke(new BasicStroke(10, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    for (int i = 0; i < 4; i++) {
      int y = 160 + i * 66;
      g.setColor(i < 3 ? new Color(0x16A34A) : new Color(0xD4D4D8));
      Path2D check = new Path2D.Double();
      check.moveTo(152, y);
      check.lineTo(166, y + 14);
      check.lineTo(190, y - 12);
      g.draw(check);
      g.setColor(new Color(0xE4E4E7));
      g.drawLine(214, y, 350, y);
    }
  }

  private static void running(Graphics2D g) {
    background(g, 0xC4B5FD, 0xFDBA74);
    g.setColor(new Color(0xFEF3C7));
    g.fill(new Ellipse2D.Double(330, 90, 90, 90));
    Path2D mountains = new Path2D.Double();
    mountains.moveTo(0, 262);
    mountains.lineTo(70, 200);
    mountains.lineTo(140, 240);
    mountains.lineTo(230, 170);
    mountains.lineTo(320, 236);
    mountains.lineTo(400, 190);
    mountains.lineTo(512, 250);
    mountains.lineTo(512, 262);
    mountains.closePath();
    g.setColor(new Color(0x7C3AED));
    g.fill(mountains);
    g.setColor(new Color(0x3F3F46));
    Path2D road = new Path2D.Double();
    road.moveTo(226, 250);
    road.lineTo(286, 250);
    road.lineTo(470, 512);
    road.lineTo(42, 512);
    road.closePath();
    g.fill(road);
    g.setColor(new Color(0xFDE047));
    for (int i = 0; i < 5; i++) {
      double t = i / 5.0;
      double y = 262 + t * t * 260;
      double h = 10 + t * 34;
      double w = 4 + t * 12;
      g.fill(new RoundRectangle2D.Double(256 - w / 2, y, w, h, w, w));
    }
  }

  private static void book(Graphics2D g) {
    background(g, 0xFFE4E6, 0xFDA4AF);
    g.setColor(new Color(255, 255, 255, 90));
    g.fill(new Ellipse2D.Double(116, 60, 280, 280));
    g.setColor(new Color(0xBE123C));
    g.fill(new RoundRectangle2D.Double(92, 300, 328, 40, 12, 12));
    Path2D left = new Path2D.Double();
    left.moveTo(256, 190);
    left.quadTo(180, 160, 104, 186);
    left.lineTo(104, 306);
    left.quadTo(180, 282, 256, 310);
    left.closePath();
    Path2D right = new Path2D.Double();
    right.moveTo(256, 190);
    right.quadTo(332, 160, 408, 186);
    right.lineTo(408, 306);
    right.quadTo(332, 282, 256, 310);
    right.closePath();
    g.setColor(Color.WHITE);
    g.fill(left);
    g.fill(right);
    g.setColor(new Color(0xFECDD3));
    g.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    for (int i = 0; i < 4; i++) {
      g.drawLine(128, 216 + i * 22, 232, 214 + i * 22);
      g.drawLine(280, 214 + i * 22, 384, 216 + i * 22);
    }
  }

  private static void night(Graphics2D g) {
    background(g, 0x1E1B4B, 0x4338CA);
    Random random = new Random(11);
    g.setColor(new Color(255, 255, 255, 200));
    for (int i = 0; i < 40; i++) {
      int d = 2 + random.nextInt(5);
      g.fill(new Ellipse2D.Double(random.nextInt(SIZE), random.nextInt(360), d, d));
    }
    java.awt.geom.Area moon = new java.awt.geom.Area(new Ellipse2D.Double(190, 120, 170, 170));
    moon.subtract(new java.awt.geom.Area(new Ellipse2D.Double(236, 104, 160, 160)));
    g.setColor(new Color(0xFEF9C3));
    g.fill(moon);
    g.setColor(new Color(0x312E81));
    g.fill(hill(60, 420, 420, 400, 512));
  }

  private static void coffee(Graphics2D g) {
    background(g, 0xFAF5F0, 0xE7D3C1);
    g.setColor(new Color(0xD6B89C));
    g.fill(new Ellipse2D.Double(110, 372, 292, 56));
    g.setColor(new Color(0x7C4A2D));
    g.fill(new RoundRectangle2D.Double(150, 230, 190, 170, 40, 40));
    g.setStroke(new BasicStroke(16));
    g.draw(new Arc2D.Double(312, 262, 80, 90, -90, 180, Arc2D.OPEN));
    g.setColor(new Color(0xA0673F));
    g.fill(new Ellipse2D.Double(150, 214, 190, 40));
    g.setColor(new Color(0xA8A29E));
    g.setStroke(new BasicStroke(8, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    for (int i = 0; i < 3; i++) {
      Path2D steam = new Path2D.Double();
      double x = 200 + i * 45;
      steam.moveTo(x, 196);
      steam.curveTo(x - 20, 170, x + 20, 150, x, 120);
      g.draw(steam);
    }
  }
}
