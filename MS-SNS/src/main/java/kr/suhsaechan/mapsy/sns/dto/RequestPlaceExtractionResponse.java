package kr.suhsaechan.mapsy.sns.dto;

import kr.suhsaechan.mapsy.common.constant.ContentStatus;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestPlaceExtractionResponse {

  private UUID contentId;
  private UUID memberId;
  private ContentStatus status;
}
