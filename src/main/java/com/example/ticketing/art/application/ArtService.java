package com.example.ticketing.art.application;

import com.example.ticketing.art.domain.Art;
import com.example.ticketing.art.domain.ArtRepository;
import com.example.ticketing.art.dto.ArtCreateRequest;
import com.example.ticketing.art.dto.ArtResponse;
import com.example.ticketing.art.dto.ArtUpdateRequest;
import com.example.ticketing.client.component.ClientManager;
import com.example.ticketing.client.domain.Client;
import com.example.ticketing.common.auth.ClientInfo;
import com.example.ticketing.common.exception.ErrorCode;
import com.example.ticketing.common.exception.GlobalException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ClientManager clientManager;
    private final ObjectMapper objectMapper;

    @Transactional
    public ArtResponse createArt(ArtCreateRequest request, ClientInfo clientInfo) {
        Client client = clientManager.findById(clientInfo.getClientId());
        validatePixelData(request.getPixelData(), request.getWidth(), request.getHeight());

        Art art = Art.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .pixelData(request.getPixelData())
                .width(request.getWidth())
                .height(request.getHeight())
                .isPublic(request.getIsPublic())
                .client(client)
                .build();

        Art savedArt = artRepository.save(art);
        return ArtResponse.from(savedArt);
    }

    public Page<ArtResponse> getPublicArts(Pageable pageable) {
        Page<Art> arts = artRepository.findByIsPublicTrueOrderByCreatedAtDesc(pageable);
        return arts.map(ArtResponse::from);
    }

    public ArtResponse getArt(Long artId) {
        Art art = artRepository.findByIdAndIsPublicTrue(artId)
                .orElseThrow(() -> new GlobalException(ErrorCode.INTERNAL_SERVER_ERROR));

        // 조회수 증가
        art.incrementViewCount();
        artRepository.save(art);

        return ArtResponse.from(art);
    }

    public Page<ArtResponse> getMyArts(ClientInfo clientInfo, Pageable pageable) {
        Client client = clientManager.findById(clientInfo.getClientId());
        Page<Art> arts = artRepository.findByClientAndIsPublicTrueOrderByCreatedAtDesc(client, pageable);
        return arts.map(ArtResponse::from);
    }

    @Transactional
    public ArtResponse updateArt(Long artId, ArtUpdateRequest request, ClientInfo clientInfo) {
        Art art = artRepository.findById(artId)
                .orElseThrow(() -> new GlobalException(ErrorCode.INTERNAL_SERVER_ERROR));

        if (!art.getClient().getId().equals(clientInfo.getClientId())) {
            throw new GlobalException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        art.updateInfo(request.getTitle(), request.getDescription());
        if (request.getIsPublic() != null) {
            art.toggleVisibility();
        }

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