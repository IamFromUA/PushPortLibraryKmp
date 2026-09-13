package consumer

import dev.pushport.kmp.createApplePushPort

/** Forces the final application linker to resolve the native Objective-C bridge. */
fun nativeStatus(): String = createApplePushPort().status().configured.toString()
