package com.btproject.loanplatform.notification_service.application.port.out;

import com.btproject.loanplatform.notification_service.application.model.CustomerContact;

import java.util.Optional;

public interface CustomerContactPort {

    Optional<CustomerContact> findByCif(String cif);
}
