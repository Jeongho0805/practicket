package com.example.ticketing.art.application;

import com.example.ticketing.art.domain.entity.Art;
import com.example.ticketing.art.domain.entity.ArtComment;
import com.example.ticketing.art.domain.entity.ArtLike;
import com.example.ticketing.art.domain.entity.ArtView;
import com.example.ticketing.art.domain.repository.*;
import com.example.ticketing.art.dto.*;
import com.example.ticketing.client.component.ClientManager;
import com.example.ticketing.client.domain.Client;
import com.example.ticketing.common.auth.ClientInfo;
import com.example.ticketing.common.exception.ErrorCode;
import com.example.ticketing.common.exception.GlobalException;
import com.example.ticketing.common.exception.ValidateException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArtService {

    private final ArtRepository artRepository;
    private final ArtLikeRepository artLikeRepository;
    private final ArtCommentRepository artCommentRepository;
    private final ArtViewRepository artViewRepository;
    private final ClientManager clientManager;

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
        Long currentClientId = clientInfo != null ? clientInfo.getClientId() : null;

        ArtQueryCondition queryCondition = ArtQueryCondition.builder()
                .keyword(condition.getKeyword())
                .sortBy(condition.getSortBy())
                .sortDirection(condition.getSortDirection())
                .filterType(condition.getFilterType())
                .currentClientId(currentClientId)
                .build();

        Page<Art> arts = artRepository.searchArts(queryCondition, pageable);

        Client client = clientInfo != null ? clientManager.findById(clientInfo.getClientId()) : null;
        if (client == null) {
            return arts.map(art -> ArtResponse.from(art, false));
        }
        return arts.map(art -> {
            boolean isLiked = artLikeRepository.existsByArtAndClient(art, client);
            return ArtResponse.from(art, isLiked);
        });
    }

    @Transactional
    public ArtResponse getArt(ClientInfo clientInfo, Long artId) {
        Art art = artRepository.findById(artId)
                .orElseThrow(() -> new GlobalException(ErrorCode.INTERNAL_SERVER_ERROR));

        Client client = clientInfo != null ? clientManager.findById(clientInfo.getClientId()) : null;

        // 조회수 증가: 이미 조회한 사용자가 아닌 경우에만
        if (client != null && !artViewRepository.existsByArtAndClient(art, client)) {
            ArtView artView = ArtView.builder()
                    .art(art)
                    .client(client)
                    .build();
            artViewRepository.save(artView);
            artRepository.incrementViewCount(artId);
        }

        // 좋아요 여부 및 소유 여부 확인
        if (client == null) {
            return ArtResponse.from(art, false, false);
        }

        boolean isLiked = artLikeRepository.existsByArtAndClient(art, client);
        boolean isOwned = art.getClient().getId().equals(client.getId());

        return ArtResponse.from(art, isLiked, isOwned);
    }

    @Transactional
    public ArtResponse updateArt(Long artId, ArtUpdateRequest request, ClientInfo clientInfo) {
        Art art = artRepository.findById(artId)
                .orElseThrow(() -> new GlobalException(ErrorCode.INTERNAL_SERVER_ERROR));

        if (!art.getClient().getId().equals(clientInfo.getClientId())) {
            throw new ValidateException(ErrorCode.FORBIDDEN);
        }
        this.validatePixelData(request.getPixelData(), art.getWidth(), art.getHeight());
        art.update(request.getTitle(), request.getPixelData());

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

    public Page<ArtCommentResponse> getComments(Long artId, ClientInfo clientInfo, Pageable pageable) {
        Art art = artRepository.findById(artId).orElseThrow(() -> new GlobalException(ErrorCode.RESOURCE_NOT_FOUND));
        Page<ArtComment> comments = artCommentRepository.findByArtOrderByCreatedAtAsc(art, pageable);

        return comments.map(comment -> {
            if (comment.getClient().getId().equals(clientInfo.getClientId())) {
                return ArtCommentResponse.from(comment, true);
            }
            return ArtCommentResponse.from(comment, false);
        });
    }

    @Transactional
    public ArtCommentResponse createComment(Long artId, ArtCommentRequest request, ClientInfo clientInfo) {
        Art art = artRepository.findById(artId)
                .orElseThrow(() -> new GlobalException(ErrorCode.INTERNAL_SERVER_ERROR));

        Client client = clientManager.findById(clientInfo.getClientId());

        ArtComment comment = ArtComment.builder()
                .content(request.getContent())
                .art(art)
                .client(client)
                .build();

        ArtComment savedComment = artCommentRepository.save(comment);
        artRepository.incrementCommentCount(artId);
        return ArtCommentResponse.from(savedComment, true);
    }

    @Transactional
    public ArtCommentResponse updateComment(Long commentId, ArtCommentRequest request, ClientInfo clientInfo) {
        ArtComment comment = artCommentRepository.findById(commentId)
                .orElseThrow(() -> new GlobalException(ErrorCode.INTERNAL_SERVER_ERROR));

        if (!comment.getClient().getId().equals(clientInfo.getClientId())) {
            throw new GlobalException(ErrorCode.FORBIDDEN);
        }

        comment.updateContent(request.getContent());
        return ArtCommentResponse.from(comment, true);
    }

    @Transactional
    public void deleteComment(Long commentId, ClientInfo clientInfo) {
        ArtComment comment = artCommentRepository.findById(commentId)
                .orElseThrow(() -> new GlobalException(ErrorCode.RESOURCE_NOT_FOUND));

        if (!comment.getClient().getId().equals(clientInfo.getClientId())) {
            throw new GlobalException(ErrorCode.FORBIDDEN);
        }

        Long artId = comment.getArt().getId();
        artCommentRepository.delete(comment);
        artRepository.decrementCommentCount(artId);
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