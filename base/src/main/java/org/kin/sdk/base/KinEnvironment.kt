package org.kin.sdk.base

import io.grpc.ManagedChannel
import okhttp3.OkHttpClient
import org.kin.sdk.base.models.Key
import org.kin.sdk.base.models.KinAccount
import org.kin.sdk.base.models.asKinAccountId
import org.kin.sdk.base.network.api.FriendBotApi
import org.kin.sdk.base.network.api.KinAccountApi
import org.kin.sdk.base.network.api.KinAccountCreationApi
import org.kin.sdk.base.network.api.KinStreamingApi
import org.kin.sdk.base.network.api.KinTransactionApi
import org.kin.sdk.base.network.api.KinTransactionWhitelistingApi
import org.kin.sdk.base.network.api.agora.AgoraKinAccountsApi
import org.kin.sdk.base.network.api.agora.AgoraKinTransactionsApi
import org.kin.sdk.base.network.api.agora.AppUserAuthInterceptor
import org.kin.sdk.base.network.api.agora.OkHttpChannelBuilderForcedTls12
import org.kin.sdk.base.network.api.horizon.DefaultHorizonKinAccountCreationApi
import org.kin.sdk.base.network.api.horizon.DefaultHorizonKinTransactionWhitelistingApi
import org.kin.sdk.base.network.api.horizon.HorizonKinApi
import org.kin.sdk.base.network.services.AppInfoProvider
import org.kin.sdk.base.network.services.KinService
import org.kin.sdk.base.network.services.KinServiceImpl
import org.kin.sdk.base.repository.AppInfoRepository
import org.kin.sdk.base.repository.InMemoryAppInfoRepositoryImpl
import org.kin.sdk.base.repository.InMemoryInvoiceRepositoryImpl
import org.kin.sdk.base.repository.InvoiceRepository
import org.kin.sdk.base.stellar.models.ApiConfig
import org.kin.sdk.base.stellar.models.NetworkEnvironment
import org.kin.sdk.base.storage.KinFileStorage
import org.kin.sdk.base.storage.Storage
import org.kin.sdk.base.tools.Callback
import org.kin.sdk.base.tools.ExecutorServices
import org.kin.sdk.base.tools.NetworkOperationsHandler
import org.kin.sdk.base.tools.NetworkOperationsHandlerImpl
import org.kin.sdk.base.tools.Promise
import org.kin.sdk.base.tools.callback
import org.slf4j.ILoggerFactory
import org.slf4j.LoggerFactory

sealed class KinEnvironment {
    abstract val networkEnvironment: NetworkEnvironment
    abstract val logger: ILoggerFactory
    abstract val service: KinService
    internal abstract val storage: Storage
    internal abstract val executors: ExecutorServices
    internal abstract val networkHandler: NetworkOperationsHandler

