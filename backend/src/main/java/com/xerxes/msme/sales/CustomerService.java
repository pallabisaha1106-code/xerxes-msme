package com.xerxes.msme.sales;

import com.xerxes.msme.common.exception.ResourceNotFoundException;
import com.xerxes.msme.sales.dto.CustomerRequest;
import com.xerxes.msme.sales.dto.CustomerResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional
    public CustomerResponse create(UUID tenantId, CustomerRequest request) {
        Customer customer = new Customer();
        customer.setTenantId(tenantId);
        customer.setName(request.name());
        customer.setGstin(request.gstin());
        customer.setStateCode(request.stateCode());
        customer.setContactPerson(request.contactPerson());
        customer.setPhone(request.phone());
        customer.setEmail(request.email());
        customer.setAddressLine1(request.addressLine1());
        customer.setAddressLine2(request.addressLine2());
        customer.setCity(request.city());
        customer.setPincode(request.pincode());
        return toResponse(customerRepository.save(customer));
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> list(UUID tenantId) {
        return customerRepository.findAllByTenantId(tenantId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CustomerResponse get(UUID tenantId, UUID customerId) {
        Customer customer = customerRepository
                .findByIdAndTenantId(customerId, tenantId)
                .orElseThrow(() -> ResourceNotFoundException.of("Customer", customerId));
        return toResponse(customer);
    }

    private CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getName(),
                customer.getGstin(),
                customer.getStateCode(),
                customer.getContactPerson(),
                customer.getPhone(),
                customer.getEmail(),
                customer.getAddressLine1(),
                customer.getAddressLine2(),
                customer.getCity(),
                customer.getPincode());
    }
}
