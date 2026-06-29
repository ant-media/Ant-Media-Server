package io.antmedia.console.rest;

import io.antmedia.console.datastore.AbstractConsoleDataStore;
import io.antmedia.console.security.PasswordService;
import io.antmedia.datastore.db.types.User;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CommonRestServiceTest {

    @Mock
    User user;

    @Mock
    PasswordService.PasswordVerificationResult passwordVerificationResult;

    @Mock
    AbstractConsoleDataStore dataStore;

    @Captor
    ArgumentCaptor<String> passwordCaptor;

    CommonRestService classUnderTest;

    @Before
    public void setup() {
        classUnderTest = new CommonRestService();
        classUnderTest.dataStore = dataStore;
    }

    @Test
    public void testLegacyPasswordUpgradePath() {
        when(passwordVerificationResult.isNeedsRehash()).thenReturn(true);
        when(passwordVerificationResult.isVerified()).thenReturn(true);

        classUnderTest.upgradePasswordIfNeeded(user, "oldPassword", passwordVerificationResult);

        verify(user).setPassword(passwordCaptor.capture());
        verify(dataStore).editUser(user);
        assertTrue("The re-hashed password should be using Argon2", passwordCaptor.getValue().startsWith("$argon2id$"));
    }

    @Test
    public void testThatArgon2PasswordsAreNotHashedAgain() {
        when(passwordVerificationResult.isNeedsRehash()).thenReturn(false);
        when(passwordVerificationResult.isVerified()).thenReturn(true);

        classUnderTest.upgradePasswordIfNeeded(user, "oldPassword", passwordVerificationResult);

        verify(user, never()).setPassword(passwordCaptor.capture());
        verify(dataStore, never()).editUser(user);
    }

}