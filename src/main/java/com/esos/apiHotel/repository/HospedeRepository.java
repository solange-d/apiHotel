package com.esos.apiHotel.repository;

import com.esos.apiHotel.entity.Hospede;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface HospedeRepository extends JpaRepository<Hospede, Long>, JpaSpecificationExecutor<Hospede> {
    boolean existsByDocumento(String documento);
    boolean existsByDocumentoAndIdNot(String documento, Long id);
}