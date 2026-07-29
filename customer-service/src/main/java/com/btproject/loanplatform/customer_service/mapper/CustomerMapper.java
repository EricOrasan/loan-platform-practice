package com.btproject.loanplatform.customer_service.mapper;

import com.btproject.loanplatform.customer_service.domain.Customer;
import com.btproject.loanplatform.customer_service.dto.CreateCustomerRequest;
import com.btproject.loanplatform.customer_service.dto.CustomerResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CustomerMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Customer toEntity(CreateCustomerRequest request);

    CustomerResponse toResponse(Customer customer);
}
