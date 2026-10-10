package com.esos.apiHotel.repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import com.esos.apiHotel.enums.StatusReserva;
import com.esos.apiHotel.entity.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface ReservaRepository extends JpaRepository<Reserva, Long>, JpaSpecificationExecutor<Reserva> {
    boolean existsByHospedeId(Long id);
    boolean existsByQuartoId(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reserva r where r.id = :id and r.excluido = false")
    Optional<Reserva> buscarComBloqueio(@Param("id") Long id);

    @Query("select count(r) from Reserva r where r.excluido = false and r.quarto.id = :quartoId and r.id <> :ignorarId and r.status = com.esos.apiHotel.enums.StatusReserva.CONFIRMADA and r.entrada < :saida and r.saida > :entrada")
    long contarConflitos(@Param("quartoId") Long quartoId, @Param("entrada") LocalDate entrada, @Param("saida") LocalDate saida, @Param("ignorarId") Long ignorarId);

    @Query("select count(r) from Reserva r where r.excluido = false and r.status = com.esos.apiHotel.enums.StatusReserva.CONFIRMADA and r.quarto.id = :id and r.saida > :hoje and r.quantidadeHospedes > :capacidade")
    long contarAcimaCapacidade(@Param("id") Long id, @Param("hoje") LocalDate hoje, @Param("capacidade") Integer capacidade);

    @Query("select count(r) from Reserva r where r.excluido = false and r.status = com.esos.apiHotel.enums.StatusReserva.CONFIRMADA and r.quarto.id = :id and r.saida > :hoje")
    long contarReservasVigentes(@Param("id") Long id, @Param("hoje") LocalDate hoje);
    Optional<Reserva> findByIdAndExcluidoFalse(Long id);

    @Query("select r.quarto.id from Reserva r where r.id = :id and r.excluido = false")
    Optional<Long> buscarQuartoId(@Param("id") Long id);
}
