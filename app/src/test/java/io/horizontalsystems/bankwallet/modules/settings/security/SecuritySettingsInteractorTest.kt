package io.horizontalsystems.bankwallet.modules.settings.security

import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.ILockManager
import io.horizontalsystems.bankwallet.core.ISystemInfoManager
import io.horizontalsystems.bankwallet.core.managers.AuthManager
import io.horizontalsystems.bankwallet.entities.BiometryType
import io.horizontalsystems.bankwallet.modules.RxBaseTest
import io.reactivex.Flowable
import io.reactivex.subjects.PublishSubject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class SecuritySettingsInteractorTest {

    private val delegate = mock(SecuritySettingsModule.ISecuritySettingsInteractorDelegate::class.java)
    private val authManager = mock(AuthManager::class.java)

    private lateinit var interactor: SecuritySettingsInteractor
    private lateinit var localStorage: ILocalStorage
    private lateinit var accountManager: IAccountManager
    private lateinit var systemInfoManager: ISystemInfoManager
    private lateinit var lockManager: ILockManager

    private val backedUpSignal = Flowable.empty<Int>()

    @Before
    fun setup() {
        RxBaseTest.setup()

        accountManager = mock {
            on { nonBackedUpCount } doReturn 0
            on { nonBackedUpCountFlowable } doReturn backedUpSignal
        }

        localStorage = mock {
            on { isBiometricOn } doReturn true
        }

        systemInfoManager = mock {
            on { biometryType } doReturn BiometryType.FINGER
        }

        lockManager = mock {
            on { isLocked } doReturn false
        }

        interactor = SecuritySettingsInteractor(authManager, accountManager, localStorage, systemInfoManager, lockManager)
        interactor.delegate = delegate
    }

    @After
    fun teardown() {
        verifyNoMoreInteractions(delegate)
    }

    @Test
    fun getBiometryType() {
        assertEquals(BiometryType.FINGER, interactor.biometryType)
    }

    @Test
    fun isBackedUp() {
        assertTrue(interactor.nonBackedUpCount == 0)
    }

    @Test
    fun getBiometricUnlockOn() {
        assertTrue(interactor.getBiometricUnlockOn())
    }

    @Test
    fun isNotBackedUp() {
        accountManager = mock {
            on { nonBackedUpCount } doReturn 1
            on { nonBackedUpCountFlowable } doReturn backedUpSignal
        }
        interactor = SecuritySettingsInteractor(authManager, accountManager, localStorage, systemInfoManager, lockManager)
        interactor.delegate = delegate

        assertFalse(interactor.nonBackedUpCount == 0)
    }

    @Test
    fun getBiometricUnlockOff() {
        localStorage = mock {
            on { isBiometricOn } doReturn false
        }
        interactor = SecuritySettingsInteractor(authManager, accountManager, localStorage, systemInfoManager, lockManager)
        interactor.delegate = delegate

        assertFalse(interactor.getBiometricUnlockOn())
    }

    @Test
    fun setBiometricUnlockOn() {
        interactor.setBiometricUnlockOn(false)
        verify(localStorage).isBiometricOn = false
    }

    @Test
    fun unlinkWallet() {
        interactor.unlinkWallet()

        verify(authManager).logout()
        verify(delegate).didUnlinkWallet()
    }

    @Test
    fun didTapOnBackupWallet() {
        val lockSubject = PublishSubject.create<Unit>()

        lockManager = mock {
            on { lockStateUpdatedSignal } doReturn lockSubject
            on { isLocked } doReturn false
        }

        interactor = SecuritySettingsInteractor(authManager, accountManager, localStorage, systemInfoManager, lockManager)
        interactor.delegate = delegate


        interactor.didTapOnBackupWallet()
        verify(delegate).accessIsRestricted()

        lockSubject.onNext(Unit)
        verify(delegate).openBackupWallet()
    }
}
