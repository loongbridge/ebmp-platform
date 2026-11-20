package com.ebmp.masterdata;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContactRepository extends JpaRepository<Contact, Long> {
    List<Contact> findByCustomerIdOrderByIsPrimaryDescIdAsc(Long customerId);

    void deleteByCustomerId(Long customerId);
}
