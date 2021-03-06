package org.kin.sdk.demo.viewmodel.modern

import org.kin.sdk.base.KinAccountContext
import org.kin.sdk.base.KinEnvironment
import org.kin.sdk.demo.viewmodel.HomeViewModel
import org.kin.sdk.demo.viewmodel.InvoicesViewModel
import org.kin.sdk.demo.viewmodel.DemoNavigator
import org.kin.sdk.demo.viewmodel.WalletViewModel
import org.kin.sdk.design.viewmodel.tools.BaseViewModel

class ModernHomeViewModel(
    private val navigator: DemoNavigator,
    args: HomeViewModel.NavigationArgs,
    private val testNetKinEnvironment: KinEnvironment,
    private val mainNetKinEnvironment: KinEnvironment
) : HomeViewModel, BaseViewModel<HomeViewModel.NavigationArgs, HomeViewModel.State>(args) {

    private data class WalletItemViewModel(
        val index: Int,
        override val publicAddress: String,
        val navigator: DemoNavigator
    ) : HomeViewModel.WalletItemViewModel {
        override fun onItemTapped() =
            navigator.navigateTo(WalletViewModel.NavigationArgs(index, publicAddress))
    }

    private inner class AddTestNetWalletItemViewModel : HomeViewModel.AddWalletItemViewModel {
        override fun onItemTapped() = createTestNetAccount()

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is AddTestNetWalletItemViewModel) return false
            return true
        }

        override fun hashCode(): Int {
            return javaClass.hashCode()
        }
    }

    private inner class AddMainNetWalletItemViewModel() : HomeViewModel.AddWalletItemViewModel {
        override fun onItemTapped() {
            TODO("Not yet implemented")
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is AddMainNetWalletItemViewModel) return false
            return true
        }

        override fun hashCode(): Int {
            return javaClass.hashCode()
        }
    }

    private data class InvoiceItemViewModel(val navigator: DemoNavigator) : HomeViewModel.InvoiceItemViewModel {
        override fun onItemTapped() = navigator.navigateTo(InvoicesViewModel.NavigationArgs())
    }

    private fun updateAccounts() {
        testNetKinEnvironment.allAccountIds()
            .then { accountIds ->
                updateState { previousState ->
                    previousState.copy(
                        /*mainNetWallets = listOf(AddMainNetWalletItemViewModel()) , */
                        testNetWallets = accountIds.mapIndexed { index, accountId ->
                            WalletItemViewModel(index, accountId.encodeAsString(), navigator)
                        } + AddTestNetWalletItemViewModel()
                    )
                }
            }
    }

    init {
        testNetKinEnvironment.allAccountIds().then {
            if (it.count() < 1) {
                createTestNetAccount()
            }
            updateAccounts()
        }
    }

    private fun createTestNetAccount() {
        val context = KinAccountContext.Builder(testNetKinEnvironment)
            .createNewAccount()
            .build()

        context.getAccount()
            .flatMap { context.service.testService.fundAccount(it.id) }
            .then {
                updateAccounts()
            }

        updateAccounts()
    }

    override fun onCreateWalletTapped() = createTestNetAccount()

    override fun getDefaultState(): HomeViewModel.State =
        HomeViewModel.State(args.resolverType, emptyList(), emptyList(), emptyList())

    override fun onUseBaseTapped() =
        navigator.navigateTo(HomeViewModel.NavigationArgs(HomeViewModel.NavigationArgs.ResolverType.Modern))

    override fun onUseBaseCompatTapped() =
        navigator.navigateTo(HomeViewModel.NavigationArgs(HomeViewModel.NavigationArgs.ResolverType.Compat))

    override fun pokeWalletUpdate() {
        testNetKinEnvironment.allAccountIds().then {
            if (it.count() < 1) {
                createTestNetAccount()
            }
            updateAccounts()
        }
    }
}
