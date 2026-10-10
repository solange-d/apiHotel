package com.esos.apiHotel.repository;

import java.math.BigDecimal;
import com.esos.apiHotel.enums.StatusPagamento;
import com.esos.apiHotel.entity.Pagamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface PagamentoRepository extends JpaRepository<Pagamento, Long>, JpaSpecificationExecutor<Pagamento> {
    @Query("select coalesce(sum(p.valor), 0) from Pagamento p where p.excluido = false and p.reserva.id = :id and p.status = com.esos.apiHotel.enums.StatusPagamento.CONFIRMADO")
    BigDecimal totalPago(@Param("id") Long id);
    boolean existsByReservaIdAndStatusAndExcluidoFalse(Long id, StatusPagamento status);
    Optional<Pagamento> findByIdAndExcluidoFalse(Long id);

    @Query("select p.reserva.id from Pagamento p where p.id = :id and p.excluido = false")
    Optional<Long> buscarReservaId(@Param("id") Long id);
}
