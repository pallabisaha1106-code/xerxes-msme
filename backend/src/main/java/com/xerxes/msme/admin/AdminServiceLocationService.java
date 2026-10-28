package com.xerxes.msme.admin;

import com.xerxes.msme.admin.dto.AdminServiceAreaResponse;
import com.xerxes.msme.admin.dto.AdminServiceCityResponse;
import com.xerxes.msme.admin.dto.CreateServiceAreaRequest;
import com.xerxes.msme.admin.dto.CreateServiceCityRequest;
import com.xerxes.msme.admin.dto.UpdateServiceAreaRequest;
import com.xerxes.msme.admin.dto.UpdateServiceCityRequest;
import com.xerxes.msme.common.exception.ResourceNotFoundException;
import com.xerxes.msme.marketplace.ServiceArea;
import com.xerxes.msme.marketplace.ServiceAreaRepository;
import com.xerxes.msme.marketplace.ServiceCity;
import com.xerxes.msme.marketplace.ServiceCityRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Super Admin curation of where the marketplace is supported - see ServiceCity/ServiceArea. */
@Service
public class AdminServiceLocationService {

    private final ServiceCityRepository cityRepository;
    private final ServiceAreaRepository areaRepository;

    public AdminServiceLocationService(ServiceCityRepository cityRepository, ServiceAreaRepository areaRepository) {
        this.cityRepository = cityRepository;
        this.areaRepository = areaRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminServiceCityResponse> listCities(String stateCode) {
        return cityRepository.findAllByStateCodeOrderByDisplayOrderAsc(stateCode).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AdminServiceCityResponse createCity(CreateServiceCityRequest request) {
        ServiceCity city = new ServiceCity();
        city.setStateCode(request.stateCode());
        city.setName(request.name());
        city.setDisplayOrder(request.displayOrder());
        cityRepository.save(city);
        return toResponse(city);
    }

    @Transactional
    public AdminServiceCityResponse updateCity(UUID id, UpdateServiceCityRequest request) {
        ServiceCity city = cityRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("ServiceCity", id));
        city.setName(request.name());
        city.setDisplayOrder(request.displayOrder());
        city.setActive(request.active());
        cityRepository.save(city);
        return toResponse(city);
    }

    @Transactional(readOnly = true)
    public List<AdminServiceAreaResponse> listAreas(UUID cityId) {
        return areaRepository.findAllByCityIdOrderByDisplayOrderAsc(cityId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public AdminServiceAreaResponse createArea(CreateServiceAreaRequest request) {
        if (!cityRepository.existsById(request.cityId())) {
            throw ResourceNotFoundException.of("ServiceCity", request.cityId());
        }
        ServiceArea area = new ServiceArea();
        area.setCityId(request.cityId());
        area.setName(request.name());
        area.setPincode(request.pincode());
        area.setDisplayOrder(request.displayOrder());
        areaRepository.save(area);
        return toResponse(area);
    }

    @Transactional
    public AdminServiceAreaResponse updateArea(UUID id, UpdateServiceAreaRequest request) {
        ServiceArea area = areaRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("ServiceArea", id));
        area.setName(request.name());
        area.setPincode(request.pincode());
        area.setDisplayOrder(request.displayOrder());
        area.setActive(request.active());
        areaRepository.save(area);
        return toResponse(area);
    }

    private AdminServiceCityResponse toResponse(ServiceCity city) {
        return new AdminServiceCityResponse(city.getId(), city.getStateCode(), city.getName(), city.getDisplayOrder(), city.isActive());
    }

    private AdminServiceAreaResponse toResponse(ServiceArea area) {
        return new AdminServiceAreaResponse(
                area.getId(), area.getCityId(), area.getName(), area.getPincode(), area.getDisplayOrder(), area.isActive());
    }
}
