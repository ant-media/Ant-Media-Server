package org.red5.server.scope;

import static org.assertj.core.api.Assertions.assertThat;

import io.antmedia.test.UnitTestBase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.red5.server.api.scope.IScope;


@ExtendWith(MockitoExtension.class)
class ScopeListTest extends UnitTestBase<ScopeList> {

    @Mock
    IScope scope1, scope2;

    @Test
    void testScopeAddingAndRemoval() {
        classUnderTest = new ScopeList();

        assertThat(classUnderTest.getScopes()).isEmpty();

        classUnderTest.notifyScopeCreated(scope1);
        classUnderTest.notifyScopeCreated(scope2);

        assertThat(classUnderTest.getScopes()).containsExactly(scope1, scope2);

        classUnderTest.notifyScopeRemoved(scope1);
        assertThat(classUnderTest.getScopes()).containsExactly(scope2);

        classUnderTest.notifyScopeRemoved(scope2);
        assertThat(classUnderTest.getScopes()).isEmpty();
    }
}
