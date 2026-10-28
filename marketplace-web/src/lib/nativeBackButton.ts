import { App } from "@capacitor/app";
import { Capacitor } from "@capacitor/core";

/**
 * Android's Back button, inside the Capacitor app only (the website is untouched): close an open
 * dialog first, otherwise go back a page, and leave the app only from the first page.
 */
export function setUpNativeBackButton() {
  if (!Capacitor.isNativePlatform()) return;
  void App.addListener("backButton", ({ canGoBack }) => {
    const dialog = document.querySelector<HTMLDialogElement>("dialog[open]");
    if (dialog) dialog.close();
    else if (canGoBack) window.history.back();
    else void App.exitApp();
  });
}