    @Deprecated("Please use [KinEnvironment.Agora] instead. Horizon may dissapear in a future blockchain migration.")
    class Horizon private constructor(
        internal val okHttpClient: OkHttpClient,
        override val networkEnvironment: NetworkEnvironment,
        override val logger: ILoggerFactory,
        override val storage: Storage,
        override val executors: ExecutorServices,
        override val networkHandler: NetworkOperationsHandler,
        override val service: KinService
    ) : KinEnvironment() {
        class Builder(private val networkEnvironment: NetworkEnvironment) {
            private var accountCreationApi: KinAccountCreationApi? = null
            private var transactionWhitelistingApi: KinTransactionWhitelistingApi? = null
            private var okHttpClient: OkHttpClient? = null
            private var executors: ExecutorServices? = null
            private var logger: ILoggerFactory? = null
            private var networkHandler: NetworkOperationsHandler? = null
            private var service: KinService? = null

            private lateinit var storage: Storage
            private var storageBuilder: KinFileStorage.Builder? = null

            inner class CompletedBuilder internal constructor() {
                private fun NetworkEnvironment.horizonApiConfig() = when (this) {
                    NetworkEnvironment.KinStellarTestNet -> ApiConfig.TestNetHorizon
                    NetworkEnvironment.KinStellarMainNet -> ApiConfig.MainNetHorizon
                }

                fun build(): KinEnvironment {
                    val okHttpClient = okHttpClient ?: OkHttpClient.Builder().build()
                    val logger = logger ?: LoggerFactory.getILoggerFactory()
                    val executors = executors ?: ExecutorServices()
                    val networkHandler = networkHandler ?: NetworkOperationsHandlerImpl(
                        executors.sequentialScheduled,
                        executors.parallelIO,
                        logger,
                        shouldRetryError = { it is KinService.FatalError.TransientFailure }
                    )
                    val api = HorizonKinApi(
                        networkEnvironment.horizonApiConfig(),
                        okHttpClient
                    )
                    val service = service ?: KinServiceImpl(
                        networkEnvironment,
                        networkHandler,
                        api as KinAccountApi,
                        api as KinTransactionApi,
                        api as KinStreamingApi,
                        accountCreationApi ?: DefaultHorizonKinAccountCreationApi(
                            networkEnvironment.horizonApiConfig(),
                            FriendBotApi(okHttpClient)
                        ),
                        transactionWhitelistingApi ?: DefaultHorizonKinTransactionWhitelistingApi()
                    )

                    val storageBuilder = storageBuilder
                    if (!this@Builder::storage.isInitialized && storageBuilder != null) {
                        storage = storageBuilder.setNetworkEnvironment(networkEnvironment).build()
                    }

                    return Horizon(
                        okHttpClient = okHttpClient,
                        networkEnvironment = networkEnvironment,
                        logger = logger,
                        storage = storage,
                        executors = executors,
                        networkHandler = networkHandler,
                        service = service
                    )
                }
            }

            internal fun setOkHttpClient(okHttpClient: OkHttpClient): Builder = apply {
                this.okHttpClient = okHttpClient
            }

            internal fun setExecutorServices(executors: ExecutorServices): Builder = apply {
                this.executors = executors
            }

            internal fun setNetworkOperationsHandler(networkHandler: NetworkOperationsHandler): Builder =
                apply {
                    this.networkHandler = networkHandler
                }

            fun setLogger(logger: ILoggerFactory): Builder = apply {
                this.logger = logger
            }

            fun setKinService(kinService: KinService): Builder = apply {
                this.service = kinService
            }

            fun setKinAccountCreationApi(accountCreationApi: KinAccountCreationApi): Builder =
                apply {
                    this.accountCreationApi = accountCreationApi
                }

            fun setKinTransactionWhitelistingApi(transactionWhitelistingApi: KinTransactionWhitelistingApi): Builder =
                apply {
                    this.transactionWhitelistingApi = transactionWhitelistingApi
                }

            fun setStorage(storage: Storage): CompletedBuilder {
                this.storage = storage
                return CompletedBuilder()
            }

            fun setStorage(fileStorageBuilder: KinFileStorage.Builder): CompletedBuilder =
                with(this) {
                    this.storageBuilder = fileStorageBuilder
                    CompletedBuilder()
                }
        }
    }

