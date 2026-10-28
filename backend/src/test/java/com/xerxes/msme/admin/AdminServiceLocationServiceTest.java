package com.xerxes.msme.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

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
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminServiceLocationServiceTest {

    @Mock private ServiceCityRepository cityRepository;
    @Mock private ServiceAreaRepository areaRepository;

    private AdminServiceLocationService locationService;

    @BeforeEach
    void setUp() {
        locationService = new AdminServiceLocationService(cityRepository, areaRepository);
        lenient().when(cityRepository.save(any(ServiceCity.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(areaRepository.save(any(ServiceArea.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void listCities_returnsEveryCityForTheStateRegardlessOfActiveFlag() {
        ServiceCity pune = new ServiceCity();
        pune.setId(UUID.randomUUID());
        pune.setStateCode("27");
        pune.setName("Pune");
        when(cityRepository.findAllByStateCodeOrderByDisplayOrderAsc("27")).thenReturn(List.of(pune));

        List<AdminServiceCityResponse> result = locationService.listCities("27");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Pune");
    }

    @Test
    void createCity_savesAndReturnsIt() {
        AdminServiceCityResponse response = locationService.createCity(new CreateServiceCityRequest("27", "Pune", 1));

        assertThat(response.name()).isEqualTo("Pune");
        assertThat(response.stateCode()).isEqualTo("27");
        assertThat(response.active()).isTrue();
    }

    @Test
    void updateCity_existing_updatesFields() {
        ServiceCity pune = new ServiceCity();
        pune.setId(UUID.randomUUID());
        pune.setStateCode("27");
        pune.setName("Pune");
        when(cityRepository.findById(pune.getId())).thenReturn(Optional.of(pune));

        AdminServiceCityResponse response = locationService.updateCity(pune.getId(), new UpdateServiceCityRequest("Pune City", 2, false));

        assertThat(response.name()).isEqualTo("Pune City");
        assertThat(response.active()).isFalse();
    }

    @Test
    void updateCity_unknown_throwsResourceNotFound() {
        UUID id = UUID.randomUUID();
        when(cityRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> locationService.updateCity(id, new UpdateServiceCityRequest("X", 1, true)));
    }

    @Test
    void listAreas_returnsEveryAreaForTheCityRegardlessOfActiveFlag() {
        UUID cityId = UUID.randomUUID();
        ServiceArea nigdi = new ServiceArea();
        nigdi.setId(UUID.randomUUID());
        nigdi.setCityId(cityId);
        nigdi.setName("Nigdi");
        nigdi.setPincode("411044");
        when(areaRepository.findAllByCityIdOrderByDisplayOrderAsc(cityId)).thenReturn(List.of(nigdi));

        List<AdminServiceAreaResponse> result = locationService.listAreas(cityId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Nigdi");
        assertThat(result.get(0).pincode()).isEqualTo("411044");
    }

    @Test
    void createArea_validCity_savesAndReturnsIt() {
        UUID cityId = UUID.randomUUID();
        when(cityRepository.existsById(cityId)).thenReturn(true);

        AdminServiceAreaResponse response = locationService.createArea(new CreateServiceAreaRequest(cityId, "Nigdi", "411044", 1));

        assertThat(response.name()).isEqualTo("Nigdi");
        assertThat(response.pincode()).isEqualTo("411044");
        assertThat(response.cityId()).isEqualTo(cityId);
    }

    @Test
    void createArea_unknownCity_throwsResourceNotFound() {
        UUID cityId = UUID.randomUUID();
        when(cityRepository.existsById(cityId)).thenReturn(false);

        assertThrows(
                ResourceNotFoundException.class,
                () -> locationService.createArea(new CreateServiceAreaRequest(cityId, "Nigdi", "411044", 1)));
    }

    @Test
    void updateArea_existing_updatesFields() {
        ServiceArea nigdi = new ServiceArea();
        nigdi.setId(UUID.randomUUID());
        nigdi.setCityId(UUID.randomUUID());
        nigdi.setName("Nigdi");
        nigdi.setPincode("411044");
        when(areaRepository.findById(nigdi.getId())).thenReturn(Optional.of(nigdi));

        AdminServiceAreaResponse response =
                locationService.updateArea(nigdi.getId(), new UpdateServiceAreaRequest("Nigdi Sector 26", "411044", 2, false));

        assertThat(response.name()).isEqualTo("Nigdi Sector 26");
        assertThat(response.active()).isFalse();
    }

    @Test
    void updateArea_unknown_throwsResourceNotFound() {
        UUID id = UUID.randomUUID();
        when(areaRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> locationService.updateArea(id, new UpdateServiceAreaRequest("X", "000000", 1, true)));
    }
}
