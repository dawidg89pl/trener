(() => {
  const storageKey = "trener-ai-theme";
  const preferredTheme = () => {
    let saved = null;
    try { saved = localStorage.getItem(storageKey); } catch (_error) { /* Tryb prywatny może blokować zapis. */ }
    if (saved === "dark" || saved === "light") return saved;
    return window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
  };

  const applyTheme = (theme) => {
    document.documentElement.dataset.theme = theme;
    document.documentElement.style.colorScheme = theme;
    document.querySelectorAll("[data-theme-toggle]").forEach((button) => {
      const dark = theme === "dark";
      button.setAttribute("aria-label", dark ? "Włącz jasny motyw" : "Włącz ciemny motyw");
      button.setAttribute("title", dark ? "Jasny motyw" : "Ciemny motyw");
      button.querySelector(".theme-icon-sun")?.setAttribute("aria-hidden", String(!dark));
      button.querySelector(".theme-icon-moon")?.setAttribute("aria-hidden", String(dark));
    });
  };

  applyTheme(preferredTheme());
  document.addEventListener("DOMContentLoaded", () => {
    applyTheme(preferredTheme());
    document.querySelectorAll("[data-theme-toggle]").forEach((button) => {
      button.addEventListener("click", () => {
        const next = document.documentElement.dataset.theme === "dark" ? "light" : "dark";
        try { localStorage.setItem(storageKey, next); } catch (_error) { /* Motyw nadal działa w bieżącej karcie. */ }
        applyTheme(next);
      });
    });
  });
})();
