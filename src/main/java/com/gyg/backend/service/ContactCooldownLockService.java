package com.gyg.backend.service;

import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.locks.ReentrantLock;

@Service
public class ContactCooldownLockService {

    private static final ReentrantLock NON_POSTGRES_LOCK = new ReentrantLock();

    private final EntityManager entityManager;
    private final boolean postgres;

    public ContactCooldownLockService(
            EntityManager entityManager,
            @Value("${spring.datasource.url:}") String datasourceUrl) {
        this.entityManager = entityManager;
        this.postgres = datasourceUrl.startsWith("jdbc:postgresql:");
    }

    public void lockByEmail(String normalizedEmail) {
        if (postgres) {
            entityManager.createNativeQuery(
                            "SELECT pg_advisory_xact_lock(hashtextextended(:email, 0))")
                    .setParameter("email", normalizedEmail)
                    .getResultList();
            return;
        }

        NON_POSTGRES_LOCK.lock();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    NON_POSTGRES_LOCK.unlock();
                }
            });
        } else {
            NON_POSTGRES_LOCK.unlock();
        }
    }
}
