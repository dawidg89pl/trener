(() => {
  const storageKey = "trener-ai:scroll-after-post";

  document.addEventListener("submit", (event) => {
    const form = event.target;
    if (!(form instanceof HTMLFormElement) || form.method.toLowerCase() !== "post") return;

    const action = new URL(form.action, window.location.href);
    if (action.pathname === "/logout") {
      sessionStorage.removeItem(storageKey);
      return;
    }

    sessionStorage.setItem(storageKey, JSON.stringify({
      x: window.scrollX,
      y: window.scrollY,
      savedAt: Date.now()
    }));
  }, true);

  const savedValue = sessionStorage.getItem(storageKey);
  if (!savedValue) return;

  // Pozycja dotyczy tylko pierwszej strony otwartej po wysłaniu formularza.
  sessionStorage.removeItem(storageKey);

  try {
    const saved = JSON.parse(savedValue);
    if (Date.now() - saved.savedAt > 120000) return;

    if ("scrollRestoration" in history) history.scrollRestoration = "manual";
    requestAnimationFrame(() => requestAnimationFrame(() => {
      window.scrollTo(saved.x, saved.y);
    }));
  } catch {
    // Uszkodzony wpis w sessionStorage nie powinien blokować działania strony.
  }
})();