    class Agora private constructor(
        private val managedChannel: ManagedChannel,
        override val networkEnvironment: NetworkEnvironment,
        override val logger: ILoggerFactory,
        override val storage: Storage,
        override val executors: ExecutorServices,
        override val networkHandler: NetworkOperationsHandler,
        override val service: KinService,
        val appInfoRepository: AppInfoRepository = InMemoryAppInfoRepositoryImpl(),
        val invoiceRepository: InvoiceRepository = InMemoryInvoiceRepositoryImpl(),
        val appInfoProvider: AppInfoProvider
    ) : KinEnvironment() {
        class Builder(private val networkEnvironment: NetworkEnvironment) {
            private var managedChannel: ManagedChannel? = null
            private var executors: ExecutorServices? = null
            private var logger: ILoggerFactory? = null
            private var networkHandler: NetworkOperationsHandler? = null
            private var appInfoProvider: AppInfoProvider? = null
            private var service: KinService? = null

            private lateinit var storage: Storage
            private var storageBuilder: KinFileStorage.Builder? = null

            inner class CompletedBuilder internal constructor() {
                private fun NetworkEnvironment.horizonApiConfig() = when (this) {
                    NetworkEnvironment.KinStellarTestNet -> ApiConfig.TestNetHorizon
                    NetworkEnvironment.KinStellarMainNet -> ApiConfig.MainNetHorizon
                }

                fun build(): Agora {
                    val logger = logger ?: LoggerFactory.getILoggerFactory()
                    val executors = executors ?: ExecutorServices()
                    val networkHandler = networkHandler ?: NetworkOperationsHandlerImpl(
                        executors.sequentialScheduled,
                        executors.parallelIO,
                        logger,
                        shouldRetryError = { it is KinService.FatalError.TransientFailure }
                    )
                    val appInfoProvider = appInfoProvider
                        ?: throw KinEnvironmentBuilderException("Must provide an ApplicationDelegate!")
                    val managedChannel =
                        managedChannel ?: networkEnvironment.agoraApiConfig()
                            .asManagedChannel()
                    val accountsApi = AgoraKinAccountsApi(managedChannel, networkEnvironment)
                    val transactionsApi =
                        AgoraKinTransactionsApi(
                            managedChannel,
                            networkEnvironment
                        )
                    val service = service ?: KinServiceImpl(
                        networkEnvironment,
                        networkHandler,
                        accountsApi,
                        transactionsApi,
                        accountsApi,
                        accountsApi,
                        transactionsApi
                    )

                    val storageBuilder = storageBuilder
                    if (!this@Builder::storage.isInitialized && storageBuilder != null) {
                        storage = storageBuilder.setNetworkEnvironment(networkEnvironment).build()
                    }

                    return Agora(
                        managedChannel,
                        networkEnvironment = networkEnvironment,
                        logger = logger,
                        storage = storage,
                        executors = executors,
                        networkHandler = networkHandler,
                        service = service,
                        appInfoProvider = appInfoProvider
                    ).apply {
                        appInfoRepository.addAppInfo(appInfoProvider.appInfo)

                        with(storage) {
                            getAllAccountIds().forEach {
                                getInvoiceListsMapForAccountId(it)
                                    .flatMap {
                                        invoiceRepository.addAllInvoices(it.values.map { it.invoices }
                                            .reduce { acc, list -> acc + list })
                                    }.resolve()
                            }
                        }
                    }
                }

                private fun NetworkEnvironment.agoraApiConfig() = when (this) {
                    NetworkEnvironment.KinStellarTestNet -> ApiConfig.TestNetAgora
                    NetworkEnvironment.KinStellarMainNet -> ApiConfig.MainNetAgora
                }

                private fun ApiConfig.asManagedChannel() =
                    OkHttpChannelBuilderForcedTls12.forAddress(networkEndpoint, tlsPort)
                        .intercept(
                            *listOf(
                                AppUserAuthInterceptor(appInfoProvider!!)
                            ).toTypedArray()
                        )
                        .build()
            }

            internal fun setManagedChannel(managedChannel: ManagedChannel): Builder = apply {
                this.managedChannel = managedChannel
            }

            internal fun setExecutorServices(executors: ExecutorServices): Builder = apply {
                this.executors = executors
            }

            internal fun setNetworkOperationsHandler(networkHandler: NetworkOperationsHandler): Builder =
                apply {
                    this.networkHandler = networkHandler
                }

            fun setLogger(logger: ILoggerFactory): Builder = apply {
                this.logger = logger
            }

            fun setAppInfoProvider(appInfoProvider: AppInfoProvider) = apply {
                this.appInfoProvider = appInfoProvider
            }

            fun setKinService(kinService: KinService): Builder = apply {
                this.service = kinService
            }

            fun setStorage(storage: Storage): CompletedBuilder = with(this) {
                this.storage = storage
                CompletedBuilder()
            }

            fun setStorage(fileStorageBuilder: KinFileStorage.Builder): Builder.CompletedBuilder =
                with(this) {
                    this.storageBuilder = fileStorageBuilder
                    CompletedBuilder()
                }
        }
    }

    fun importPrivateKey(privateKey: Key.PrivateKey): Promise<Boolean> {
        return Promise.create<Boolean> { resolve, reject ->
            if (storage.getAccount(privateKey.asKinAccountId()) == null) {
                service.getAccount(privateKey.asKinAccountId())
                    .then({
                        try {
                            resolve(storage.addAccount(it.copy(key = privateKey)))
                        } catch (t: Throwable) {
                            reject(t)
                        }
                    }, {
                        try {
                            resolve(storage.addAccount(KinAccount(privateKey)))
                        } catch (t: Throwable) {
                            reject(t)
                        }
                    })
            } else resolve(true)
        }
    }

    fun importPrivateKey(privateKey: Key.PrivateKey, callback: Callback<Boolean>) {
        return importPrivateKey(privateKey).callback(callback)
    }

    fun allAccountIds(): Promise<List<KinAccount.Id>> {
        return Promise.create { resolve, reject ->
            try {
                resolve(storage.getAllAccountIds())
            } catch (t: Throwable) {
                reject(t)
            }
        }
    }

    class KinEnvironmentBuilderException(s: String) : IllegalStateException(s)
}




