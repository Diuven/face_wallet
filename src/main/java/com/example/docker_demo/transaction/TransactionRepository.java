package com.example.docker_demo.transaction;

import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface TransactionRepository extends CrudRepository<TransactionEntity, Long> {
    Optional<TransactionEntity> findByTransactionHash(String hash);
    Optional<TransactionEntity> findByToAddressAndNonce(String address, Long nonce);
}
