package com.example.server.domain.user.dto;

import com.example.server.global.init.DemoData;
import com.example.server.global.util.TextPatterns;
import com.example.server.global.util.Ulid;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;

public class UserRequestDTO {

  @Getter
  @Schema(name = "UserProfileUpdateRequest", description = "변경할 프로필 필드만 전달합니다.")
  public static class UpdateProfile {

    @JsonProperty
    @Schema(description = "새 닉네임 (2~10자). 예시는 데모 유저의 현재 이름이라 그대로 실행해도 중복되지 않습니다.", example = DemoData.USER_NICKNAME)
    private String nickname;

    @Schema(description = "한 줄 소개 (최대 50자). null을 보내면 지웁니다.", example = "매일 조금씩 운동해요.", nullable = true)
    @Size(max = 50)
    @Pattern(regexp = TextPatterns.SINGLE_LINE, message = "한 줄 소개에는 " + TextPatterns.SINGLE_LINE_MESSAGE)
    private String bio;

    @Getter(AccessLevel.NONE)
    private boolean bioProvided;

    @JsonProperty
    @Schema(description = "새 대표 이미지 publicId", example = DemoData.IMAGE_ID_2)
    @Pattern(regexp = Ulid.REGEX, message = Ulid.MESSAGE)
    private String profileImagePublicId;

    @JsonSetter("bio")
    public void acceptBio(String bio) {
      this.bio = bio;
      this.bioProvided = true;
    }

    public boolean hasBio() {
      return bioProvided;
    }
  }
}
