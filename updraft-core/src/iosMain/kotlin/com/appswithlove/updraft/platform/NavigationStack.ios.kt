package com.appswithlove.updraft.platform

import platform.UIKit.UIApplication
import platform.UIKit.UINavigationController
import platform.UIKit.UITabBarController
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow

actual fun currentNavigationStack(): String {
    val root = UIApplication.sharedApplication.windows
        .filterIsInstance<UIWindow>()
        .firstOrNull { it.isKeyWindow() }
        ?.rootViewController ?: return ""
    val names = mutableListOf<String>()
    var pointed: UIViewController? = root
    while (pointed != null) {
        names += shownControllerNames(pointed)
        pointed = pointed.presentedViewController
    }
    return names
        .filterNot { it.startsWith("Updraft") || it.startsWith("Compose") }
        .joinToString(", ")
}

private fun shownControllerNames(viewController: UIViewController): List<String> = when (viewController) {
    is UINavigationController ->
        viewController.viewControllers.filterIsInstance<UIViewController>().map { name(it) }
    is UITabBarController ->
        listOfNotNull(viewController.selectedViewController?.let { name(it) })
    else -> listOf(name(viewController))
}

private fun name(viewController: UIViewController): String =
    viewController::class.simpleName ?: "UIViewController"
