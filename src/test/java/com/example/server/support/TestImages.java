package com.example.server.support;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import javax.imageio.ImageIO;

/** 테스트용 실제 이미지 바이트 생성기 */
public final class TestImages {

  private TestImages() {
  }

  /** ImageIO로 만든 진짜 png / jpg / gif */
  public static byte[] encode(String format, int width, int height) {
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      if (!ImageIO.write(image, format, out)) {
        throw new IllegalArgumentException(format);
      }
      return out.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static byte[] png(int width, int height) {
    return encode("png", width, height);
  }

  public static byte[] png() {
    return png(4, 3);
  }

  /** 파일 뒤에 0을 붙여 용량만 키운다 (헤더는 그대로 유효) */
  public static byte[] padTo(byte[] image, int size) {
    return Arrays.copyOf(image, size);
  }

  /**
   * PNG IHDR의 가로 · 세로 값만 바꾼 이미지 (실제 픽셀 없이 거대한 해상도를 주장하는 파일).
   * 디코딩하지 않고 헤더만 읽으므로 해상도 검증 테스트에 충분하다.
   */
  public static byte[] pngClaiming(int width, int height) {
    byte[] data = png(1, 1);
    ByteBuffer.wrap(data, 16, 8).order(ByteOrder.BIG_ENDIAN).putInt(width).putInt(height);
    return data;
  }

  /** WebP (VP8X 확장 헤더) */
  public static byte[] webp(int width, int height) {
    ByteBuffer b = ByteBuffer.allocate(30).order(ByteOrder.LITTLE_ENDIAN);
    b.put("RIFF".getBytes(StandardCharsets.US_ASCII)).putInt(22);
    b.put("WEBP".getBytes(StandardCharsets.US_ASCII));
    b.put("VP8X".getBytes(StandardCharsets.US_ASCII)).putInt(10);
    b.putInt(0); // flags + reserved
    put24(b, width - 1);
    put24(b, height - 1);
    return b.array();
  }

  /** HEIC (ftyp + meta 안의 ispe 박스). 썸네일 ispe도 넣어 가장 큰 값을 고르는지 확인한다. */
  public static byte[] heic(String brand, int width, int height) {
    ByteBuffer b = ByteBuffer.allocate(80).order(ByteOrder.BIG_ENDIAN);
    b.putInt(24).put("ftyp".getBytes(StandardCharsets.US_ASCII)).put(brand.getBytes(StandardCharsets.US_ASCII))
        .putInt(0).put("mif1".getBytes(StandardCharsets.US_ASCII)).put("heic".getBytes(StandardCharsets.US_ASCII));
    b.putInt(48).put("meta".getBytes(StandardCharsets.US_ASCII)).putInt(0);
    ispe(b, Math.max(1, width / 8), Math.max(1, height / 8)); // 썸네일 (원본보다 작음)
    ispe(b, width, height); // 원본
    return Arrays.copyOf(b.array(), b.position());
  }

  private static void ispe(ByteBuffer b, int width, int height) {
    b.putInt(20).put("ispe".getBytes(StandardCharsets.US_ASCII)).putInt(0).putInt(width).putInt(height);
  }

  private static void put24(ByteBuffer b, int value) {
    b.put((byte) value).put((byte) (value >> 8)).put((byte) (value >> 16));
  }
}
