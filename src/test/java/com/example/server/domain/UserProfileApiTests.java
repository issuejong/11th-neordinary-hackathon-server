package com.example.server.domain;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.server.global.auth.LoginUser;
import com.example.server.support.ApiTestSupport;
import com.example.server.support.TestImages;
import com.jayway.jsonpath.JsonPath;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.util.unit.DataSize;

class UserProfileApiTests extends ApiTestSupport {

  private static final int PROFILE_LIMIT = (int) DataSize.ofMegabytes(5).toBytes();

  @Test
  void updatesOwnProfileAndCanClearBioWithoutChangingOtherFields() throws Exception {
    String token = newUser();
    String imageId = imageId(uploadProfile(token, TestImages.png()));
    update(token, """
        {"nickname":"아침산책","bio":"하루를 산책으로 시작해요.","profileImagePublicId":"%s"}
        """.formatted(imageId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result.nickname").value("아침산책"))
        .andExpect(jsonPath("$.result.bio").value("하루를 산책으로 시작해요."))
        .andExpect(jsonPath("$.result.profileImagePublicId").value(imageId));

    update(token, """
        {"nickname":"저녁산책"}
        """)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result.bio").value("하루를 산책으로 시작해요."));
    update(token, """
        {"bio":null}
        """).andExpect(status().isOk());

    mvc.perform(get("/api/users/me").header(LoginUser.HEADER, authorization(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result.nickname").value("저녁산책"))
        .andExpect(jsonPath("$.result.bio").value(Matchers.nullValue()))
        .andExpect(jsonPath("$.result.profileImagePublicId").value(imageId));
  }

  @Test
  void validatesProfileChanges() throws Exception {
    String token = newUser();
    update(token, "{\"nickname\":\"짧\"}").andExpect(status().isBadRequest());
    update(token, "{\"nickname\":\"데모 유저\"}").andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("USER4091"));
    update(token, "{\"bio\":\"" + "가".repeat(51) + "\"}").andExpect(status().isBadRequest());
    String otherImage = imageId(uploadProfile(newUser(), TestImages.png()));
    update(token, "{\"profileImagePublicId\":\"%s\"}".formatted(otherImage))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("IMAGE404"));
  }

  @Test
  void profileLimitAppliesToUploadAndToSelectingAnExistingImage() throws Exception {
    String token = newUser();
    byte[] approved = TestImages.padTo(TestImages.png(), PROFILE_LIMIT);
    uploadProfile(token, approved).andExpect(status().isCreated());

    byte[] oversized = TestImages.padTo(TestImages.png(), PROFILE_LIMIT + 1);
    uploadProfile(token, oversized).andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("IMAGE4004"));
    String largeImage = imageId(uploadImage(token, "image/png", oversized));
    update(token, "{\"profileImagePublicId\":\"%s\"}".formatted(largeImage))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("IMAGE4004"));
    uploadProfile(token, TestImages.webp(4, 3)).andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("IMAGE4005"));
  }

  private ResultActions update(String token, String json) throws Exception {
    return mvc.perform(patch("/api/users/me").header(LoginUser.HEADER, authorization(token))
        .contentType(MediaType.APPLICATION_JSON).content(json));
  }

  private ResultActions uploadProfile(String token, byte[] data) throws Exception {
    return mvc.perform(multipart("/api/images/profile")
        .file(new MockMultipartFile("file", "profile.png", "image/png", data))
        .header(LoginUser.HEADER, authorization(token)));
  }

  private String imageId(ResultActions upload) throws Exception {
    return JsonPath.read(upload.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(),
        "$.result.imageId");
  }
}
