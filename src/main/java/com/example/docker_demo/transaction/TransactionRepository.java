package com.example.docker_demo.transaction;

import org.springframework.data.repository.CrudRepository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends CrudRepository<TransactionEntity, Long>, CustomTransactionRepository{
    Optional<TransactionEntity> findByTransactionHash(String hash);
    Optional<TransactionEntity> findByToAddressAndNonce(String address, Long nonce);

}

interface CustomTransactionRepository {
    List<TransactionEntity> findFirstKByAddress(String address, Date starting_after, Date ending_before, int size);
}

class CustomTransactionRepositoryImpl implements CustomTransactionRepository {

    @PersistenceContext
    private EntityManager entityManager;
    @Override
    public List<TransactionEntity> findFirstKByAddress(String address, Date starting_after, Date ending_before, int size) {
        StringBuilder queryString = new StringBuilder("SELECT t FROM transaction t WHERE t.address = :address ");
        if (starting_after != null){
            queryString.append("AND t.ts >= :startDate ");
        }
        if (ending_before != null) {
            queryString.append("AND t.ts <= :endDate ");
        }
        queryString.append("ORDER BY t.ts ");
        return entityManager.createQuery(queryString.toString(), TransactionEntity.class)
                .setParameter("address", address)
                .setParameter("startDate", starting_after)
                .setParameter("endDate", ending_before)
                .setMaxResults(size)
                .getResultList();
    }
}