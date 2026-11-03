# Code Snippet Viewer

Module `code-snippets` by Example Student for the Android Teaching Framework.

Searchable Kotlin/Compose snippets with syntax highlighting, copy-to-clipboard, favourites (module storage) and a "Snippet of the day" Home widget.

| | |
|---|---|
| Code | `src/main/java/edu/sdwu/teaching/modules/codesnippets/CodeSnippetsModule.kt` |
| Discovery file | `src/main/resources/META-INF/services/edu.sdwu.teaching.framework.ModuleProvider` |
| Tests | `./gradlew :modules:code-snippets:testDebugUnitTest` |

## Checklist

- [ ] Description in `ModuleInfo` says what the module does
- [ ] Screen works in the @Preview and in the app (Modules tab → Code Snippet Viewer)
- [ ] Logic lives in plain functions with unit tests
- [ ] Module can be switched off and on in the Modules tab without problems
- [ ] Shared with the class (Modules → Code Snippet Viewer → Share with class)
