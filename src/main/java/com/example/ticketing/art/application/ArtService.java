package com.example.ticketing.art.application;

import com.example.ticketing.art.domain.*;
import com.example.ticketing.art.dto.*;
import com.example.ticketing.client.component.ClientManager;
import com.example.ticketing.client.domain.Client;
import com.example.ticketing.client.domain.ClientRepository;
import com.example.ticketing.common.auth.ClientInfo;
import com.example.ticketing.common.exception.ErrorCode;
import com.example.ticketing.common.exception.GlobalException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArtService {

    private final ArtRepository artRepository;
    private final ArtLikeRepository artLikeRepository;
    private final ClientManager clientManager;
    private final ClientRepository clientRepository;

    @Transactional
    public ArtResponse createArt(ArtCreateRequest request, ClientInfo clientInfo) {
        Client client = clientManager.findById(clientInfo.getClientId());
        validatePixelData(request.getPixelData(), request.getWidth(), request.getHeight());

        Art art = Art.builder()
                .title(request.getTitle())
                .pixelData(request.getPixelData())
                .width(request.getWidth())
                .height(request.getHeight())
                .client(client)
                .build();

        Art savedArt = artRepository.save(art);
        return ArtResponse.from(savedArt);
    }

    public Page<ArtResponse> searchArts(ArtSearchCondition condition, ClientInfo clientInfo, Pageable pageable) {
        // 1. 키워드로 닉네임 검색하여 clientId 목록 추출
        List<Long> matchedClientIds = null;
        if (condition.getKeyword() != null && !condition.getKeyword().isEmpty()) {
            List<Client> matchedClients = clientRepository.findByNameContainingIgnoreCase(condition.getKeyword());
            matchedClientIds = matchedClients.stream()
                    .map(Client::getId)
                    .collect(Collectors.toList());
        }

        // 2. ArtQueryCondition 생성
        Long currentClientId = null;
        if (condition.getOnlyMine() != null && condition.getOnlyMine() && clientInfo != null) {
            currentClientId = clientInfo.getClientId();
        }

        ArtQueryCondition queryCondition = ArtQueryCondition.builder()
                .keyword(condition.getKeyword())
                .matchedClientIds(matchedClientIds)
                .sortBy(condition.getSortBy())
                .sortDirection(condition.getSortDirection() != null ? condition.getSortDirection() : "desc")
                .currentClientId(currentClientId)
                .build();

        // 3. 검색 실행
        Page<Art> arts = artRepository.searchArts(queryCondition, pageable);

        // 4. 좋아요 정보 포함하여 응답 생성
        Client client = clientInfo != null ? clientManager.findById(clientInfo.getClientId()) : null;
        if (client == null) {
            return arts.map(art -> ArtResponse.from(art, false));
        }
        return arts.map(art -> {
            boolean isLiked = artLikeRepository.existsByArtAndClient(art, client);
            return ArtResponse.from(art, isLiked);
        });
    }

    public ArtResponse getArt(Long artId) {
        Art art = artRepository.findById(artId)
                .orElseThrow(() -> new GlobalException(ErrorCode.INTERNAL_SERVER_ERROR));

        // 조회수 증가
        art.incrementViewCount();
        artRepository.save(art);

        return ArtResponse.from(art);
    }

    @Transactional
    public ArtResponse updateArt(Long artId, ArtUpdateRequest request, ClientInfo clientInfo) {
        Art art = artRepository.findById(artId)
                .orElseThrow(() -> new GlobalException(ErrorCode.INTERNAL_SERVER_ERROR));

        if (!art.getClient().getId().equals(clientInfo.getClientId())) {
            throw new GlobalException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        art.updateInfo(request.getTitle());

        return ArtResponse.from(art);
    }

    @Transactional
    public void deleteArt(Long artId, ClientInfo clientInfo) {
        Art art = artRepository.findById(artId)
                .orElseThrow(() -> new GlobalException(ErrorCode.INTERNAL_SERVER_ERROR));

        if (!art.getClient().getId().equals(clientInfo.getClientId())) {
            throw new GlobalException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        artRepository.delete(art);
    }

    @Transactional
    public ArtLikeResponse toggleLike(Long artId, ClientInfo clientInfo) {
        Art art = artRepository.findById(artId)
                .orElseThrow(() -> new GlobalException(ErrorCode.INTERNAL_SERVER_ERROR));

        Client client = clientManager.findById(clientInfo.getClientId());
        int likeCount = art.getLikeCount();

        boolean isLiked;
        if (artLikeRepository.existsByArtAndClient(art, client)) {
            artLikeRepository.deleteByArtAndClient(art, client);
            artRepository.decrementLikeCount(artId);
            isLiked = false;
            likeCount--;
        } else {
            ArtLike artLike = ArtLike.builder()
                    .art(art)
                    .client(client)
                    .build();
            artLikeRepository.save(artLike);
            artRepository.incrementLikeCount(artId);
            isLiked = true;
            likeCount++;
        }

        return ArtLikeResponse.builder()
                .isLiked(isLiked)
                .likeCount(likeCount)
                .build();
    }

    private void validatePixelData(String pixelData, Integer width, Integer height) {
        if (pixelData == null || pixelData.isEmpty()) {
            throw new GlobalException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        // 30x30 고정 크기 검증
        if (width != 30 || height != 30) {
            throw new GlobalException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        // 픽셀 데이터 길이 검증
        if (pixelData.length() != width * height) {
            throw new GlobalException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        // 0과 1로만 구성되어 있는지 검증
        if (!pixelData.matches("[01]+")) {
            throw new GlobalException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}