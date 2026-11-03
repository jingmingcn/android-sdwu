package edu.sdwu.teaching.ui

import edu.sdwu.teaching.data.repository.toPublishDto
import edu.sdwu.teaching.domain.model.SessionState
import edu.sdwu.teaching.domain.repository.CatalogModule
import edu.sdwu.teaching.framework.ModuleSource
import edu.sdwu.teaching.modules.studytimer.StudyTimerModule
import edu.sdwu.teaching.testing.FakeAuthRepository
import edu.sdwu.teaching.testing.FakeLessonRepository
import edu.sdwu.teaching.testing.FakeModuleCatalogRepository
import edu.sdwu.teaching.testing.FakeProgressRepository
import edu.sdwu.teaching.testing.MainDispatcherRule
import edu.sdwu.teaching.testing.testModuleRegistry
import edu.sdwu.teaching.ui.home.HomeViewModel
import edu.sdwu.teaching.ui.modules.CatalogState
import edu.sdwu.teaching.ui.modules.ModuleDetailViewModel
import edu.sdwu.teaching.ui.modules.ModulesViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

private fun <T> TestScope.observe(flow: StateFlow<T>): StateFlow<T> {
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { flow.collect {} }
    return flow
}

class ModulesViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val registry = testModuleRegistry()
    private val catalog = FakeModuleCatalogRepository()

    @Test
    fun `installed modules and the class catalog are shown`() = runTest {
        registry.register(StudyTimerModule(), ModuleSource.BUILT_IN)
        catalog.fetchResult = Result.success(listOf(CatalogModule("calc", "Calc", "", "Bob", "1.0.0", "UTILITY", emptyList(), "Bob")))
        val state = observe(ModulesViewModel(registry, catalog).uiState)

        assertEquals(listOf("study-timer"), state.value.installed.map { it.info.id })
        assertEquals("calc", (state.value.catalog as CatalogState.Loaded).modules.single().id)
    }

    @Test
    fun `catalog errors do not hide installed modules`() = runTest {
        registry.register(StudyTimerModule())
        catalog.fetchResult = Result.failure(IOException())
        val state = observe(ModulesViewModel(registry, catalog).uiState)
        assertTrue(state.value.catalog is CatalogState.Error)
        assertEquals(1, state.value.installed.size)
    }

    @Test
    fun `only signed-in users can share a module`() = runTest {
        registry.register(StudyTimerModule())
        val auth = FakeAuthRepository(SessionState.Guest)
        val vm = ModuleDetailViewModel("study-timer", registry, catalog, auth)
        val state = observe(vm.uiState)
        assertFalse(state.value.canPublish)

        auth.session.value = SessionState.SignedIn(edu.sdwu.teaching.testing.DemoUser, "token")
        assertTrue(state.value.canPublish)
        vm.publish()
        assertEquals(listOf("study-timer"), catalog.published)
        assertEquals("Shared with your class", state.value.message)
    }

    @Test
    fun `home shows tools of enabled modules only`() = runTest {
        registry.register(StudyTimerModule())
        val home = HomeViewModel(FakeLessonRepository(), FakeProgressRepository(), FakeAuthRepository(), registry)
        val state = observe(home.uiState)
        assertEquals(listOf("study-timer-widget"), state.value.tools.map { it.item.id })

        registry.setEnabled("study-timer", false)
        assertTrue(state.value.tools.isEmpty())
    }

    @Test
    fun `publish payload lists every contribution with its type`() {
        val dto = StudyTimerModule().toPublishDto()
        assertEquals("study-timer", dto.id)
        assertEquals("STUDY_TOOL", dto.category)
        assertEquals(listOf("function", "tool"), dto.contributions.map { it.type })
    }
}
