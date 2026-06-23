package com.meetbowl.application.minutes;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.domain.minutes.Minutes;
import com.meetbowl.domain.minutes.MinutesFavoriteRepositoryPort;
import com.meetbowl.domain.minutes.MinutesRepositoryPort;

/** 사용자가 속한 조직의 회의록 목록을 조회하고, 사용자별 즐겨찾기 여부를 함께 계산한다. */
@Service
public class GetMinutesListUseCase {

    private final MinutesRepositoryPort minutesRepositoryPort;
    private final MinutesFavoriteRepositoryPort favoriteRepositoryPort;
    private final MinutesMeetingMetadataAssembler metadataAssembler;

    public GetMinutesListUseCase(
            MinutesRepositoryPort minutesRepositoryPort,
            MinutesFavoriteRepositoryPort favoriteRepositoryPort,
            MinutesMeetingMetadataAssembler metadataAssembler) {
        this.minutesRepositoryPort = minutesRepositoryPort;
        this.favoriteRepositoryPort = favoriteRepositoryPort;
        this.metadataAssembler = metadataAssembler;
    }

    @Transactional(readOnly = true)
    public List<MinutesListItemResult> execute(
            UUID actorUserId, UUID actorOrganizationId, String keyword) {
        // 회의록 접근 권한은 현재 단계에서 조직 경계로 제한한다. 개인별 상태인 즐겨찾기는 별도 조회해 응답에 합성한다.
        List<Minutes> minutes =
                hasKeyword(keyword)
                        ? minutesRepositoryPort.searchByOrganizationId(
                                actorOrganizationId, keyword.trim())
                        : minutesRepositoryPort.findByOrganizationId(actorOrganizationId);
        Set<UUID> favoriteMinutesIds =
                favoriteRepositoryPort.findByUserId(actorUserId).stream()
                        .map(favorite -> favorite.minutesId())
                        .collect(Collectors.toSet());
        Map<UUID, MinutesMeetingMetadata> metadataByMeetingId =
                metadataAssembler.assemble(
                        minutes.stream().map(Minutes::meetingId).toList(), actorOrganizationId);

        return minutes.stream()
                .map(
                        item ->
                                MinutesListItemResult.from(
                                        item,
                                        favoriteMinutesIds.contains(item.id()),
                                        metadataByMeetingId.getOrDefault(
                                                item.meetingId(),
                                                MinutesMeetingMetadata.empty(
                                                        item.reviewerUserId()))))
                .toList();
    }

    private static boolean hasKeyword(String keyword) {
        return keyword != null && !keyword.isBlank();
    }
}
