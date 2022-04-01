A client implementation in Kotlin for [Netlify's GoTrue API](https://github.com/netlify/gotrue).  
  
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)  
[![Build](https://github.com/Revxrsal/gotrue-kt/actions/workflows/gradle.yml/badge.svg)](https://github.com/Revxrsal/Lamp/actions/workflows/gradle.yml)  
[![](https://jitpack.io/v/Revxrsal/gotrue-kt.svg)](https://jitpack.io/#Revxrsal/gotrue-kt)  
  
# Features
- **⚡Fast and powerful**: The client was built mainly on two popular, reliable, well-known and performant frameworks: [Moshi](https://github.com/square/moshi) for JSON and [OkHttp](https://github.com/square/okhttp) for sending and receiving requests. They work together well and perform on high standards.
- **🔑 Type safe**: The client uses data types for sending requests and receiving responses. This makes it easier to deal with data and reduces the chance of bugs that occur due to human mistake.
- **🧵 Thread safe**:  Everything in the client is immutable and stateless, making it feasible for multi-threaded applications or background services (such as mobile apps).
- **📋 Built-in persisting session**: It is possible to supply a custom storage type to tell the client to persist user sessions, with optionally supplying an expiration for it.

# Example
```kotlin
val client = GoTrueClient(
    url = "https://hello123.supabase.co/auth/v1",
    authorization = "eyJhbGciOiJIUzI1Ni...",
    storage = MemoryStorage()
)
client.signUp(email = "bruce.wayne@gotham.com", password = "...")
client.signUp(phone = "+123456789", password = "...")
client.on(AuthChangeEvent.SIGNED_IN) { session ->
    if (session != null) {
        println("User signed in: ${session.user.id}")
    }
}
client.on(AuthChangeEvent.SIGNED_OUT) {
    println("Client logged out!")
}
```

## Todo
- [x] Wrap the whole GoTrue client
- [x] Persist sessions
- [x] Exceptions
- [ ] More thorough testing
- [ ] Polish out any bugs
- [ ] Support Android platforms natively by providing `Context`  auth storage implementation