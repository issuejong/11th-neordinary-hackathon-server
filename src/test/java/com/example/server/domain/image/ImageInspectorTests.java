package com.example.server.domain.image;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.server.domain.image.support.ImageInspector;
import com.example.server.domain.image.support.ImageInspector.ImageInfo;
import com.example.server.support.TestImages;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ImageInspectorTests {

  @Test
  @DisplayName("형식별 시그니처와 해상도를 판별한다")
  void detects() {
    assertThat(ImageInspector.inspect(TestImages.encode("jpg", 12, 7))).contains(new ImageInfo("image/jpeg", 12, 7));
    assertThat(ImageInspector.inspect(TestImages.png(12, 7))).contains(new ImageInfo("image/png", 12, 7));
    assertThat(ImageInspector.inspect(TestImages.encode("gif", 12, 7))).contains(new ImageInfo("image/gif", 12, 7));
    assertThat(ImageInspector.inspect(TestImages.webp(800, 600))).contains(new ImageInfo("image/webp", 800, 600));
    assertThat(ImageInspector.inspect(TestImages.heic("heic", 4032, 3024))).contains(new ImageInfo("image/heic", 4032, 3024));
    assertThat(ImageInspector.inspect(TestImages.heic("heix", 100, 50))).contains(new ImageInfo("image/heic", 100, 50));
    assertThat(ImageInspector.inspect(TestImages.heic("mif1", 100, 50))).contains(new ImageInfo("image/heif", 100, 50));
  }

  @Test
  @DisplayName("HEIC는 썸네일이 아니라 가장 큰 ispe(원본) 크기를 쓴다")
  void heicUsesLargestIspe() {
    assertThat(ImageInspector.inspect(TestImages.heic("heic", 8064, 6048)).orElseThrow().width()).isEqualTo(8064);
  }

  @Test
  @DisplayName("이미지가 아니거나 잘린 파일은 empty")
  void rejects() {
    assertThat(ImageInspector.inspect(new byte[0])).isEmpty();
    assertThat(ImageInspector.inspect("hello".getBytes(StandardCharsets.UTF_8))).isEmpty();
    assertThat(ImageInspector.inspect(java.util.Arrays.copyOf(TestImages.png(), 10))).isEmpty();
    assertThat(ImageInspector.inspect("RIFF\0\0\0\0WEBPXXXX".getBytes(StandardCharsets.US_ASCII))).isEmpty();
    assertThat(ImageInspector.inspect(TestImages.heic("avif", 10, 10))).isEmpty();
  }
}
