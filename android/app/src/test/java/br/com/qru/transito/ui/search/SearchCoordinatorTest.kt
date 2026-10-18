package br.com.qru.transito.ui.search

import br.com.qru.transito.domain.model.SearchResult
import kotlinx.coroutines.*
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import org.junit.Assert.*
import org.junit.Test

class SearchCoordinatorTest {
    private fun result(key: String) = SearchResult(key, key, null, null, "BR", "test")
    private suspend fun scenario(block: suspend (CoroutineScope) -> Unit) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try { block(scope) } finally { scope.cancel() }
    }

    @Test fun typingSearchesOnlyTheLastQueryAfterDebounce() = runBlocking {
        scenario { scope ->
            val gate = CompletableDeferred<Unit>()
            val calls = mutableListOf<String>()
            val engine = SearchCoordinator(scope, { calls.add(it); listOf(result(it)) }, { gate.await() })
            engine.query("p"); engine.query("pn"); engine.query("pneu")
            assertTrue(calls.isEmpty())
            assertTrue(engine.state.value.loading)
            gate.complete(Unit)
            assertEquals(listOf("pneu"), calls)
            assertEquals("pneu", engine.state.value.results.single().stableKey)
            assertFalse(engine.state.value.loading)
        }
    }
    @Test fun manualSearchDoesNotWaitForDebounce() = runBlocking {
        scenario { scope ->
            val gate = CompletableDeferred<Unit>()
            val calls = mutableListOf<String>()
            val engine = SearchCoordinator(scope, { calls.add(it); listOf(result(it)) }, { gate.await() })
            engine.query("  pneu  "); engine.run()
            assertEquals(listOf("pneu"), calls)
            assertEquals("  pneu  ", engine.state.value.query)
        }
    }
    @Test fun lateNonCooperativeResultCannotReplaceLatestQuery() = runBlocking {
        scenario { scope ->
            lateinit var old: Continuation<List<SearchResult>>
            val engine = SearchCoordinator(scope, { query ->
                if (query=="antiga") suspendCoroutine { old=it } else listOf(result(query))
            }, {})
            engine.query("antiga"); engine.query("nova")
            old.resume(listOf(result("antiga")))
            assertEquals("nova", engine.state.value.query)
            assertEquals("nova", engine.state.value.results.single().stableKey)
        }
    }
    @Test fun lateErrorCannotReplaceCurrentSuccess() = runBlocking {
        scenario { scope ->
            lateinit var old: Continuation<List<SearchResult>>
            val engine = SearchCoordinator(scope, { query ->
                if (query=="antiga") suspendCoroutine { old=it } else listOf(result(query))
            }, {})
            engine.query("antiga"); engine.query("nova")
            old.resumeWithException(IllegalStateException("old request"))
            assertEquals("nova", engine.state.value.results.single().stableKey)
            assertFalse(engine.state.value.message.contains("Não foi possível"))
        }
    }
    @Test fun clearingFieldRejectsLateResultsAndStopsLoading() = runBlocking {
        scenario { scope ->
            lateinit var old: Continuation<List<SearchResult>>
            val engine = SearchCoordinator(scope, { suspendCoroutine { old=it } }, {})
            engine.query("pneu"); engine.query("")
            old.resume(listOf(result("pneu")))
            assertTrue(engine.state.value.results.isEmpty())
            assertFalse(engine.state.value.loading)
            assertEquals("", engine.state.value.query)
        }
    }
    @Test fun currentFailureCanBeRetriedImmediately() = runBlocking {
        scenario { scope ->
            var fails = true
            val engine = SearchCoordinator(scope, {
                if (fails) error("current request") else listOf(result(it))
            }, {})
            engine.query("pneu")
            assertFalse(engine.state.value.loading)
            assertTrue(engine.state.value.message.contains("Não foi possível"))
            fails=false; engine.run()
            assertEquals("pneu", engine.state.value.results.single().stableKey)
        }
    }
    @Test fun newQueryClearsOldResultsAndOldMessageWhileWaiting() = runBlocking {
        scenario { scope ->
            val gate = CompletableDeferred<Unit>()
            val engine = SearchCoordinator(scope, { emptyList() }, { gate.await() })
            engine.query("inexistente"); engine.run()
            assertTrue(engine.state.value.message.contains("Nenhum conteúdo"))
            engine.query("pneu")
            assertEquals("", engine.state.value.message)
            assertTrue(engine.state.value.loading)
            assertTrue(engine.state.value.results.isEmpty())
        }
    }
}
