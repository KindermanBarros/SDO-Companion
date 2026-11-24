package com.kinderman.sdo.data.di

import com.kinderman.sdo.data.auth.FirebaseAuthRepository
import com.kinderman.sdo.data.repository.DeityGmRemoteDataSource
import com.kinderman.sdo.data.repository.FirestoreCampaignJournalRepository
import com.kinderman.sdo.data.repository.FirestoreDeityGmRemoteDataSource
import com.kinderman.sdo.data.repository.LocalCatalogRepository
import com.kinderman.sdo.data.repository.SecuredDeityRepository
import com.kinderman.sdo.data.repository.SyncedCampaignRepository
import com.kinderman.sdo.data.repository.SyncedCharacterRepository
import com.kinderman.sdo.data.repository.SyncedOperationsRepository
import com.kinderman.sdo.data.repository.SyncedOwnerRepository
import com.kinderman.sdo.data.sync.AndroidConnectivityMonitor
import com.kinderman.sdo.domain.repository.AuthRepository
import com.kinderman.sdo.domain.repository.CampaignJournalRepository
import com.kinderman.sdo.domain.repository.CampaignRepository
import com.kinderman.sdo.domain.repository.CatalogRepository
import com.kinderman.sdo.domain.repository.CharacterRepository
import com.kinderman.sdo.domain.repository.ConnectivityMonitor
import com.kinderman.sdo.domain.repository.DeityRepository
import com.kinderman.sdo.domain.repository.OperationsRepository
import com.kinderman.sdo.domain.repository.OwnerRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindCharacterRepository(impl: SyncedCharacterRepository): CharacterRepository

    @Binds
    @Singleton
    abstract fun bindOwnerRepository(impl: SyncedOwnerRepository): OwnerRepository

    @Binds
    @Singleton
    abstract fun bindCatalogRepository(impl: LocalCatalogRepository): CatalogRepository

    @Binds
    @Singleton
    abstract fun bindCampaignRepository(impl: SyncedCampaignRepository): CampaignRepository

    @Binds
    @Singleton
    abstract fun bindCampaignJournalRepository(
        impl: FirestoreCampaignJournalRepository,
    ): CampaignJournalRepository

    @Binds
    @Singleton
    abstract fun bindOperationsRepository(impl: SyncedOperationsRepository): OperationsRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: FirebaseAuthRepository): AuthRepository

    @Binds
    @Singleton
    abstract fun bindConnectivityMonitor(impl: AndroidConnectivityMonitor): ConnectivityMonitor

    @Binds
    @Singleton
    abstract fun bindDeityGmRemoteDataSource(impl: FirestoreDeityGmRemoteDataSource): DeityGmRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindDeityRepository(impl: SecuredDeityRepository): DeityRepository
}
