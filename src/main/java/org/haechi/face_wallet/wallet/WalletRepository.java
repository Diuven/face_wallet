package org.haechi.face_wallet.wallet;

import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface WalletRepository extends CrudRepository<WalletEntity, Long> {
    Optional<WalletEntity> findByAddress(String address);
}
