package com.xerxes.msme.purchase;

import com.xerxes.msme.common.exception.ResourceNotFoundException;
import com.xerxes.msme.purchase.dto.VendorRequest;
import com.xerxes.msme.purchase.dto.VendorResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VendorService {

    private final VendorRepository vendorRepository;

    public VendorService(VendorRepository vendorRepository) {
        this.vendorRepository = vendorRepository;
    }

    @Transactional
    public VendorResponse create(UUID tenantId, VendorRequest request) {
        Vendor vendor = new Vendor();
        vendor.setTenantId(tenantId);
        vendor.setName(request.name());
        vendor.setGstin(request.gstin());
        vendor.setStateCode(request.stateCode());
        vendor.setContactPerson(request.contactPerson());
        vendor.setPhone(request.phone());
        vendor.setEmail(request.email());
        vendor.setAddressLine1(request.addressLine1());
        vendor.setAddressLine2(request.addressLine2());
        vendor.setCity(request.city());
        vendor.setPincode(request.pincode());
        return toResponse(vendorRepository.save(vendor));
    }

    @Transactional(readOnly = true)
    public List<VendorResponse> list(UUID tenantId) {
        return vendorRepository.findAllByTenantId(tenantId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public VendorResponse get(UUID tenantId, UUID vendorId) {
        Vendor vendor = vendorRepository
                .findByIdAndTenantId(vendorId, tenantId)
                .orElseThrow(() -> ResourceNotFoundException.of("Vendor", vendorId));
        return toResponse(vendor);
    }

    private VendorResponse toResponse(Vendor vendor) {
        return new VendorResponse(
                vendor.getId(),
                vendor.getName(),
                vendor.getGstin(),
                vendor.getStateCode(),
                vendor.getContactPerson(),
                vendor.getPhone(),
                vendor.getEmail(),
                vendor.getAddressLine1(),
                vendor.getAddressLine2(),
                vendor.getCity(),
                vendor.getPincode());
    }
}
