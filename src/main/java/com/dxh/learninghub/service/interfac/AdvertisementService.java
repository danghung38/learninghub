package com.dxh.learninghub.service.interfac;

import com.dxh.learninghub.dto.request.AdvertisementCreationRequest;
import com.dxh.learninghub.dto.request.AdvertisementUpdateRequest;
import com.dxh.learninghub.dto.response.AdvertisementResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AdvertisementService {

    AdvertisementResponse create(AdvertisementCreationRequest request, MultipartFile image);

    AdvertisementResponse update(Long id, AdvertisementUpdateRequest request, MultipartFile image);

    AdvertisementResponse deactivate(Long id);

    List<AdvertisementResponse> getActiveAdvertisements();
}
