package com.example.server.domain.image.support;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

/**
 * 업로드된 바이트의 실제 형식(파일 시그니처)과 해상도를 판별한다.
 * 클라이언트가 보낸 Content-Type은 믿지 않는다. 디코딩 없이 헤더만 읽는다.
 */
public final class ImageInspector {

  public record ImageInfo(String contentType, int width, int height) {
  }

  private static final Set<String> HEIC_BRANDS = Set.of("heic", "heix", "hevc", "hevx");
  private static final Set<String> HEIF_BRANDS = Set.of("mif1", "msf1", "heim", "heis");
  /** HEIF의 ispe(크기) 박스를 찾을 범위. 메타데이터는 파일 앞부분에 있다. */
  private static final int HEIF_SCAN_LIMIT = 512 * 1024;

  private ImageInspector() {
  }

  /** 지원 형식이 아니거나 헤더를 읽을 수 없으면 empty */
  public static Optional<ImageInfo> inspect(byte[] data) {
    try {
      if (startsWith(data, 0, 0xFF, 0xD8, 0xFF)) {
        return readWithImageIo(data, "image/jpeg");
      }
      if (startsWith(data, 0, 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A)) {
        return readWithImageIo(data, "image/png");
      }
      if (ascii(data, 0, 6).equals("GIF87a") || ascii(data, 0, 6).equals("GIF89a")) {
        return readWithImageIo(data, "image/gif");
      }
      if (ascii(data, 0, 4).equals("RIFF") && ascii(data, 8, 4).equals("WEBP")) {
        return readWebp(data);
      }
      if (ascii(data, 4, 4).equals("ftyp")) {
        return readHeif(data);
      }
    } catch (IOException | RuntimeException e) {
      return Optional.empty();
    }
    return Optional.empty();
  }

  private static Optional<ImageInfo> readWithImageIo(byte[] data, String contentType) throws IOException {
    try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
      Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
      if (!readers.hasNext()) {
        return Optional.empty();
      }
      ImageReader reader = readers.next();
      try {
        reader.setInput(in, true, true);
        return Optional.of(new ImageInfo(contentType, reader.getWidth(0), reader.getHeight(0)));
      } finally {
        reader.dispose();
      }
    }
  }

  private static Optional<ImageInfo> readWebp(byte[] d) {
    String chunk = ascii(d, 12, 4);
    int width;
    int height;
    switch (chunk) {
      case "VP8 " -> {
        width = u16le(d, 26) & 0x3FFF;
        height = u16le(d, 28) & 0x3FFF;
      }
      case "VP8L" -> {
        int b1 = u8(d, 21);
        int b2 = u8(d, 22);
        int b3 = u8(d, 23);
        int b4 = u8(d, 24);
        width = 1 + (((b2 & 0x3F) << 8) | b1);
        height = 1 + (((b4 & 0x0F) << 10) | (b3 << 2) | ((b2 & 0xC0) >> 6));
      }
      case "VP8X" -> {
        width = 1 + u24le(d, 24);
        height = 1 + u24le(d, 27);
      }
      default -> {
        return Optional.empty();
      }
    }
    return Optional.of(new ImageInfo("image/webp", width, height));
  }

  private static Optional<ImageInfo> readHeif(byte[] d) {
    String brand = ascii(d, 8, 4);
    String contentType;
    if (HEIC_BRANDS.contains(brand)) {
      contentType = "image/heic";
    } else if (HEIF_BRANDS.contains(brand)) {
      contentType = "image/heif";
    } else {
      return Optional.empty();
    }
    // ispe 박스: [size 4][ "ispe" 4][version/flags 4][width 4][height 4]
    // 썸네일 · 타일마다 ispe가 있으므로 면적이 가장 큰 것을 원본 크기로 본다.
    int width = 0;
    int height = 0;
    // i + 16 <= 끝 (width · height 8바이트까지 읽을 수 있는 위치까지)
    int last = Math.min(d.length, HEIF_SCAN_LIMIT) - 16;
    for (int i = 4; i <= last; i++) {
      if (d[i] == 'i' && d[i + 1] == 's' && d[i + 2] == 'p' && d[i + 3] == 'e') {
        int w = u32be(d, i + 8);
        int h = u32be(d, i + 12);
        if ((long) w * h > (long) width * height) {
          width = w;
          height = h;
        }
      }
    }
    if (width <= 0 || height <= 0) {
      return Optional.empty();
    }
    return Optional.of(new ImageInfo(contentType, width, height));
  }

  // ---------- byte helpers ----------

  private static boolean startsWith(byte[] d, int offset, int... expected) {
    if (d.length < offset + expected.length) {
      return false;
    }
    for (int i = 0; i < expected.length; i++) {
      if ((d[offset + i] & 0xFF) != expected[i]) {
        return false;
      }
    }
    return true;
  }

  private static String ascii(byte[] d, int offset, int length) {
    if (d.length < offset + length) {
      return "";
    }
    return new String(d, offset, length, StandardCharsets.US_ASCII);
  }

  private static int u8(byte[] d, int i) {
    return d[i] & 0xFF;
  }

  private static int u16le(byte[] d, int i) {
    return u8(d, i) | (u8(d, i + 1) << 8);
  }

  private static int u24le(byte[] d, int i) {
    return u8(d, i) | (u8(d, i + 1) << 8) | (u8(d, i + 2) << 16);
  }

  private static int u32be(byte[] d, int i) {
    long value = ((long) u8(d, i) << 24) | (u8(d, i + 1) << 16) | (u8(d, i + 2) << 8) | u8(d, i + 3);
    return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
  }
}
