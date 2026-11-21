package com.example.server.domain.image;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.server.global.auth.LoginUser;
import com.example.server.global.init.DemoData;
import com.example.server.support.TestImages;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.io.ByteArrayOutputStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

/**
 * MockMvc는 multipart 크기 제한을 적용하지 않으므로, 실제 Tomcat을 띄워 HTTP로 검증한다.
 * (application.yml: 파일 10MB / 요청 20MB, max-swallow-size 32MB)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ImageUploadLimitTests {

  private static final int MB = 1024 * 1024;

  @LocalServerPort
  private int port;

  private HttpResponse<String> upload(int size) throws Exception {
    String boundary = "----test-boundary";
    ByteArrayOutputStream body = new ByteArrayOutputStream();
    body.write(("--" + boundary + "\r\n"
        + "Content-Disposition: form-data; name=\"file\"; filename=\"big.png\"\r\n"
        + "Content-Type: image/png\r\n\r\n").getBytes(StandardCharsets.UTF_8));
    body.write(TestImages.padTo(TestImages.png(), size)); // 진짜 PNG 헤더 + 0 패딩
    body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

    HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/images"))
        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
        .header(LoginUser.HEADER, DemoData.USER_DEVICE_TOKEN)
        .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
        .build();
    return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
  }

  @Test
  @DisplayName("10MB 이하(9MB) → 201")
  void underLimit() throws Exception {
    HttpResponse<String> response = upload(9 * MB);
    assertThat(response.statusCode()).isEqualTo(201);
  }

  @Test
  @DisplayName("10MB 초과 → 연결이 끊기지 않고 400 FILE400 JSON")
  void overLimit() throws Exception {
    HttpResponse<String> response = upload(12 * MB);
    assertThat(response.statusCode()).isEqualTo(400);
    assertThat(response.body()).contains("\"code\":\"FILE400\"").contains("\"isSuccess\":false");
  }
}
